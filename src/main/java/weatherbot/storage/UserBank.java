package weatherbot.storage;

import weatherbot.model.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище пользователей. Выдаёт каждому новому пользователю уникальный ID.
 * Если передан файл, при создании хранилища из него читаются все ранее сохранённые
 * пользователи, а каждый новый пользователь сразу дописывается в конец файла.
 * Без файла хранилище работает только в памяти
 */
public class UserBank {
    private static final String SEPARATOR = "\t";

    private final Map<Long, User> users = new HashMap<>();
    private final Path file;
    private long idCounter = 1;

    /** Хранилище только в памяти, без файла. */
    public UserBank() {
        this.file = null;
    }

    /**
     * Хранилище, связанное с файлом. Если файл уже есть, пользователи загружаются из него.
     * Если файла нет, он будет создан при первом сохранении.
     */
    public UserBank(Path file) {
        this.file = file;
        load();
    }

    public User save(String name, String city) {
        User newUser = new User(idCounter++, clean(name), clean(city));
        users.put(newUser.getId(), newUser);
        appendToFile(newUser);
        return newUser;
    }

    public User findById(long id) {
        return users.get(id);
    }

    /** Читает пользователей из файла и выставляет счётчик ID после самого большого из них. */
    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл пользователей: " + file, e);
        }
        for (String line : lines) {
            User user = parse(line);
            if (user == null) {
                continue; // пустая или испорченная строка — пропускаем
            }
            users.put(user.getId(), user);
            idCounter = Math.max(idCounter, user.getId() + 1);
        }
    }

    private static User parse(String line) {
        String[] parts = line.split(SEPARATOR, -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            long id = Long.parseLong(parts[0].trim());
            return new User(id, parts[1], parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void appendToFile(User user) {
        if (file == null) {
            return;
        }
        String line = user.getId() + SEPARATOR + user.getName() + SEPARATOR + user.getCity()
                + System.lineSeparator();
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось записать пользователя в файл: " + file, e);
        }
    }

    /** Убирает из значения символы, которые сломали бы формат файла. */
    private static String clean(String value) {
        return value.replace(SEPARATOR, " ").replace("\r", " ").replace("\n", " ");
    }
}
