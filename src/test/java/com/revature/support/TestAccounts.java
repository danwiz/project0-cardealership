package com.revature.support;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;
import com.revature.service.PasswordHasher;
import com.revature.service.Pbkdf2PasswordHasher;
import com.revature.service.UserAccountRepository;

/** Privileged identities may only be planted by trusted test fixtures. */
public final class TestAccounts {
    private TestAccounts() { }

    public static User seed(UserAccountRepository repository, AccountRole role,
            String username, String plaintext) {
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        User user = new User();
        user.setUsername(username);
        user.setPassword(hasher.hash(plaintext));
        user.setRole(role);
        return repository.save(user);
    }
}
