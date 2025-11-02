package com.vsa.paymentgateway.customer.api;

import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import com.vsa.paymentgateway.customer.commands.AddPaymentMethodCommand;
import com.vsa.paymentgateway.customer.commands.RegisterCustomerCommand;
import com.vsa.paymentgateway.customer.readmodel.CustomerReadModel;
import com.vsa.paymentgateway.customer.service.CustomerQueryService;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for Customer operations
 * Part of the Customer Onboarding vertical slice
 * Demonstrates command/query separation in VSA
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CommandGateway commandGateway;
    private final CustomerQueryService customerQueryService;

    public CustomerController(CommandGateway commandGateway, CustomerQueryService customerQueryService) {
        this.commandGateway = commandGateway;
        this.customerQueryService = customerQueryService;
    }

    // Command operations (writes)
    @PostMapping("/register")
    public ResponseEntity<RegisterCustomerResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        // Check if customer already exists
        if (customerQueryService.customerExistsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new RegisterCustomerResponse(null, "Customer with this email already exists"));
        }

        // Use provided customerId or generate new one
        String customerId = request.getCustomerId() != null ? request.getCustomerId() : UUID.randomUUID().toString();
        
        // Map request fields - support both firstName/lastName and customerName
        String fullName = request.getCustomerName();
        if (fullName == null && request.getFirstName() != null) {
            fullName = request.getFirstName() + (request.getLastName() != null ? " " + request.getLastName() : "");
        }
        
        RegisterCustomerCommand command = new RegisterCustomerCommand(
            customerId,
            fullName != null ? fullName : "Unknown",
            request.getEmail(),
            request.getPhoneNumber() != null ? request.getPhoneNumber() : "",
            request.getAddress() != null ? request.getAddress() : ""
        );

        try {
            commandGateway.sendAndWait(command);
            return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterCustomerResponse(customerId, "Customer registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new RegisterCustomerResponse(null, "Failed to register customer: " + e.getMessage()));
        }
    }

    @PostMapping("/payment-methods")
    public ResponseEntity<String> addPaymentMethod(
            @Valid @RequestBody AddPaymentMethodRequest request) {
        
        // Verify customer exists
        if (customerQueryService.findCustomerById(request.getCustomerId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PaymentCard paymentCard = new PaymentCard(
            request.getCardNumber(),
            request.getExpiryMonth(),
            request.getExpiryYear(),
            request.getCvv(),
            request.getCardHolderName()
        );

        AddPaymentMethodCommand command = new AddPaymentMethodCommand(
            request.getCustomerId(),
            paymentCard,
            request.isDefault()
        );

        try {
            commandGateway.sendAndWait(command);
            return ResponseEntity.ok("Payment method added successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add payment method: " + e.getMessage());
        }
    }

    // Query operations (reads)
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerReadModel> getCustomer(@PathVariable String customerId) {
        Optional<CustomerReadModel> customer = customerQueryService.findCustomerById(customerId);
        return customer.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{customerId}/with-payment-methods")
    public ResponseEntity<CustomerReadModel> getCustomerWithPaymentMethods(@PathVariable String customerId) {
        Optional<CustomerReadModel> customer = customerQueryService.findCustomerWithPaymentMethods(customerId);
        return customer.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<CustomerReadModel>> getAllCustomers() {
        List<CustomerReadModel> customers = customerQueryService.findAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerReadModel>> searchCustomers(@RequestParam String name) {
        List<CustomerReadModel> customers = customerQueryService.searchCustomersByName(name);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/by-email")
    public ResponseEntity<CustomerReadModel> getCustomerByEmail(@RequestParam String email) {
        Optional<CustomerReadModel> customer = customerQueryService.findCustomerByEmail(email);
        return customer.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // DTOs for requests
    public static class RegisterCustomerRequest {
        private String customerId;  // Optional - will be generated if not provided
        private String customerName;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String address;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
    
    public static class RegisterCustomerResponse {
        private String customerId;
        private String message;
        
        public RegisterCustomerResponse(String customerId, String message) {
            this.customerId = customerId;
            this.message = message;
        }
        
        public String getCustomerId() { return customerId; }
        public String getMessage() { return message; }
    }

    public static class AddPaymentMethodRequest {
        private String customerId;
        private String paymentMethodId;  // Optional
        private String cardNumber;
        private String expiryMonth;
        private String expiryYear;
        private String cvv;
        private String cardHolderName;
        private boolean isDefault;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public String getExpiryMonth() { return expiryMonth; }
        public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }
        public String getExpiryYear() { return expiryYear; }
        public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }
        public String getCvv() { return cvv; }
        public void setCvv(String cvv) { this.cvv = cvv; }
        public String getCardHolderName() { return cardHolderName; }
        public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    }
}