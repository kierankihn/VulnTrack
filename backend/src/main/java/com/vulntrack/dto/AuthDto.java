package com.vulntrack.dto;

import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank
        private String usernameOrEmail;
        @NotBlank
        private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class LogoutRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        private String newPassword;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String newPassword;
    }

    @Data
    public static class AuthenticatedUserResponse {
        private Long id;
        private String username;
        private String fullName;
        private UserRole role;
        private Long devGroupId;
        private String devGroupName;
        private Boolean active;

        public static AuthenticatedUserResponse from(User user) {
            AuthenticatedUserResponse response = new AuthenticatedUserResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setFullName(user.getFullName());
            response.setRole(user.getRole());
            response.setActive(user.getActive());
            if (user.getDevGroup() != null) {
                response.setDevGroupId(user.getDevGroup().getId());
                response.setDevGroupName(user.getDevGroup().getName());
            }
            return response;
        }
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;
        private long refreshExpiresIn;
        private AuthenticatedUserResponse user;
    }
}
