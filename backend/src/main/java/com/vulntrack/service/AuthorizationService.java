package com.vulntrack.service;

import com.vulntrack.entity.Asset;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.TicketStatus;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Set;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public User getCurrentUser() {
        return authenticatedUserProvider.getCurrentUserOrThrow();
    }

    public boolean canViewTicket(User actor, Ticket ticket) {
        return switch (actor.getRole()) {
            case ADMIN -> true;
            case GROUP_LEAD -> sameGroup(actor, ticket.getAsset());
            case DEVELOPER -> ticket.getAssignee() != null && Objects.equals(actor.getId(), ticket.getAssignee().getId());
            case TESTER -> ticket.getReporter() != null && Objects.equals(actor.getId(), ticket.getReporter().getId());
        };
    }

    public boolean canViewAsset(User actor, Asset asset) {
        if (actor.getRole() == UserRole.ADMIN) {
            return true;
        }
        return sameGroup(actor, asset);
    }

    public boolean canViewDevGroup(User actor, DevGroup devGroup) {
        if (actor.getRole() == UserRole.ADMIN) {
            return true;
        }
        return actor.getDevGroup() != null && Objects.equals(actor.getDevGroup().getId(), devGroup.getId());
    }

    public void requireCanCreateTicket(Asset asset) {
        User actor = getCurrentUser();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() != UserRole.TESTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot create tickets");
        }
        if (!canViewAsset(actor, asset)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot create tickets for this asset");
        }
    }

    public void requireCanAssignTicket(Ticket ticket, User assignee) {
        User actor = getCurrentUser();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() != UserRole.GROUP_LEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot assign tickets");
        }
        if (!sameGroup(actor, ticket.getAsset())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot assign tickets outside own group");
        }
        if (!Boolean.TRUE.equals(assignee.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignee must be active");
        }
        if (assignee.getDevGroup() == null || actor.getDevGroup() == null
            || !Objects.equals(actor.getDevGroup().getId(), assignee.getDevGroup().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignee must belong to the same development group");
        }
        if (assignee.getRole() != UserRole.DEVELOPER && assignee.getRole() != UserRole.GROUP_LEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignee role is not assignable");
        }
    }

    public Set<TicketStatus> allowedTransitions(User actor, Ticket ticket) {
        EnumSet<TicketStatus> allowed = EnumSet.noneOf(TicketStatus.class);
        for (TicketStatus target : ticket.getStatus().getAllowedTransitions()) {
            if (canTransitionTicket(actor, ticket, target)) {
                allowed.add(target);
            }
        }
        return allowed;
    }

    public Set<TicketStatus> allowedTransitions(Ticket ticket) {
        return allowedTransitions(getCurrentUser(), ticket);
    }

    public void requireCanTransitionTicket(Ticket ticket, TicketStatus target) {
        User actor = getCurrentUser();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.GROUP_LEAD && sameGroup(actor, ticket.getAsset())) {
            return;
        }
        if (actor.getRole() == UserRole.DEVELOPER
            && ticket.getAssignee() != null
            && Objects.equals(actor.getId(), ticket.getAssignee().getId())) {
            if (target == TicketStatus.RESOLVED || target == TicketStatus.CLOSED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Developers cannot mark tickets resolved or closed; submit for review instead");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot transition this ticket");
    }

    public boolean canTransitionTicket(User actor, Ticket ticket, TicketStatus target) {
        if (actor.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (actor.getRole() == UserRole.GROUP_LEAD && sameGroup(actor, ticket.getAsset())) {
            return true;
        }
        return actor.getRole() == UserRole.DEVELOPER
            && ticket.getAssignee() != null
            && Objects.equals(actor.getId(), ticket.getAssignee().getId())
            && target != TicketStatus.RESOLVED
            && target != TicketStatus.CLOSED;
    }

    public void requireCanViewTicket(Ticket ticket) {
        if (!canViewTicket(getCurrentUser(), ticket)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticket.getId());
        }
    }

    public void requireCanViewAsset(Asset asset) {
        if (!canViewAsset(getCurrentUser(), asset)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + asset.getId());
        }
    }

    public void requireCanViewDevGroup(DevGroup devGroup) {
        if (!canViewDevGroup(getCurrentUser(), devGroup)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dev group not found: " + devGroup.getId());
        }
    }

    private boolean sameGroup(User actor, Asset asset) {
        return actor.getDevGroup() != null
            && asset != null
            && asset.getDevGroup() != null
            && Objects.equals(actor.getDevGroup().getId(), asset.getDevGroup().getId());
    }
}
