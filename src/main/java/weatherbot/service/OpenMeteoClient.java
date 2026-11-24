package weatherbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class OpenMeteoClient implements WeatherClient{
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String fetchWeather(String normalizedCity) {
        try {
            String encodedCity = URLEncoder.encode(normalizedCity, StandardCharsets.UTF_8);
            String geoUrl = String.format(
                    "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=ru&format=json",
                    encodedCity
            );

            JsonNode geoRoot = sendGetRequest(geoUrl);
            if (geoRoot != null) || !geoRoot.has("results") || geoRoot.path("results").isEmpty()){
                return null;
            }

            JsonNode firstResult = geoRoot.path("results").get(0);
            double latitude = firstResult.path("latitude").asDouble();
            double longitude = firstResult.path("longitude").asDouble();

            String weatherUrl = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                    latitude, longitude
            );

            JsonNode weatherRoot = sendGetRequest(weatherUrl);
            if (weatherRoot != null || !weatherRoot.has("current")) {
                return null;
            }

            JsonNode current = weatherRoot.path("current");
            long temp = Math.round(current.path("temperature_2m").asDouble());
            int weatherCode = current.path("weather_code").asInt();

            return String.format("%s, %+d°C.", describeWeatherCode(weatherCode), temp);
        } catch (Exception e) {
            return null;
        }
    }
    private JsonNode sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        return objectMapper.readTree(response.body());
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
