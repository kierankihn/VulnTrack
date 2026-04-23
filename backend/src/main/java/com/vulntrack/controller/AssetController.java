package com.vulntrack.controller;

import com.vulntrack.dto.AssetDto;
import com.vulntrack.dto.PageResponse;
import com.vulntrack.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<AssetDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return assetService.search(q, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AssetDto getById(@PathVariable Long id) {
        return assetService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetDto create(@RequestBody AssetDto.CreateRequest req) {
        return assetService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AssetDto update(@PathVariable Long id, @RequestBody AssetDto.CreateRequest req) {
        return assetService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        assetService.delete(id);
    }
}
