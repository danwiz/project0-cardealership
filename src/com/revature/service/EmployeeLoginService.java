package com.revature.service;

import com.revature.cardealer.User;

//import com.revature.cardealer.Employee;

public class EmployeeLoginService extends UserLoginService {

	@Override
	public boolean authenticateUser(User user) {
		return super.authenticateUser(user);
	}
}
