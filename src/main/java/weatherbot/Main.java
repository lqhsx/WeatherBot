package weatherbot;

import weatherbot.bot.WeatherBot;
import weatherbot.console.ConsoleBot;
import weatherbot.service.WeatherService;
import weatherbot.storage.UserBank;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Точка входа. Здесь только собираются части программы и запускается консоль.
 */
public class Main {
    public static void main(String[] args) {
        UserBank userBank = new UserBank(Path.of("users.txt"));
        WeatherService weatherService = new WeatherService();
        WeatherBot bot = new WeatherBot(userBank, weatherService);

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        new ConsoleBot(bot, scanner, out).run();
    }
}
