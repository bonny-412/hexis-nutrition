package com.hexisnutrition.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/password-dimenticata")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void passwordDimenticata(@Valid @RequestBody PasswordDimenticataRequest request) {
        authService.richiediResetPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.nuovaPassword());
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UUID id = (UUID) authentication.getPrincipal();
        Ruolo ruolo = Ruolo.valueOf(authentication.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", ""));
        return authService.me(id, ruolo);
    }
}
