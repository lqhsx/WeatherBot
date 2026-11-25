package weatherbot.service;

/**
 * Формирует ответ с прогнозом: получает данные у WeatherClient
 * и оформляет их (название города в предложном падеже).
 */
public class WeatherService {
    private final WeatherClient client;

    public WeatherService(WeatherClient client) {
        this.client = client;
    }

    public String getForecast(String city) {
        String normalizedCity = city.toLowerCase().trim();
        if (normalizedCity.isEmpty()) {
            return "Название города не может быть пустым.";
        }

        String weatherData = client.fetchWeather(normalizedCity);

        if (weatherData == null) {
            return "Не удалось получить прогноз для города " + city
                    + ". Проверь название или попробуй позже.";
        }

        return "В " + toPrepostionalCase(normalizedCity) + " " + weatherData;
    }

    private String toPrepostionalCase(String city) {
        String formattedcity = capitalizeHyphenated(city);
        /*
          Проверяет, что город оканчивается на о, е, и, у или ю
           '.*' говорит о том, что в начале слова может быть любое количество символов
           '[оеиую]$' говорит о том, что одна из букв из [] стоит в конце слова
         */
        if (formattedcity.matches(".*[оеиую]$")) {
            return formattedcity;
        }

        if (formattedcity.endsWith("ий") || formattedcity.endsWith("ый")) {
            return formattedcity.substring(0, formattedcity.length() - 2) + "ем";
        }

        if (formattedcity.endsWith("ь") || formattedcity.endsWith("я")) {
            return formattedcity.substring(0, formattedcity.length() - 1) + "и";
        }

        if (formattedcity.endsWith("а")) {
            return formattedcity.substring(0, formattedcity.length() - 1) + "е";
        }
        return formattedcity + "е";
    }

    private String capitalizeHyphenated(String str) {
        String[] part = str.split("-");
        for (int i = 0; i < part.length; i++) {
            if (!part[i].isEmpty()) {
                part[i] = part[i].substring(0, 1).toUpperCase() + part[i].substring(1);
            }
        }
        return String.join("-", part);
    }
}