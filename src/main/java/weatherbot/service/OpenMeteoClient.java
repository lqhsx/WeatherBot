package weatherbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;

public class OpenMeteoClient implements WeatherClient {
    private static final String GEO_URL =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=ru&format=json";
    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&%s";

    private static final String CURRENT_PARAMS =
            "current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code"
                    + "&wind_speed_unit=ms&timezone=auto";


    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String fetchWeather(String normalizedCity) {
        return fetch(normalizedCity, CURRENT_PARAMS, this::parseCurrent);
    }

    /** Находит координаты города, запрашивает прогноз с нужными параметрами и разбирает ответ. */
    private String fetch(String city, String params, Function<JsonNode, String> parser) {
        try {
            String geoUrl = String.format(GEO_URL, URLEncoder.encode(city, StandardCharsets.UTF_8));
            JsonNode geoRoot = sendGetRequest(geoUrl);
            if (geoRoot == null || !geoRoot.has("results") || geoRoot.path("results").isEmpty()) {
                return null; // город не найден
            }

            JsonNode first = geoRoot.path("results").get(0);
            double latitude = first.path("latitude").asDouble();
            double longitude = first.path("longitude").asDouble();

            String url = String.format(Locale.US, FORECAST_URL, latitude, longitude, params);
            return parser.apply(sendGetRequest(url));
        } catch (Exception e) {
            System.err.println("Ошибка запроса погоды: " + e);
            return null;
        }
    }

    /** GET-запрос с повтором при сетевых сбоях. Возвращает null, если статус не 200. */
    private JsonNode sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return null;
                }
                return objectMapper.readTree(response.body());
            } catch (IOException e) { // сеть: таймаут, обрыв соединения
                lastError = e;
            }
        }
        throw lastError;
    }

    /** Текущая погода: «дождь, +5°C (ощущается как +2°C), ветер 4 м/с, влажность 81%.» */
    String parseCurrent(JsonNode root) {
        if (root == null || !root.has("current")) {
            return null;
        }
        JsonNode current = root.path("current");
        long temp = Math.round(current.path("temperature_2m").asDouble());
        long feelsLike = Math.round(current.path("apparent_temperature").asDouble());
        long wind = Math.round(current.path("wind_speed_10m").asDouble());
        long humidity = Math.round(current.path("relative_humidity_2m").asDouble());
        int code = current.path("weather_code").asInt();

        return String.format(Locale.ROOT, "%s, %+d°C (ощущается как %+d°C), ветер %d м/с, влажность %d%%.",
                describeWeatherCode(code), temp, feelsLike, wind, humidity);
    }

    /** Погода на завтра (элементы с индексом 1 в массивах daily). */
    String parseTomorrow(JsonNode root) {
        if (root == null || !root.has("daily")) {
            return null;
        }
        JsonNode daily = root.path("daily");
        if (daily.path("time").size() < 2) {
            return null;
        }
        int code = daily.path("weather_code").path(1).asInt();
        long max = Math.round(daily.path("temperature_2m_max").path(1).asDouble());
        long min = Math.round(daily.path("temperature_2m_min").path(1).asDouble());
        long wind = Math.round(daily.path("wind_speed_10m_max").path(1).asDouble());
        long precipitation = Math.round(daily.path("precipitation_probability_max").path(1).asDouble());

        return String.format(Locale.ROOT, "%s, от %+d°C до %+d°C, ветер до %d м/с, вероятность осадков %d%%.",
                describeWeatherCode(code), min, max, wind, precipitation);
    }

    // Стандартные коды погоды из документации Open-Meteo
    private String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "ясно";
            case 1, 2 -> "переменная облачность";
            case 3 -> "пасмурно";
            case 45, 48 -> "туман";
            case 51, 53, 55, 56, 57 -> "морось";
            case 61, 63, 65, 66, 67 -> "дождь";
            case 71, 73, 75, 77 -> "снег";
            case 80, 81, 82 -> "ливень";
            case 85, 86 -> "снегопад";
            case 95, 96, 99 -> "гроза";
            default -> "облачно";
        };
    }
}