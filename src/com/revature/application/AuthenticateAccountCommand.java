package com.revature.application;

import java.util.Objects;
import java.util.Optional;

import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.User;
import com.revature.service.Permission;
import com.revature.service.UserLoginService;

/** Authenticates credentials and applies one required authorization decision. */
public final class AuthenticateAccountCommand {

    public enum Status {
        AUTHENTICATED,
        INVALID_CREDENTIALS,
        ACCESS_DENIED
    }

    public static final class Result {
        private final Status status;
        private final User account;

        private Result(Status status, User account) {
            this.status = status;
            this.account = account;
        }

        public static Result authenticated(User account) {
            return new Result(Status.AUTHENTICATED, Objects.requireNonNull(account, "account"));
        }

        public static Result failure(Status status) {
            if (status == Status.AUTHENTICATED) {
                throw new IllegalArgumentException("failure status must not be AUTHENTICATED");
            }
            return new Result(status, null);
        }

        public Status getStatus() { return status; }
        public Optional<User> getAccount() { return Optional.ofNullable(account); }
        public boolean isAuthenticated() { return status == Status.AUTHENTICATED; }
    }

    private final DealershipApplicationContext context;

    public AuthenticateAccountCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public Result execute(UserLoginService loginService, User credentials, Permission permission) {
        Objects.requireNonNull(loginService, "loginService");
        Objects.requireNonNull(permission, "permission");

        Optional<User> authenticated = loginService.authenticate(credentials);
        if (!authenticated.isPresent()) {
            context.setCurrentAccount(null);
            return Result.failure(Status.INVALID_CREDENTIALS);
        }

        User account = authenticated.get();
        if (!context.getAuthorizationService().isAuthorized(account, permission)) {
            context.setCurrentAccount(null);
            return Result.failure(Status.ACCESS_DENIED);
        }

        context.setCurrentAccount(account);
        return Result.authenticated(account);
    }
}
