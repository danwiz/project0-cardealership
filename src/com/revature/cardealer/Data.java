package com.revature.cardealer;

import java.io.Serializable;

import com.revature.service.CustomerLoginService;
import com.revature.service.EmployeeLoginService;

/*
 * Serializable is a Marker Interface
 * Marker Interface - Interface with no abstract methods
 * It works as check for the compiler.
 */
public class Data implements Serializable {

    private EmployeeLoginService dEls;
    private CustomerLoginService dCls;
    private User dCustomer;
    private User dEmployee;
    private Offer dCarlot;
    private Payments dPayments;
    String caption;

    public void setCustomerLoginService(CustomerLoginService customerLoginService) {
        dCls = customerLoginService;
    }

    public void setEmployeeLoginService(EmployeeLoginService employeeLoginService) {
        dEls = employeeLoginService;
    }

    public EmployeeLoginService getEmployeeLoginService() {
        return dEls;
    }

    public CustomerLoginService getCustomerLoginService() {
        return dCls;
    }

    public void setOffer(Offer carlot) {
        dCarlot = carlot;
    }

    public User getCustomer() {
        return dCustomer;
    }

    public void setCustomer(User customer) {
        dCustomer = customer;
    }

    public User getEmployee() {
        return dEmployee;
    }

    public void setEmployee(User employee) {
        dEmployee = employee;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public String toString() {
        return "[ Data Objects ]: Logins: Customer=" + dCls.getUserNames()
                + " Employee=" + dEls.getUserNames()
                + "\nCars: " + (dCarlot == null ? "[]" : dCarlot.getListings())
                + "\nCustomer Payments: "
                + (dPayments == null ? "" : dPayments.getpPayment());
    }

    public Data() {
        super();
    }

    public Data(CustomerLoginService customerLoginService,
            EmployeeLoginService employeeLoginService,
            User employee,
            User customer,
            Offer carlot,
            Payments payments) {
        super();
        dCls = customerLoginService;
        dEls = employeeLoginService;
        dCustomer = customer;
        dEmployee = employee;
        // Offer and Payments assignments remain deferred to the persistence increment.
    }
}
