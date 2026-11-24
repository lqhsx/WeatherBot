package weatherbot.storage;

import weatherbot.model.User;

public interface UserBankInt {
    User save(String name, String city);

    User findById(long id);

}







