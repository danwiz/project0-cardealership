package com.revature.service;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Car;
import com.revature.cardealer.User;

public class CustomerLoginService extends UserLoginService {

    private Car[] cOwnedDB = new Car[20];
    private int cOwnedIndex = 0;
    private int[] cPrice = new int[20];
    private int[] pMths = new int[20];
    private int cPayments[] = new int[20];

    public CustomerLoginService() {
        super();
    }

    public CustomerLoginService(UserAccountRepository userRepository) {
        super(userRepository);
    }

    @Override
    public User registerUser(String username, String password) {
        return super.registerUser(username, password, AccountRole.CUSTOMER);
    }

    public void setCarsOwned(Car owned, int cprice, int pmths) {
        if (cOwnedIndex <= cOwnedDB.length)
            cOwnedDB[cOwnedIndex] = owned;
            cPrice[cOwnedIndex] = cprice;
            pMths[cOwnedIndex] = pmths;

        cOwnedIndex++;
    }

    public void setPayments(int payments) {
        cPayments[cOwnedIndex] = payments;
        System.out.println(payments);
    }

    public void setPayments(String[] payaments) {
        /* Legacy overload retained for a later ownership/payment refactor. */
    }

    public void getCarsOwned() {
        for (int i = 0; i <= cOwnedDB.length; i++) {
            System.out.println(cOwnedDB[i].getCar() + "   Price: " + cPrice[i]
                    + " Monthly Cost: " + (cPrice[i] / pMths[i]));
        }
    }
}
