package com.trucks_logistics.Trucks.Logistics.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.trucks_logistics.Trucks.Logistics.auth.AuthTokenResponse;
import com.trucks_logistics.Trucks.Logistics.auth.JwtService;
import com.trucks_logistics.Trucks.Logistics.auth.User;
import com.trucks_logistics.Trucks.Logistics.auth.UserRole;
import com.trucks_logistics.Trucks.Logistics.auth.UserStatus;
import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshToken;
import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshTokenRepository;
import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshTokenService;
import com.trucks_logistics.Trucks.Logistics.exceptions.InvalidTokenException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

        @Mock
        private RefreshTokenRepository refreshTokenRepository;
        @Mock
        private JwtService jwtService;

        @InjectMocks
        private RefreshTokenService refreshTokenService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(refreshTokenService, "expirationMs", 900000L);
        }

        // ─── Helpers ────────────────────────────────────────────────────────────────

        private User buildUser() {
                User user = new User();
                user.setEmail("test@trazo.com");
                user.setPassword("hashedPassword");
                user.setUsername("testuser");
                user.setRole(UserRole.ROLE_DRIVER);
                user.setUserStatus(UserStatus.ACTIVO);
                return user;
        }

        private RefreshToken buildToken(User user, boolean used, Instant expiresAt) {
                RefreshToken token = new RefreshToken();
                token.setToken("refresh_token_value");
                token.setUser(user);
                token.setUsed(used);
                token.setExpiresAt(expiresAt);
                return token;
        }

        // ─── generateToken ──────────────────────────────────────────────────────────

        @Nested
        @DisplayName("generateToken")
        class GenerateToken {

                @Test
                @DisplayName("Debe generar y persistir un refresh token exitosamente")
                void shouldGenerateTokenSuccessfully() {
                        User user = buildUser();
                        when(refreshTokenRepository.save(any(RefreshToken.class)))
                                        .thenAnswer(i -> i.getArgument(0));

                        RefreshToken result = refreshTokenService.generateToken(user);

                        assertThat(result).isNotNull();
                        assertThat(result.getToken()).isNotBlank();
                        assertThat(result.getUser()).isEqualTo(user);
                        assertThat(result.isUsed()).isFalse();
                        assertThat(result.getExpiresAt()).isAfter(Instant.now());
                        verify(refreshTokenRepository).save(any(RefreshToken.class));
                }
        }

        // ─── refreshToken ───────────────────────────────────────────────────────────

        @Nested
        @DisplayName("refreshToken")
        class RefreshTokenTest {

                @Test
                @DisplayName("Debe rotar el token y devolver nuevo par exitosamente")
                void shouldRefreshTokenSuccessfully() {
                        User user = buildUser();
                        RefreshToken existingToken = buildToken(
                                        user, false, Instant.now().plus(30, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("refresh_token_value"))
                                        .thenReturn(Optional.of(existingToken));
                        when(refreshTokenRepository.save(any(RefreshToken.class)))
                                        .thenAnswer(i -> i.getArgument(0));
                        when(jwtService.generateToken(user)).thenReturn("new_access_token");

                        AuthTokenResponse response = refreshTokenService.refreshToken("refresh_token_value");

                        assertThat(response).isNotNull();
                        assertThat(response.accessToken()).isEqualTo("new_access_token");
                        assertThat(response.refreshToken()).isNotBlank();
                        assertThat(response.tokenType()).isEqualTo("Bearer");
                        assertThat(existingToken.isUsed()).isTrue();
                }

                @Test
                @DisplayName("Debe lanzar InvalidTokenException si el token no existe")
                void shouldThrowInvalidTokenWhenTokenNotFound() {
                        when(refreshTokenRepository.findByToken("invalid_token"))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> refreshTokenService.refreshToken("invalid_token"))
                                        .isInstanceOf(InvalidTokenException.class)
                                        .hasMessage("Token invalido");
                }

                @Test
                @DisplayName("Debe invalidar toda la sesion si el token ya fue usado — reuse detection")
                void shouldInvalidateSessionWhenTokenReused() {
                        User user = buildUser();
                        RefreshToken usedToken = buildToken(
                                        user, true, Instant.now().plus(30, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("used_token"))
                                        .thenReturn(Optional.of(usedToken));

                        assertThatThrownBy(() -> refreshTokenService.refreshToken("used_token"))
                                        .isInstanceOf(InvalidTokenException.class)
                                        .hasMessage("Sesion invalidada por seguridad, inicia sesion nuevamente");

                        verify(refreshTokenRepository).deleteAllByUser(user);
                }

                @Test
                @DisplayName("Debe lanzar InvalidTokenException si el token está expirado")
                void shouldThrowInvalidTokenWhenTokenIsExpired() {
                        User user = buildUser();
                        RefreshToken expiredToken = buildToken(
                                        user, false, Instant.now().minus(1, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("expired_token"))
                                        .thenReturn(Optional.of(expiredToken));

                        assertThatThrownBy(() -> refreshTokenService.refreshToken("expired_token"))
                                        .isInstanceOf(InvalidTokenException.class)
                                        .hasMessage("Token expirado");

                        verify(refreshTokenRepository).delete(expiredToken);
                        verify(refreshTokenRepository, never()).save(any());
                }
        }

        // ─── revoke ─────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("revoke")
        class Revoke {

                @Test
                @DisplayName("Debe revocar todos los tokens del usuario exitosamente")
                void shouldRevokeSuccessfully() {
                        User user = buildUser();
                        RefreshToken token = buildToken(
                                        user, false, Instant.now().plus(30, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("refresh_token_value"))
                                        .thenReturn(Optional.of(token));

                        refreshTokenService.revoke("refresh_token_value", "test@trazo.com");

                        verify(refreshTokenRepository).deleteAllByUser(user);
                }

                @Test
                @DisplayName("Debe lanzar InvalidTokenException si el token no existe")
                void shouldThrowInvalidTokenWhenTokenNotFound() {
                        when(refreshTokenRepository.findByToken("invalid_token"))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> refreshTokenService.revoke("invalid_token", "test@trazo.com"))
                                        .isInstanceOf(InvalidTokenException.class)
                                        .hasMessage("Refresh token invalido");
                }

                @Test
                @DisplayName("Debe lanzar AccessDeniedException si el token no pertenece al usuario")
                void shouldThrowAccessDeniedWhenTokenDoesNotBelongToUser() {
                        User user = buildUser();
                        RefreshToken token = buildToken(
                                        user, false, Instant.now().plus(30, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("refresh_token_value"))
                                        .thenReturn(Optional.of(token));

                        assertThatThrownBy(() -> refreshTokenService.revoke("refresh_token_value", "otro@trazo.com"))
                                        .isInstanceOf(AccessDeniedException.class)
                                        .hasMessage("No tienes permiso para revocar esta sesion");

                        verify(refreshTokenRepository, never()).deleteAllByUser(any());
                }

                @Test
                @DisplayName("Debe lanzar InvalidTokenException si el token ya fue usado")
                void shouldThrowInvalidTokenWhenTokenAlreadyUsed() {
                        User user = buildUser();
                        RefreshToken token = buildToken(
                                        user, true, Instant.now().plus(30, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("used_token"))
                                        .thenReturn(Optional.of(token));

                        assertThatThrownBy(() -> refreshTokenService.revoke("used_token", "test@trazo.com"))
                                        .isInstanceOf(InvalidTokenException.class)
                                        .hasMessage("Refresh token ya fue utilizado");

                        verify(refreshTokenRepository, never()).deleteAllByUser(any());
                }

                @Test
                @DisplayName("Debe lanzar InvalidTokenException y eliminar el token si está expirado")
                void shouldThrowInvalidTokenAndDeleteWhenTokenIsExpired() {
                        User user = buildUser();
                        RefreshToken token = buildToken(
                                        user, false, Instant.now().minus(1, ChronoUnit.DAYS));

                        when(refreshTokenRepository.findByToken("expired_token"))
                                        .thenReturn(Optional.of(token));

                        assertThatThrownBy(() -> refreshTokenService.revoke("expired_token", "test@trazo.com"))
                                        .isInstanceOf(InvalidTokenException.class)
                                        .hasMessage("Refresh token expirado");

                        verify(refreshTokenRepository).delete(token);
                        verify(refreshTokenRepository, never()).deleteAllByUser(any());
                }
        }
}