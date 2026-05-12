package com.trucks_logistics.Trucks.Logistics.auth;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import com.trucks_logistics.Trucks.Logistics.exceptions.InvalidTokenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String generateToken(User user) {
        Instant now = Instant.now();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder().subject(
                user.getEmail())
                .issuedAt(now)
                .claim("role", user.getRole().name())
                .claim("user_status", user.getUserStatus().name())
                .expiresAt(now.plusMillis(expirationMs))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Instant expiresAt = jwt.getExpiresAt();
            return expiresAt != null && expiresAt.isAfter(Instant.now());
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getSubject();
        } catch (JwtException e) {
            throw new InvalidTokenException("Token invalido o expirado");
        }
    }

    public String extractRole(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("role");
        } catch (JwtException e) {
            throw new InvalidTokenException("Token invalido o expirado");
        }
    }

    public String extractUserStatus(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("user_status");
        } catch (JwtException e) {
            throw new InvalidTokenException("Token invalido o expirado");
        }
    }

    public Instant extractTokenExpireTime(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getExpiresAt();
        } catch (JwtException e) {
            throw new InvalidTokenException("Token invalido o expirado");
        }
    }

    public Map<String, Object> extractClaims(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaims();
        } catch (JwtException e) {
            throw new InvalidTokenException("Token invalido o expirado");
        }
    }
}
