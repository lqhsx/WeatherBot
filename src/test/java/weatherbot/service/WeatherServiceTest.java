package weatherbot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherServiceTest {

    /** Сервис, у которого клиент всегда возвращает заданный ответ. */
    private static WeatherService serviceWith(String weatherData) {
        return new WeatherService(city -> weatherData);
    }

    @Test
    @DisplayName("Прогноз = «В <город в предложном падеже> » + ответ клиента")
    void forecastCombinesCityAndClientData() {
        assertEquals("В Москве ясно, +12°C.", serviceWith("ясно, +12°C.").getForecast("москва"));
    }

    @ParameterizedTest(name = "{0} -> В {1}")
    @DisplayName("Название города склоняется в предложный падеж")
    @CsvSource({
            "москва,                Москве",
            "пермь,                 Перми",
            "казань,                Казани",
            "екатеринбург,          Екатеринбурге",
            "челябинск,             Челябинске",
            "санкт-петербург,       Санкт-Петербурге",
            "сочи,                  Сочи",
            "тольятти,              Тольятти",
            "иваново,               Иваново",
            "хабаровск,             Хабаровске",
            "нижний,                Нижнем",
            "новороссийск,          Новороссийске"
    })
    void cityIsPutIntoPrepositionalCase(String city, String expectedForm) {
        assertEquals("В " + expectedForm + " ок", serviceWith("ок").getForecast(city));
    }

    @Test
    @DisplayName("Регистр букв и пробелы по краям не важны")
    void caseAndSpacesAreIgnored() {
        WeatherService service = serviceWith("ок");
        String expected = service.getForecast("москва");

        assertEquals(expected, service.getForecast("МОСКВА"));
        assertEquals(expected, service.getForecast("Москва"));
        assertEquals(expected, service.getForecast("  Москва  "));
    }

    @Test
    @DisplayName("Клиенту передаётся нормализованное название: нижний регистр, без пробелов")
    void clientReceivesNormalizedCity() {
        AtomicReference<String> received = new AtomicReference<>();
        WeatherService service = new WeatherService(city -> {
            received.set(city);
            return "ок";
        });

        service.getForecast("  МоСкВа ");

        assertEquals("москва", received.get());
    }

    @Test
    @DisplayName("Каждая часть названия с дефисом пишется с заглавной буквы")
    void hyphenatedCityPartsAreCapitalized() {
        String forecast = serviceWith("ок").getForecast("санкт-петербург");
        assertTrue(forecast.startsWith("В Санкт-Петербурге "), forecast);
    }

    @Test
    @DisplayName("Если клиент вернул null, пользователь получает сообщение об ошибке с названием города")
    void unknownCityReturnsErrorMessage() {
        assertEquals("Не удалось получить прогноз для города Омск. Проверь название или попробуй позже.",
                serviceWith(null).getForecast("Омск"));
    }

    @Test
    @DisplayName("Пустое название и одни пробелы дают сообщение, клиент при этом не вызывается")
    void emptyCityDoesNotCallClient() {
        AtomicInteger calls = new AtomicInteger();
        WeatherService service = new WeatherService(city -> {
            calls.incrementAndGet();
            return "ок";
        });

        assertEquals("Название города не может быть пустым.", service.getForecast(""));
        assertEquals("Название города не может быть пустым.", service.getForecast("   "));
        assertEquals(0, calls.get());
    }
}
