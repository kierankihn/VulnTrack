package com.vulntrack.controller;

import com.vulntrack.dto.AuthDto;
import com.vulntrack.dto.PageResponse;
import com.vulntrack.dto.UserDto;
import jakarta.validation.Valid;
import com.vulntrack.entity.UserRole;
import com.vulntrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) UserRole role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return userService.search(q, role, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto getById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody UserDto.CreateRequest req) {
        return userService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto update(@PathVariable Long id, @RequestBody UserDto.CreateRequest req) {
        return userService.update(id, req);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto setActive(@PathVariable Long id, @Valid @RequestBody UserDto.ActiveRequest req) {
        return userService.setActive(id, req.getActive());
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody AuthDto.ResetPasswordRequest req) {
        userService.resetPassword(id, req.getNewPassword());
    }

    @GetMapping("/assignable")
    public PageResponse<UserDto> assignable() {
        return userService.listAssignable();
    }
}
