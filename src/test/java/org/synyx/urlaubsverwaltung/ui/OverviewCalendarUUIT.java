package org.synyx.urlaubsverwaltung.ui;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.synyx.urlaubsverwaltung.SingleTenantTestPostgreSQLContainer;
import org.synyx.urlaubsverwaltung.TestKeycloakContainer;
import org.synyx.urlaubsverwaltung.account.AccountInteractionService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.Role;
import org.synyx.urlaubsverwaltung.ui.extension.UiTest;
import org.synyx.urlaubsverwaltung.ui.pages.ApplicationPage;
import org.synyx.urlaubsverwaltung.ui.pages.LoginPage;
import org.synyx.urlaubsverwaltung.ui.pages.OverviewPage;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeWriteService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.Month.APRIL;
import static java.time.Month.DECEMBER;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.util.StringUtils.trimAllWhitespace;
import static java.lang.String.format;
import static org.synyx.urlaubsverwaltung.person.Role.USER;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;

@Testcontainers(parallel = true)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.main.allow-bean-definition-overriding=true"})
@UiTest
class OverviewCalendarUUIT {

    @LocalServerPort
    private int port;

    @Container
    private static final SingleTenantTestPostgreSQLContainer postgre = new SingleTenantTestPostgreSQLContainer();
    @Container
    private static final TestKeycloakContainer keycloak = new TestKeycloakContainer();

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        postgre.configureSpringDataSource(registry);
        keycloak.configureSpringDataSource(registry);
    }

    private static final LocalDate FIXED_DATE = LocalDate.of(2022, 2, 5);

    @TestConfiguration
    static class Configuration {

        @Bean
        @Primary
        public Clock clock() {
            // use a fixed clock to avoid weekends or public holidays while creating sick notes

            return Clock.fixed(FIXED_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }

    @Autowired
    private PersonService personService;
    @Autowired
    private AccountInteractionService accountInteractionService;
    @Autowired
    private WorkingTimeWriteService workingTimeWriteService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private Clock clock;

    @Test
    void userCanStartSingleDayLeaveApplicationFromCalendar(Page page) {
        page.clock().setFixedTime(clock.instant().toEpochMilli());

        final Person officePerson = createPerson("Alfred", "Pennyworth the fourth", List.of(USER, OFFICE));

        final LoginPage loginPage = new LoginPage(page);
        final OverviewPage overviewPage = new OverviewPage(page, messageSource, GERMAN);
        final ApplicationPage applicationPage = new ApplicationPage(page);

        page.navigate("http://localhost:" + port + "/oauth2/authorization/keycloak");
        loginPage.login(new LoginPage.Credentials(officePerson.getEmail(), officePerson.getEmail()));

        assertThat(overviewPage.isVisibleForPerson(officePerson.getNiceName(), FIXED_DATE.getYear())).isTrue();

        // Click on a day in the next month
        final LocalDate date = LocalDate.of(2022, 3, 15);
        overviewPage.clickDay(date);

        // Ensure navigation to ApplicationPage
        page.context().waitForCondition(applicationPage::isVisible);
        //... with correct dates
        applicationPage.assertDatesAreSelected(date, date);
    }


    @Test
    void userCanStartMultiDayLeaveApplicationFromCalendar(Page page) {
        page.clock().setFixedTime(clock.instant().toEpochMilli());

        final Person officePerson = createPerson("Bob", "Bobson", List.of(USER, OFFICE));

        final LoginPage loginPage = new LoginPage(page);
        final OverviewPage overviewPage = new OverviewPage(page, messageSource, GERMAN);
        final ApplicationPage applicationPage = new ApplicationPage(page);

        page.navigate("http://localhost:" + port + "/oauth2/authorization/keycloak");
        loginPage.login(new LoginPage.Credentials(officePerson.getEmail(), officePerson.getEmail()));

        assertThat(overviewPage.isVisibleForPerson(officePerson.getNiceName(), FIXED_DATE.getYear())).isTrue();

        // Select a range of 3 days (Tuesday to Thursday) in the next month
        final LocalDate startDate = LocalDate.of(2022, 3, 8);
        final LocalDate endDate = LocalDate.of(2022, 3, 10);

        overviewPage.selectDateRange(startDate, endDate);
        overviewPage.clickDay(endDate);

        // Ensure navigation to ApplicationPage
        page.context().waitForCondition(applicationPage::isVisible);
        //... with correct dates
        applicationPage.assertDatesAreSelected(startDate, endDate);
    }


    private Person createPerson(String firstName, String lastName, List<Role> roles) {

        final String email = format("%s.%s@example.org", trimAllWhitespace(firstName), trimAllWhitespace(lastName)).toLowerCase();
        final Optional<Person> personByMailAddress = personService.getPersonByMailAddress(email);
        if (personByMailAddress.isPresent()) {
            return personByMailAddress.get();
        }

        final String userId = keycloak.createUser(email, firstName, lastName, email, email);
        final Person savedPerson = personService.create(userId, firstName, lastName, email, List.of(), roles);

        final Year currentYear = Year.now();
        final LocalDate firstDayOfYear = currentYear.atDay(1);
        final List<Integer> workingDays = Stream.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY).map(DayOfWeek::getValue).toList();
        workingTimeWriteService.touch(workingDays, firstDayOfYear, savedPerson);

        final LocalDate lastDayOfYear = firstDayOfYear.withMonth(DECEMBER.getValue()).withDayOfMonth(31);
        final LocalDate expiryDate = LocalDate.of(currentYear.getValue(), APRIL, 1);
        accountInteractionService.updateOrCreateHolidaysAccount(savedPerson, firstDayOfYear, lastDayOfYear, true, expiryDate, TEN, TEN, TEN, ZERO, null);
        accountInteractionService.updateOrCreateHolidaysAccount(savedPerson, firstDayOfYear.plusYears(1), lastDayOfYear.plusYears(1), true, expiryDate.plusYears(1), TEN, TEN, TEN, ZERO, null);

        return savedPerson;
    }
}
