package com.vulntrack.service;

import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.CveRepository;
import com.vulntrack.repository.TicketRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CveServiceTest {

    @Test
    void nonAdminCannotReadCveWithoutVisibleTicket() {
        CveRepository cveRepository = mock(CveRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        CveService cveService = new CveService(cveRepository, ticketRepository, new AuthorizationService(authenticatedUserProvider));

        User tester = new User();
        tester.setId(10L);
        tester.setRole(UserRole.TESTER);
        tester.setActive(true);

        User anotherReporter = new User();
        anotherReporter.setId(11L);

        Ticket hiddenTicket = new Ticket();
        hiddenTicket.setId(20L);
        hiddenTicket.setReporter(anotherReporter);

        CveEntry cveEntry = new CveEntry();
        cveEntry.setId(1L);
        cveEntry.setCveId("CVE-2026-0001");

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(tester);
        when(cveRepository.findById(1L)).thenReturn(Optional.of(cveEntry));
        when(ticketRepository.findByCveId(1L)).thenReturn(List.of(hiddenTicket));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cveService.findById(1L));

        assertEquals(404, ex.getStatusCode().value());
    }
}
