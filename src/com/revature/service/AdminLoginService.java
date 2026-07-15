package com.revature.service;

import com.revature.cardealer.*;
import com.revature.DAOService.DAOService;
import com.revature.DAOService.DataDAO;

public class AdminLoginService extends UserLoginService {

	DataDAO cDao = new DAOService();

	@Override
	public boolean authenticateUser(User user) {
		return super.authenticateUser(user);
	}

	public void deleteAllUsers() {
		System.out.println("You better be sure you want to do this!!!!!!!");
	}
}
