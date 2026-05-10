package com.trucks_logistics.Trucks.Logistics.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.verification-url}")
    private String verificationUrl;

    private final PasswordEncoder passwordEncoder;
    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RateLimitService rateLimitService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserRegisterResponse createUser(UserRegisterRequest request) {
        if (authRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailDuplicated("El email ya está registrado");
        }

        // Crear usuario
        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(hashedPassword);
        user.setUsername(request.username());
        user.setRole(request.role());
        // Guardar user en DB
        authRepository.save(user);

        String verificationLink = generateVerificationLink(user);

        // Email luego de persistir en DB por si hay errores
        emailService.enviarCodigoVerificacion(user.getEmail(), verificationLink);
        return UserMapper.toDTO(user);
    }

    @Override
    public AuthTokenResponse loginUser(UserLoginRequest request) {
        User user = authRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (user.getUserStatus() == UserStatus.PENDIENTE) {
            throw new DisabledException("Debes verificar tu email antes de iniciar sesion");
        }

        if (user.getUserStatus() == UserStatus.DESACTIVADO) {
            throw new DisabledException("Tu cuenta ha sido desactivada, contacta al soporte");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.generateToken(user);
        return new AuthTokenResponse(accessToken, "Bearer", expirationMs / 1000, refreshToken.getToken());
    }

    @Override
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token invalido"));

        if (LocalDateTime.now().isAfter(verificationToken.getExpiresAt())) {
            verificationTokenRepository.delete(verificationToken);
            throw new InvalidTokenException("Token expirado, solicita uno nuevo");
        }

        // Obtener usuario del token de validacion y activar la cuenta
        User user = verificationToken.getUser();
        user.setUserStatus(UserStatus.ACTIVO);
        authRepository.save(user);

        // Eliminar el token de verificacion que se uso para evitar que se use otra vez
        verificationTokenRepository.delete(verificationToken);
    }

    @Override
    public void resendVerificationLink(String email) {

        if (!rateLimitService.allowResendVerification(email)) {
            throw new TooManyRequestException("Numero maximo de peticiones alcanzado!");
        }

        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario invalido"));

        if (user.getUserStatus() != UserStatus.PENDIENTE) {
            throw new InvalidUserException("Usuario no valido para esta peticion");
        }

        verificationTokenRepository.findByUserEmail(email)
                .ifPresent(verificationTokenRepository::delete);

        String verificationLink = generateVerificationLink(user);

        emailService.enviarCodigoVerificacion(user.getEmail(), verificationLink);
    }

    private String generateVerificationLink(User user) {
        // Crear token de verificacion
        String code = UUID.randomUUID().toString().replace("-", "");

        // Crear link de verificacion
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(code);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));

        // Guardar verification token en DB
        verificationTokenRepository.save(verificationToken);

        // Crear link de verificacion
        String verificationLink = verificationUrl + code;
        return verificationLink;
    }
}
