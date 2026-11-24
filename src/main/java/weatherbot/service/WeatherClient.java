package weatherbot.service;

public interface WeatherClient {
    /**
     * @return описание погоды (например, "ясно, +12°C.") или null, если город не найден
     */
    String fetchWeather(String normalizedCity);
}