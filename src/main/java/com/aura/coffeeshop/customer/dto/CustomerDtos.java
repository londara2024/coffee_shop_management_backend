package com.aura.coffeeshop.customer.dto;

import com.aura.coffeeshop.customer.entity.Customer;
import com.aura.coffeeshop.customer.entity.LoyaltyAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    /** Same rules as sign-up-form.tsx. */
    public record SignUpRequest(
            @Schema(example = "Sophia Vance") @NotBlank @Size(min = 2, max = 150, message = "Tell us the name for your cup.") String name,
            @Schema(example = "sophia.vance@botanica.com") @NotBlank @Email @Size(max = 200) String email,
            @Schema(example = "+1 (555) 234-5678", description = "Optional")
            @Pattern(regexp = "^$|^(?:\\D*\\d){7,}\\D*$", message = "That number looks too short.") @Size(max = 30) String phone,
            @Schema(example = "Botanica2026")
            @NotBlank @Size(min = 8, max = 72, message = "Use at least 8 characters.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Include both letters and numbers.") String password,
            @Schema(description = "Must be true") @AssertTrue(message = "Please accept the Terms of Service and Privacy Policy.") boolean acceptedTerms) {
    }

    public record SignInRequest(
            @Schema(example = "elena.rostova@botanica.com") @NotBlank @Email String email,
            @Schema(example = "Password123") @NotBlank String password) {
    }

    public record LoyaltyResponse(String tier, int stars, int starsGoal, int starsToNextReward) {
        public static LoyaltyResponse from(LoyaltyAccount a) {
            return a == null ? null
                    : new LoyaltyResponse(a.getTier(), a.getStars(), a.getStarsGoal(), Math.max(0, a.getStarsGoal() - a.getStars()));
        }
    }

    public record CustomerResponse(String id, String name, String email, String phone, String avatarUrl,
                                   Instant createdAt, LoyaltyResponse loyalty) {
        public static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAvatarUrl(),
                    c.getCreatedAt(), LoyaltyResponse.from(c.getLoyalty()));
        }
    }
}
