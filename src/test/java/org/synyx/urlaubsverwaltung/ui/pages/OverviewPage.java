package org.synyx.urlaubsverwaltung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.context.MessageSource;

import java.time.LocalDate;
import java.util.Locale;

import static java.lang.String.format;

public class OverviewPage {

    private final Page page;
    private final MessageSource messageSource;
    private final Locale locale;

    public OverviewPage(Page page, MessageSource messageSource, Locale locale) {
        this.page = page;
        this.messageSource = messageSource;
        this.locale = locale;
    }

    public boolean isVisibleForPerson(String username, int year) {
        final String titleText = messageSource.getMessage("overview.header.title", new Object[]{username, year}, locale);
        return page.title().contains(titleText);
    }

    public void selectDateRange(LocalDate startDate, LocalDate endDate) {
        dayLocator(startDate).hover();
        page.mouse().down();
        dayLocator(endDate).hover();
        page.mouse().up();
    }

    public void clickDay(LocalDate date) {
        dayLocator(date).click();
    }

    private Locator dayLocator(LocalDate date) {
        final String dayName = format("%s%02d", date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, locale), date.getDayOfMonth());
        return page.getByRole(AriaRole.LISTITEM).filter(new Locator.FilterOptions().setHasText(dayName)).locator("div");
    }
}
