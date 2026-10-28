package com.revature.service;

import com.revature.DAOService.DAOService;
import com.revature.DAOService.DataDAO;
import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;

public class AdminLoginService extends UserLoginService {

    DataDAO cDao = new DAOService();

    public AdminLoginService() {
        super();
    }

    public AdminLoginService(UserAccountRepository userRepository) {
        super(userRepository);
    }

    @Override
    public User registerUser(String username, String password) {
        throw new SecurityException("administrator provisioning requires authenticated administrator");
    }

    public void deleteAllUsers() {
        System.out.println("You better be sure you want to do this!!!!!!!");
    }
}
