package com.vulntrack.service;

import com.vulntrack.dto.AssetDto;
import com.vulntrack.dto.PageResponse;
import com.vulntrack.entity.Asset;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.AssetRepository;
import com.vulntrack.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetService {

    private final AssetRepository repo;
    private final DevGroupService devGroupService;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResponse<AssetDto> search(String q, int page, int size) {
        User actor = authorizationService.getCurrentUser();
        if (actor.getRole() != UserRole.ADMIN) {
            List<AssetDto> scopedAssets = repo.findByDevGroupId(actor.getDevGroup().getId()).stream()
                .filter(asset -> matchesSearch(asset, q))
                .map(this::toDtoWithOpenCount)
                .toList();
            return PageResponse.of(new PageImpl<>(
                paginate(scopedAssets, page, size),
                PageRequest.of(page, size),
                scopedAssets.size()
            ));
        }
        return PageResponse.of(
            repo.search(normalizeSearchPattern(q), PageRequest.of(page, size)).map(a -> {
                return toDtoWithOpenCount(a);
            })
        );
    }

    @Transactional(readOnly = true)
    public AssetDto findById(Long id) {
        Asset a = getOrThrow(id);
        authorizationService.requireCanViewAsset(a);
        return toDtoWithOpenCount(a);
    }

    public AssetDto create(AssetDto.CreateRequest req) {
        Asset a = new Asset();
        a.setName(req.getName());
        a.setDescription(req.getDescription());
        a.setRepoUrl(req.getRepoUrl());
        a.setProjectName(req.getProjectName());
        if (req.getDevGroupId() != null) {
            a.setDevGroup(devGroupService.getOrThrow(req.getDevGroupId()));
        }
        return AssetDto.from(repo.save(a));
    }

    public AssetDto update(Long id, AssetDto.CreateRequest req) {
        Asset a = getOrThrow(id);
        a.setName(req.getName());
        a.setDescription(req.getDescription());
        a.setRepoUrl(req.getRepoUrl());
        a.setProjectName(req.getProjectName());
        if (req.getDevGroupId() != null) {
            a.setDevGroup(devGroupService.getOrThrow(req.getDevGroupId()));
        } else {
            a.setDevGroup(null);
        }
        return AssetDto.from(repo.save(a));
    }

    public void delete(Long id) {
        getOrThrow(id);
        repo.deleteById(id);
    }

    public Asset getOrThrow(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + id));
    }

    private String normalizeSearchPattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private AssetDto toDtoWithOpenCount(Asset asset) {
        AssetDto dto = AssetDto.from(asset);
        dto.setOpenTicketCount(ticketRepository.findOpenByAssetId(asset.getId()).size());
        return dto;
    }

    private boolean matchesSearch(Asset asset, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String query = q.trim().toLowerCase(Locale.ROOT);
        return (asset.getName() != null && asset.getName().toLowerCase(Locale.ROOT).contains(query))
            || (asset.getProjectName() != null && asset.getProjectName().toLowerCase(Locale.ROOT).contains(query));
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
