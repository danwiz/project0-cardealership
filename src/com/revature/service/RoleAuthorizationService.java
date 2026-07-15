package com.revature.service;

import java.util.EnumSet;
import java.util.Map;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;

/**
 * Central role-based authorization policy.
 */
public class RoleAuthorizationService {

    private static final Map<AccountRole, EnumSet<Permission>> PERMISSIONS_BY_ROLE = Map.of(
            AccountRole.CUSTOMER, EnumSet.of(
                    Permission.VIEW_INVENTORY,
                    Permission.REQUEST_PURCHASE,
                    Permission.VIEW_OWNED_VEHICLES,
                    Permission.VIEW_OWN_PAYMENTS),
            AccountRole.EMPLOYEE, EnumSet.of(
                    Permission.VIEW_INVENTORY,
                    Permission.MANAGE_INVENTORY,
                    Permission.REVIEW_PURCHASE_REQUESTS,
                    Permission.VIEW_CUSTOMER_PAYMENTS),
            AccountRole.ADMINISTRATOR, EnumSet.allOf(Permission.class));

    public boolean isAuthorized(User account, Permission permission) {
        if (account == null || account.getRole() == null || permission == null) {
            return false;
        }
        return PERMISSIONS_BY_ROLE.getOrDefault(account.getRole(), EnumSet.noneOf(Permission.class))
                .contains(permission);
    }

    public void requireAuthorized(User account, Permission permission) {
        if (!isAuthorized(account, permission)) {
            throw new SecurityException("account is not authorized for " + permission);
        }
    }
}
