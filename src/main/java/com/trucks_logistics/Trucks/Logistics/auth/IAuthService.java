package com.trucks_logistics.Trucks.Logistics.auth;

public interface IAuthService {
    UserRegisterResponse createUser(UserRegisterRequest request);

    AuthTokenResponse loginUser(UserLoginRequest request);

    void verifyEmail(String token);

    void resendVerificationLink(String email);
}
