package com.trucks_logistics.Trucks.Logistics.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @JsonProperty("email") @Email @NotBlank String email,

        @JsonProperty("username") @NotBlank @Size(min = 3, max = 25) String username,

        @JsonProperty("password") @NotBlank @Size(min = 8) String password,
        @JsonProperty("role") @NotNull(message = "El rol es obligatorio y debe ser ROLE_ADMIN, ROLE_DRIVER o ROLE_DISPATCHER") UserRole role) {
}
