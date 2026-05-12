package com.trucks_logistics.Trucks.Logistics.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @JsonProperty("email") @Email @NotBlank String email,
        @JsonProperty("password") @NotBlank String password) {
}