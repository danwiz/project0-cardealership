package com.revature.service;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;

public class EmployeeLoginService extends UserLoginService {

    public EmployeeLoginService() {
        super();
    }

    public EmployeeLoginService(UserAccountRepository userRepository) {
        super(userRepository);
    }

    @Override
    public User registerUser(String username, String password) {
        throw new SecurityException("employee provisioning requires authenticated administrator");
    }
}
