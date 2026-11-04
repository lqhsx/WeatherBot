package weatherbot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherServiceTest {
    private final WeatherService service = new WeatherService();

    @Test
    @DisplayName("Для известного города возвращается его прогноз")
    void knownCityReturnsForecast() {
        assertEquals("В Перми сегодня дождь, целый день идет дождь, +5°C.",
                service.getForecast("пермь"));
    }

    @Test
    @DisplayName("Регистр букв в названии города не важен")
    void cityCaseIsIgnored() {
        String expected = service.getForecast("москва");
        assertEquals(expected, service.getForecast("МОСКВА"));
        assertEquals(expected, service.getForecast("Москва"));
    }

    @Test
    @DisplayName("Пробелы по краям названия города не важны")
    void citySpacesAreIgnored() {
        assertEquals(service.getForecast("екатеринбург"), service.getForecast("  Екатеринбург  "));
    }

    @Test
    @DisplayName("Города с дефисом распознаются")
    void hyphenatedCitiesAreRecognized() {
        assertEquals("В Питере как всегда: ветер, дожди, берите зонт, +13°C.",
                service.getForecast("Санкт-Петербург"));
        assertTrue(service.getForecast("Найт-Сити").startsWith("Доброе утро, Найт-Сити!"));
    }

    @Test
    @DisplayName("У каждого города из базы свой прогноз")
    void everyKnownCityHasOwnForecast() {
        assertTrue(service.getForecast("челябинск").contains("Челябинске"));
        assertTrue(service.getForecast("екатеринбург").contains("Екатеринбурге"));
        assertTrue(service.getForecast("москва").contains("Москве"));
    }

    @Test
    @DisplayName("Для неизвестного города возвращается сообщение-заглушка с его названием")
    void unknownCityReturnsDefaultMessage() {
        assertEquals("Прогноз для города Омск пока не добавлен в мою базу, но там наверняка отличная погода!",
                service.getForecast("Омск"));
    }
}
