package org.synyx.urlaubsverwaltung.workingtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.publicholiday.PublicHoliday;
import org.synyx.urlaubsverwaltung.publicholiday.PublicHolidaysService;
import org.synyx.urlaubsverwaltung.settings.Settings;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.period.DayLength.FULL;
import static org.synyx.urlaubsverwaltung.period.DayLength.MORNING;
import static org.synyx.urlaubsverwaltung.period.DayLength.ZERO;
import static org.synyx.urlaubsverwaltung.workingtime.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static org.synyx.urlaubsverwaltung.workingtime.FederalState.GERMANY_BAYERN;
import static org.synyx.urlaubsverwaltung.workingtime.FederalState.GERMANY_BERLIN;
import static org.synyx.urlaubsverwaltung.workingtime.FederalState.GERMANY_BREMEN;
import static org.synyx.urlaubsverwaltung.workingtime.FederalState.GERMANY_RHEINLAND_PFALZ;
import static org.synyx.urlaubsverwaltung.workingtime.FederalState.SWITZERLAND_GENF;

@ExtendWith(MockitoExtension.class)
class WorkingTimeServiceImplTest {

    private WorkingTimeServiceImpl sut;

    @Mock
    private WorkingTimeProperties workingTimeProperties;
    @Mock
    private WorkingTimeRepository workingTimeRepository;
    @Mock
    private PublicHolidaysService publicHolidaysService;
    @Mock
    private SettingsService settingsService;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2019-08-13T00:00:00.00Z"), UTC);

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeServiceImpl(workingTimeProperties, workingTimeRepository, publicHolidaysService, settingsService, fixedClock);
    }

    @Test
    void ensureDefaultWorkingTimeCreationFromProperties() {

        when(workingTimeProperties.isDefaultWorkingDaysDeactivated()).thenReturn(false);
        when(workingTimeProperties.getDefaultWorkingDays()).thenReturn(List.of(1, 2, 3, 4, 5));

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        sut.createDefaultWorkingTime(person);

        final ArgumentCaptor<WorkingTimeEntity> argument = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(argument.capture());

        final WorkingTimeEntity persistedWorkingTimeEntity = argument.getValue();
        assertThat(persistedWorkingTimeEntity.getPerson()).isEqualTo(person);
        assertThat(persistedWorkingTimeEntity.getValidFrom()).isEqualTo(LocalDate.now(fixedClock).with(firstDayOfYear()));
        assertThat(persistedWorkingTimeEntity.getMonday()).isEqualTo(FULL);
        assertThat(persistedWorkingTimeEntity.getTuesday()).isEqualTo(FULL);
        assertThat(persistedWorkingTimeEntity.getWednesday()).isEqualTo(FULL);
        assertThat(persistedWorkingTimeEntity.getThursday()).isEqualTo(FULL);
        assertThat(persistedWorkingTimeEntity.getFriday()).isEqualTo(FULL);
        assertThat(persistedWorkingTimeEntity.getSaturday()).isEqualTo(ZERO);
        assertThat(persistedWorkingTimeEntity.getSunday()).isEqualTo(ZERO);
    }

    @Test
    void ensureDefaultWorkingTimeCreationFromGui() {

        when(workingTimeProperties.isDefaultWorkingDaysDeactivated()).thenReturn(true);

        final Settings settings = new Settings();
        settings.getWorkingTimeSettings().setMonday(ZERO);
        settings.getWorkingTimeSettings().setTuesday(ZERO);
        settings.getWorkingTimeSettings().setWednesday(ZERO);
        settings.getWorkingTimeSettings().setThursday(ZERO);
        settings.getWorkingTimeSettings().setFriday(FULL);
        settings.getWorkingTimeSettings().setSaturday(ZERO);
        settings.getWorkingTimeSettings().setSunday(ZERO);
        when(settingsService.getSettings()).thenReturn(settings);

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        sut.createDefaultWorkingTime(person);

        final ArgumentCaptor<WorkingTimeEntity> argument = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(argument.capture());

        final WorkingTimeEntity persistedWorkingTimeEntity = argument.getValue();
        assertThat(persistedWorkingTimeEntity.getPerson()).isEqualTo(person);
        assertThat(persistedWorkingTimeEntity.getValidFrom()).isEqualTo(LocalDate.now(fixedClock).with(firstDayOfYear()));
        assertThat(persistedWorkingTimeEntity.getMonday()).isEqualTo(ZERO);
        assertThat(persistedWorkingTimeEntity.getTuesday()).isEqualTo(ZERO);
        assertThat(persistedWorkingTimeEntity.getWednesday()).isEqualTo(ZERO);
        assertThat(persistedWorkingTimeEntity.getThursday()).isEqualTo(ZERO);
        assertThat(persistedWorkingTimeEntity.getFriday()).isEqualTo(FULL);
        assertThat(persistedWorkingTimeEntity.getSaturday()).isEqualTo(ZERO);
        assertThat(persistedWorkingTimeEntity.getSunday()).isEqualTo(ZERO);
    }

    @Test
    void ensureGetFederalStateForPerson() {

        final LocalDate date = LocalDate.now(UTC);

        final Person person = new Person();
        person.setId(1);

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setFederalStateOverride(GERMANY_BAYERN);
        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(person, date)).thenReturn(workingTimeEntity);

        final FederalState federalState = sut.getFederalStateForPerson(person, date);

        assertThat(federalState).isEqualTo(GERMANY_BAYERN);
        verifyNoInteractions(settingsService);
    }

    @Test
    void ensureGetFederalStateForPersonReturnSystemDefaultWhenEntityHasNullValue() {

        final LocalDate date = LocalDate.now(UTC);

        final Person person = new Person();
        person.setId(1);

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(person, date)).thenReturn(workingTimeEntity);

        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        workingTimeSettings.setFederalState(GERMANY_BREMEN);
        final Settings settings = new Settings();
        settings.setWorkingTimeSettings(workingTimeSettings);

        when(settingsService.getSettings()).thenReturn(settings);

        final FederalState federalState = sut.getFederalStateForPerson(person, date);

        assertThat(federalState).isEqualTo(GERMANY_BREMEN);
    }

    @Test
    void ensureGetFederalStateForPersonReturnSystemDefaultWhenNothingFound() {

        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(any(), any())).thenReturn(null);

        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        workingTimeSettings.setFederalState(GERMANY_BREMEN);
        final Settings settings = new Settings();
        settings.setWorkingTimeSettings(workingTimeSettings);

        when(settingsService.getSettings()).thenReturn(settings);

        final FederalState federalState = sut.getFederalStateForPerson(new Person(), LocalDate.now(UTC));

        assertThat(federalState).isEqualTo(GERMANY_BREMEN);
    }

    @Test
    void ensureTouchSetsFederalStateOverrideOfWorkingTimeEntityToNull() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        sut.touch(List.of(1, 2), LocalDate.now(UTC), person);

        final ArgumentCaptor<WorkingTimeEntity> workingTimeArgumentCaptor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(workingTimeArgumentCaptor.capture());

        final WorkingTimeEntity persistedWorkingTimeEntity = workingTimeArgumentCaptor.getValue();
        assertThat(persistedWorkingTimeEntity.getFederalStateOverride()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = FederalState.class, names = {"GERMANY_BAYERN"})
    @NullSource
    void ensureTouchSetsFederalStateOverrideOfWorkingTimeEntity(FederalState federalState) {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        sut.touch(List.of(1, 2), LocalDate.now(UTC), person, federalState);

        final ArgumentCaptor<WorkingTimeEntity> workingTimeArgumentCaptor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(workingTimeArgumentCaptor.capture());

        final WorkingTimeEntity persistedWorkingTimeEntity = workingTimeArgumentCaptor.getValue();
        assertThat(persistedWorkingTimeEntity.getFederalStateOverride()).isEqualTo(federalState);
    }


    @Test
    void getByPerson() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2021, JUNE, 11));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);
        workingTimeEntity.setMonday(FULL);
        workingTimeEntity.setTuesday(FULL);
        workingTimeEntity.setWednesday(FULL);
        workingTimeEntity.setThursday(FULL);
        workingTimeEntity.setFriday(FULL);
        workingTimeEntity.setSaturday(FULL);
        workingTimeEntity.setSunday(FULL);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntity));

        final List<WorkingTime> workingTimes = sut.getByPerson(person);
        assertThat(workingTimes).hasSize(1);
        assertThat(workingTimes.get(0).getPerson()).isSameAs(person);
        assertThat(workingTimes.get(0).getValidFrom()).isEqualTo(LocalDate.of(2021, JUNE, 11));
        assertThat(workingTimes.get(0).getFederalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
        assertThat(workingTimes.get(0).getMonday()).isEqualTo(FULL);
        assertThat(workingTimes.get(0).getTuesday()).isEqualTo(FULL);
        assertThat(workingTimes.get(0).getWednesday()).isEqualTo(FULL);
        assertThat(workingTimes.get(0).getThursday()).isEqualTo(FULL);
        assertThat(workingTimes.get(0).getFriday()).isEqualTo(FULL);
        assertThat(workingTimes.get(0).getSaturday()).isEqualTo(FULL);
        assertThat(workingTimes.get(0).getSunday()).isEqualTo(FULL);
    }

    @Test
    void ensureGetByPersonCallsSystemDefaultFederalStateOnlyOnce() {
        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        final WorkingTimeEntity workingTimeEntityOne = new WorkingTimeEntity();
        workingTimeEntityOne.setId(1);
        workingTimeEntityOne.setPerson(person);
        workingTimeEntityOne.setValidFrom(LocalDate.of(2021, JUNE, 11));
        workingTimeEntityOne.setFederalStateOverride(null);

        final WorkingTimeEntity workingTimeEntityTwo = new WorkingTimeEntity();
        workingTimeEntityTwo.setId(2);
        workingTimeEntityTwo.setPerson(person);
        workingTimeEntityTwo.setValidFrom(LocalDate.of(2022, JUNE, 11));
        workingTimeEntityTwo.setFederalStateOverride(null);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityOne, workingTimeEntityTwo));

        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        workingTimeSettings.setFederalState(GERMANY_BREMEN);
        final Settings settings = new Settings();
        settings.setWorkingTimeSettings(workingTimeSettings);

        when(settingsService.getSettings()).thenReturn(settings);

        final List<WorkingTime> workingTimes = sut.getByPerson(person);

        assertThat(workingTimes).hasSize(2);
        assertThat(workingTimes.get(0).getFederalState()).isEqualTo(GERMANY_BREMEN);
        assertThat(workingTimes.get(1).getFederalState()).isEqualTo(GERMANY_BREMEN);
        verify(settingsService).getSettings();
    }

    @Test
    void ensureGetByPersonAndValidityDateEqualsOrMinorDate() {
        final Person batman = new Person();
        batman.setId(1);

        final LocalDate date = LocalDate.now();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(batman);
        workingTimeEntity.setValidFrom(date);
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(batman, date))
            .thenReturn(workingTimeEntity);

        final Optional<WorkingTime> actualWorkingTime = sut.getWorkingTime(batman, date);

        assertThat(actualWorkingTime).isNotEmpty();
        assertThat(actualWorkingTime.get().getPerson()).isEqualTo(batman);
        assertThat(actualWorkingTime.get().getValidFrom()).isEqualTo(date);
        assertThat(actualWorkingTime.get().getFederalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
    }

    @Test
    void ensureGetByPersonAndValidityDateEqualsOrMinorDateUsesSystemDefaultFederalStateWhenEntityHasNullValue() {
        final Person batman = new Person();
        batman.setId(1);

        final LocalDate date = LocalDate.now();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(batman);
        workingTimeEntity.setValidFrom(date);

        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(batman, date))
            .thenReturn(workingTimeEntity);

        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        workingTimeSettings.setFederalState(GERMANY_BREMEN);
        final Settings settings = new Settings();
        settings.setWorkingTimeSettings(workingTimeSettings);

        when(settingsService.getSettings()).thenReturn(settings);

        final Optional<WorkingTime> actualWorkingTime = sut.getWorkingTime(batman, date);

        assertThat(actualWorkingTime).isNotEmpty();
        assertThat(actualWorkingTime.get().getPerson()).isEqualTo(batman);
        assertThat(actualWorkingTime.get().getValidFrom()).isEqualTo(date);
        assertThat(actualWorkingTime.get().getFederalState()).isEqualTo(GERMANY_BREMEN);
    }

    @Test
    void ensureGetByPersonAndValidityDateEqualsOrMinorDateReturnsEmptyOptionalWhenNothingFound() {
        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(any(), any()))
            .thenReturn(null);

        final Optional<WorkingTime> actualWorkingTime = sut.getWorkingTime(new Person(), LocalDate.now());

        assertThat(actualWorkingTime).isEmpty();
    }

    @Test
    void ensureGetSystemDefaultFederalState() {

        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        workingTimeSettings.setFederalState(GERMANY_BREMEN);
        final Settings settings = new Settings();
        settings.setWorkingTimeSettings(workingTimeSettings);

        when(settingsService.getSettings()).thenReturn(settings);

        final FederalState defaultFederalState = sut.getSystemDefaultFederalState();

        assertThat(defaultFederalState).isEqualTo(GERMANY_BREMEN);
    }

    @Test
    void getFederalStatesByPersonAndDateRangeAndDateRangeIsInBetweenWorkingTimes() {

        final Person person = new Person();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2020, 9, 1));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntityChanged = new WorkingTimeEntity();
        workingTimeEntityChanged.setId(2);
        workingTimeEntityChanged.setPerson(person);
        workingTimeEntityChanged.setValidFrom(LocalDate.of(2021, 11, 15));
        workingTimeEntityChanged.setFederalStateOverride(GERMANY_RHEINLAND_PFALZ);

        final WorkingTimeEntity workingTimeEntityFuture = new WorkingTimeEntity();
        workingTimeEntityFuture.setId(3);
        workingTimeEntityFuture.setPerson(person);
        workingTimeEntityFuture.setValidFrom(LocalDate.of(2022, 1, 1));
        workingTimeEntityFuture.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityFuture, workingTimeEntityChanged, workingTimeEntity));

        final DateRange dateRange = new DateRange(LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 30));
        final Map<DateRange, FederalState> federalStatesByPersonAndDateRange = sut.getFederalStatesByPersonAndDateRange(person, dateRange);
        assertThat(federalStatesByPersonAndDateRange)
            .isNotEmpty()
            .hasSize(2)
            .containsExactly(
                entry(new DateRange(LocalDate.of(2021, 11, 15), LocalDate.of(2021, 11, 30)), GERMANY_RHEINLAND_PFALZ),
                entry(new DateRange(LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 14)), GERMANY_BADEN_WUERTTEMBERG)
            );
    }

    @Test
    void getFederalStatesByPersonAndDateRangeStartsOnWorkingTimeValidFrom() {

        final Person person = new Person();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2020, 1, 1));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntityChanged = new WorkingTimeEntity();
        workingTimeEntityChanged.setId(2);
        workingTimeEntityChanged.setPerson(person);
        workingTimeEntityChanged.setValidFrom(LocalDate.of(2020, 9, 1));
        workingTimeEntityChanged.setFederalStateOverride(GERMANY_RHEINLAND_PFALZ);

        final WorkingTimeEntity workingTimeEntityFuture = new WorkingTimeEntity();
        workingTimeEntityFuture.setId(3);
        workingTimeEntityFuture.setPerson(person);
        workingTimeEntityFuture.setValidFrom(LocalDate.of(2022, 1, 1));
        workingTimeEntityFuture.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityFuture, workingTimeEntityChanged, workingTimeEntity));

        final DateRange dateRange = new DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 10));
        final Map<DateRange, FederalState> federalStatesByPersonAndDateRange = sut.getFederalStatesByPersonAndDateRange(person, dateRange);
        assertThat(federalStatesByPersonAndDateRange)
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(entry(new DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 10)), GERMANY_RHEINLAND_PFALZ));
    }

    @Test
    void getFederalStatesByPersonAndDateRangeEndsOnWorkingTimeValidFrom() {

        final Person person = new Person();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2020, 1, 1));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntityChanged = new WorkingTimeEntity();
        workingTimeEntityChanged.setId(2);
        workingTimeEntityChanged.setPerson(person);
        workingTimeEntityChanged.setValidFrom(LocalDate.of(2020, 9, 1));
        workingTimeEntityChanged.setFederalStateOverride(GERMANY_RHEINLAND_PFALZ);

        final WorkingTimeEntity workingTimeEntityFuture = new WorkingTimeEntity();
        workingTimeEntityFuture.setId(3);
        workingTimeEntityFuture.setPerson(person);
        workingTimeEntityFuture.setValidFrom(LocalDate.of(2022, 1, 1));
        workingTimeEntityFuture.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityFuture, workingTimeEntityChanged, workingTimeEntity));

        final DateRange dateRange = new DateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 9, 1));
        final Map<DateRange, FederalState> federalStatesByPersonAndDateRange = sut.getFederalStatesByPersonAndDateRange(person, dateRange);
        assertThat(federalStatesByPersonAndDateRange)
            .isNotEmpty()
            .hasSize(2)
            .containsExactly(
                entry(new DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 1)), GERMANY_RHEINLAND_PFALZ),
                entry(new DateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 8, 31)), GERMANY_BADEN_WUERTTEMBERG)
            );
    }

    @Test
    void getFederalStatesByPersonAndDateRangeWithoutWorkingTimes() {

        final Person batman = new Person();

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(batman)).thenReturn(emptyList());

        assertThat(sut.getFederalStatesByPersonAndDateRange(batman,
            new DateRange(
                LocalDate.of(2021, 11, 1),
                LocalDate.of(2021, 11, 30)))).isEmpty();
    }

    @Test
    void getWorkingTimesByPersonAndDateRangeAndDateRangeIsInBetweenWorkingTimes() {

        final Person person = new Person();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2020, 9, 1));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntityChanged = new WorkingTimeEntity();
        workingTimeEntityChanged.setId(2);
        workingTimeEntityChanged.setPerson(person);
        workingTimeEntityChanged.setValidFrom(LocalDate.of(2021, 11, 15));
        workingTimeEntityChanged.setFederalStateOverride(GERMANY_RHEINLAND_PFALZ);

        final WorkingTimeEntity workingTimeEntityFuture = new WorkingTimeEntity();
        workingTimeEntityFuture.setId(3);
        workingTimeEntityFuture.setPerson(person);
        workingTimeEntityFuture.setValidFrom(LocalDate.of(2022, 1, 1));
        workingTimeEntityFuture.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityFuture, workingTimeEntityChanged, workingTimeEntity));

        final DateRange dateRange = new DateRange(LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 30));
        final Map<DateRange, WorkingTime> federalStatesByPersonAndDateRange = sut.getWorkingTimesByPersonAndDateRange(person, dateRange);
        assertThat(federalStatesByPersonAndDateRange)
            .isNotEmpty()
            .hasSize(2)
            .containsExactly(
                entry(new DateRange(LocalDate.of(2021, 11, 15), LocalDate.of(2021, 11, 30)), new WorkingTime(person, LocalDate.of(2021, 11, 15), GERMANY_RHEINLAND_PFALZ, false)),
                entry(new DateRange(LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 14)), new WorkingTime(person, LocalDate.of(2020, 9, 1), GERMANY_BADEN_WUERTTEMBERG, false))
            );
    }

    @Test
    void getWorkingTimesByPersonAndDateRangeStartsOnWorkingTimeValidFrom() {

        final Person person = new Person();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2020, 1, 1));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntityChanged = new WorkingTimeEntity();
        workingTimeEntityChanged.setId(2);
        workingTimeEntityChanged.setPerson(person);
        workingTimeEntityChanged.setValidFrom(LocalDate.of(2020, 9, 1));
        workingTimeEntityChanged.setFederalStateOverride(GERMANY_RHEINLAND_PFALZ);

        final WorkingTimeEntity workingTimeEntityFuture = new WorkingTimeEntity();
        workingTimeEntityFuture.setId(3);
        workingTimeEntityFuture.setPerson(person);
        workingTimeEntityFuture.setValidFrom(LocalDate.of(2022, 1, 1));
        workingTimeEntityFuture.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityFuture, workingTimeEntityChanged, workingTimeEntity));

        final DateRange dateRange = new DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 10));
        final Map<DateRange, WorkingTime> federalStatesByPersonAndDateRange = sut.getWorkingTimesByPersonAndDateRange(person, dateRange);
        assertThat(federalStatesByPersonAndDateRange)
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(entry(new DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 10)), new WorkingTime(person, LocalDate.of(2020, 9, 1), GERMANY_RHEINLAND_PFALZ, false)));
    }

    @Test
    void getWorkingTimesByPersonAndDateRangeEndsOnWorkingTimeValidFrom() {

        final Person person = new Person();

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setId(1);
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setValidFrom(LocalDate.of(2020, 1, 1));
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntityChanged = new WorkingTimeEntity();
        workingTimeEntityChanged.setId(2);
        workingTimeEntityChanged.setPerson(person);
        workingTimeEntityChanged.setValidFrom(LocalDate.of(2020, 9, 1));
        workingTimeEntityChanged.setFederalStateOverride(GERMANY_RHEINLAND_PFALZ);

        final WorkingTimeEntity workingTimeEntityFuture = new WorkingTimeEntity();
        workingTimeEntityFuture.setId(3);
        workingTimeEntityFuture.setPerson(person);
        workingTimeEntityFuture.setValidFrom(LocalDate.of(2022, 1, 1));
        workingTimeEntityFuture.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(List.of(workingTimeEntityFuture, workingTimeEntityChanged, workingTimeEntity));

        final DateRange dateRange = new DateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 9, 1));
        final Map<DateRange, WorkingTime> federalStatesByPersonAndDateRange = sut.getWorkingTimesByPersonAndDateRange(person, dateRange);
        assertThat(federalStatesByPersonAndDateRange)
            .isNotEmpty()
            .hasSize(2)
            .containsExactly(
                entry(new DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 1)), new WorkingTime(person, LocalDate.of(2020, 9, 1), GERMANY_RHEINLAND_PFALZ, false)),
                entry(new DateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 8, 31)), new WorkingTime(person, LocalDate.of(2020, 1, 1), GERMANY_BADEN_WUERTTEMBERG, false))
            );
    }

    @Test
    void getWorkingTimesByPersonAndDateRangeWithoutWorkingTimes() {

        final Person person = new Person();
        when(workingTimeRepository.findByPersonOrderByValidFromDesc(person)).thenReturn(emptyList());

        assertThat(sut.getWorkingTimesByPersonAndDateRange(person,
            new DateRange(
                LocalDate.of(2021, 11, 1),
                LocalDate.of(2021, 11, 30)))).isEmpty();
    }

    @Test
    void ensureGetWorkingTimesByPersonsAndYear() {
        final Person person = new Person();
        person.setId(1);

        final Person person2 = new Person();
        person2.setId(2);

        final List<Person> persons = List.of(person, person2);
        final DateRange dateRange = new DateRange(LocalDate.of(2022, JANUARY, 1), LocalDate.of(2022, DECEMBER, 31));

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setMonday(FULL);
        workingTimeEntity.setTuesday(FULL);
        workingTimeEntity.setWednesday(FULL);
        workingTimeEntity.setThursday(FULL);
        workingTimeEntity.setFriday(FULL);
        workingTimeEntity.setSaturday(FULL);
        workingTimeEntity.setSunday(FULL);
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntity2 = new WorkingTimeEntity();
        workingTimeEntity2.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity2.setPerson(person2);
        workingTimeEntity2.setMonday(DayLength.NOON);
        workingTimeEntity2.setTuesday(DayLength.NOON);
        workingTimeEntity2.setWednesday(DayLength.NOON);
        workingTimeEntity2.setThursday(DayLength.NOON);
        workingTimeEntity2.setFriday(DayLength.NOON);
        workingTimeEntity2.setSaturday(DayLength.NOON);
        workingTimeEntity2.setSunday(DayLength.NOON);
        workingTimeEntity2.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        when(workingTimeRepository.findByPersonIsInOrderByValidFromDesc(persons))
            .thenReturn(List.of(workingTimeEntity, workingTimeEntity2));

        when(settingsService.getSettings()).thenReturn(new Settings());

        final Map<Person, WorkingTimeCalendar> actual = sut.getWorkingTimesByPersons(persons, Year.of(2022));
        assertThat(actual)
            .hasSize(2)
            .containsKeys(person, person2);

        final WorkingTimeCalendar personWorkingTimeCalendar = actual.get(person);
        for (LocalDate date : dateRange) {
            assertThat(personWorkingTimeCalendar.workingTime(date)).hasValue(BigDecimal.ONE);
        }

        final WorkingTimeCalendar person2WorkingTimeCalendar = actual.get(person2);
        for (LocalDate date : dateRange) {
            assertThat(person2WorkingTimeCalendar.workingTime(date)).hasValue(BigDecimal.valueOf(0.5));
        }
    }

    @Test
    void ensureGetWorkingTimesByPersonsAndYearUsesDefaultFederalStateWhenWorkingTimeDoesNotDefineIt() {
        final Person person = new Person();
        person.setId(1);

        final List<Person> persons = List.of(person);

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setMonday(FULL);
        workingTimeEntity.setTuesday(FULL);
        workingTimeEntity.setWednesday(FULL);
        workingTimeEntity.setThursday(FULL);
        workingTimeEntity.setFriday(FULL);
        workingTimeEntity.setSaturday(FULL);
        workingTimeEntity.setSunday(FULL);
        workingTimeEntity.setFederalStateOverride(null);

        when(workingTimeRepository.findByPersonIsInOrderByValidFromDesc(persons)).thenReturn(List.of(workingTimeEntity));

        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        workingTimeSettings.setFederalState(GERMANY_BERLIN);
        final Settings settings = new Settings();
        settings.setWorkingTimeSettings(workingTimeSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        sut.getWorkingTimesByPersons(persons, Year.of(2022));

        verify(publicHolidaysService, times(365)).getPublicHoliday(any(LocalDate.class), eq(GERMANY_BERLIN), eq(workingTimeSettings));
    }

    @Test
    void ensureGetWorkingTimesByPersonsAndYearIgnoresPublicHolidays() {
        final Person person = new Person();
        person.setId(1);

        final Person person2 = new Person();
        person2.setId(2);

        final List<Person> persons = List.of(person, person2);

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setMonday(FULL);
        workingTimeEntity.setTuesday(FULL);
        workingTimeEntity.setWednesday(FULL);
        workingTimeEntity.setThursday(FULL);
        workingTimeEntity.setFriday(FULL);
        workingTimeEntity.setSaturday(FULL);
        workingTimeEntity.setSunday(FULL);
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeEntity2 = new WorkingTimeEntity();
        workingTimeEntity2.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity2.setPerson(person2);
        workingTimeEntity2.setMonday(DayLength.NOON);
        workingTimeEntity2.setTuesday(DayLength.NOON);
        workingTimeEntity2.setWednesday(DayLength.NOON);
        workingTimeEntity2.setThursday(DayLength.NOON);
        workingTimeEntity2.setFriday(DayLength.NOON);
        workingTimeEntity2.setSaturday(DayLength.NOON);
        workingTimeEntity2.setSunday(DayLength.NOON);
        workingTimeEntity2.setFederalStateOverride(GERMANY_BERLIN);

        when(workingTimeRepository.findByPersonIsInOrderByValidFromDesc(persons)).thenReturn(List.of(workingTimeEntity, workingTimeEntity2));

        final Settings settings = new Settings();
        final WorkingTimeSettings workingTimeSettings = new WorkingTimeSettings();
        settings.setWorkingTimeSettings(workingTimeSettings);
        when(settingsService.getSettings()).thenReturn(settings);
        when(publicHolidaysService.getPublicHoliday(any(LocalDate.class), any(FederalState.class), eq(workingTimeSettings))).thenReturn(Optional.empty());
        when(publicHolidaysService.getPublicHoliday(LocalDate.of(2022, AUGUST, 5), GERMANY_BADEN_WUERTTEMBERG, workingTimeSettings))
            .thenReturn(Optional.of(new PublicHoliday(LocalDate.of(2022, AUGUST, 5), FULL, "")));
        when(publicHolidaysService.getPublicHoliday(LocalDate.of(2022, AUGUST, 10), GERMANY_BERLIN, workingTimeSettings))
            .thenReturn(Optional.of(new PublicHoliday(LocalDate.of(2022, AUGUST, 10), FULL, "")));

        final Map<Person, WorkingTimeCalendar> actual = sut.getWorkingTimesByPersons(persons, Year.of(2022));
        assertThat(actual)
            .hasSize(2)
            .containsKeys(person, person2);

        assertThat(actual.get(person).workingTime(LocalDate.of(2022, AUGUST, 5))).hasValue(BigDecimal.ZERO);
        assertThat(actual.get(person).workingTime(LocalDate.of(2022, AUGUST, 10))).hasValue(BigDecimal.ONE);
        assertThat(actual.get(person2).workingTime(LocalDate.of(2022, AUGUST, 5))).hasValue(BigDecimal.valueOf(0.5));
        assertThat(actual.get(person2).workingTime(LocalDate.of(2022, AUGUST, 10))).hasValue(BigDecimal.ZERO);
    }

    @Test
    void ensureGetWorkingTimesByPersonsAndYearIgnoresWorkingTimesNotInDateRange() {
        final Person person = new Person();
        person.setId(1);

        final List<Person> persons = List.of(person);
        final DateRange dateRange = new DateRange(LocalDate.of(2022, JANUARY, 1), LocalDate.of(2022, DECEMBER, 31));

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setMonday(FULL);
        workingTimeEntity.setTuesday(FULL);
        workingTimeEntity.setWednesday(FULL);
        workingTimeEntity.setThursday(FULL);
        workingTimeEntity.setFriday(FULL);
        workingTimeEntity.setSaturday(FULL);
        workingTimeEntity.setSunday(FULL);
        workingTimeEntity.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimePreviousYear = new WorkingTimeEntity();
        workingTimePreviousYear.setValidFrom(LocalDate.of(2021, JANUARY, 1));
        workingTimePreviousYear.setPerson(person);
        workingTimePreviousYear.setMonday(MORNING);
        workingTimePreviousYear.setTuesday(MORNING);
        workingTimePreviousYear.setWednesday(MORNING);
        workingTimePreviousYear.setThursday(MORNING);
        workingTimePreviousYear.setFriday(MORNING);
        workingTimePreviousYear.setSaturday(MORNING);
        workingTimePreviousYear.setSunday(MORNING);
        workingTimePreviousYear.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        final WorkingTimeEntity workingTimeNextYear = new WorkingTimeEntity();
        workingTimeNextYear.setValidFrom(LocalDate.of(2023, JANUARY, 1));
        workingTimeNextYear.setPerson(person);
        workingTimeNextYear.setMonday(DayLength.NOON);
        workingTimeNextYear.setTuesday(DayLength.NOON);
        workingTimeNextYear.setWednesday(DayLength.NOON);
        workingTimeNextYear.setThursday(DayLength.NOON);
        workingTimeNextYear.setFriday(DayLength.NOON);
        workingTimeNextYear.setSaturday(DayLength.NOON);
        workingTimeNextYear.setSunday(DayLength.NOON);
        workingTimeNextYear.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        when(workingTimeRepository.findByPersonIsInOrderByValidFromDesc(persons))
            .thenReturn(List.of(workingTimeEntity, workingTimeNextYear));

        when(settingsService.getSettings()).thenReturn(new Settings());

        final Map<Person, WorkingTimeCalendar> actual = sut.getWorkingTimesByPersons(persons, Year.of(2022));
        assertThat(actual)
            .hasSize(1)
            .containsKey(person);

        final WorkingTimeCalendar workingTimeCalendar = actual.get(person);
        for (LocalDate date : dateRange) {
            assertThat(workingTimeCalendar.workingTime(date)).hasValue(BigDecimal.ONE);
        }

        assertThat(workingTimeCalendar.workingTime(LocalDate.of(2021, DECEMBER, 31))).isEmpty();
        assertThat(workingTimeCalendar.workingTime(LocalDate.of(2023, JANUARY, 1))).isEmpty();
    }

    @Test
    void ensureGetWorkingTimesByPersonsAndYearWithMultipleWorkingTimesInOneYear() {

        final Person person = new Person();
        person.setId(1);

        final WorkingTimeEntity workingTimeEntity = new WorkingTimeEntity();
        workingTimeEntity.setValidFrom(LocalDate.of(2022, JANUARY, 1));
        workingTimeEntity.setPerson(person);
        workingTimeEntity.setMonday(FULL);
        workingTimeEntity.setTuesday(FULL);
        workingTimeEntity.setWednesday(FULL);
        workingTimeEntity.setThursday(FULL);
        workingTimeEntity.setFriday(FULL);
        workingTimeEntity.setSaturday(FULL);
        workingTimeEntity.setSunday(FULL);
        workingTimeEntity.setFederalStateOverride(SWITZERLAND_GENF);

        final WorkingTimeEntity workingTimeLaterInYear = new WorkingTimeEntity();
        workingTimeLaterInYear.setValidFrom(LocalDate.of(2022, APRIL, 1));
        workingTimeLaterInYear.setPerson(person);
        workingTimeLaterInYear.setMonday(MORNING);
        workingTimeLaterInYear.setTuesday(MORNING);
        workingTimeLaterInYear.setWednesday(MORNING);
        workingTimeLaterInYear.setThursday(MORNING);
        workingTimeLaterInYear.setFriday(MORNING);
        workingTimeLaterInYear.setSaturday(MORNING);
        workingTimeLaterInYear.setSunday(MORNING);
        workingTimeLaterInYear.setFederalStateOverride(GERMANY_BADEN_WUERTTEMBERG);

        when(workingTimeRepository.findByPersonIsInOrderByValidFromDesc(List.of(person)))
            .thenReturn(List.of(workingTimeLaterInYear, workingTimeEntity));

        when(settingsService.getSettings()).thenReturn(new Settings());

        final Map<Person, WorkingTimeCalendar> actual = sut.getWorkingTimesByPersons(List.of(person), Year.of(2022));
        assertThat(actual)
            .hasSize(1)
            .containsKey(person);

        final WorkingTimeCalendar workingTimeCalendar = actual.get(person);
        for (LocalDate date : new DateRange(LocalDate.of(2022, JANUARY, 1), LocalDate.of(2022, MARCH, 31))) {
            assertThat(workingTimeCalendar.workingTime(date)).hasValue(BigDecimal.ONE);
        }
        for (LocalDate date : new DateRange(LocalDate.of(2022, APRIL, 1), LocalDate.of(2022, DECEMBER, 31))) {
            assertThat(workingTimeCalendar.workingTime(date)).hasValue(BigDecimal.valueOf(0.5));
        }
    }

    @Test
    void deleteAllDelegatesToRepo() {
        final Person person = new Person();

        sut.deleteAllByPerson(person);

        verify(workingTimeRepository).deleteByPerson(person);
    }

    @Test
    void ensureGetWorkingTimesByPersonsAndYearReturnsEmptyWorkingTimeCalendarForPersonWithoutWorkingTime() {
        final Person person = new Person();
        person.setId(1);

        final List<Person> persons = List.of(person);
        final DateRange dateRange = new DateRange(LocalDate.of(2022, JANUARY, 1), LocalDate.of(2022, DECEMBER, 31));

        when(settingsService.getSettings()).thenReturn(new Settings());

        when(workingTimeRepository.findByPersonIsInOrderByValidFromDesc(persons)).thenReturn(List.of());

        final Map<Person, WorkingTimeCalendar> actual = sut.getWorkingTimesByPersons(persons, Year.of(2022));
        assertThat(actual)
            .hasSize(1)
            .containsKey(person);

        final WorkingTimeCalendar workingTimeCalendar = actual.get(person);
        for (LocalDate date : dateRange) {
            assertThat(workingTimeCalendar.workingTime(date)).isEmpty();
        }
    }
}
