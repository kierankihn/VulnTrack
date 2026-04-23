package com.vulntrack.dto;

import com.vulntrack.entity.DevGroup;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DevGroupDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private int memberCount;
    private int assetCount;
    private Long leaderId;
    private String leaderName;
    private List<UserDto> members;

    public static DevGroupDto from(DevGroup g) {
        DevGroupDto dto = new DevGroupDto();
        dto.setId(g.getId());
        dto.setName(g.getName());
        dto.setDescription(g.getDescription());
        dto.setCreatedAt(g.getCreatedAt());
        dto.setMemberCount(g.getMembers() != null ? g.getMembers().size() : 0);
        dto.setAssetCount(g.getAssets() != null ? g.getAssets().size() : 0);
        if (g.getLeader() != null) {
            dto.setLeaderId(g.getLeader().getId());
            dto.setLeaderName(g.getLeader().getFullName());
        }
        return dto;
    }

    public static DevGroupDto withMembers(DevGroup g) {
        DevGroupDto dto = from(g);
        if (g.getMembers() != null) {
            dto.setMembers(g.getMembers().stream().map(UserDto::from).toList());
        }
        return dto;
    }

    @Data
    public static class CreateRequest {
        private String name;
        private String description;
        private Long leaderId;
    }
}
