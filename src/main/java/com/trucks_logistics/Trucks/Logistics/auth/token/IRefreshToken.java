package com.trucks_logistics.Trucks.Logistics.auth.token;

import com.trucks_logistics.Trucks.Logistics.auth.AuthTokenResponse;
import com.trucks_logistics.Trucks.Logistics.auth.User;

public interface IRefreshToken {
    RefreshToken generateToken(User user);

    AuthTokenResponse refreshToken(String token);

    void revoke(String token, String email);
}
