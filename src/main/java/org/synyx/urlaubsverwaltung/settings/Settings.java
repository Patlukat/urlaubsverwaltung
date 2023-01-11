package org.synyx.urlaubsverwaltung.settings;

import org.synyx.urlaubsverwaltung.absence.TimeSettings;
import org.synyx.urlaubsverwaltung.account.AccountSettings;
import org.synyx.urlaubsverwaltung.application.settings.ApplicationSettings;
import org.synyx.urlaubsverwaltung.calendarintegration.CalendarSettings;
import org.synyx.urlaubsverwaltung.overtime.OvertimeSettings;
import org.synyx.urlaubsverwaltung.sicknote.settings.SickNoteSettings;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeSettings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.util.Objects;

import static javax.persistence.GenerationType.SEQUENCE;


/**
 * Represents the settings / business rules for the application.
 */
@Entity
public class Settings {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @GeneratedValue(strategy = SEQUENCE, generator = "settings_generator")
    @SequenceGenerator(name = "settings_generator", sequenceName = "settings_id_seq", allocationSize = 1)
    private Long id;

    private ApplicationSettings applicationSettings = new ApplicationSettings();
    private AccountSettings accountSettings = new AccountSettings();
    private WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
    private OvertimeSettings overtimeSettings = new OvertimeSettings();
    private TimeSettings timeSettings = new TimeSettings();
    private SickNoteSettings sickNoteSettings = new SickNoteSettings();
    @Deprecated(since = "4.0.0", forRemoval = true)
    private CalendarSettings calendarSettings = new CalendarSettings();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApplicationSettings getApplicationSettings() {
        return applicationSettings;
    }

    public void setApplicationSettings(ApplicationSettings applicationSettings) {
        this.applicationSettings = applicationSettings;
    }

    public AccountSettings getAccountSettings() {
        return accountSettings;
    }

    public void setAccountSettings(AccountSettings accountSettings) {
        this.accountSettings = accountSettings;
    }

    public WorkingTimeSettings getWorkingTimeSettings() {
        return workingTimeSettings;
    }

    public void setWorkingTimeSettings(WorkingTimeSettings workingTimeSettings) {
        this.workingTimeSettings = workingTimeSettings;
    }

    public OvertimeSettings getOvertimeSettings() {
        return overtimeSettings;
    }

    public void setOvertimeSettings(OvertimeSettings overtimeSettings) {
        this.overtimeSettings = overtimeSettings;
    }

    public CalendarSettings getCalendarSettings() {
        return calendarSettings;
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    public void setCalendarSettings(CalendarSettings calendarSettings) {
        this.calendarSettings = calendarSettings;
    }

    public TimeSettings getTimeSettings() {
        return timeSettings;
    }

    public void setTimeSettings(TimeSettings timeSettings) {
        this.timeSettings = timeSettings;
    }

    public SickNoteSettings getSickNoteSettings() {
        return sickNoteSettings;
    }

    public void setSickNoteSettings(SickNoteSettings sickNoteSettings) {
        this.sickNoteSettings = sickNoteSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Settings that = (Settings) o;
        return null != this.getId() && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
