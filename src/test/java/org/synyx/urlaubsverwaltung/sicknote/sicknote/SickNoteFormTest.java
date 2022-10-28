package org.synyx.urlaubsverwaltung.sicknote.sicknote;

import org.junit.jupiter.api.Test;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.sicknote.sicknotetype.SickNoteType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SickNoteFormTest {

    private final LocalDate day2019_04_16 = LocalDate.of(2019, 4, 16);
    private final Integer id = 1;
    private final Person person = new Person();
    private final SickNoteType type = new SickNoteType();

    @Test
    void ensureEmptyStartDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setStartDate(null);

        assertThat(sut.getStartDateIsoValue()).isEmpty();
    }

    @Test
    void ensureStartDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setStartDate(LocalDate.parse("2020-10-30"));

        assertThat(sut.getStartDateIsoValue()).isEqualTo("2020-10-30");
    }

    @Test
    void ensureEmptyEndDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setEndDate(null);

        assertThat(sut.getEndDateIsoValue()).isEmpty();
    }

    @Test
    void ensureEndDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setEndDate(LocalDate.parse("2020-10-30"));

        assertThat(sut.getEndDateIsoValue()).isEqualTo("2020-10-30");
    }

    @Test
    void ensureEmptyAubStartDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setAubStartDate(null);

        assertThat(sut.getAubStartDateIsoValue()).isEmpty();
    }

    @Test
    void ensureAubStartDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setAubStartDate(LocalDate.parse("2020-10-30"));

        assertThat(sut.getAubStartDateIsoValue()).isEqualTo("2020-10-30");
    }

    @Test
    void ensureEmptyAubEndDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setAubEndDate(null);

        assertThat(sut.getAubEndDateIsoValue()).isEmpty();
    }

    @Test
    void ensureAubEndDateValidFromIsoValue() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setAubEndDate(LocalDate.parse("2020-10-30"));

        assertThat(sut.getAubEndDateIsoValue()).isEqualTo("2020-10-30");
    }

    @Test
    void checkGeneratedSickNote() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setId(id);
        sut.setPerson(person);
        sut.setSickNoteType(type);
        sut.setStartDate(day2019_04_16);
        sut.setEndDate(day2019_04_16);
        sut.setDayLength(DayLength.FULL);
        sut.setAubStartDate(day2019_04_16);
        sut.setAubEndDate(day2019_04_16);
        sut.setComment("my comment");

        SickNote sickNote = sut.generateSickNote();


        assertThat(sickNote.getId()).isEqualTo(id);
        assertThat(sickNote.getPerson()).isEqualTo(person);
        assertThat(sickNote.getSickNoteType()).isEqualTo(type);
        assertThat(sickNote.getStartDate()).isEqualTo(day2019_04_16);
        assertThat(sickNote.getEndDate()).isEqualTo(day2019_04_16);
        assertThat(sickNote.getDayLength()).isEqualTo(DayLength.FULL);
        assertThat(sickNote.getAubStartDate()).isEqualTo(day2019_04_16);
        assertThat(sickNote.getAubEndDate()).isEqualTo(day2019_04_16);
    }

    @Test
    void checkCopyConstructure() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setId(id);
        sut.setPerson(person);
        sut.setSickNoteType(type);
        sut.setStartDate(day2019_04_16);
        sut.setEndDate(day2019_04_16);
        sut.setDayLength(DayLength.FULL);
        sut.setAubStartDate(day2019_04_16);
        sut.setAubEndDate(day2019_04_16);
        sut.setComment("my comment");

        SickNote sickNote = sut.generateSickNote();

        SickNoteForm sickNoteForm = new SickNoteForm(sickNote);

        assertThat(sickNoteForm.getId()).isEqualTo(id);
        assertThat(sickNoteForm.getPerson()).isEqualTo(person);
        assertThat(sickNoteForm.getSickNoteType()).isEqualTo(type);
        assertThat(sickNoteForm.getStartDate()).isEqualTo(day2019_04_16);
        assertThat(sickNoteForm.getEndDate()).isEqualTo(day2019_04_16);
        assertThat(sickNoteForm.getDayLength()).isEqualTo(DayLength.FULL);
        assertThat(sickNoteForm.getAubStartDate()).isEqualTo(day2019_04_16);
        assertThat(sickNoteForm.getAubEndDate()).isEqualTo(day2019_04_16);
    }

    @Test
    void toStringTest() {

        final SickNoteForm sut = new SickNoteForm();
        sut.setId(id);
        sut.setPerson(person);
        sut.setSickNoteType(type);
        sut.setStartDate(day2019_04_16);
        sut.setEndDate(day2019_04_16);
        sut.setDayLength(DayLength.FULL);
        sut.setAubStartDate(day2019_04_16);
        sut.setAubEndDate(day2019_04_16);
        sut.setComment("my comment");

        SickNote sickNote = sut.generateSickNote();
        SickNoteForm sickNoteForm = new SickNoteForm(sickNote);

        assertThat(sickNoteForm).hasToString("SickNoteForm{id=1, person=Person{id='null'}, " +
            "sickNoteType=SickNoteType{category=null, messageKey='null'}, " +
            "startDate=2019-04-16, endDate=2019-04-16, dayLength=FULL, aubStartDate=2019-04-16, aubEndDate=2019-04-16'}");
    }
}
