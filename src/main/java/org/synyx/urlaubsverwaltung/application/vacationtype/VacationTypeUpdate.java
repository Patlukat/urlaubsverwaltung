package org.synyx.urlaubsverwaltung.application.vacationtype;

public class VacationTypeUpdate {

    private final Long id;
    private final boolean active;
    private final boolean requiresApproval;
    private final VacationTypeColor color;
    private final boolean visibleToEveryone;

    public VacationTypeUpdate(Long id, boolean active, boolean requiresApproval, VacationTypeColor color, boolean visibleToEveryone) {
        this.id = id;
        this.active = active;
        this.requiresApproval = requiresApproval;
        this.color = color;
        this.visibleToEveryone = visibleToEveryone;
    }

    Long getId() {
        return id;
    }

    boolean isActive() {
        return active;
    }

    boolean isRequiresApproval() {
        return requiresApproval;
    }

    public VacationTypeColor getColor() {
        return color;
    }

    public boolean isVisibleToEveryone() {
        return visibleToEveryone;
    }
}
