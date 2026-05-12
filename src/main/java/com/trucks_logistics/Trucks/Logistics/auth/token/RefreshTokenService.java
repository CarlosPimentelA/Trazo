package com.trucks_logistics.Trucks.Logistics.auth.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trucks_logistics.Trucks.Logistics.auth.AuthTokenResponse;
import com.trucks_logistics.Trucks.Logistics.auth.JwtService;
import com.trucks_logistics.Trucks.Logistics.auth.User;
import com.trucks_logistics.Trucks.Logistics.exceptions.InvalidTokenException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RefreshTokenService implements IRefreshToken {
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    public RefreshToken generateToken(User user) {
        RefreshToken token = createToken(user);
        refreshTokenRepository.save(token);
        return token;
    }

    @Override
    public AuthTokenResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Token invalido"));

        if (token.isUsed()) {
            refreshTokenRepository.deleteAllByUser(token.getUser());
            throw new InvalidTokenException("Sesion invalidada por seguridad, inicia sesion nuevamente");
        }

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Token expirado");
        }

        token.setUsed(true);
        refreshTokenRepository.save(token);

        RefreshToken newToken = createToken(token.getUser());

        String accessToken = jwtService.generateToken(newToken.getUser());
        refreshTokenRepository.save(newToken);

        return new AuthTokenResponse(accessToken, "Bearer", expirationMs / 1000, newToken.getToken());
    }

    private RefreshToken createToken(User user) {
        RefreshToken token = new RefreshToken();

        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        return token;
    }

    @Override
    @Transactional
    public void revoke(String refreshToken, String email) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalido"));

        if (!token.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("No tienes permiso para revocar esta sesion");
        }

        if (token.isUsed()) {
            throw new InvalidTokenException("Refresh token ya fue utilizado");
        }

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token expirado");
        }

        refreshTokenRepository.deleteAllByUser(token.getUser());
    }
}
