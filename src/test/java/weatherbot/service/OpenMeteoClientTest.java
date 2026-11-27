package weatherbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenMeteoClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String text) throws IOException {
        return MAPPER.readTree(text);
    }

    private final OpenMeteoClient client = new OpenMeteoClient();

    // ---------------------------------------------------------------- parseCurrent

    @Nested
    @DisplayName("parseCurrent: разбор текущей погоды")
    class ParseCurrent {

        private static String current(double temp, double feels, double wind, double humidity, int code) {
            return """
                    {"current": {"temperature_2m": %s, "apparent_temperature": %s,
                                 "wind_speed_10m": %s, "relative_humidity_2m": %s, "weather_code": %d}}
                    """.formatted(temp, feels, wind, humidity, code);
        }

        @Test
        @DisplayName("Собирает строку из всех полей, числа округляются")
        void formatsAllFields() throws IOException {
            String result = client.parseCurrent(json(current(5.4, 1.6, 3.6, 81.0, 61)));
            assertEquals("дождь, +5°C (ощущается как +2°C), ветер 4 м/с, влажность 81%.", result);
        }

        @Test
        @DisplayName("Отрицательная температура выводится с минусом")
        void negativeTemperature() throws IOException {
            String result = client.parseCurrent(json(current(-3.4, -7.6, 2.0, 90.0, 71)));
            assertEquals("снег, -3°C (ощущается как -8°C), ветер 2 м/с, влажность 90%.", result);
        }

        @Test
        @DisplayName("Ноль градусов выводится как +0°C")
        void zeroTemperature() throws IOException {
            String result = client.parseCurrent(json(current(0.2, 0.0, 0.0, 50.0, 0)));
            assertEquals("ясно, +0°C (ощущается как +0°C), ветер 0 м/с, влажность 50%.", result);
        }

        @ParameterizedTest(name = "код {0} -> {1}")
        @DisplayName("Код погоды превращается в описание")
        @CsvSource({
                "0,  ясно",
                "1,  переменная облачность",
                "2,  переменная облачность",
                "3,  пасмурно",
                "45, туман",
                "48, туман",
                "51, морось",
                "57, морось",
                "61, дождь",
                "67, дождь",
                "71, снег",
                "77, снег",
                "80, ливень",
                "82, ливень",
                "85, снегопад",
                "86, снегопад",
                "95, гроза",
                "99, гроза",
                "4,  облачно",
                "100, облачно"
        })
        void weatherCodeDescription(int code, String description) throws IOException {
            String result = client.parseCurrent(json(current(10, 10, 1, 50, code)));
            assertTrue(result.startsWith(description + ", "), result);
        }

        @Test
        @DisplayName("null и ответ без блока current дают null")
        void missingDataReturnsNull() throws IOException {
            assertNull(client.parseCurrent(null));
            assertNull(client.parseCurrent(json("{}")));
            assertNull(client.parseCurrent(json("{\"error\": true, \"reason\": \"oops\"}")));
        }
    }

    // ---------------------------------------------------------------- parseTomorrow

    @Nested
    @DisplayName("parseTomorrow: разбор прогноза на завтра")
    class ParseTomorrow {

        private static final String DAILY = """
                {"daily": {
                    "time": ["2026-11-25", "2026-11-26"],
                    "weather_code": [0, 71],
                    "temperature_2m_max": [3.0, -1.2],
                    "temperature_2m_min": [0.0, -6.4],
                    "wind_speed_10m_max": [5.0, 9.4],
                    "precipitation_probability_max": [10, 64]
                }}
                """;

        @Test
        @DisplayName("Берёт данные из элементов с индексом 1 (завтра), а не сегодняшние")
        void usesTomorrowValues() throws IOException {
            assertEquals("снег, от -6°C до -1°C, ветер до 9 м/с, вероятность осадков 64%.",
                    client.parseTomorrow(json(DAILY)));
        }

        @Test
        @DisplayName("Если в daily меньше двух дней, возвращается null")
        void notEnoughDays() throws IOException {
            assertNull(client.parseTomorrow(json("{\"daily\": {\"time\": [\"2026-11-25\"]}}")));
            assertNull(client.parseTomorrow(json("{\"daily\": {\"time\": []}}")));
        }

        @Test
        @DisplayName("null и ответ без блока daily дают null")
        void missingDataReturnsNull() throws IOException {
            assertNull(client.parseTomorrow(null));
            assertNull(client.parseTomorrow(json("{}")));
        }
    }

    // ---------------------------------------------------------------- fetchWeather

    @Nested
    @DisplayName("fetchWeather: запросы к API (HttpClient подменён)")
    class FetchWeather {

        private static final String GEO_OK =
                "{\"results\": [{\"latitude\": 58.01046, \"longitude\": 56.25017}]}";
        private static final String FORECAST_OK = """
                {"current": {"temperature_2m": 5.0, "apparent_temperature": 2.0,
                             "wind_speed_10m": 4.0, "relative_humidity_2m": 81.0, "weather_code": 61}}
                """;
        private static final String EXPECTED_WEATHER =
                "дождь, +5°C (ощущается как +2°C), ветер 4 м/с, влажность 81%.";

        @Test
        @DisplayName("Успешный сценарий: геокодинг, затем прогноз")
        void successfulFlow() {
            FakeHttpClient http = new FakeHttpClient().respond(200, GEO_OK).respond(200, FORECAST_OK);

            assertEquals(EXPECTED_WEATHER, new OpenMeteoClient(http).fetchWeather("пермь"));
            assertEquals(2, http.requests.size());
        }

        @Test
        @DisplayName("Название города кодируется в URL геокодинга")
        void cityIsUrlEncoded() {
            FakeHttpClient http = new FakeHttpClient().respond(200, GEO_OK).respond(200, FORECAST_OK);

            new OpenMeteoClient(http).fetchWeather("пермь");

            String encoded = URLEncoder.encode("пермь", StandardCharsets.UTF_8);
            URI geoUri = http.requests.get(0);
            assertEquals("geocoding-api.open-meteo.com", geoUri.getHost());
            assertTrue(geoUri.getRawQuery().contains("name=" + encoded), geoUri.toString());
        }

        @Test
        @DisplayName("Координаты из геокодинга попадают в запрос прогноза с точкой в числах")
        void coordinatesArePassedToForecast() {
            FakeHttpClient http = new FakeHttpClient().respond(200, GEO_OK).respond(200, FORECAST_OK);

            new OpenMeteoClient(http).fetchWeather("пермь");

            URI forecastUri = http.requests.get(1);
            assertEquals("api.open-meteo.com", forecastUri.getHost());
            String query = forecastUri.getRawQuery();
            assertTrue(query.contains("latitude=58.0105"), query);
            assertTrue(query.contains("longitude=56.2502"), query);
            assertTrue(query.contains("current=temperature_2m"), query);
        }

        @Test
        @DisplayName("Город не найден (нет results или он пустой): null, прогноз не запрашивается")
        void cityNotFound() {
            FakeHttpClient noResults = new FakeHttpClient().respond(200, "{}");
            assertNull(new OpenMeteoClient(noResults).fetchWeather("abracadabra"));
            assertEquals(1, noResults.requests.size());

            FakeHttpClient emptyResults = new FakeHttpClient().respond(200, "{\"results\": []}");
            assertNull(new OpenMeteoClient(emptyResults).fetchWeather("abracadabra"));
            assertEquals(1, emptyResults.requests.size());
        }

        @Test
        @DisplayName("Статус не 200 у геокодинга: null без повторов")
        void geocodingBadStatus() {
            FakeHttpClient http = new FakeHttpClient().respond(500, "");

            assertNull(new OpenMeteoClient(http).fetchWeather("пермь"));
            assertEquals(1, http.requests.size());
        }

        @Test
        @DisplayName("Статус не 200 у прогноза: null")
        void forecastBadStatus() {
            FakeHttpClient http = new FakeHttpClient().respond(200, GEO_OK).respond(503, "");

            assertNull(new OpenMeteoClient(http).fetchWeather("пермь"));
            assertEquals(2, http.requests.size());
        }

        @Test
        @DisplayName("Прогноз без блока current: null")
        void forecastWithoutCurrent() {
            FakeHttpClient http = new FakeHttpClient().respond(200, GEO_OK).respond(200, "{}");

            assertNull(new OpenMeteoClient(http).fetchWeather("пермь"));
        }

        @Test
        @DisplayName("Сетевой сбой повторяется, и если потом всё хорошо, результат есть")
        void networkErrorIsRetried() {
            FakeHttpClient http = new FakeHttpClient()
                    .fail(new IOException("timeout"))
                    .respond(200, GEO_OK)
                    .respond(200, FORECAST_OK);

            assertEquals(EXPECTED_WEATHER, new OpenMeteoClient(http).fetchWeather("пермь"));
            assertEquals(3, http.requests.size());
        }

        @Test
        @DisplayName("После трёх неудачных попыток возвращается null")
        void givesUpAfterThreeAttempts() {
            FakeHttpClient http = new FakeHttpClient()
                    .fail(new IOException("1"))
                    .fail(new IOException("2"))
                    .fail(new IOException("3"));

            assertNull(new OpenMeteoClient(http).fetchWeather("пермь"));
            assertEquals(3, http.requests.size());
        }

        @Test
        @DisplayName("Некорректный JSON в ответе: null, а не исключение")
        void malformedJson() {
            FakeHttpClient http = new FakeHttpClient()
                    .respond(200, "это не json")
                    .respond(200, "это не json")
                    .respond(200, "это не json");

            assertNull(new OpenMeteoClient(http).fetchWeather("пермь"));
        }
    }

    // ---------------------------------------------------------------- заглушки

    /** HttpClient, который отдаёт заранее заданные ответы по очереди и запоминает запросы. */
    private static class FakeHttpClient extends HttpClient {
        private final Deque<Object> script = new ArrayDeque<>();
        final List<URI> requests = new ArrayList<>();

        FakeHttpClient respond(int status, String body) {
            script.add(new FakeResponse(status, body));
            return this;
        }

        FakeHttpClient fail(IOException error) {
            script.add(error);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException {
            requests.add(request.uri());
            Object next = script.poll();
            if (next == null) {
                throw new AssertionError("Лишний запрос: " + request.uri());
            }
            if (next instanceof IOException e) {
                throw e;
            }
            FakeResponse response = (FakeResponse) next;
            response.uri = request.uri();
            return (HttpResponse<T>) (HttpResponse<?>) response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> handler,
                                                                HttpResponse.PushPromiseHandler<T> pushHandler) {
            throw new UnsupportedOperationException();
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
    }

    private static class FakeResponse implements HttpResponse<String> {
        private final int status;
        private final String body;
        URI uri;

        FakeResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        @Override public int statusCode() { return status; }
        @Override public String body() { return body; }
        @Override public URI uri() { return uri; }
        @Override public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
