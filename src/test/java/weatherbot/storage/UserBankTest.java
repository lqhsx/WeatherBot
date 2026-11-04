package weatherbot.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weatherbot.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
