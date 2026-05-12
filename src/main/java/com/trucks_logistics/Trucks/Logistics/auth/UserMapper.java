package com.trucks_logistics.Trucks.Logistics.auth;

public class UserMapper {

    public static UserRegisterResponse toDTO(User user) {
        UserRegisterResponse response = new UserRegisterResponse(user.getUsername(), user.getEmail());
        return response;
    }
}
