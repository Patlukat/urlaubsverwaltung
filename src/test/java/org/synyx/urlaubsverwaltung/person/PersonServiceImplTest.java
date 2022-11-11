package org.synyx.urlaubsverwaltung.person;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.synyx.urlaubsverwaltung.account.AccountInteractionService;
import org.synyx.urlaubsverwaltung.search.PageableSearchQuery;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeWriteService;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.TestDataCreator.createPerson;
import static org.synyx.urlaubsverwaltung.person.MailNotification.NOTIFICATION_BOSS_ALL;
import static org.synyx.urlaubsverwaltung.person.MailNotification.NOTIFICATION_OFFICE;
import static org.synyx.urlaubsverwaltung.person.MailNotification.NOTIFICATION_USER;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.INACTIVE;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.USER;

@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

    private PersonServiceImpl sut;

    @Mock
    private PersonRepository personRepository;
    @Mock
    private AccountInteractionService accountInteractionService;
    @Mock
    private WorkingTimeWriteService workingTimeWriteService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<PersonDisabledEvent> personDisabledEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<PersonCreatedEvent> personCreatedEventArgumentCaptor;
    private final ArgumentCaptor<PersonDeletedEvent> personDeletedEventArgumentCaptor = ArgumentCaptor.forClass(PersonDeletedEvent.class);

    @BeforeEach
    void setUp() {
        sut = new PersonServiceImpl(personRepository, accountInteractionService, workingTimeWriteService, applicationEventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ensureDefaultAccountAndWorkingTimeCreation() {
        when(personRepository.save(any(Person.class))).thenReturn(new Person());

        sut.create("rick", "Grimes", "Rick", "rick@grimes.de", emptyList(), emptyList());
        verify(accountInteractionService).createDefaultAccount(any(Person.class));
        verify(workingTimeWriteService).createDefaultWorkingTime(any(Person.class));
    }

    @Test
    void ensurePersonCreatedEventIsFired() {

        final Person activePerson = createPerson("my person", USER);
        activePerson.setId(1);

        when(personRepository.save(activePerson)).thenReturn(activePerson);

        sut.create(activePerson);

        verify(applicationEventPublisher).publishEvent(personCreatedEventArgumentCaptor.capture());
        assertThat(personCreatedEventArgumentCaptor.getValue().getPersonId())
            .isEqualTo(activePerson.getId());
    }

    @Test
    void ensureCreatedPersonHasCorrectAttributes() {

        final Person person = new Person("rick", "Grimes", "Rick", "rick@grimes.de");
        person.setPermissions(asList(USER, BOSS));
        person.setNotifications(asList(NOTIFICATION_USER, NOTIFICATION_BOSS_ALL));

        when(personRepository.save(person)).thenReturn(person);

        final Person createdPerson = sut.create(person);
        assertThat(createdPerson.getUsername()).isEqualTo("rick");
        assertThat(createdPerson.getFirstName()).isEqualTo("Rick");
        assertThat(createdPerson.getLastName()).isEqualTo("Grimes");
        assertThat(createdPerson.getEmail()).isEqualTo("rick@grimes.de");

        assertThat(createdPerson.getNotifications())
            .hasSize(2)
            .contains(NOTIFICATION_USER, NOTIFICATION_BOSS_ALL);

        assertThat(createdPerson.getPermissions())
            .hasSize(2)
            .contains(USER, BOSS);

        verify(accountInteractionService).createDefaultAccount(person);
        verify(workingTimeWriteService).createDefaultWorkingTime(person);
    }

    @Test
    void ensureCreatedPersonIsPersisted() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        when(personRepository.save(person)).thenReturn(person);

        final Person savedPerson = sut.create(person);
        assertThat(savedPerson).isEqualTo(person);
    }

    @Test
    void ensureNotificationIsSendForCreatedPerson() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        when(personRepository.save(person)).thenReturn(person);

        final Person createdPerson = sut.create(person);

        verify(applicationEventPublisher).publishEvent(personCreatedEventArgumentCaptor.capture());

        final PersonCreatedEvent personCreatedEvent = personCreatedEventArgumentCaptor.getValue();
        assertThat(personCreatedEvent.getSource()).isEqualTo(sut);
        assertThat(personCreatedEvent.getPersonId()).isEqualTo(createdPerson.getId());
        assertThat(personCreatedEvent.getPersonNiceName()).isEqualTo(createdPerson.getNiceName());
    }

    @Test
    void ensureUpdatedPersonHasCorrectAttributes() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(42);

        when(personRepository.findById(anyInt())).thenReturn(Optional.of(person));
        when(personRepository.save(person)).thenReturn(person);

        final Person updatedPerson = sut.update(42, "rick", "Grimes", "Rick", "rick@grimes.de",
            asList(NOTIFICATION_USER, NOTIFICATION_BOSS_ALL),
            asList(USER, BOSS));
        assertThat(updatedPerson.getUsername()).isEqualTo("rick");
        assertThat(updatedPerson.getFirstName()).isEqualTo("Rick");
        assertThat(updatedPerson.getLastName()).isEqualTo("Grimes");
        assertThat(updatedPerson.getEmail()).isEqualTo("rick@grimes.de");

        assertThat(updatedPerson.getNotifications())
            .hasSize(2)
            .contains(NOTIFICATION_USER)
            .contains(NOTIFICATION_BOSS_ALL);

        assertThat(updatedPerson.getPermissions())
            .hasSize(2)
            .contains(USER)
            .contains(BOSS);
    }

    @Test
    void ensureUpdatedPersonIsPersisted() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        when(personRepository.save(person)).thenReturn(person);

        sut.update(person);
        verify(personRepository).save(person);
    }

    @Test
    void ensureThrowsIfPersonToBeUpdatedHasNoID() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(null);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> sut.update(person));
    }

    @Test
    void ensureSaveCallsCorrectDaoMethod() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        when(personRepository.save(person)).thenReturn(person);

        final Person savedPerson = sut.create(person);
        assertThat(savedPerson).isEqualTo(person);
    }

    @Test
    void ensureGetPersonByIDCallsCorrectDaoMethod() {

        sut.getPersonByID(123);
        verify(personRepository).findById(123);
    }

    @Test
    void ensureGetPersonByLoginCallsCorrectDaoMethod() {
        final String username = "foo";
        sut.getPersonByUsername(username);

        verify(personRepository).findByUsername(username);
    }

    @Test
    void ensureGetPersonByMailAddressDelegatesToRepository() {
        final String mailAddress = "foo@bar.test";
        sut.getPersonByMailAddress(mailAddress);

        verify(personRepository).findByEmail(mailAddress);
    }

    @Test
    void ensureGetActivePersonsReturnsOnlyPersonsThatHaveNotInactiveRole() {

        final Person user = new Person("muster", "Muster", "Marlene", "muster@example.org");
        user.setPermissions(List.of(USER));

        final Person boss = new Person("muster", "Muster", "Marlene", "muster@example.org");
        boss.setPermissions(asList(USER, BOSS));

        final Person office = new Person("muster", "Muster", "Marlene", "muster@example.org");
        office.setPermissions(asList(USER, BOSS, OFFICE));

        when(personRepository.findByPermissionsNotContainingOrderByFirstNameAscLastNameAsc(INACTIVE)).thenReturn(List.of(user, boss, office));

        final List<Person> activePersons = sut.getActivePersons();
        assertThat(activePersons)
            .hasSize(3)
            .contains(user)
            .contains(boss)
            .contains(office);
    }

    @Test
    void ensureGetActivePersonsPage() {

        final Page<Person> expected = Page.empty();
        final PageRequest pageRequest = PageRequest.of(1, 100);
        final PageableSearchQuery personPageableSearchQuery = new PageableSearchQuery(pageRequest, "name-query");

        when(personRepository.findByPermissionsNotContainingAndByNiceNameContainingIgnoreCase(INACTIVE, "name-query", pageRequest)).thenReturn(expected);

        final Page<Person> actual = sut.getActivePersons(personPageableSearchQuery);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void ensureGetInactivePersonsReturnsOnlyPersonsThatHaveInactiveRole() {

        final Person inactive = new Person("muster", "Muster", "Marlene", "muster@example.org");
        inactive.setPermissions(List.of(INACTIVE));

        when(personRepository.findByPermissionsContainingOrderByFirstNameAscLastNameAsc(INACTIVE)).thenReturn(List.of(inactive));

        List<Person> inactivePersons = sut.getInactivePersons();
        assertThat(inactivePersons)
            .hasSize(1)
            .contains(inactive);
    }

    @Test
    void ensureGetInactivePersonsPage() {

        final Page<Person> expected = Page.empty();
        final PageRequest pageRequest = PageRequest.of(1, 100, Sort.by(Sort.Direction.ASC, "firstName"));
        final PageableSearchQuery personPageableSearchQuery = new PageableSearchQuery(pageRequest, "name-query");

        // currently a hard coded pageRequest is used in implementation
        final PageRequest pageRequestInternal = PageRequest.of(1, 100, Sort.Direction.ASC, "firstName", "lastName");
        when(personRepository.findByPermissionsContainingAndNiceNameContainingIgnoreCase(INACTIVE, "name-query", pageRequestInternal)).thenReturn(expected);

        final Page<Person> actual = sut.getInactivePersons(personPageableSearchQuery);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void ensureGetPersonsByRoleReturnsOnlyPersonsWithTheGivenRole() {

        final Person boss = new Person("muster", "Muster", "Marlene", "muster@example.org");
        boss.setPermissions(asList(USER, BOSS));

        final Person bossOffice = new Person("muster", "Muster", "Marlene", "muster@example.org");
        bossOffice.setPermissions(asList(USER, BOSS, OFFICE));

        when(personRepository.findByPermissionsContainingAndPermissionsNotContainingOrderByFirstNameAscLastNameAsc(BOSS, INACTIVE)).thenReturn(asList(boss, bossOffice));

        final List<Person> filteredList = sut.getActivePersonsByRole(BOSS);
        assertThat(filteredList)
            .hasSize(2)
            .contains(boss)
            .contains(bossOffice);
    }

    @Test
    void ensureGetPersonsByNotificationTypeReturnsOnlyPersonsWithTheGivenNotificationType() {

        final Person boss = new Person("muster", "Muster", "Marlene", "muster@example.org");
        boss.setPermissions(asList(USER, BOSS));
        boss.setNotifications(asList(NOTIFICATION_USER, NOTIFICATION_BOSS_ALL));

        final Person office = new Person("muster", "Muster", "Marlene", "muster@example.org");
        office.setPermissions(asList(USER, BOSS, OFFICE));
        office.setNotifications(asList(NOTIFICATION_USER, NOTIFICATION_BOSS_ALL, NOTIFICATION_OFFICE));

        when(personRepository.findByPermissionsNotContainingAndNotificationsContainingOrderByFirstNameAscLastNameAsc(INACTIVE, NOTIFICATION_BOSS_ALL)).thenReturn(List.of(boss, office));

        final List<Person> filteredList = sut.getActivePersonsWithNotificationType(NOTIFICATION_BOSS_ALL);
        assertThat(filteredList)
            .hasSize(2)
            .contains(boss)
            .contains(office);
    }

    @Test
    void ensureThrowsIfNoPersonCanBeFoundForTheCurrentlySignedInUser() {
        assertThatIllegalStateException()
            .isThrownBy(() -> sut.getSignedInUser());
    }

    @Test
    void ensureReturnsPersonForCurrentlySignedInUser() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        when(personRepository.findByUsername("muster")).thenReturn(Optional.of(person));

        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(person.getUsername());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        final Person signedInUser = sut.getSignedInUser();
        assertThat(signedInUser).isEqualTo(person);
    }

    @Test
    void ensureThrowsIllegalOnNullAuthentication() {
        assertThatIllegalStateException()
            .isThrownBy(() -> sut.getSignedInUser());
    }

    @Test
    void ensureCanAppointPersonAsOfficeUser() {

        when(personRepository.findByPermissionsContainingAndPermissionsNotContainingOrderByFirstNameAscLastNameAsc(OFFICE, INACTIVE)).thenReturn(emptyList());
        when(personRepository.save(any())).then(returnsFirstArg());

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setPermissions(List.of(USER));
        assertThat(person.getPermissions()).containsOnly(USER);

        final Person personWithOfficeRole = sut.appointAsOfficeUserIfNoOfficeUserPresent(person);
        assertThat(personWithOfficeRole.getPermissions())
            .hasSize(2)
            .contains(USER, OFFICE);
    }

    @Test
    void ensureCanNotAppointPersonAsOfficeUser() {

        final Person officePerson = new Person();
        officePerson.setPermissions(List.of(OFFICE));
        when(personRepository.findByPermissionsContainingAndPermissionsNotContainingOrderByFirstNameAscLastNameAsc(OFFICE, INACTIVE)).thenReturn(List.of(officePerson));

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setPermissions(List.of(USER));
        assertThat(person.getPermissions()).containsOnly(USER);

        final Person personWithOfficeRole = sut.appointAsOfficeUserIfNoOfficeUserPresent(person);
        assertThat(personWithOfficeRole.getPermissions())
            .containsOnly(USER);
    }

    @Test
    void ensurePersonUpdatedEventIsFiredAfterUpdate() {

        final Person activePerson = createPerson("active person", USER);
        activePerson.setId(1);
        when(personRepository.save(activePerson)).thenReturn(activePerson);

        sut.update(activePerson);
        verify(applicationEventPublisher).publishEvent(any(PersonUpdatedEvent.class));
    }

    @Test
    void ensurePersonDisabledEventIsFiredAfterPersonUpdate() {

        final Person inactivePerson = createPerson("inactive person", INACTIVE);
        inactivePerson.setId(1);
        when(personRepository.save(inactivePerson)).thenReturn(inactivePerson);

        sut.update(inactivePerson);
        verify(applicationEventPublisher).publishEvent(any(PersonDisabledEvent.class));
    }

    @Test
    void ensurePersonDisabledEventIsNotFiredAfterPersonUpdateAndRoleNotInactive() {

        final Person inactivePerson = createPerson("inactive person", USER);
        inactivePerson.setId(1);
        when(personRepository.save(inactivePerson)).thenReturn(inactivePerson);

        sut.update(inactivePerson);
        verify(applicationEventPublisher, never()).publishEvent(any(PersonDisabledEvent.class));
    }

    @Test
    void numberOfActivePersons() {

        when(personRepository.countByPermissionsNotContaining(INACTIVE)).thenReturn(2);

        final int numberOfActivePersons = sut.numberOfActivePersons();
        assertThat(numberOfActivePersons).isEqualTo(2);
    }

    @Test
    void deletesExistingPersonDelegatesAndSendsEvent() {

        final Person signedInUser = new Person("signedInUser", "signed", "in", "user@example.org");

        final Person person = new Person();
        final int personId = 42;
        person.setId(personId);
        when(personRepository.existsById(personId)).thenReturn(true);

        sut.delete(person, signedInUser);

        final InOrder inOrder = inOrder(applicationEventPublisher, accountInteractionService, workingTimeWriteService, personRepository);

        inOrder.verify(personRepository).existsById(42);
        inOrder.verify(applicationEventPublisher).publishEvent(personDeletedEventArgumentCaptor.capture());
        assertThat(personDeletedEventArgumentCaptor.getValue().getPerson())
            .isEqualTo(person);

        inOrder.verify(accountInteractionService).deleteAllByPerson(person);
        inOrder.verify(workingTimeWriteService).deleteAllByPerson(person);
        inOrder.verify(personRepository).delete(person);
    }

    @Test
    void deletingNotExistingPersonThrowsException() {
        final Person signedInUser = new Person("signedInUser", "signed", "in", "user@example.org");

        final Person person = new Person();
        person.setId(1);
        assertThatThrownBy(() -> sut.delete(person, signedInUser)).isInstanceOf(IllegalArgumentException.class);

        verify(personRepository).existsById(1);
        verifyNoMoreInteractions(applicationEventPublisher, workingTimeWriteService, accountInteractionService, personRepository);
    }

    @Test
    void numberOfPersonsWithRoleWithoutId() {

        when(personRepository.countByPermissionsContainingAndIdNotIn(OFFICE, List.of(1))).thenReturn(2);

        final int numberOfOfficeExceptId = sut.numberOfPersonsWithOfficeRoleExcludingPerson(1);
        assertThat(numberOfOfficeExceptId).isEqualTo(2);
    }
}
