package weatherbot.model;

/**
 * Пользователь бота. Неизменяемый объект: данные задаются один раз при создании.
 */
public class User {
    private final long id;
    private final String name;
    private final String city;

    public User(long id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}
