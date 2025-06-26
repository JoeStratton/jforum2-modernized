package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.*;
import net.jforum.service.AuthService;
import net.jforum.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthenticationResponseDto> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registrationDto));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user")
    public ResponseEntity<AuthenticationResponseDto> authenticate(@Valid @RequestBody AuthenticationRequestDto authRequest) {
        return ResponseEntity.ok(authService.authenticate(authRequest));
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "Request a password reset")
    public ResponseEntity<ApiResponseDto<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto resetRequest) {
        userService.resetPassword(resetRequest.getEmail());
        return ResponseEntity.ok(ApiResponseDto.success("Password reset email sent"));
    }

    @PostMapping("/password/reset-confirm")
    @Operation(summary = "Confirm a password reset")
    public ResponseEntity<ApiResponseDto<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDto resetConfirm) {
        userService.confirmPasswordReset(resetConfirm.getToken(), resetConfirm.getNewPassword());
        return ResponseEntity.ok(ApiResponseDto.success("Password has been reset successfully"));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<ApiResponseDto<Void>> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponseDto.success("Email verified successfully"));
    }
}