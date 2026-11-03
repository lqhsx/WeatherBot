package weatherbot.bot;

import weatherbot.model.User;
import weatherbot.service.WeatherService;
import weatherbot.storage.UserBank;

/**
 * Логика диалога. Принимает строку от пользователя и возвращает строку-ответ.
 * Про консоль ничего не знает, поэтому её можно тестировать без ввода с клавиатуры.
 */
public class WeatherBot {
    static final String COMMANDS_HINT =
            "Введи 'погода', чтобы узнать прогноз, 'профиль', чтобы увидеть свои данные, или 'выход' для завершения.";

    private final UserBank userBank;
    private final WeatherService weatherService;

    private BotState state = BotState.WAITING_NAME;
    private String pendingName;
    private User currentUser;

    public WeatherBot(UserBank userBank, WeatherService weatherService) {
        this.userBank = userBank;
        this.weatherService = weatherService;
    }

    /** Первое сообщение бота. */
    public String start() {
        state = BotState.WAITING_NAME;
        return "Привет! Я консольный погодный бот.\n"
                + "Давай познакомимся. Как тебя зовут?";
    }

    /** Обрабатывает одну строку ввода в зависимости от этапа диалога. */
    public String reply(String input) {
        return switch (state) {
            case WAITING_NAME -> handleName(input);
            case WAITING_CITY -> handleCity(input);
            case WAITING_COMMAND -> handleCommand(input);
            case FINISHED -> "Диалог завершён.";
        };
    }

    private String handleName(String name) {
        pendingName = name;
        state = BotState.WAITING_CITY;
        return "Из какого ты города?";
    }

    private String handleCity(String city) {
        currentUser = userBank.save(pendingName, city);
        pendingName = null;
        state = BotState.WAITING_COMMAND;
        return "Отлично, " + currentUser.getName() + "! Твой ID в системе: " + currentUser.getId() + "\n"
                + COMMANDS_HINT;
    }

    private String handleCommand(String input) {
        String command = input.toLowerCase().trim();
        return switch (command) {
            case "выход" -> {
                state = BotState.FINISHED;
                yield "До встречи!";
            }
            case "погода" -> weatherService.getForecast(currentUser.getCity());
            case "профиль" -> "Твой профиль: Имя - " + currentUser.getName()
                    + ", Город - " + currentUser.getCity()
                    + ", ID - " + currentUser.getId();
            default -> "Неизвестная команда. Доступные команды: погода, профиль, выход.";
        };
    }

    public BotState getState() {
        return state;
    }

    public boolean isFinished() {
        return state == BotState.FINISHED;
    }

    /** @return текущий пользователь или {@code null}, если знакомство ещё не закончено */
    public User getCurrentUser() {
        return currentUser;
    }
}
