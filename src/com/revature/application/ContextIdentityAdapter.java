package com.revature.application;

import java.util.Objects;
import java.util.Optional;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.User;
import com.revature.service.UserLoginService;

/** Binds the identity port to the current in-memory application context. */
public final class ContextIdentityAdapter implements ApplicationPorts.Identity {
    private final DealershipApplicationContext context;

    public ContextIdentityAdapter(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public User register(AccountRole role, String username, String credential) {
        User account;
        switch (Objects.requireNonNull(role, "role")) {
        case CUSTOMER: account = context.getCustomerLoginService().registerUser(username, credential); break;
        case EMPLOYEE: account = context.getEmployeeLoginService().registerUser(username, credential); break;
        case ADMINISTRATOR: account = context.getAdminLoginService().registerUser(username, credential); break;
        default: throw new IllegalArgumentException("unsupported account role: " + role);
        }
        return account;
    }

    @Override
    public Optional<User> authenticate(AccountRole role, User credentials) {
        UserLoginService service;
        switch (Objects.requireNonNull(role, "role")) {
        case CUSTOMER: service = context.getCustomerLoginService(); break;
        case EMPLOYEE: service = context.getEmployeeLoginService(); break;
        case ADMINISTRATOR: service = context.getAdminLoginService(); break;
        default: throw new IllegalArgumentException("unsupported account role: " + role);
        }
        return service.authenticate(credentials);
    }

    @Override public void setCurrentAccount(User account) { context.setCurrentAccount(account); }

    @Override
    public void rememberRegisteredAccount(User account) {
        if (account.getRole() == AccountRole.CUSTOMER) context.setLatestCustomer(account);
        else if (account.getRole() == AccountRole.EMPLOYEE) context.setLatestEmployee(account);
    }
}