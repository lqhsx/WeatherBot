package weatherbot.storage;

import weatherbot.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Хранилище пользователей в памяти. Выдаёт каждому новому пользователю уникальный ID.
 */
public class UserBank {
    private final Map<Long, User> users = new HashMap<>();
    private long idCounter = 1;

    public User save(String name, String city) {
        User newUser = new User(idCounter++, name, city);
        users.put(newUser.getId(), newUser);
        return newUser;
    }

    /**
     * @return пользователь или {@code null}, если такого ID нет
     */
    public User findById(long id) {
        return users.get(id);
    }
}
