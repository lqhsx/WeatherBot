package weatherbot.service;

/**
 * Источник прогнозов погоды. Сейчас прогнозы заданы прямо в коде.
 */
public class WeatherService {
    public String getForecast(String city) {
        String normalizedCity = city.toLowerCase().trim();
        String weatherData = switch (normalizedCity) {
            case "пермь" -> "сегодня дождь, целый день идет дождь, +5°C.";
            case "москва" -> "сегодня облачно, возможен небольшой дождь, +15°C.";
            case "екатеринбург" -> "прохладно, переменная облачность, +10°C.";
            case "челябинск" -> "ясно, без осадков, +12°C.";
            case "санкт-петербург" -> "как всегда: ветер, дожди, берите зонт, +13°C.";
            default -> null;
        };

        if (weatherData == null) {
            return "Прогноз для города " + city + " пока не добавлен в мою базу, но там наверняка отличная погода!";
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
