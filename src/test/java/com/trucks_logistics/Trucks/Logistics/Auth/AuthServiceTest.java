package com.trucks_logistics.Trucks.Logistics.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.trucks_logistics.Trucks.Logistics.auth.AuthRepository;
import com.trucks_logistics.Trucks.Logistics.auth.AuthService;
import com.trucks_logistics.Trucks.Logistics.auth.AuthTokenResponse;
import com.trucks_logistics.Trucks.Logistics.auth.JwtService;
import com.trucks_logistics.Trucks.Logistics.auth.User;
import com.trucks_logistics.Trucks.Logistics.auth.UserLoginRequest;
import com.trucks_logistics.Trucks.Logistics.auth.UserRegisterRequest;
import com.trucks_logistics.Trucks.Logistics.auth.UserRegisterResponse;
import com.trucks_logistics.Trucks.Logistics.auth.UserRole;
import com.trucks_logistics.Trucks.Logistics.auth.UserStatus;
import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshToken;
import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshTokenService;
import com.trucks_logistics.Trucks.Logistics.auth.token.VerificationToken;
import com.trucks_logistics.Trucks.Logistics.auth.token.VerificationTokenRepository;
import com.trucks_logistics.Trucks.Logistics.exceptions.EmailDuplicated;
import com.trucks_logistics.Trucks.Logistics.exceptions.InvalidTokenException;
import com.trucks_logistics.Trucks.Logistics.exceptions.InvalidUserException;
import com.trucks_logistics.Trucks.Logistics.exceptions.TooManyRequestException;
import com.trucks_logistics.Trucks.Logistics.infra.email.EmailService;
import com.trucks_logistics.Trucks.Logistics.infra.ratelimit.RateLimitService;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthRepository authRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationMs", 900000L);
        ReflectionTestUtils.setField(authService, "verificationUrl", "http://localhost:8080/api/auth/verify?token=");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User buildUser(UserStatus status) {
        User user = new User();
        user.setEmail("test@trazo.com");
        user.setPassword("hashedPassword");
        user.setUsername("testuser");
        user.setRole(UserRole.ROLE_DRIVER);
        user.setUserStatus(status);
        return user;
    }

    private VerificationToken buildVerificationToken(User user, LocalDateTime expiresAt) {
        VerificationToken token = new VerificationToken();
        token.setToken("validtoken123");
        token.setUser(user);
        token.setExpiresAt(expiresAt);
        return token;
    }

    // ─── createUser ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("Debe registrar un usuario exitosamente")
        void shouldRegisterUserSuccessfully() {
            UserRegisterRequest request = new UserRegisterRequest(
                    "test@trazo.com", "password123", "testuser", UserRole.ROLE_DRIVER);

            when(authRepository.findByEmail(request.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
            when(authRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(verificationTokenRepository.save(any(VerificationToken.class)))
                    .thenAnswer(i -> i.getArgument(0));

            UserRegisterResponse response = authService.createUser(request);

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(request.email());
            verify(emailService).enviarCodigoVerificacion(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe lanzar EmailDuplicated si el email ya existe")
        void shouldThrowEmailDuplicatedWhenEmailExists() {
            UserRegisterRequest request = new UserRegisterRequest(
                    "test@trazo.com", "password123", "testuser", UserRole.ROLE_DRIVER);

            when(authRepository.findByEmail(request.email()))
                    .thenReturn(Optional.of(buildUser(UserStatus.ACTIVO)));

            assertThatThrownBy(() -> authService.createUser(request))
                    .isInstanceOf(EmailDuplicated.class)
                    .hasMessage("El email ya está registrado");

            verify(authRepository, never()).save(any());
            verify(emailService, never()).enviarCodigoVerificacion(anyString(), anyString());
        }
    }

    // ─── loginUser ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginUser")
    class LoginUser {

        @Test
        @DisplayName("Debe hacer login exitosamente")
        void shouldLoginSuccessfully() {
            User user = buildUser(UserStatus.ACTIVO);
            UserLoginRequest request = new UserLoginRequest("test@trazo.com", "password123");
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("refresh_token_value");

            when(authRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("access_token_value");
            when(refreshTokenService.generateToken(user)).thenReturn(refreshToken);

            AuthTokenResponse response = authService.loginUser(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access_token_value");
            assertThat(response.refreshToken()).isEqualTo("refresh_token_value");
            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Debe lanzar BadCredentialsException si el email no existe")
        void shouldThrowBadCredentialsWhenEmailNotFound() {
            UserLoginRequest request = new UserLoginRequest("noexiste@trazo.com", "password123");

            when(authRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.loginUser(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Credenciales invalidas");
        }

        @Test
        @DisplayName("Debe lanzar DisabledException si el usuario está PENDIENTE")
        void shouldThrowDisabledExceptionWhenUserIsPending() {
            User user = buildUser(UserStatus.PENDIENTE);
            UserLoginRequest request = new UserLoginRequest("test@trazo.com", "password123");

            when(authRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.loginUser(request))
                    .isInstanceOf(DisabledException.class)
                    .hasMessage("Debes verificar tu email antes de iniciar sesion");
        }

        @Test
        @DisplayName("Debe lanzar DisabledException si el usuario está DESACTIVADO")
        void shouldThrowDisabledExceptionWhenUserIsDeactivated() {
            User user = buildUser(UserStatus.DESACTIVADO);
            UserLoginRequest request = new UserLoginRequest("test@trazo.com", "password123");

            when(authRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.loginUser(request))
                    .isInstanceOf(DisabledException.class)
                    .hasMessage("Tu cuenta ha sido desactivada, contacta al soporte");
        }

        @Test
        @DisplayName("Debe lanzar BadCredentialsException si la contraseña es incorrecta")
        void shouldThrowBadCredentialsWhenPasswordIsWrong() {
            User user = buildUser(UserStatus.ACTIVO);
            UserLoginRequest request = new UserLoginRequest("test@trazo.com", "wrongpassword");

            when(authRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.loginUser(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Credenciales invalidas");
        }
    }

    // ─── verifyEmail ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("Debe verificar el email exitosamente")
        void shouldVerifyEmailSuccessfully() {
            User user = buildUser(UserStatus.PENDIENTE);
            VerificationToken token = buildVerificationToken(user, LocalDateTime.now().plusHours(1));

            when(verificationTokenRepository.findByToken("validtoken123"))
                    .thenReturn(Optional.of(token));
            when(authRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            authService.verifyEmail("validtoken123");

            assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVO);
            verify(verificationTokenRepository).delete(token);
        }

        @Test
        @DisplayName("Debe lanzar InvalidTokenException si el token no existe")
        void shouldThrowInvalidTokenWhenTokenNotFound() {
            when(verificationTokenRepository.findByToken("invalidtoken"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("invalidtoken"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Token invalido");
        }

        @Test
        @DisplayName("Debe lanzar InvalidTokenException si el token está expirado")
        void shouldThrowInvalidTokenWhenTokenIsExpired() {
            User user = buildUser(UserStatus.PENDIENTE);
            VerificationToken token = buildVerificationToken(user, LocalDateTime.now().minusHours(1));

            when(verificationTokenRepository.findByToken("expiredtoken"))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyEmail("expiredtoken"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Token expirado, solicita uno nuevo");

            verify(verificationTokenRepository).delete(token);
        }
    }

    // ─── resendVerificationLink ──────────────────────────────────────────────────

    @Nested
    @DisplayName("resendVerificationLink")
    class ResendVerificationLink {

        @Test
        @DisplayName("Debe reenviar el link exitosamente")
        void shouldResendVerificationLinkSuccessfully() {
            User user = buildUser(UserStatus.PENDIENTE);

            when(rateLimitService.allowResendVerification("test@trazo.com")).thenReturn(true);
            when(authRepository.findByEmail("test@trazo.com")).thenReturn(Optional.of(user));
            when(verificationTokenRepository.findByUserEmail("test@trazo.com"))
                    .thenReturn(Optional.empty());
            when(verificationTokenRepository.save(any(VerificationToken.class)))
                    .thenAnswer(i -> i.getArgument(0));

            authService.resendVerificationLink("test@trazo.com");

            verify(emailService).enviarCodigoVerificacion(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe lanzar TooManyRequestException si se excede el rate limit")
        void shouldThrowTooManyRequestsWhenRateLimitExceeded() {
            when(rateLimitService.allowResendVerification("test@trazo.com")).thenReturn(false);

            assertThatThrownBy(() -> authService.resendVerificationLink("test@trazo.com"))
                    .isInstanceOf(TooManyRequestException.class)
                    .hasMessage("Numero maximo de peticiones alcanzado!");

            verify(authRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el usuario no existe")
        void shouldThrowEntityNotFoundWhenUserNotFound() {
            when(rateLimitService.allowResendVerification("noexiste@trazo.com")).thenReturn(true);
            when(authRepository.findByEmail("noexiste@trazo.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendVerificationLink("noexiste@trazo.com"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Usuario invalido");
        }

        @Test
        @DisplayName("Debe lanzar InvalidUserException si el usuario no está PENDIENTE")
        void shouldThrowInvalidUserWhenUserIsNotPending() {
            User user = buildUser(UserStatus.ACTIVO);

            when(rateLimitService.allowResendVerification("test@trazo.com")).thenReturn(true);
            when(authRepository.findByEmail("test@trazo.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resendVerificationLink("test@trazo.com"))
                    .isInstanceOf(InvalidUserException.class)
                    .hasMessage("Usuario no valido para esta peticion");
        }
    }
}