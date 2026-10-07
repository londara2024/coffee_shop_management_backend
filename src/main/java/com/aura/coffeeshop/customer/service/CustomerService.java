package com.aura.coffeeshop.customer.service;

import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.customer.dto.CustomerDtos.CustomerResponse;
import com.aura.coffeeshop.customer.dto.CustomerDtos.SignInRequest;
import com.aura.coffeeshop.customer.dto.CustomerDtos.SignUpRequest;
import com.aura.coffeeshop.customer.entity.Customer;
import com.aura.coffeeshop.customer.entity.LoyaltyAccount;
import com.aura.coffeeshop.customer.repository.CustomerRepository;
import com.aura.coffeeshop.customer.repository.LoyaltyAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerResponse signUp(SignUpRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        if (phone != null && customerRepository.existsByPhone(phone)) {
            throw new BusinessRuleException("An account with this phone number already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.name().trim());
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setPasswordHash(passwordEncoder.encode(request.password()));
        customer.setLoyalty(new LoyaltyAccount(customer));
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse signIn(SignInRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(c -> c.getPasswordHash() != null && passwordEncoder.matches(request.password(), c.getPasswordHash()))
                .orElseThrow(() -> new BusinessRuleException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getProfile(String id) {
        return CustomerResponse.from(getCustomer(id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(String id) {
        return customerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Customer", id));
    }

    /** One star per whole dollar spent, credited when an order is completed. */
    public void awardStars(String customerId, BigDecimal orderTotal) {
        loyaltyRepository.findById(customerId).ifPresent(account -> {
            int earned = orderTotal.setScale(0, RoundingMode.DOWN).intValue();
            account.setStars(account.getStars() + earned);
        });
    }
}
