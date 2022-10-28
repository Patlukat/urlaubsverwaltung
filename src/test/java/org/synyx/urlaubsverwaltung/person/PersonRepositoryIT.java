package org.synyx.urlaubsverwaltung.person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.synyx.urlaubsverwaltung.TestContainersBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.synyx.urlaubsverwaltung.person.MailNotification.NOTIFICATION_OFFICE;
import static org.synyx.urlaubsverwaltung.person.MailNotification.NOTIFICATION_USER;
import static org.synyx.urlaubsverwaltung.person.Role.INACTIVE;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.USER;

@SpringBootTest
@Transactional
class PersonRepositoryIT extends TestContainersBase {

    @Autowired
    private PersonRepository sut;

    @Autowired
    private PersonService personService;

    @Test
    void countPersonByPermissionsIsNot() {

        final Person marlene = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        marlene.setPermissions(List.of(USER, INACTIVE));
        personService.create(marlene);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final int countOfActivePersons = sut.countByPermissionsNotContaining(INACTIVE);
        assertThat(countOfActivePersons).isEqualTo(2);
    }

    @Test
    void ensureToFindPersonsWithRoleWithoutTheId() {

        final Person marlene = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        marlene.setPermissions(List.of(USER, INACTIVE));
        personService.create(marlene);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        personService.create(peter);

        final Person simone = new Person("simone", "Muster", "Peter", "simone@example.org");
        simone.setPermissions(List.of(USER, OFFICE));
        personService.create(simone);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER, OFFICE));
        final Person savedBettina = personService.create(bettina);

        final Integer id = savedBettina.getId();
        final int countOfActivePersons = sut.countByPermissionsContainingAndIdNotIn(OFFICE, List.of(id));
        assertThat(countOfActivePersons).isEqualTo(2);
    }

    @Test
    void findByPersonByPermissionsNotContaining() {

        final Person marlene = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        marlene.setPermissions(List.of(USER, INACTIVE));
        personService.create(marlene);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final List<Person> notInactivePersons = sut.findByPermissionsNotContainingOrderByFirstNameAscLastNameAsc(INACTIVE);
        assertThat(notInactivePersons).containsExactly(bettina, peter);
    }

    @Test
    void ensureFindByPersonByPermissionsNotContainingOrderingIsCorrect() {

        final Person xenia = new Person("xenia", "Basta", "xenia", "xenia@example.org");
        xenia.setPermissions(List.of(USER));
        personService.create(xenia);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final List<Person> notInactivePersons = sut.findByPermissionsNotContainingOrderByFirstNameAscLastNameAsc(INACTIVE);
        assertThat(notInactivePersons).containsExactly(bettina, peter, xenia);
    }

    @Test
    void findByPersonByPermissionsContaining() {

        final Person marlene = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        marlene.setPermissions(List.of(USER, INACTIVE));
        personService.create(marlene);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final List<Person> personsWithOfficeRole = sut.findByPermissionsContainingOrderByFirstNameAscLastNameAsc(OFFICE);
        assertThat(personsWithOfficeRole).containsExactly(peter);
    }

    @Test
    void ensureFindByPersonByPermissionsContainingOrderingIsCorrect() {

        final Person xenia = new Person("xenia", "Basta", "xenia", "xenia@example.org");
        xenia.setPermissions(List.of(USER));
        personService.create(xenia);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final List<Person> personsWithUserRole = sut.findByPermissionsContainingOrderByFirstNameAscLastNameAsc(USER);
        assertThat(personsWithUserRole).containsExactly(bettina, peter, xenia);
    }

    @Test
    void ensureFindByPersonByPermissionsContainingAndNotContaining() {

        final Person marlene = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        marlene.setPermissions(List.of(USER, OFFICE, INACTIVE));
        personService.create(marlene);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final List<Person> personsWithOfficeRole = sut.findByPermissionsContainingAndPermissionsNotContainingOrderByFirstNameAscLastNameAsc(OFFICE, INACTIVE);
        assertThat(personsWithOfficeRole).containsExactly(peter);
    }

    @Test
    void ensureFindByPersonByPermissionsContainingAndNotContainingOrderingIsCorrect() {

        final Person xenia = new Person("xenia", "Basta", "xenia", "xenia@example.org");
        xenia.setPermissions(List.of(USER));
        personService.create(xenia);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        personService.create(bettina);

        final List<Person> personsWithUserRole = sut.findByPermissionsContainingAndPermissionsNotContainingOrderByFirstNameAscLastNameAsc(USER, INACTIVE);
        assertThat(personsWithUserRole).containsExactly(bettina, peter, xenia);
    }

    @Test
    void ensureFindByPersonByPermissionsNotContainingAndContainingNotification() {

        final Person marlene = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        marlene.setPermissions(List.of(USER, OFFICE, INACTIVE));
        marlene.setNotifications(List.of(NOTIFICATION_OFFICE));
        personService.create(marlene);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER, OFFICE));
        peter.setNotifications(List.of(NOTIFICATION_OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        bettina.setNotifications(List.of(NOTIFICATION_USER));
        personService.create(bettina);

        final List<Person> personsWithOfficeRole = sut.findByPermissionsNotContainingAndNotificationsContainingOrderByFirstNameAscLastNameAsc(INACTIVE, NOTIFICATION_OFFICE);
        assertThat(personsWithOfficeRole).containsExactly(peter);
    }

    @Test
    void ensureFindByPersonByPermissionsContainingAndContainingNotificationsOrderingIsCorrect() {

        final Person xenia = new Person("xenia", "Basta", "xenia", "xenia@example.org");
        xenia.setPermissions(List.of(USER));
        xenia.setNotifications(List.of(NOTIFICATION_OFFICE));
        personService.create(xenia);

        final Person peter = new Person("peter", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER));
        peter.setNotifications(List.of(NOTIFICATION_OFFICE));
        personService.create(peter);

        final Person bettina = new Person("bettina", "Muster", "bettina", "bettina@example.org");
        bettina.setPermissions(List.of(USER));
        bettina.setNotifications(List.of(NOTIFICATION_OFFICE));
        personService.create(bettina);

        final List<Person> personsWithUserRole = sut.findByPermissionsNotContainingAndNotificationsContainingOrderByFirstNameAscLastNameAsc(INACTIVE, NOTIFICATION_OFFICE);
        assertThat(personsWithUserRole).containsExactly(bettina, peter, xenia);
    }

    @Test
    void ensureFindByPermissionsNotContainingAndByNiceNameContainingIgnoreCase() {

        final Person xenia = new Person("username_1", "Basta", "xenia", "xenia@example.org");
        xenia.setPermissions(List.of(USER));
        personService.create(xenia);

        final Person peter = new Person("username_2", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER));
        personService.create(peter);

        final Person mustafa = new Person("username_3", "Tunichtgut", "Mustafa", "mustafa@example.org");
        mustafa.setPermissions(List.of(INACTIVE));
        personService.create(mustafa);

        final Person rosamund = new Person("username_4", "Hatgoldimmund", "Rosamund", "rosamund@example.org");
        rosamund.setPermissions(List.of(USER));
        personService.create(rosamund);

        final PageRequest pageRequest = PageRequest.of(0, 10);
        final Page<Person> actual = sut.findByPermissionsNotContainingAndByNiceNameContainingIgnoreCase(INACTIVE, "mu", pageRequest);

        assertThat(actual.getContent()).containsExactly(peter, rosamund);
    }

    @Test
    void ensureFindByPermissionsContainingAndNiceNameContainingIgnoreCase() {

        final Person xenia = new Person("username_1", "Basta", "xenia", "xenia@example.org");
        xenia.setPermissions(List.of(USER));
        personService.create(xenia);

        final Person peter = new Person("username_2", "Muster", "Peter", "peter@example.org");
        peter.setPermissions(List.of(USER));
        personService.create(peter);

        final Person mustafa = new Person("username_3", "Tunichtgut", "Mustafa", "mustafa@example.org");
        mustafa.setPermissions(List.of(INACTIVE));
        personService.create(mustafa);

        final Person rosamund = new Person("username_4", "Hatgoldimmund", "Rosamund", "rosamund@example.org");
        rosamund.setPermissions(List.of(USER));
        personService.create(rosamund);

        final PageRequest pageRequest = PageRequest.of(0, 10);
        final Page<Person> actual = sut.findByPermissionsContainingAndNiceNameContainingIgnoreCase(INACTIVE, "mu", pageRequest);

        assertThat(actual.getContent()).containsExactly(mustafa);
    }
}



