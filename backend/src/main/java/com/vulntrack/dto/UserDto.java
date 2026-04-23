package com.vulntrack.dto;

import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
    private Boolean active;
    private Long devGroupId;
    private String devGroupName;
    private LocalDateTime createdAt;

    public static UserDto from(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFullName(u.getFullName());
        dto.setRole(u.getRole());
        dto.setActive(u.getActive());
        dto.setCreatedAt(u.getCreatedAt());
        if (u.getDevGroup() != null) {
            dto.setDevGroupId(u.getDevGroup().getId());
            dto.setDevGroupName(u.getDevGroup().getName());
        }
        return dto;
    }

    @Data
    public static class CreateRequest {
        @NotBlank
        private String username;
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String fullName;
        private UserRole role;
        private Long devGroupId;
        private String password;
    }

    @Data
    public static class ActiveRequest {
        @NotNull
        private Boolean active;
    }
}
