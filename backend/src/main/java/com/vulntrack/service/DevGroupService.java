package com.vulntrack.service;

import com.vulntrack.dto.DevGroupDto;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.DevGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DevGroupService {

    private final DevGroupRepository repo;
    private final AuthorizationService authorizationService;
    private final com.vulntrack.repository.UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<DevGroupDto> findAll() {
        User actor = authorizationService.getCurrentUser();
        if (actor.getRole() != UserRole.ADMIN) {
            return List.of(DevGroupDto.from(actor.getDevGroup()));
        }
        return repo.findAll().stream().map(DevGroupDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DevGroupDto findById(Long id) {
        DevGroup devGroup = getOrThrow(id);
        authorizationService.requireCanViewDevGroup(devGroup);
        return DevGroupDto.withMembers(devGroup);
    }

    public DevGroupDto create(DevGroupDto.CreateRequest req) {
        if (repo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dev group name already exists");
        }
        DevGroup g = new DevGroup();
        g.setName(req.getName());
        g.setDescription(req.getDescription());
        DevGroup saved = repo.save(g);
        applyLeader(saved, req.getLeaderId());
        return DevGroupDto.from(repo.save(saved));
    }

    public DevGroupDto update(Long id, DevGroupDto.CreateRequest req) {
        DevGroup g = getOrThrow(id);
        repo.findByName(req.getName())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Dev group name already exists");
            });
        g.setName(req.getName());
        g.setDescription(req.getDescription());
        applyLeader(g, req.getLeaderId());
        return DevGroupDto.from(repo.save(g));
    }

    private void applyLeader(DevGroup g, Long leaderId) {
        User previous = g.getLeader();
        if (leaderId == null) {
            if (previous != null && previous.getRole() == UserRole.GROUP_LEAD) {
                previous.setRole(UserRole.DEVELOPER);
                userRepo.save(previous);
            }
            g.setLeader(null);
            return;
        }
        User leader = userRepo.findById(leaderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leader user not found: " + leaderId));
        if (leader.getDevGroup() == null || !leader.getDevGroup().getId().equals(g.getId())) {
            leader.setDevGroup(g);
        }
        repo.findByLeaderId(leader.getId())
            .filter(existingGroup -> !existingGroup.getId().equals(g.getId()))
            .ifPresent(existingGroup -> {
                existingGroup.setLeader(null);
                repo.save(existingGroup);
            });
        if (previous != null && !previous.getId().equals(leader.getId())
            && previous.getRole() == UserRole.GROUP_LEAD) {
            previous.setRole(UserRole.DEVELOPER);
            userRepo.save(previous);
        }
        if (leader.getRole() != UserRole.ADMIN && leader.getRole() != UserRole.GROUP_LEAD) {
            leader.setRole(UserRole.GROUP_LEAD);
        }
        userRepo.save(leader);
        g.setLeader(leader);
    }

    public void delete(Long id) {
        getOrThrow(id);
        repo.deleteById(id);
    }

    public DevGroup getOrThrow(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dev group not found: " + id));
    }
}
