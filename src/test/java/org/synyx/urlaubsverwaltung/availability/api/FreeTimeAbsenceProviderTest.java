package org.synyx.urlaubsverwaltung.availability.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.TestDataCreator.createWorkingTime;
import static org.synyx.urlaubsverwaltung.period.DayLength.FULL;


@ExtendWith(MockitoExtension.class)
class FreeTimeAbsenceProviderTest {

    private FreeTimeAbsenceProvider sut;

    @Mock
    private PublicHolidayAbsenceProvider publicHolidayAbsenceProvider;
    @Mock
    private WorkingTimeService workingTimeService;

    @BeforeEach
    void setUp() {
        sut = new FreeTimeAbsenceProvider(publicHolidayAbsenceProvider, workingTimeService);
    }

    @Test
    void ensurePersonIsNotAvailableOnFreeDays() {

        when(workingTimeService.getWorkingTime(any(Person.class), any(LocalDate.class))).thenReturn(Optional.of(createWorkingTime()));

        final LocalDate firstSundayIn2016 = LocalDate.of(2016, 1, 3);
        final TimedAbsenceSpans emptyTimedAbsenceSpans = new TimedAbsenceSpans(new ArrayList<>());

        final TimedAbsenceSpans updatedTimedAbsenceSpans = sut.addAbsence(emptyTimedAbsenceSpans, new Person("muster", "Muster", "Marlene", "muster@example.org"), firstSundayIn2016);
        assertThat(updatedTimedAbsenceSpans.getAbsencesList()).hasSize(1);
        assertThat(updatedTimedAbsenceSpans.getAbsencesList().get(0).getPartOfDay()).isEqualTo(FULL.name());
        assertThat(updatedTimedAbsenceSpans.getAbsencesList().get(0).getRatio()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void ensureExceptionWhenPersonWorkingTimeIsNotAvailable() {

        final LocalDate firstSundayIn2016 = LocalDate.of(2016, 1, 3);
        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        when(workingTimeService.getWorkingTime(person, firstSundayIn2016)).thenReturn(Optional.empty());

        final TimedAbsenceSpans knownAbsences = new TimedAbsenceSpans(new ArrayList<>());
        assertThatThrownBy(() -> sut.addAbsence(knownAbsences, person, firstSundayIn2016))
            .isInstanceOf(FreeTimeAbsenceException.class);
    }

    @Test
    void ensureDoesNotCallNextProviderIfAlreadyAbsentForWholeDay() {

        when(workingTimeService.getWorkingTime(any(Person.class), any(LocalDate.class))).thenReturn(Optional.of(createWorkingTime()));

        final LocalDate firstSundayIn2016 = LocalDate.of(2016, 1, 3);
        final TimedAbsenceSpans timedAbsenceSpans = new TimedAbsenceSpans(new ArrayList<>());

        sut.checkForAbsence(timedAbsenceSpans, new Person("muster", "Muster", "Marlene", "muster@example.org"), firstSundayIn2016);
        verifyNoMoreInteractions(publicHolidayAbsenceProvider);
    }

    @Test
    void ensureCallsHolidayAbsenceProviderIfNotAbsentForFreeTime() {

        when(workingTimeService.getWorkingTime(any(Person.class), any(LocalDate.class))).thenReturn(Optional.of(createWorkingTime()));

        final LocalDate standardWorkingDay = LocalDate.of(2016, 1, 4);
        final TimedAbsenceSpans emptyTimedAbsenceSpans = new TimedAbsenceSpans(new ArrayList<>());
        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        sut.checkForAbsence(emptyTimedAbsenceSpans, person, standardWorkingDay);
        verify(publicHolidayAbsenceProvider, times(1)).checkForAbsence(emptyTimedAbsenceSpans, person, standardWorkingDay);
    }
}
