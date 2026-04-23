package com.vulntrack.controller;

import com.vulntrack.dto.AuthDto;
import com.vulntrack.security.AuthenticatedUserProvider;
import com.vulntrack.service.AuthService;
import com.vulntrack.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @PostMapping("/login")
    public AuthDto.TokenResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthDto.TokenResponse refresh(@Valid @RequestBody AuthDto.RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthDto.LogoutRequest req) {
        authService.logout(req);
    }

    @GetMapping("/me")
    public AuthDto.AuthenticatedUserResponse me() {
        return authService.me();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody AuthDto.ChangePasswordRequest req) {
        userService.changeOwnPassword(
            authenticatedUserProvider.getCurrentUserOrThrow(),
            req.getCurrentPassword(),
            req.getNewPassword()
        );
    }
}
