package com.devanshbhosale.channelhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterReq(
    @NotBlank(message = "Email is required") @Email(message = "Email must be a valid address") String email,
    @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
    @NotBlank(message = "Full name is required") String fullName,
    @NotBlank(message = "Company name is required") String companyName,
    String gstin,
    String phone) {
}
