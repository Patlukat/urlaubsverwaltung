package org.synyx.urlaubsverwaltung.application.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.absence.Absence;
import org.synyx.urlaubsverwaltung.absence.AbsenceTimeConfiguration;
import org.synyx.urlaubsverwaltung.absence.AbsenceType;
import org.synyx.urlaubsverwaltung.absence.TimeSettings;
import org.synyx.urlaubsverwaltung.application.comment.ApplicationComment;
import org.synyx.urlaubsverwaltung.calendar.ICalService;
import org.synyx.urlaubsverwaltung.calendar.ICalType;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.mail.Mail;
import org.synyx.urlaubsverwaltung.mail.MailService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.synyx.urlaubsverwaltung.absence.AbsenceType.DEFAULT;
import static org.synyx.urlaubsverwaltung.calendar.ICalType.CANCELLED;
import static org.synyx.urlaubsverwaltung.calendar.ICalType.PUBLISHED;

@Service
class ApplicationMailService {

    private static final String APPLICATION = "application";
    private static final String VACATION_TYPE = "vacationTypeMessageKey";
    private static final String DAY_LENGTH = "dayLength";
    private static final String COMMENT = "comment";
    private static final String CALENDAR_ICS = "calendar.ics";
    private static final String HOLIDAY_REPLACEMENT = "holidayReplacement";
    private static final String HOLIDAY_REPLACEMENT_NOTE = "holidayReplacementNote";
    private static final String RECIPIENT = "recipient";

    private final MailService mailService;
    private final DepartmentService departmentService;
    private final ApplicationRecipientService applicationRecipientService;
    private final ICalService iCalService;
    private final SettingsService settingsService;
    private final Clock clock;

    @Autowired
    ApplicationMailService(MailService mailService, DepartmentService departmentService,
                           ApplicationRecipientService applicationRecipientService, ICalService iCalService,
                           SettingsService settingsService, Clock clock) {
        this.mailService = mailService;
        this.departmentService = departmentService;
        this.applicationRecipientService = applicationRecipientService;
        this.iCalService = iCalService;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    @Async
    void sendAllowedNotification(Application application, ApplicationComment applicationComment) {

        final ByteArrayResource calendarFile = generateCalendar(application, DEFAULT, application.getPerson());

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, applicationComment);

        // Inform user that the application for leave has been allowed
        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.allowed.user")
            .withTemplate("application_allowed_to_applicant", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();
        mailService.send(mailToApplicant);

        // Inform all person of interest like boss or department head that the application for leave has been allowed
        final List<Person> relevantRecipientsToInform = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToRelevantRecipients = Mail.builder()
            .withRecipient(relevantRecipientsToInform)
            .withRecipient(applicationRecipientService.getRecipientsWithOfficeNotifications())
            .withSubject("subject.application.allowed.management", application.getPerson().getNiceName())
            .withTemplate("application_allowed_to_management", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();
        mailService.send(mailToRelevantRecipients);
    }

    /**
     * sends an email to the applicant that the application has been rejected.
     *
     * @param application the application which got rejected
     * @param comment     reason why application was rejected
     */
    @Async
    void sendRejectedNotification(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        // send reject information to the applicant
        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.rejected")
            .withTemplate("application_rejected_information_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);

        // send reject information to the management
        final List<Person> relevantRecipientsToInform = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToRelevantRecipients = Mail.builder()
            .withRecipient(relevantRecipientsToInform)
            .withSubject("subject.application.rejected_information")
            .withTemplate("application_rejected_information_to_management", model)
            .build();
        mailService.send(mailToRelevantRecipients);

    }

    /**
     * If a boss is not sure about the decision of an application (reject or allow), he can ask another boss to decide
     * about this application via a generated email.
     *
     * @param application to ask for support
     * @param recipient   to request for a second opinion
     * @param sender      person that asks for a second opinion
     */
    @Async
    void sendReferredToManagementNotification(Application application, Person recipient, Person sender) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(RECIPIENT, recipient);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put("sender", sender);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(recipient)
            .withSubject("subject.application.refer")
            .withTemplate("application_referred_to_management", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * If a applicant edited the application for leave before it was accepted/declined by a boss/department head
     * a edited notification will be send to himself and the boss/department head
     *
     * @param application that has been edited
     * @param recipient   that edited the application for leave
     */
    @Async
    void sendEditedNotification(Application application, Person recipient) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(RECIPIENT, recipient);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(recipient)
            .withSubject("subject.application.edited", application.getPerson().getNiceName())
            .withTemplate("application_edited_by_applicant_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends information to the office and applicant that the cancellation request was cancelled
     *
     * @param application cancellation requested application
     */
    @Async
    void sendDeclinedCancellationRequestApplicationNotification(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(COMMENT, comment);

        // send mail to applicant
        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.cancellationRequest.declined.applicant", application.getPerson().getNiceName())
            .withTemplate("application_cancellation_request_declined_to_applicant", model)
            .build();
        mailService.send(mailToApplicant);

        // send cancelled cancellation request information to the office and relevant persons
        final List<Person> relevantRecipientsToInform = applicationRecipientService.getRecipientsOfInterest(application);
        relevantRecipientsToInform.addAll(applicationRecipientService.getRecipientsWithOfficeNotifications());
        final Mail mailToOffice = Mail.builder()
            .withRecipient(relevantRecipientsToInform)
            .withSubject("subject.application.cancellationRequest.declined.management")
            .withTemplate("application_cancellation_request_declined_to_management", model)
            .build();
        mailService.send(mailToOffice);
    }

    /**
     * Sends mail to office and informs about
     * a cancellation request of an already allowed application.
     *
     * @param application    cancelled application
     * @param createdComment additional comment for the confirming application
     */
    @Async
    void sendCancellationRequest(Application application, ApplicationComment createdComment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(COMMENT, createdComment);

        // send mail to applicant
        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.cancellationRequest.applicant")
            .withTemplate("application_cancellation_request_to_applicant", model)
            .build();
        mailService.send(mailToApplicant);

        // send reject information to the office
        final List<Person> relevantRecipientsToInform = applicationRecipientService.getRecipientsWithOfficeNotifications();
        final Mail mailToOffice = Mail.builder()
            .withRecipient(relevantRecipientsToInform)
            .withSubject("subject.application.cancellationRequest")
            .withTemplate("application_cancellation_request_to_management", model)
            .build();
        mailService.send(mailToOffice);
    }

    /**
     * Sends mail to the affected person if sick note is converted to vacation.
     *
     * @param application the application that has been converted from sick note to vacation
     */
    @Async
    void sendSickNoteConvertedToVacationNotification(Application application) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.sicknote.converted")
            .withTemplate("sicknote_converted", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends an email to the applicant that the application
     * has been created successfully.
     *
     * @param application confirmed application
     * @param comment     additional comment for the confirming application
     */
    @Async
    void sendConfirmationAllowedDirectly(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.allowedDirectly.user")
            .withTemplate("application_allowed_directly_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends an email to the person of the given application
     * that some management person has entered an application directly on behalf of himself.
     *
     * @param application confirmed application on behalf
     * @param comment     additional comment for the application
     */
    @Async
    void sendConfirmationAllowedDirectlyByManagement(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.allowedDirectly.management")
            .withTemplate("application_allowed_directly_by_management_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends an email to the bosses notifying
     * that there is a new directly allowed application for leave
     * which has to be allowed or rejected by a boss.
     *
     * @param application
     * @param comment     additional comment for the application
     */
    @Async
    void sendDirectlyAllowedNotificationToManagement(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final List<Person> recipients = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToAllowAndRemind = Mail.builder()
            .withRecipient(recipients)
            .withSubject("subject.application.allowedDirectly.boss", application.getPerson().getNiceName())
            .withTemplate("application_allowed_directly_to_management", model)
            .build();

        mailService.send(mailToAllowAndRemind);
    }

    /**
     * Sends mail to person to inform that he/she
     * has been selected as replacement
     * for an directly allowed application
     * that stands in while someone is on holiday.
     *
     * @param application to inform the replacement
     */
    @Async
    void notifyHolidayReplacementAboutDirectlyAllowedApplication(HolidayReplacementEntity holidayReplacement, Application application) {

        final ByteArrayResource calendarFile = generateCalendar(application, AbsenceType.HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());
        model.put(HOLIDAY_REPLACEMENT_NOTE, holidayReplacement.getNote());
        model.put(DAY_LENGTH, application.getDayLength().name());

        final Mail mailToReplacement = Mail.builder()
            .withRecipient(holidayReplacement.getPerson())
            .withSubject("subject.application.allowedDirectly.holidayReplacement", application.getPerson().getNiceName())
            .withTemplate("application_allowed_directly_to_holiday_replacement", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();

        mailService.send(mailToReplacement);
    }

    /**
     * Sends mail to person to inform that he/she
     * has been selected as replacement
     * but that this application status is WAITING
     *
     * @param application to inform the replacement beforehand
     */
    @Async
    void notifyHolidayReplacementForApply(HolidayReplacementEntity holidayReplacement, Application application) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());
        model.put(HOLIDAY_REPLACEMENT_NOTE, holidayReplacement.getNote());
        model.put(DAY_LENGTH, application.getDayLength().name());

        final Mail mailToReplacement = Mail.builder()
            .withRecipient(holidayReplacement.getPerson())
            .withSubject("subject.application.holidayReplacement.apply", application.getPerson().getNiceName())
            .withTemplate("application_applied_to_holiday_replacement", model)
            .build();

        mailService.send(mailToReplacement);
    }

    /**
     * Sends mail to person to inform that he/she
     * has been selected as replacement
     * that stands in while someone is on holiday.
     *
     * @param application to inform the replacement
     */
    @Async
    void notifyHolidayReplacementAllow(HolidayReplacementEntity holidayReplacement, Application application) {

        final ByteArrayResource calendarFile = generateCalendar(application, AbsenceType.HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());
        model.put(HOLIDAY_REPLACEMENT_NOTE, holidayReplacement.getNote());
        model.put(DAY_LENGTH, application.getDayLength().name());

        final Mail mailToReplacement = Mail.builder()
            .withRecipient(holidayReplacement.getPerson())
            .withSubject("subject.application.holidayReplacement.allow", application.getPerson().getNiceName())
            .withTemplate("application_allowed_to_holiday_replacement", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();

        mailService.send(mailToReplacement);
    }

    /**
     * Sends mail to person to inform that he/she
     * has been selected as replacement
     * but that the request was cancelled/rejected/revoked/...
     *
     * @param application to inform the replacement was cancelled
     */
    @Async
    void notifyHolidayReplacementAboutCancellation(HolidayReplacementEntity holidayReplacement, Application application) {

        final ByteArrayResource calendarFile = generateCalendar(application, DEFAULT, CANCELLED, holidayReplacement.getPerson());

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());
        model.put(DAY_LENGTH, application.getDayLength().name());

        final Mail mailToReplacement = Mail.builder()
            .withRecipient(holidayReplacement.getPerson())
            .withSubject("subject.application.holidayReplacement.cancellation", application.getPerson().getNiceName())
            .withTemplate("application_cancelled_to_holiday_replacement", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();

        mailService.send(mailToReplacement);
    }

    /**
     * Sends mail to person to inform that he/she
     * has been selected as replacement
     * but that the request was cancelled/rejected/revoked/...
     *
     * @param application to inform the replacement was cancelled
     */
    @Async
    void notifyHolidayReplacementAboutEdit(HolidayReplacementEntity holidayReplacement, Application application) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(HOLIDAY_REPLACEMENT, holidayReplacement.getPerson());
        model.put(HOLIDAY_REPLACEMENT_NOTE, holidayReplacement.getNote());
        model.put(DAY_LENGTH, application.getDayLength().name());

        final Mail mailToReplacement = Mail.builder()
            .withRecipient(holidayReplacement.getPerson())
            .withSubject("subject.application.holidayReplacement.edit", application.getPerson().getNiceName())
            .withTemplate("application_edited_to_holiday_replacement", model)
            .build();

        mailService.send(mailToReplacement);
    }

    /**
     * Sends an email to the applicant that the application
     * has been made successfully.
     *
     * @param application confirmed application
     * @param comment     additional comment for the confirming application
     */
    @Async
    void sendAppliedNotification(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.applied.user")
            .withTemplate("application_applied_by_applicant_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends an email to the person of the given application
     * that some management person has applied for leave on behalf of himself.
     *
     * @param application confirmed application on behalf
     * @param comment     additional comment for the application
     */
    @Async
    void sendAppliedByManagementNotification(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.applied.management")
            .withTemplate("application_applied_by_management_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Send emails to the applicant and to all relevant persons if an application for leave got revoked.
     *
     * @param application the application which got cancelled
     * @param comment     describes the reason of the revocation
     */
    @Async
    void sendRevokedNotifications(Application application, ApplicationComment comment) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(COMMENT, comment);

        if (application.getPerson().equals(application.getCanceller())) {
            final Mail mailToApplicant = Mail.builder()
                .withRecipient(application.getPerson())
                .withSubject("subject.application.revoked.applicant")
                .withTemplate("application_revoked_by_applicant_to_applicant", model)
                .build();
            mailService.send(mailToApplicant);
        } else {
            final Mail mailToNotApplicant = Mail.builder()
                .withRecipient(application.getPerson())
                .withSubject("subject.application.revoked.notApplicant")
                .withTemplate("application_revoked_by_management_to_applicant", model)
                .build();
            mailService.send(mailToNotApplicant);
        }

        // send reject information to all other relevant persons
        final List<Person> relevantRecipientsToInform = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToRelevantPersons = Mail.builder()
            .withRecipient(relevantRecipientsToInform)
            .withSubject("subject.application.revoked.management")
            .withTemplate("application_revoked_to_management", model)
            .build();

        mailService.send(mailToRelevantPersons);
    }

    /**
     * Sends an email to the recipients of interest notifying
     * that a application for leave was directly cancelled.
     *
     * @param application that was cancelled directly
     * @param comment     additional comment for the application
     */
    @Async
    void sendCancelledDirectlyToManagement(Application application, ApplicationComment comment) {

        final Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final List<Person> recipients = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToAllowAndRemind = Mail.builder()
            .withRecipient(recipients)
            .withSubject("subject.application.cancelledDirectly.information.recipients_of_interest", application.getPerson().getNiceName())
            .withTemplate("application_cancelled_directly_to_management", model)
            .build();

        mailService.send(mailToAllowAndRemind);
    }

    /**
     * Sends an email to the applicant if an application for leave got cancelled directly by himself.
     *
     * @param application the application which got cancelled directly
     * @param comment     describes the reason of the direct cancellation
     */
    @Async
    void sendCancelledDirectlyConfirmationByApplicant(Application application, ApplicationComment comment) {

        final Person recipient = application.getPerson();
        final ByteArrayResource calendarFile = generateCalendar(application, DEFAULT, CANCELLED, recipient);

        final Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        // send cancelled by office information to the applicant
        final Mail mailToApplicant = Mail.builder()
            .withRecipient(recipient)
            .withSubject("subject.application.cancelledDirectly.user")
            .withTemplate("application_cancelled_directly_confirmation_by_applicant_to_applicant", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends an email to the person of the given application
     * that some management person has cancelled an application directly on behalf of himself.
     *
     * @param application confirmed application on behalf
     * @param comment     additional comment for the application
     */
    @Async
    void sendCancelledDirectlyConfirmationByManagement(Application application, ApplicationComment comment) {

        final Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.cancelledDirectly.management")
            .withTemplate("application_cancelled_directly_confirmation_by_management_to_applicant", model)
            .build();

        mailService.send(mailToApplicant);
    }

    /**
     * Sends an email to the applicant if an application for leave got cancelled by management.
     *
     * @param application the application which got cancelled
     * @param comment     describes the reason of the cancellation
     */
    @Async
    void sendCancelledConfirmationByManagement(Application application, ApplicationComment comment) {

        final ByteArrayResource calendarFile = generateCalendar(application, DEFAULT, CANCELLED, application.getPerson());

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(COMMENT, comment);

        // send cancelled by office information to the applicant
        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.cancelled.user")
            .withTemplate("application_cancelled_by_management_to_applicant", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();

        mailService.send(mailToApplicant);

        // send cancelled by office information to all other relevant persons
        final List<Person> relevantRecipientsToInform = applicationRecipientService.getRecipientsOfInterest(application);
        relevantRecipientsToInform.addAll(applicationRecipientService.getRecipientsWithOfficeNotifications());
        final Mail mailToRelevantPersons = Mail.builder()
            .withRecipient(relevantRecipientsToInform)
            .withSubject("subject.application.cancelled.management")
            .withTemplate("application_cancelled_by_management_to_management", model)
            .withAttachment(CALENDAR_ICS, calendarFile)
            .build();

        mailService.send(mailToRelevantPersons);
    }

    /**
     * Sends an email to the bosses notifying
     * that there is a new application for leave
     * which has to be allowed or rejected by a boss.
     *
     * @param application to allow or reject
     * @param comment     additional comment for the application
     */
    @Async
    void sendAppliedNotificationToManagement(Application application, ApplicationComment comment) {

        final List<Application> applicationsForLeave =
            departmentService.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(application.getPerson(), application.getStartDate(), application.getEndDate());

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);
        model.put("departmentVacations", applicationsForLeave);

        final List<Person> recipients = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToAllowAndRemind = Mail.builder()
            .withRecipient(recipients)
            .withSubject("subject.application.applied.boss", application.getPerson().getNiceName())
            .withTemplate("application_applied_to_management", model)
            .build();

        mailService.send(mailToAllowAndRemind);
    }

    /**
     * Sends an email to the applicant and to the second stage authorities that the application for leave has been
     * allowed temporary.
     *
     * @param application that has been allowed temporary by a department head
     * @param comment     contains reason why application for leave has been allowed temporary
     */
    @Async
    void sendTemporaryAllowedNotification(Application application, ApplicationComment comment) {

        // Inform user that the application for leave has been allowed temporary
        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);
        model.put(DAY_LENGTH, application.getDayLength().name());
        model.put(COMMENT, comment);

        final Mail mailToApplicant = Mail.builder()
            .withRecipient(application.getPerson())
            .withSubject("subject.application.temporaryAllowed.user")
            .withTemplate("application_temporary_allowed_to_applicant", model)
            .build();
        mailService.send(mailToApplicant);

        // Inform second stage authorities that there is an application for leave that must be allowed
        final List<Application> applicationsForLeave =
            departmentService.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(application.getPerson(), application.getStartDate(), application.getEndDate());

        Map<String, Object> modelSecondStage = new HashMap<>();
        modelSecondStage.put(APPLICATION, application);
        modelSecondStage.put(VACATION_TYPE, application.getVacationType().getMessageKey());
        modelSecondStage.put(DAY_LENGTH, application.getDayLength().name());
        modelSecondStage.put(COMMENT, comment);
        modelSecondStage.put("departmentVacations", applicationsForLeave);

        final List<Person> recipients = applicationRecipientService.getRecipientsForTemporaryAllow(application);
        final Mail mailToTemporaryAllow = Mail.builder()
            .withRecipient(recipients)
            .withSubject("subject.application.temporaryAllowed.secondStage")
            .withTemplate("application_temporary_allowed_to_second_stage_authority", modelSecondStage)
            .build();
        mailService.send(mailToTemporaryAllow);
    }

    /**
     * If an application has status waiting and no person with management rights
     * has decided about it after a certain time, the management will receive a
     * reminding notification.
     *
     * @param application to receive a reminding notification
     */
    @Async
    void sendRemindNotificationToManagement(Application application) {

        Map<String, Object> model = new HashMap<>();
        model.put(APPLICATION, application);

        final List<Person> recipients = applicationRecipientService.getRecipientsOfInterest(application);
        final Mail mailToAllowAndRemind = Mail.builder()
            .withRecipient(recipients)
            .withSubject("subject.application.remind")
            .withTemplate("application_remind_to_management", model)
            .build();
        mailService.send(mailToAllowAndRemind);
    }

    @Async
    void sendRemindForUpcomingApplicationsReminderNotification(List<Application> applications) {
        for (Application application : applications) {
            final Map<String, Object> model = new HashMap<>();
            model.put(APPLICATION, application);
            model.put("daysBeforeUpcomingApplication", ChronoUnit.DAYS.between(LocalDate.now(clock), application.getStartDate()));

            final Mail mailToUpcomingApplicationsPersons = Mail.builder()
                .withRecipient(application.getPerson())
                .withSubject("subject.application.remind.upcoming")
                .withTemplate("application_cron_remind_for_upcoming_application_to_applicant", model)
                .build();
            mailService.send(mailToUpcomingApplicationsPersons);
        }
    }

    @Async
    void sendRemindForUpcomingHolidayReplacement(List<Application> applications) {
        for (Application application : applications) {
            for (HolidayReplacementEntity holidayReplacement : application.getHolidayReplacements()) {

                final Map<String, Object> model = new HashMap<>();
                model.put(APPLICATION, application);
                model.put("daysBeforeUpcomingHolidayReplacement", ChronoUnit.DAYS.between(LocalDate.now(clock), application.getStartDate()));
                model.put("replacementNote", holidayReplacement.getNote());

                final Mail mailToUpcomingHolidayReplacement = Mail.builder()
                    .withRecipient(holidayReplacement.getPerson())
                    .withSubject("subject.application.remind.upcoming.holiday_replacement", application.getPerson().getNiceName())
                    .withTemplate("application_cron_upcoming_holiday_replacement_to_holiday_replacement", model)
                    .build();
                mailService.send(mailToUpcomingHolidayReplacement);
            }
        }
    }

    @Async
    void sendRemindForWaitingApplicationsReminderNotification(List<Application> waitingApplications) {

        /*
         * whats happening here?
         *
         * application a
         * person p
         *
         * map application to list of boss/department head
         * a_1 -> (p_1, p_2); a_2 -> (p_1, p_3)
         *
         * collect list of application grouped by boss/department head
         * p_1 -> (a_1, a_2); p_2 -> (a_1); (p_3 -> a_2)
         *
         * See: http://stackoverflow.com/questions/33086686/java-8-stream-collect-and-group-by-objects-that-map-to-multiple-keys
         */
        Map<Person, List<Application>> applicationsPerRecipient = waitingApplications.stream()
            .flatMap(application -> applicationRecipientService.getRecipientsOfInterest(application).stream()
                .map(person -> new AbstractMap.SimpleEntry<>(person, application)))
            .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));

        for (Map.Entry<Person, List<Application>> entry : applicationsPerRecipient.entrySet()) {

            List<Application> applications = entry.getValue();
            Person recipient = entry.getKey();

            Map<String, Object> model = new HashMap<>();
            model.put("applicationList", applications);
            model.put(RECIPIENT, recipient);

            final Mail mailToRemindForWaiting = Mail.builder()
                .withRecipient(recipient)
                .withSubject("subject.application.cronRemind")
                .withTemplate("application_remind_cron_to_management", model)
                .build();

            mailService.send(mailToRemindForWaiting);
        }
    }

    private ByteArrayResource generateCalendar(Application application, AbsenceType absenceType, Person recipient) {
        return generateCalendar(application, absenceType, PUBLISHED, recipient);
    }

    private ByteArrayResource generateCalendar(Application application, AbsenceType absenceType, ICalType iCalType, Person recipient) {
        final Absence absence = new Absence(application.getPerson(), application.getPeriod(), getAbsenceTimeConfiguration(), absenceType);
        return iCalService.getSingleAppointment(absence, iCalType, recipient);
    }

    private AbsenceTimeConfiguration getAbsenceTimeConfiguration() {
        final TimeSettings timeSettings = settingsService.getSettings().getTimeSettings();
        return new AbsenceTimeConfiguration(timeSettings);
    }
}
