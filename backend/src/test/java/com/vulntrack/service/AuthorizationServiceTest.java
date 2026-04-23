package com.vulntrack.service;

import com.vulntrack.entity.Asset;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AuthorizationServiceTest {

    @Test
    void groupLeadCanViewTicketInOwnGroup() {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthenticatedUserProvider.class));

        DevGroup group = new DevGroup();
        group.setId(1L);

        User lead = new User();
        lead.setId(10L);
        lead.setRole(UserRole.GROUP_LEAD);
        lead.setDevGroup(group);

        Asset asset = new Asset();
        asset.setId(20L);
        asset.setDevGroup(group);

        Ticket ticket = new Ticket();
        ticket.setId(30L);
        ticket.setAsset(asset);

        assertTrue(authorizationService.canViewTicket(lead, ticket));
    }

    @Test
    void testerCannotViewTicketReportedBySomeoneElse() {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthenticatedUserProvider.class));

        User tester = new User();
        tester.setId(10L);
        tester.setRole(UserRole.TESTER);

        User anotherReporter = new User();
        anotherReporter.setId(11L);

        Ticket ticket = new Ticket();
        ticket.setId(30L);
        ticket.setReporter(anotherReporter);

        assertFalse(authorizationService.canViewTicket(tester, ticket));
    }
}
