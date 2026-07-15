package com.revature.cardealer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.service.CustomerLoginService;
import com.revature.service.EmployeeLoginService;

@Tag("legacy-behavior")
@DisplayName("Data legacy constructor characterization")
class DataCharacterizationTest {

    @Test
    @DisplayName("parameterized constructor retains login services and users")
    void parameterizedConstructorRetainsAssignedCoreObjects() {
        CustomerLoginService customerService = new CustomerLoginService();
        EmployeeLoginService employeeService = new EmployeeLoginService();
        User employee = new User();
        User customer = new User();

        Data data = new Data(customerService, employeeService, employee, customer, new Offer(), new Payments());

        assertSame(customerService, data.getCustomerLoginService());
        assertSame(employeeService, data.getEmployeeLoginService());
        assertSame(customer, data.getCustomer());
        assertSame(employee, data.getEmployee());
    }

    @Test
    @Tag("known-defect")
    @DisplayName("parameterized constructor currently omits Offer and Payments assignments")
    void parameterizedConstructorCurrentlyOmitsOfferAndPayments() throws ReflectiveOperationException {
        Data data = new Data(
                new CustomerLoginService(),
                new EmployeeLoginService(),
                new User(),
                new User(),
                new Offer(),
                new Payments());

        assertNull(readField(data, "dCarlot"));
        assertNull(readField(data, "dPayments"));
    }

    private static Object readField(Data data, String fieldName) throws ReflectiveOperationException {
        Field field = Data.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(data);
    }
}
