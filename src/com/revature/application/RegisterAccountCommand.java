package com.revature.application;

import java.util.Objects;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.User;

/** Application use case for registering a role-specific account. */
public final class RegisterAccountCommand {

    private final DealershipApplicationContext context;

    public RegisterAccountCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public User execute(AccountRole role, String username, String password) {
        Objects.requireNonNull(role, "role");
        User account;
        switch (role) {
        case CUSTOMER:
            account = context.getCustomerLoginService().registerUser(username, password);
            context.setLatestCustomer(account);
            return account;
        case EMPLOYEE:
            account = context.getEmployeeLoginService().registerUser(username, password);
            context.setLatestEmployee(account);
            return account;
        case ADMINISTRATOR:
            return context.getAdminLoginService().registerUser(username, password);
        default:
            throw new IllegalArgumentException("unsupported account role: " + role);
        }
    }
}
