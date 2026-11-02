import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weatherbot.model.User;
import weatherbot.service.WeatherService;
import weatherbot.storage.UserBank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherBotTest {
    private static final String UNKNOWN_COMMAND =
            "Неизвестная команда. Доступные команды: погода, профиль, выход.";

    private UserBank userBank;
    private WeatherBot bot;

    @BeforeEach
    void setUp() {
        userBank = new UserBank();
        bot = new WeatherBot(userBank, new WeatherService());
    }

    /** Проводит знакомство: старт, имя, город. */
    private void register(String name, String city) {
        bot.start();
        bot.reply(name);
        bot.reply(city);
    }

    // ---------- Знакомство ----------

    @Test
    @DisplayName("start здоровается и спрашивает имя")
    void startGreetsAndAsksName() {
        String reply = bot.start();
        assertEquals("Привет! Я консольный погодный бот.\nДавай познакомимся. Как тебя зовут?", reply);
        assertEquals(BotState.WAITING_NAME, bot.getState());
    }

    @Test
    @DisplayName("После имени бот спрашивает город")
    void afterNameAsksCity() {
        bot.start();
        assertEquals("Из какого ты города?", bot.reply("Аня"));
        assertEquals(BotState.WAITING_CITY, bot.getState());
    }

    @Test
    @DisplayName("После города бот называет имя, ID и список команд")
    void afterCityShowsNameIdAndCommands() {
        bot.start();
        bot.reply("Аня");
        String reply = bot.reply("Пермь");
        assertEquals("Отлично, Аня! Твой ID в системе: 1\n" + WeatherBot.COMMANDS_HINT, reply);
        assertEquals(BotState.WAITING_COMMAND, bot.getState());
    }

    @Test
    @DisplayName("После знакомства пользователь сохраняется в хранилище")
    void userIsSavedToBank() {
        register("Аня", "Пермь");
        User user = bot.getCurrentUser();
        assertNotNull(user);
        assertEquals("Аня", user.getName());
        assertEquals("Пермь", user.getCity());
        assertSame(user, userBank.findById(user.getId()));
    }

    @Test
    @DisplayName("До окончания знакомства текущего пользователя нет")
    void noCurrentUserBeforeRegistration() {
        bot.start();
        assertNull(bot.getCurrentUser());
        bot.reply("Аня");
        assertNull(bot.getCurrentUser());
        assertNull(userBank.findById(1));
    }

    @Test
    @DisplayName("Имя, совпадающее с командой, не считается командой")
    void nameIsNotTreatedAsCommand() {
        bot.start();
        assertEquals("Из какого ты города?", bot.reply("выход"));
        assertFalse(bot.isFinished());
        assertEquals("Отлично, выход! Твой ID в системе: 1\n" + WeatherBot.COMMANDS_HINT,
                bot.reply("погода"));
        assertEquals("погода", bot.getCurrentUser().getCity());
    }

    @Test
    @DisplayName("Два бота с общим хранилищем выдают пользователям разные ID")
    void sharedBankGivesDifferentIds() {
        register("Аня", "Пермь");
        WeatherBot secondBot = new WeatherBot(userBank, new WeatherService());
        secondBot.start();
        secondBot.reply("Боря");
        assertTrue(secondBot.reply("Москва").contains("Твой ID в системе: 2"));
    }

    // ---------- Команды ----------

    @Test
    @DisplayName("Команда «погода» выдаёт прогноз для города пользователя")
    void weatherCommandUsesUserCity() {
        register("Аня", "Пермь");
        assertEquals("В Перми сегодня дождь, целый день идет дождь, +5°C.", bot.reply("погода"));
    }

    @Test
    @DisplayName("Команда «погода» для города не из базы выдаёт заглушку")
    void weatherCommandForUnknownCity() {
        register("Аня", "Омск");
        assertTrue(bot.reply("погода").startsWith("Прогноз для города Омск пока не добавлен"));
    }

    @Test
    @DisplayName("Команда «профиль» показывает имя, город и ID")
    void profileCommandShowsUserData() {
        register("Аня", "Пермь");
        assertEquals("Твой профиль: Имя - Аня, Город - Пермь, ID - 1", bot.reply("профиль"));
    }

    @Test
    @DisplayName("Регистр и пробелы в командах не важны")
    void commandsIgnoreCaseAndSpaces() {
        register("Аня", "Пермь");
        assertEquals(bot.reply("погода"), bot.reply("  ПОГОДА "));
        assertEquals(bot.reply("профиль"), bot.reply("Профиль"));
    }

    @Test
    @DisplayName("Неизвестная команда — подсказка со списком команд")
    void unknownCommandShowsHint() {
        register("Аня", "Пермь");
        assertEquals(UNKNOWN_COMMAND, bot.reply("привет"));
        assertEquals(UNKNOWN_COMMAND, bot.reply(""));
        assertEquals(BotState.WAITING_COMMAND, bot.getState());
    }

    @Test
    @DisplayName("Команды можно вводить сколько угодно раз")
    void commandsCanBeRepeated() {
        register("Аня", "Пермь");
        for (int i = 0; i < 5; i++) {
            assertEquals("Твой профиль: Имя - Аня, Город - Пермь, ID - 1", bot.reply("профиль"));
        }
        assertFalse(bot.isFinished());
    }

    // ---------- Выход ----------

    @Test
    @DisplayName("Команда «выход» прощается и завершает диалог")
    void exitCommandFinishesDialog() {
        register("Аня", "Пермь");
        assertFalse(bot.isFinished());
        assertEquals("До встречи!", bot.reply("выход"));
        assertTrue(bot.isFinished());
        assertEquals(BotState.FINISHED, bot.getState());
    }

    @Test
    @DisplayName("После выхода бот не выполняет команды")
    void noCommandsAfterExit() {
        register("Аня", "Пермь");
        bot.reply("выход");
        assertEquals("Диалог завершён.", bot.reply("погода"));
        assertTrue(bot.isFinished());
    }
}
