package com.vulntrack.entity;

import java.util.Set;
import java.util.EnumSet;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    IN_REVIEW,
    RESOLVED,
    CLOSED,
    WONT_FIX;

    public Set<TicketStatus> getAllowedTransitions() {
        return switch (this) {
            case OPEN        -> EnumSet.of(IN_PROGRESS, WONT_FIX);
            case IN_PROGRESS -> EnumSet.of(IN_REVIEW, WONT_FIX);
            case IN_REVIEW   -> EnumSet.of(RESOLVED, WONT_FIX);
            case RESOLVED    -> EnumSet.of(CLOSED, WONT_FIX);
            case CLOSED, WONT_FIX -> EnumSet.noneOf(TicketStatus.class);
        };
    }

    public boolean canTransitionTo(TicketStatus target) {
        return getAllowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == WONT_FIX;
    }
}
