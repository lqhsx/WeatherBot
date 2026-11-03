package weatherbot.bot;
/**
 * Этап диалога, на котором сейчас находится бот.
 */
public enum BotState {
    /** Ждём имя пользователя. */
    WAITING_NAME,
    /** Ждём город пользователя. */
    WAITING_CITY,
    /** Знакомство закончено, ждём команды. */
    WAITING_COMMAND,
    /** Пользователь ввёл «выход». */
    FINISHED
}
