package com.vulntrack.controller;

import com.vulntrack.dto.DevGroupDto;
import com.vulntrack.service.DevGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dev-groups")
@RequiredArgsConstructor
public class DevGroupController {

    private final DevGroupService devGroupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_LEAD')")
    public List<DevGroupDto> findAll() {
        return devGroupService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_LEAD')")
    public DevGroupDto getById(@PathVariable Long id) {
        return devGroupService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DevGroupDto create(@RequestBody DevGroupDto.CreateRequest req) {
        return devGroupService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DevGroupDto update(@PathVariable Long id, @RequestBody DevGroupDto.CreateRequest req) {
        return devGroupService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        devGroupService.delete(id);
    }
}
