package com.vulntrack.dto;

import com.vulntrack.entity.Asset;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetDto {
    private Long id;
    private String name;
    private String description;
    private String repoUrl;
    private String projectName;
    private Long devGroupId;
    private String devGroupName;
    private LocalDateTime createdAt;
    private int openTicketCount;

    public static AssetDto from(Asset a) {
        AssetDto dto = new AssetDto();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setDescription(a.getDescription());
        dto.setRepoUrl(a.getRepoUrl());
        dto.setProjectName(a.getProjectName());
        dto.setCreatedAt(a.getCreatedAt());
        if (a.getDevGroup() != null) {
            dto.setDevGroupId(a.getDevGroup().getId());
            dto.setDevGroupName(a.getDevGroup().getName());
        }
        return dto;
    }

    @Data
    public static class CreateRequest {
        private String name;
        private String description;
        private String repoUrl;
        private String projectName;
        private Long devGroupId;
    }
}
