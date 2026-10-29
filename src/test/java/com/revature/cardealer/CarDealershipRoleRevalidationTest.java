package com.revature.cardealer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.service.Permission;

class CarDealershipRoleRevalidationTest {

    @Test
    @Tag("security")
    void rejectsMissingAccountAndNullPermission() {
        assertFalse(CarDealership.isCurrentRoleAuthorized(null, Permission.MANAGE_PERSISTENCE));
        User unknown = new User();
        unknown.setUsername("unregistered");
        unknown.setRole(AccountRole.ADMINISTRATOR);
        assertFalse(CarDealership.isCurrentRoleAuthorized(unknown, Permission.MANAGE_PERSISTENCE));
        assertFalse(CarDealership.isCurrentRoleAuthorized(unknown, null));
    }

    @Test
    @Tag("security")
    void rejectsCallerSuppliedAdministratorRoleWithoutPersistedIdentity() {
        User forged = new User();
        forged.setUsername("not-registered");
        forged.setRole(AccountRole.ADMINISTRATOR);
        assertFalse(CarDealership.isCurrentRoleAuthorized(forged, Permission.MANAGE_USERS));
        assertFalse(CarDealership.isCurrentRoleAuthorized(forged, Permission.MANAGE_PERSISTENCE));
    }

    @Test
    @Tag("security")
    void rejectsMissingUsername() {
        User user = new User();
        user.setRole(AccountRole.ADMINISTRATOR);
        assertFalse(CarDealership.isCurrentRoleAuthorized(user, Permission.MANAGE_PERSISTENCE));
    }
}
