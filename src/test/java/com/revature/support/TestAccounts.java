package com.revature.support;

import java.lang.reflect.Field;

import com.revature.application.ConfiguredDealershipApplication;
import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;
import com.revature.service.ReadOnlyUserAccountRepository;
import com.revature.service.PasswordHasher;
import com.revature.service.Pbkdf2PasswordHasher;
import com.revature.service.UserAccountRepository;

/** Privileged identities may only be planted by trusted test fixtures. */
public final class TestAccounts {
    private TestAccounts() { }

    /**
     * Explicitly trusted test-only fixture access. Never publish this helper in
     * production source; the application-facing repository is intentionally read-only.
     */
    public static UserAccountRepository trustedRepository(ConfiguredDealershipApplication application) {
        return trustedRepository(application.getContext());
    }

    public static UserAccountRepository trustedRepository(DealershipApplicationContext context) {
        try {
            Field rawField = DealershipApplicationContext.class.getDeclaredField("accountRepository");
            rawField.setAccessible(true);
            Object raw = rawField.get(context);
            if (!(raw instanceof UserAccountRepository)
                    || raw instanceof ReadOnlyUserAccountRepository) {
                throw new IllegalStateException("test fixture requires the private writer repository");
            }
            return (UserAccountRepository) raw;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("could not locate trusted test repository fixture", exception);
        }
    }

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
