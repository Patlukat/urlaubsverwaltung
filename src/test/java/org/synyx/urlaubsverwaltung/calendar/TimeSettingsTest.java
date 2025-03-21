package org.synyx.urlaubsverwaltung.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSettingsTest {

    @Test
    void ensureHasDefaultValues() {
        final TimeSettings timeSettings = new TimeSettings();
        assertThat(timeSettings.getTimeZoneId()).isEqualTo("Europe/Berlin");
        assertThat(timeSettings.getWorkDayBeginHour()).isEqualTo(8);
        assertThat(timeSettings.getWorkDayBeginMinute()).isZero();
        assertThat(timeSettings.getWorkDayEndHour()).isEqualTo(16);
        assertThat(timeSettings.getWorkDayEndMinute()).isZero();
    }
}
