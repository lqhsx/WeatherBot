package weatherbot.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import weatherbot.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class UserBankTest {
    private UserBank bank;

    @BeforeEach
    void setUp() {
        bank = new UserBank();
    }

    @Test
    @DisplayName("save сохраняет имя и город пользователя")
    void saveKeepsNameAndCity() {
        User user = bank.save("Аня", "Пермь");
        assertEquals("Аня", user.getName());
        assertEquals("Пермь", user.getCity());
    }

    @Test
    @DisplayName("Первый пользователь получает ID 1")
    void firstUserGetsIdOne() {
        assertEquals(1, bank.save("Аня", "Пермь").getId());
    }

    @Test
    @DisplayName("Каждый следующий пользователь получает ID на 1 больше")
    void idsIncreaseByOne() {
        User first = bank.save("Аня", "Пермь");
        User second = bank.save("Боря", "Москва");
        User third = bank.save("Вика", "Челябинск");
        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(3, third.getId());
    }

    @Test
    @DisplayName("Пользователи с одинаковыми данными получают разные ID")
    void sameDataGetsDifferentIds() {
        User first = bank.save("Аня", "Пермь");
        User second = bank.save("Аня", "Пермь");
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    @DisplayName("findById находит сохранённого пользователя")
    void findByIdReturnsSavedUser() {
        User saved = bank.save("Аня", "Пермь");
        bank.save("Боря", "Москва");
        assertSame(saved, bank.findById(saved.getId()));
    }

    @Test
    @DisplayName("findById возвращает null для несуществующего ID")
    void findByIdReturnsNullForUnknownId() {
        bank.save("Аня", "Пермь");
        assertNull(bank.findById(42));
        assertNull(bank.findById(0));
    }

    @Test
    @DisplayName("В пустом хранилище никого нет")
    void emptyBankFindsNobody() {
        assertNull(bank.findById(1));
    }

    @Test
    @DisplayName("У разных хранилищ свои счётчики ID")
    void banksHaveIndependentCounters() {
        bank.save("Аня", "Пермь");
        UserBank otherBank = new UserBank();
        assertEquals(1, otherBank.save("Боря", "Москва").getId());
    }

    // ---------- Сохранение в файл ----------

    @Test
    @DisplayName("Если файла нет, хранилище пустое и ID начинаются с 1")
    void missingFileGivesEmptyBank(@TempDir Path dir) {
        Path file = dir.resolve("users.txt");
        UserBank fileBank = new UserBank(file);
        assertNull(fileBank.findById(1));
        assertFalse(Files.exists(file));
        assertEquals(1, fileBank.save("Аня", "Пермь").getId());
    }

    @Test
    @DisplayName("save дописывает пользователя в файл")
    void saveAppendsLineToFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("users.txt");
        UserBank fileBank = new UserBank(file);
        fileBank.save("Аня", "Пермь");
        fileBank.save("Боря", "Москва");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(List.of("1\tАня\tПермь", "2\tБоря\tМосква"), lines);
    }

    @Test
    @DisplayName("Новое хранилище читает пользователей, сохранённые прошлым запуском")
    void usersAreLoadedOnStart(@TempDir Path dir) {
        Path file = dir.resolve("users.txt");
        UserBank first = new UserBank(file);
        first.save("Аня", "Пермь");
        first.save("Боря", "Москва");

        UserBank second = new UserBank(file);
        User anya = second.findById(1);
        assertNotNull(anya);
        assertEquals("Аня", anya.getName());
        assertEquals("Пермь", anya.getCity());
        assertEquals("Москва", second.findById(2).getCity());
    }

    @Test
    @DisplayName("После загрузки ID продолжаются, а не начинаются заново")
    void idsContinueAfterLoad(@TempDir Path dir) {
        Path file = dir.resolve("users.txt");
        UserBank first = new UserBank(file);
        first.save("Аня", "Пермь");
        first.save("Боря", "Москва");

        assertEquals(3, new UserBank(file).save("Вика", "Челябинск").getId());
    }

    @Test
    @DisplayName("Пустые и испорченные строки в файле пропускаются")
    void brokenLinesAreSkipped(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("users.txt");
        Files.writeString(file, "1\tАня\tПермь\n\nмусор\nабв\tБоря\tМосква\n5\tВика\tЧелябинск\n",
                StandardCharsets.UTF_8);

        UserBank fileBank = new UserBank(file);
        assertEquals("Аня", fileBank.findById(1).getName());
        assertEquals("Вика", fileBank.findById(5).getName());
        assertEquals(6, fileBank.save("Гена", "Омск").getId());
    }

    @Test
    @DisplayName("Табуляция в имени не ломает файл")
    void tabInNameDoesNotBreakFile(@TempDir Path dir) {
        Path file = dir.resolve("users.txt");
        new UserBank(file).save("Анна\tМария", "Пермь");

        User loaded = new UserBank(file).findById(1);
        assertEquals("Анна Мария", loaded.getName());
        assertEquals("Пермь", loaded.getCity());
    }
}
