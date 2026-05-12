package com.trucks_logistics.Trucks.Logistics.auth.token;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @JsonProperty("refresh_token") @NotBlank String refreshToken) {

}
