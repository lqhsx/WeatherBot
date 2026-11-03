package weatherbot.console;

import weatherbot.bot.BotState;
import weatherbot.bot.WeatherBot;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * Работа с консолью: читает строки, передаёт их боту и печатает ответы.
 * Ввод и вывод передаются снаружи, поэтому в тестах их можно подменить.
 */
public class ConsoleBot {
    private final WeatherBot bot;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleBot(WeatherBot bot, Scanner in, PrintStream out) {
        this.bot = bot;
        this.in = in;
        this.out = out;
    }

    public void run() {
        out.println(bot.start());

        while (!bot.isFinished()) {
            if (bot.getState() == BotState.WAITING_COMMAND) {
                out.print("> ");
                out.flush();
            }
            if (!in.hasNextLine()) {
                break; // ввод закончился (Ctrl+D / Ctrl+Z)
            }
            out.println(bot.reply(in.nextLine()));
        }
    }
}
