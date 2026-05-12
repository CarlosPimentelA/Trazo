package com.trucks_logistics.Trucks.Logistics.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshTokenRequest;
import com.trucks_logistics.Trucks.Logistics.auth.token.RefreshTokenService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Endpoints de autenticacion de usuario")
public class AuthController {

    private final AuthService authService;

    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegisterResponse> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.ok(authService.createUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> loginUser(@Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Cuenta verificada exitosamente");
    }

    @PostMapping("/verify/resend-verification")
    public ResponseEntity<String> resendVerificationLink(@Valid @RequestBody ResendLinkRequest request) {
        authService.resendVerificationLink(request.email());
        return ResponseEntity.ok("Link reenviado exitosamente");
    }

    @GetMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refreshSession(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.refreshToken(request.refreshToken()));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/revoke-all")
    public ResponseEntity<String> logoutAll(@Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        refreshTokenService.revoke(request.refreshToken(), jwt.getSubject());
        return ResponseEntity.ok("Token revocados con exito");
    }
}
