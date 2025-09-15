package org.synyx.urlaubsverwaltung.overtime;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

public class OvertimeDetailRecordDto {

    private final Long id;
    private final OvertimeDetailPersonDto person;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Duration duration;
    private final Map<Integer, Duration> durationByYear;
    private final LocalDate lastModificationDate;
    private final boolean isExternal;

    OvertimeDetailRecordDto(
        Long id,
        OvertimeDetailPersonDto person,
        LocalDate startDate,
        LocalDate endDate,
        Duration duration,
        Map<Integer, Duration> durationByYear,
        LocalDate lastModificationDate,
        boolean isExternal
    ) {
        this.id = id;
        this.person = person;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.durationByYear = durationByYear;
        this.lastModificationDate = lastModificationDate;
        this.isExternal = isExternal;
    }

    public Long getId() {
        return id;
    }

    public OvertimeDetailPersonDto getPerson() {
        return person;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Duration getDuration() {
        return duration;
    }

    public LocalDate getLastModificationDate() {
        return lastModificationDate;
    }

    public Map<Integer, Duration> getDurationByYear() {
        return durationByYear;
    }

    public Map<Integer, Duration> getDurationByYear(int withoutYear) {
        return durationByYear.entrySet().stream()
            .filter(e -> e.getKey() != withoutYear)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isExternal() {
        return isExternal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeDetailRecordDto that = (OvertimeDetailRecordDto) o;
        return Objects.equals(id, that.id)
            && Objects.equals(person, that.person)
            && Objects.equals(startDate, that.startDate)
            && Objects.equals(endDate, that.endDate)
            && Objects.equals(duration, that.duration)
            && Objects.equals(durationByYear, that.durationByYear)
            && Objects.equals(lastModificationDate, that.lastModificationDate)
            && Objects.equals(isExternal, that.isExternal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, person, startDate, endDate, duration, durationByYear, lastModificationDate, isExternal);
    }

    @Override
    public String toString() {
        return "OvertimeDetailRecordDto{" +
            "id=" + id +
            ", person=" + person +
            ", startDate=" + startDate +
            ", endDate=" + endDate +
            ", duration=" + duration +
            ", durationByYear=" + durationByYear +
            ", lastModificationDate=" + lastModificationDate +
            ", isExternal=" + isExternal +
            '}';
    }
}
