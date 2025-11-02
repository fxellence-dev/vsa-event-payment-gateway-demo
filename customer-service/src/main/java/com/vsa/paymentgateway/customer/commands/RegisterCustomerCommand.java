package com.vsa.paymentgateway.customer.commands;

import com.vsa.paymentgateway.common.commands.PaymentCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Command to register a new customer
 * Part of the Customer Onboarding vertical slice
 */
public class RegisterCustomerCommand extends PaymentCommand {

    @NotBlank(message = "Customer name cannot be blank")
    private final String customerName;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private final String email;
    
    @NotBlank(message = "Phone number cannot be blank")
    private final String phoneNumber;
    
    private final String address;

    public RegisterCustomerCommand(String customerId, String customerName, String email, 
                                   String phoneNumber, String address) {
        super(customerId);
        this.customerName = customerName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String getCustomerId() { return getTargetAggregateIdentifier(); }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
}