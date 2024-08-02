package org.synyx.urlaubsverwaltung.sicknote.sicknote;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.synyx.urlaubsverwaltung.TestContainersBase;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;

import java.time.LocalDate;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SickNoteExtensionRepositoryIT  extends TestContainersBase {

    @Autowired
    private SickNoteExtensionRepository sut;

    @Autowired
    private SickNoteService sickNoteService;
    @Autowired
    private SickNoteExtensionService sickNoteExtensionService;
    @Autowired
    private PersonService personService;

    @Test
    void ensureFindAllBySickNoteIdOrderByCreatedAt() {

        final LocalDate now = LocalDate.now(UTC);

        final Person person = personService.create("batman", "Bruce", "Wayne", "batman@example.org");
        final SickNote sickNoteToSave = sickNoteService.save(SickNote.builder().person(person).startDate(now.minusDays(10)).endDate(now.minusDays(10)).build());
        final SickNote sickNote = sickNoteService.save(sickNoteToSave);

        final Long sickNoteId = sickNote.getId();

        sickNoteExtensionService.submitSickNoteExtension(person, sickNoteId, now.plusDays(1), false);
        sickNoteExtensionService.submitSickNoteExtension(person, sickNoteId, now.plusDays(2), false);

        final List<SickNoteExtensionEntity> actual = sut.findAllBySickNoteIdOrderByCreatedAtDesc(sickNoteId);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getNewEndDate()).isEqualTo(now.plusDays(2));
        assertThat(actual.get(1).getNewEndDate()).isEqualTo(now.plusDays(1));
    }
}
