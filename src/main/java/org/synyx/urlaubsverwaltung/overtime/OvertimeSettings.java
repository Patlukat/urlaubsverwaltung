package org.synyx.urlaubsverwaltung.overtime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OvertimeSettings implements Serializable {

    /**
     * Activates or deactivates overtime settings.
     *
     * @since 2.15.0
     */
    @Column(name = "overtime_active")
    private boolean overtimeActive = false;

    /**
     * Activates or deactivates overtime sync settings.
     *
     * @since 5.26.0
     */
    @Column(name = "overtime_sync_active")
    private boolean overtimeSyncActive = false;

    @Column(name = "overtime_reduction_without_application_active")
    private boolean overtimeReductionWithoutApplicationActive = true;

    /**
     * Activates or deactivates add, edit or delete of overtime records for privileged users only
     * If this setting is set to true users are only allowed to see their overtime entries but
     * not to allowed to add, edit or delete them.
     *
     * @since 4.21.0
     */
    @Column(name = "overtime_write_privileged_only")
    private boolean overtimeWritePrivilegedOnly = false;

    /**
     * Defines the maximum number of overtime a person can have.
     *
     * @since 2.13.0
     */
    @Column(name = "overtime_maximum")
    private Integer maximumOvertime = 100;

    /**
     * Defines the minimum number of overtime a person can have. Minimum overtime means missing hours (equates to
     * negative)
     *
     * @since 2.15.0
     */
    @Column(name = "overtime_minimum")
    private Integer minimumOvertime = 5;

    /**
     * Defines the minimum overtime reduction value a person has to use for an application.
     *
     * @since 4.21.0
     */
    @Column(name = "overtime_minimum_reduction")
    private Integer minimumOvertimeReduction = 0;


    public boolean isOvertimeActive() {
        return overtimeActive;
    }

    public void setOvertimeActive(boolean overtimeActive) {
        this.overtimeActive = overtimeActive;
    }

    public boolean isOvertimeSyncActive() {
        return overtimeSyncActive;
    }

    public void setOvertimeSyncActive(boolean overtimeSyncActive) {
        this.overtimeSyncActive = overtimeSyncActive;
    }

    public boolean isOvertimeReductionWithoutApplicationActive() {
        return overtimeReductionWithoutApplicationActive;
    }

    public void setOvertimeReductionWithoutApplicationActive(boolean overtimeReductionWithoutApplicationActive) {
        this.overtimeReductionWithoutApplicationActive = overtimeReductionWithoutApplicationActive;
    }

    public Integer getMaximumOvertime() {
        return maximumOvertime;
    }

    public void setMaximumOvertime(Integer maximumOvertime) {
        this.maximumOvertime = maximumOvertime;
    }

    public Integer getMinimumOvertime() {
        return minimumOvertime;
    }

    public void setMinimumOvertime(Integer minimumOvertime) {
        this.minimumOvertime = minimumOvertime;
    }

    public Integer getMinimumOvertimeReduction() {
        return minimumOvertimeReduction;
    }

    public void setMinimumOvertimeReduction(Integer minimumOvertimeReduction) {
        this.minimumOvertimeReduction = minimumOvertimeReduction;
    }

    public boolean isOvertimeWritePrivilegedOnly() {
        return overtimeWritePrivilegedOnly;
    }

    public void setOvertimeWritePrivilegedOnly(boolean overtimeWritePrivilegedOnly) {
        this.overtimeWritePrivilegedOnly = overtimeWritePrivilegedOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeSettings that = (OvertimeSettings) o;
        return overtimeActive == that.overtimeActive
            && overtimeReductionWithoutApplicationActive == that.overtimeReductionWithoutApplicationActive
            && overtimeWritePrivilegedOnly == that.overtimeWritePrivilegedOnly
            && Objects.equals(maximumOvertime, that.maximumOvertime)
            && Objects.equals(minimumOvertime, that.minimumOvertime)
            && Objects.equals(minimumOvertimeReduction, that.minimumOvertimeReduction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overtimeActive, overtimeReductionWithoutApplicationActive, overtimeWritePrivilegedOnly,
            maximumOvertime, minimumOvertime, minimumOvertimeReduction);
    }
}
