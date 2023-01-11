package org.synyx.urlaubsverwaltung.calendarintegration;

import javax.persistence.Column;
import javax.persistence.Embeddable;


/**
 * Settings to sync absences with a calendar provider.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@Embeddable
public class CalendarSettings {

    private GoogleCalendarSettings googleCalendarSettings = new GoogleCalendarSettings();

    private ExchangeCalendarSettings exchangeCalendarSettings = new ExchangeCalendarSettings();

    @Column(name = "calendar_provider")
    private String provider = "NoopCalendarSyncProvider";

    public ExchangeCalendarSettings getExchangeCalendarSettings() {
        return exchangeCalendarSettings;
    }

    public void setExchangeCalendarSettings(ExchangeCalendarSettings exchangeCalendarSettings) {
        this.exchangeCalendarSettings = exchangeCalendarSettings;
    }

    public GoogleCalendarSettings getGoogleCalendarSettings() {
        return googleCalendarSettings;
    }

    public void setGoogleCalendarSettings(GoogleCalendarSettings googleCalendarSettings) {
        this.googleCalendarSettings = googleCalendarSettings;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
