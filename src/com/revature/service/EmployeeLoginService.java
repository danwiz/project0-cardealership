package com.revature.service;

import com.revature.cardealer.User;

public class EmployeeLoginService extends UserLoginService {

    public EmployeeLoginService() {
        super();
    }

    public EmployeeLoginService(UserAccountRepository userRepository) {
        super(userRepository);
    }

    @Override
    public boolean authenticateUser(User user) {
        return super.authenticateUser(user);
    }
}
