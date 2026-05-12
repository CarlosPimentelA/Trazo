package com.trucks_logistics.Trucks.Logistics.auth.token;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trucks_logistics.Trucks.Logistics.auth.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUser(User user);
}
