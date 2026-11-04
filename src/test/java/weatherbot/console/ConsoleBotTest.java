package weatherbot.console;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weatherbot.bot.WeatherBot;
import weatherbot.service.WeatherService;
import weatherbot.storage.UserBank;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleBotTest {

    /** Запускает консольного бота на заданном вводе и возвращает всё, что он напечатал. */
    private String runWithInput(String... lines) {
        String input = String.join("\n", lines) + (lines.length > 0 ? "\n" : "");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        WeatherBot bot = new WeatherBot(new UserBank(), new WeatherService());
        new ConsoleBot(bot, new Scanner(input), out).run();

        return buffer.toString(StandardCharsets.UTF_8);
    }

    private int countOccurrences(String text, String part) {
        int count = 0;
        int index = text.indexOf(part);
        while (index != -1) {
            count++;
            index = text.indexOf(part, index + part.length());
        }
        return count;
    }

    @Test
    @DisplayName("Полный диалог печатается в правильном порядке")
    void fullDialogIsPrintedInOrder() {
        String output = runWithInput("Аня", "Пермь", "погода", "профиль", "выход");

        String[] expectedInOrder = {
                "Привет! Я консольный погодный бот.",
                "Как тебя зовут?",
                "Из какого ты города?",
                "Отлично, Аня! Твой ID в системе: 1",
                "В Перми сегодня дождь",
                "Твой профиль: Имя - Аня, Город - Пермь, ID - 1",
                "До встречи!"
        };
        int position = 0;
        for (String expected : expectedInOrder) {
            int found = output.indexOf(expected, position);
            assertTrue(found >= 0, "Не найдено (или не по порядку): " + expected);
            position = found + expected.length();
        }
    }

    @Test
    @DisplayName("Приглашение «> » печатается только перед командами")
    void promptIsShownOnlyBeforeCommands() {
        // имя и город вводятся без «> », перед двумя командами — по одному «> »
        String output = runWithInput("Аня", "Пермь", "погода", "выход");
        assertEquals(2, countOccurrences(output, "> "));
    }

    @Test
    @DisplayName("После «выход» остальной ввод не обрабатывается")
    void inputAfterExitIsIgnored() {
        String output = runWithInput("Аня", "Пермь", "выход", "погода");
        assertTrue(output.contains("До встречи!"));
        assertFalse(output.contains("В Перми сегодня дождь"));
    }

    @Test
    @DisplayName("Если ввод закончился без «выход», программа завершается без ошибки")
    void endOfInputWithoutExit() {
        String output = runWithInput("Аня", "Пермь", "погода");
        assertTrue(output.contains("В Перми сегодня дождь"));
        assertFalse(output.contains("До встречи!"));
    }

    @Test
    @DisplayName("Пустой ввод: бот только здоровается")
    void emptyInput() {
        String output = runWithInput();
        assertTrue(output.contains("Как тебя зовут?"));
        assertFalse(output.contains("Из какого ты города?"));
    }
}
