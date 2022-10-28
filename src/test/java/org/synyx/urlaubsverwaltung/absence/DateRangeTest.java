package org.synyx.urlaubsverwaltung.absence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateRangeTest {

    static Stream<Arguments> canInstantiateDateRange() {
        return Stream.of(
            Arguments.of("2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-10")
        );
    }

    @ParameterizedTest
    @MethodSource("canInstantiateDateRange")
    void canInstantiateDateRange(LocalDate startDate, LocalDate endDate) {
        assertThatCode(() -> new DateRange(startDate, endDate)).doesNotThrowAnyException();
    }

    @Test
    void canNotInstantiateDateRange() {
        final LocalDate startDate = LocalDate.parse("2020-10-16");
        final LocalDate endDate = LocalDate.parse("2020-10-15");
        assertThrows(IllegalArgumentException.class, () -> new DateRange(startDate, endDate));
    }

    static Stream<Arguments> overlappingDateRanges() {
        return Stream.of(
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-12", "2020-10-13"),
            Arguments.of("2020-10-12", "2020-10-13", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-14", "2020-10-16"),
            Arguments.of("2020-10-14", "2020-10-16", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-09", "2020-10-11"),
            Arguments.of("2020-10-09", "2020-10-11", "2020-10-10", "2020-10-15"),

            Arguments.of("2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10"),
            Arguments.of("2020-10-09", "2020-10-10", "2020-10-10", "2020-10-10"),
            Arguments.of("2020-10-10", "2020-10-10", "2020-10-09", "2020-10-10"),
            Arguments.of("2020-10-10", "2020-10-10", "2020-10-10", "2020-10-11"),
            Arguments.of("2020-10-10", "2020-10-11", "2020-10-10", "2020-10-10")
        );
    }

    @ParameterizedTest
    @MethodSource("overlappingDateRanges")
    void isOverlapping(LocalDate startDateOne, LocalDate endDateOne, LocalDate startDateTwo, LocalDate endDateTwo) {
        final DateRange rangeOne = new DateRange(startDateOne, endDateOne);
        final DateRange rangeTwo = new DateRange(startDateTwo, endDateTwo);

        final boolean overlapping = rangeOne.isOverlapping(rangeTwo);
        assertThat(overlapping).isTrue();
    }

    static Stream<Arguments> notOverlappingDateRanges() {
        return Stream.of(
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-08", "2020-10-09"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-16", "2020-10-17"),
            Arguments.of("2020-10-08", "2020-10-09", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-16", "2020-10-17", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-16", "2020-10-17", "2020-10-15", "2020-10-15"),
            Arguments.of("2020-10-15", "2020-10-15", "2020-10-16", "2020-10-17"),
            Arguments.of("2020-10-16", "2020-10-17", "2020-10-18", "2020-10-18"),
            Arguments.of("2020-10-18", "2020-10-18", "2020-10-16", "2020-10-17"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-16", "2020-10-17"),
            Arguments.of("2020-10-16", "2020-10-17", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-07", "2020-10-08"),
            Arguments.of("2020-10-07", "2020-10-08", "2020-10-10", "2020-10-15")
        );
    }

    @ParameterizedTest
    @MethodSource("notOverlappingDateRanges")
    void notOverlapping(LocalDate startDateOne, LocalDate endDateOne, LocalDate startDateTwo, LocalDate endDateTwo) {
        final DateRange rangeOne = new DateRange(startDateOne, endDateOne);
        final DateRange rangeTwo = new DateRange(startDateTwo, endDateTwo);

        final boolean overlapping = rangeOne.isOverlapping(rangeTwo);
        assertThat(overlapping).isFalse();
    }

    static Stream<Arguments> overlapDateRanges() {
        return Stream.of(
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-10", "2020-10-15", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-12", "2020-10-13", "2020-10-12", "2020-10-13"),
            Arguments.of("2020-10-12", "2020-10-13", "2020-10-10", "2020-10-15", "2020-10-12", "2020-10-13"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-14", "2020-10-16", "2020-10-14", "2020-10-15"),
            Arguments.of("2020-10-14", "2020-10-16", "2020-10-10", "2020-10-15", "2020-10-14", "2020-10-15"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-09", "2020-10-11", "2020-10-10", "2020-10-11"),
            Arguments.of("2020-10-09", "2020-10-11", "2020-10-10", "2020-10-15", "2020-10-10", "2020-10-11"),

            Arguments.of("2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10"),
            Arguments.of("2020-10-09", "2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10"),
            Arguments.of("2020-10-10", "2020-10-10", "2020-10-09", "2020-10-10", "2020-10-10", "2020-10-10"),
            Arguments.of("2020-10-10", "2020-10-10", "2020-10-10", "2020-10-11", "2020-10-10", "2020-10-10"),
            Arguments.of("2020-10-10", "2020-10-11", "2020-10-10", "2020-10-10", "2020-10-10", "2020-10-10")
        );
    }

    @ParameterizedTest
    @MethodSource("overlapDateRanges")
    void withOverlap(LocalDate startDateOne, LocalDate endDateOne, LocalDate startDateTwo, LocalDate endDateTwo, LocalDate overlapStartDate, LocalDate overlapEndDate) {
        final DateRange rangeOne = new DateRange(startDateOne, endDateOne);
        final DateRange rangeTwo = new DateRange(startDateTwo, endDateTwo);

        final Optional<DateRange> maybeOverlap = rangeOne.overlap(rangeTwo);
        assertThat(maybeOverlap.get().getStartDate()).isEqualTo(overlapStartDate);
        assertThat(maybeOverlap.get().getEndDate()).isEqualTo(overlapEndDate);
    }

    static Stream<Arguments> gapDateRanges() {
        return Stream.of(
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-07", "2020-10-08", "2020-10-09", "2020-10-09"),
            Arguments.of("2020-10-07", "2020-10-08", "2020-10-10", "2020-10-15", "2020-10-09", "2020-10-09"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-07", "2020-10-07", "2020-10-08", "2020-10-09"),
            Arguments.of("2020-10-07", "2020-10-07", "2020-10-10", "2020-10-15", "2020-10-08", "2020-10-09")
        );
    }

    @ParameterizedTest
    @MethodSource("gapDateRanges")
    void gap(LocalDate startDateOne, LocalDate endDateOne, LocalDate startDateTwo, LocalDate endDateTwo, LocalDate gapStartDate, LocalDate gapEndDate) {
        final DateRange rangeOne = new DateRange(startDateOne, endDateOne);
        final DateRange rangeTwo = new DateRange(startDateTwo, endDateTwo);

        final Optional<DateRange> maybeGap = rangeOne.gap(rangeTwo);
        assertThat(maybeGap)
            .hasValueSatisfying(localDates -> assertThat(localDates.getStartDate()).isEqualTo(gapStartDate))
            .hasValueSatisfying(localDates -> assertThat(localDates.getEndDate()).isEqualTo(gapEndDate));
    }

    static Stream<Arguments> gapDateRangesEmpty() {
        return Stream.of(
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-08", "2020-10-09"),
            Arguments.of("2020-10-10", "2020-10-15", "2020-10-16", "2020-10-17"),
            Arguments.of("2020-10-08", "2020-10-09", "2020-10-10", "2020-10-15"),
            Arguments.of("2020-10-16", "2020-10-17", "2020-10-10", "2020-10-15")
        );
    }

    @ParameterizedTest
    @MethodSource("gapDateRangesEmpty")
    void gapEmpty(LocalDate startDateOne, LocalDate endDateOne, LocalDate startDateTwo, LocalDate endDateTwo) {
        final DateRange rangeOne = new DateRange(startDateOne, endDateOne);
        final DateRange rangeTwo = new DateRange(startDateTwo, endDateTwo);

        final Optional<DateRange> maybeGap = rangeOne.gap(rangeTwo);
        assertThat(maybeGap).isEmpty();
    }

    @Test
    void dateRangeisEmpty() {
        final boolean isEmpty = new DateRange(null, null).isEmpty();
        assertThat(isEmpty).isTrue();
    }

    @Test
    void dateRangeIsNotEmpty() {
        final boolean isEmpty = new DateRange(LocalDate.MIN, LocalDate.MAX).isEmpty();
        assertThat(isEmpty).isFalse();
    }

    @Test
    void dateRangeDurationReturnsCorrectValueIfNotEmpty() {
        final Duration duration = new DateRange(LocalDate.of(2022,10, 10), LocalDate.of(2022,10, 20)).duration();
        assertThat(duration).isEqualTo(Duration.ofDays(11));
    }

    @Test
    void dateRangeDurationReturnsOneOnSameDay() {
        final LocalDate date = LocalDate.of(2022, 10, 10);
        final Duration duration = new DateRange(date, date).duration();
        assertThat(duration).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void dateRangeDurationReturnsZeroIfEmpty() {
        final Duration duration = new DateRange(null, null).duration();
        assertThat(duration).isZero();
    }

    @Test
    void equalsTest() {
        final DateRange commentOne = new DateRange(LocalDate.MIN, LocalDate.MAX);
        final DateRange commentOneOne = new DateRange(LocalDate.MIN, LocalDate.MAX);
        final DateRange commentTwo = new DateRange(LocalDate.MIN.plusDays(1), LocalDate.MAX.minusDays(1));

        assertThat(commentOne)
            .isEqualTo(commentOne)
            .isEqualTo(commentOneOne)
            .isNotEqualTo(commentTwo)
            .isNotEqualTo(new Object())
            .isNotEqualTo(null);
    }

    @Test
    void hashCodeTest() {
        final DateRange dateRange = new DateRange(LocalDate.MIN, LocalDate.MAX);
        assertThat(dateRange.hashCode()).isEqualTo(-1163458881);
    }
}
