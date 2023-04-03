package org.synyx.urlaubsverwaltung.application.application;

import de.focus_shift.launchpad.api.HasLaunchpad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.account.Account;
import org.synyx.urlaubsverwaltung.account.AccountService;
import org.synyx.urlaubsverwaltung.account.VacationDaysLeft;
import org.synyx.urlaubsverwaltung.account.VacationDaysService;
import org.synyx.urlaubsverwaltung.application.comment.ApplicationComment;
import org.synyx.urlaubsverwaltung.application.comment.ApplicationCommentForm;
import org.synyx.urlaubsverwaltung.application.comment.ApplicationCommentService;
import org.synyx.urlaubsverwaltung.application.comment.ApplicationCommentValidator;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.UnknownPersonException;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTime;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toMap;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToAllowTemporaryAllowedApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToAllowWaitingApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToCancelApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToCancelDirectlyApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToDeclineCancellationRequest;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToEditApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToReferApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToRejectApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToRemindApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToRevokeApplication;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationForLeavePermissionEvaluator.isAllowedToStartCancellationRequest;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.security.SecurityRules.IS_BOSS_OR_DEPARTMENT_HEAD_OR_SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.security.SecurityRules.IS_PRIVILEGED_USER;

/**
 * Controller to manage applications for leave.
 */
@RequestMapping("/web/application")
@Controller
class ApplicationForLeaveDetailsViewController implements HasLaunchpad {

    private static final String REDIRECT_WEB_APPLICATION = "redirect:/web/application/";
    private static final String ATTRIBUTE_ERRORS = "errors";

    private final PersonService personService;
    private final AccountService accountService;
    private final ApplicationService applicationService;
    private final ApplicationInteractionService applicationInteractionService;
    private final VacationDaysService vacationDaysService;
    private final ApplicationCommentService commentService;
    private final WorkDaysCountService workDaysCountService;
    private final ApplicationCommentValidator commentValidator;
    private final DepartmentService departmentService;
    private final WorkingTimeService workingTimeService;
    private final Clock clock;

    @Autowired
    ApplicationForLeaveDetailsViewController(VacationDaysService vacationDaysService, PersonService personService,
                                             AccountService accountService, ApplicationService applicationService,
                                             ApplicationInteractionService applicationInteractionService,
                                             ApplicationCommentService commentService, WorkDaysCountService workDaysCountService,
                                             ApplicationCommentValidator commentValidator,
                                             DepartmentService departmentService, WorkingTimeService workingTimeService, Clock clock) {
        this.vacationDaysService = vacationDaysService;
        this.personService = personService;
        this.accountService = accountService;
        this.applicationService = applicationService;
        this.applicationInteractionService = applicationInteractionService;
        this.commentService = commentService;
        this.workDaysCountService = workDaysCountService;
        this.commentValidator = commentValidator;
        this.departmentService = departmentService;
        this.workingTimeService = workingTimeService;
        this.clock = clock;
    }

    @GetMapping("/{applicationId}")
    public String showApplicationDetail(@PathVariable("applicationId") Integer applicationId,
                                        @RequestParam(value = "year", required = false) Integer requestedYear,
                                        @RequestParam(value = "action", required = false) String action,
                                        @RequestParam(value = "shortcut", required = false) boolean shortcut, Model model)
        throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final Person signedInUser = personService.getSignedInUser();
        final Person person = application.getPerson();

        if (!departmentService.isSignedInUserAllowedToAccessPersonData(signedInUser, person)) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to see application for " +
                "leave of user '%s'", signedInUser.getId(), person.getId()));
        }

        final int year = requestedYear == null ? application.getEndDate().getYear() : requestedYear;
        prepareDetailView(application, year, action, shortcut, model, signedInUser);

        return "application/application-detail";
    }

    /*
     * Allow a not yet allowed application for leave (Privileged user only!).
     */
    @PreAuthorize(IS_BOSS_OR_DEPARTMENT_HEAD_OR_SECOND_STAGE_AUTHORITY)
    @PostMapping("/{applicationId}/allow")
    public String allowApplication(@PathVariable("applicationId") Integer applicationId,
                                   @ModelAttribute("comment") ApplicationCommentForm comment, Errors errors,
                                   @RequestParam(value = "redirect", required = false) String redirectUrl,
                                   RedirectAttributes redirectAttributes) throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final Person signedInUser = personService.getSignedInUser();
        final Person person = application.getPerson();

        final boolean isDepartmentHead = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, person);
        final boolean allowedToAllowWaitingApplication = isAllowedToAllowWaitingApplication(application, signedInUser, isDepartmentHead, isSecondStageAuthority);
        final boolean allowedToAllowTemporaryAllowedApplication = isAllowedToAllowTemporaryAllowedApplication(application, signedInUser, isSecondStageAuthority);
        if (!(allowedToAllowWaitingApplication || allowedToAllowTemporaryAllowedApplication)) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to allow application for leave of user '%s'",
                signedInUser.getId(), person.getId()));
        }

        comment.setMandatory(false);
        commentValidator.validate(comment, errors);

        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute(ATTRIBUTE_ERRORS, errors);
            return REDIRECT_WEB_APPLICATION + applicationId + "?action=allow";
        }

        final Application allowedApplicationForLeave;
        try {
            allowedApplicationForLeave = applicationInteractionService.allow(application, signedInUser, Optional.ofNullable(comment.getText()));
        } catch (NotPrivilegedToApproveException e) {
            redirectAttributes.addFlashAttribute("notPrivilegedToApprove", true);
            return REDIRECT_WEB_APPLICATION + applicationId;
        }

        if (allowedApplicationForLeave.hasStatus(ALLOWED)) {
            redirectAttributes.addFlashAttribute("allowSuccess", true);
        } else if (allowedApplicationForLeave.hasStatus(TEMPORARY_ALLOWED)) {
            redirectAttributes.addFlashAttribute("temporaryAllowSuccess", true);
        }

        if (redirectUrl != null) {
            return "redirect:" + redirectUrl;
        }

        return REDIRECT_WEB_APPLICATION + applicationId;
    }

    /*
     * If a boss is not sure about the decision if an application should be allowed or rejected,
     * he can ask another boss to decide about this application (an email is sent).
     */
    @PreAuthorize(IS_PRIVILEGED_USER)
    @PostMapping("/{applicationId}/refer")
    public String referApplication(@PathVariable("applicationId") Integer applicationId,
                                   @ModelAttribute("referredPerson") ReferredPerson referredPerson, RedirectAttributes redirectAttributes)
        throws UnknownApplicationForLeaveException, UnknownPersonException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final String referUsername = referredPerson.getUsername();
        final Person recipient = personService.getPersonByUsername(referUsername)
            .orElseThrow(() -> new UnknownPersonException(referUsername));

        final Person person = application.getPerson();
        final Person signedInUser = personService.getSignedInUser();

        final boolean isDepartmentHead = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, person);
        final boolean allowedToReferApplication = isAllowedToReferApplication(application, signedInUser, isDepartmentHead, isSecondStageAuthority);
        if (!allowedToReferApplication) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to refer application for " +
                "leave to user '%s'", signedInUser.getId(), referUsername));
        }

        applicationInteractionService.refer(application, recipient, signedInUser);
        redirectAttributes.addFlashAttribute("referSuccess", true);
        return REDIRECT_WEB_APPLICATION + applicationId;
    }

    @PreAuthorize(IS_BOSS_OR_DEPARTMENT_HEAD_OR_SECOND_STAGE_AUTHORITY)
    @PostMapping("/{applicationId}/reject")
    public String rejectApplication(@PathVariable("applicationId") Integer applicationId,
                                    @ModelAttribute("comment") ApplicationCommentForm comment, Errors errors,
                                    @RequestParam(value = "redirect", required = false) String redirectUrl,
                                    RedirectAttributes redirectAttributes) throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final Person person = application.getPerson();
        final Person signedInUser = personService.getSignedInUser();

        final boolean isDepartmentHead = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, person);
        final boolean allowedToRejectApplication = isAllowedToRejectApplication(application, signedInUser, isDepartmentHead, isSecondStageAuthority);
        if (!allowedToRejectApplication) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to reject application for " +
                "leave of user '%s'", signedInUser.getId(), person.getId()));
        }

        comment.setMandatory(true);
        commentValidator.validate(comment, errors);

        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute(ATTRIBUTE_ERRORS, errors);

            if (redirectUrl != null) {
                return REDIRECT_WEB_APPLICATION + applicationId + "?action=reject&shortcut=true";
            }

            return REDIRECT_WEB_APPLICATION + applicationId + "?action=reject";
        }

        applicationInteractionService.reject(application, signedInUser, Optional.ofNullable(comment.getText()));
        redirectAttributes.addFlashAttribute("rejectSuccess", true);

        if (redirectUrl != null) {
            return "redirect:" + redirectUrl;
        }

        return REDIRECT_WEB_APPLICATION + applicationId;
    }

    /*
     * Cancel an application for leave.
     *
     * Cancelling an application for leave on behalf for someone is allowed only for Office.
     */
    @PostMapping("/{applicationId}/cancel")
    public String cancelApplication(@PathVariable("applicationId") Integer applicationId,
                                    @ModelAttribute("comment") ApplicationCommentForm comment, Errors errors,
                                    RedirectAttributes redirectAttributes)
        throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final Person person = application.getPerson();
        final Person signedInUser = personService.getSignedInUser();

        final boolean isDepartmentHead = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, person);

        final boolean requiresApproval = application.getVacationType().isRequiresApproval();

        final boolean allowedToRevokeApplication = isAllowedToRevokeApplication(application, signedInUser, requiresApproval);
        final boolean allowedToCancelApplication = isAllowedToCancelApplication(application, signedInUser, isDepartmentHead, isSecondStageAuthority);
        final boolean allowedToCancelDirectlyApplication = isAllowedToCancelDirectlyApplication(application, signedInUser, isDepartmentHead, isSecondStageAuthority, requiresApproval);
        final boolean allowedToStartCancellationRequest = isAllowedToStartCancellationRequest(application, signedInUser, isDepartmentHead, isSecondStageAuthority, requiresApproval);
        if (!(allowedToRevokeApplication || allowedToCancelApplication || allowedToCancelDirectlyApplication || allowedToStartCancellationRequest)) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to cancel or revoke application " +
                "for leave of user '%s'", signedInUser.getId(), application.getPerson().getId()));
        }

        // user can revoke their own applications, so the comment is NOT mandatory
        final boolean isCommentMandatory = !signedInUser.equals(application.getPerson());
        comment.setMandatory(isCommentMandatory);

        commentValidator.validate(comment, errors);
        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute(ATTRIBUTE_ERRORS, errors);
            return REDIRECT_WEB_APPLICATION + applicationId + "?action=cancel";
        }

        if (requiresApproval) {
            applicationInteractionService.cancel(application, signedInUser, Optional.ofNullable(comment.getText()));
        } else {
            applicationInteractionService.directCancel(application, signedInUser, Optional.ofNullable(comment.getText()));
        }
        return REDIRECT_WEB_APPLICATION + applicationId;
    }

    /*
     * Cancel the cancellation request of an application for leave.
     */
    @PostMapping("/{applicationId}/decline-cancellation-request")
    public String declineCancellationRequestApplication(@PathVariable("applicationId") Integer applicationId,
                                                        @ModelAttribute("comment") ApplicationCommentForm comment, Errors errors,
                                                        RedirectAttributes redirectAttributes)
        throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final Person person = application.getPerson();
        final Person signedInUser = personService.getSignedInUser();

        final boolean isDepartmentHead = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, person);
        final boolean allowedToDeclineCancellationRequest = isAllowedToDeclineCancellationRequest(application, signedInUser, isDepartmentHead, isSecondStageAuthority);
        if (!allowedToDeclineCancellationRequest) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to cancel a cancellation request of " +
                "application for leave of user '%s'", signedInUser.getId(), application.getPerson().getId()));
        }

        comment.setMandatory(true);

        commentValidator.validate(comment, errors);
        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute(ATTRIBUTE_ERRORS, errors);
            return REDIRECT_WEB_APPLICATION + applicationId + "?action=decline-cancellation-request";
        }

        applicationInteractionService.declineCancellationRequest(application, signedInUser, Optional.ofNullable(comment.getText()));
        return REDIRECT_WEB_APPLICATION + applicationId;
    }

    /*
     * Remind the bosses about the decision of an application for leave.
     */
    @PostMapping("/{applicationId}/remind")
    public String remindBoss(@PathVariable("applicationId") Integer applicationId,
                             RedirectAttributes redirectAttributes) throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId)
            .orElseThrow(() -> new UnknownApplicationForLeaveException(applicationId));

        final Person person = application.getPerson();
        final Person signedInUser = personService.getSignedInUser();

        final boolean isDepartmentHead = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, person);
        final boolean allowedToRemindApplication = isAllowedToRemindApplication(application, signedInUser, isDepartmentHead, isSecondStageAuthority);
        if (!allowedToRemindApplication) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to remind application for " +
                "leave of user '%s'", signedInUser.getId(), person.getId()));
        }

        try {
            applicationInteractionService.remind(application);
            redirectAttributes.addFlashAttribute("remindIsSent", true);
        } catch (RemindAlreadySentException ex) {
            redirectAttributes.addFlashAttribute("remindAlreadySent", true);
        } catch (ImpatientAboutApplicationForLeaveProcessException ex) {
            redirectAttributes.addFlashAttribute("remindNoWay", true);
        }

        return REDIRECT_WEB_APPLICATION + applicationId;
    }

    private void prepareDetailView(Application application, int year, String action, boolean shortcut, Model model, Person signedInUser) {

        // signed in user
        model.addAttribute("signedInUser", signedInUser);

        // person information with departments
        model.addAttribute("departmentsOfPerson", departmentService.getAssignedDepartmentsOfMember(application.getPerson()));

        // COMMENTS
        final List<ApplicationComment> comments = commentService.getCommentsByApplication(application);
        model.addAttribute("comment", new ApplicationCommentForm());
        model.addAttribute("comments", comments);
        model.addAttribute("lastComment", comments.get(comments.size() - 1));

        // SPECIAL ATTRIBUTES FOR BOSSES / DEPARTMENT HEADS
        boolean isNotYetAllowed = application.hasStatus(WAITING) || application.hasStatus(TEMPORARY_ALLOWED);
        boolean isPrivilegedUser = signedInUser.hasRole(BOSS) || signedInUser.hasRole(OFFICE) || signedInUser.hasRole(DEPARTMENT_HEAD)
            || signedInUser.hasRole(SECOND_STAGE_AUTHORITY);

        if (isNotYetAllowed && isPrivilegedUser) {
            model.addAttribute("bosses", personService.getActivePersonsByRole(BOSS));
            model.addAttribute("referredPerson", new ReferredPerson());
        }

        // APPLICATION FOR LEAVE
        model.addAttribute("app", new ApplicationForLeave(application, workDaysCountService));

        final Map<DateRange, WorkingTime> workingTime = workingTimeService.getWorkingTimesByPersonAndDateRange(
                application.getPerson(), new DateRange(application.getStartDate(), application.getEndDate())).entrySet().stream()
            .sorted(Map.Entry.comparingByKey(comparing(DateRange::getStartDate)))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> newValue, LinkedHashMap::new));
        model.addAttribute("dateRangeWorkingTimes", workingTime);

        // DEPARTMENT APPLICATIONS FOR LEAVE
        final List<Application> departmentApplications =
            departmentService.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(application.getPerson(),
                application.getStartDate(), application.getEndDate());
        model.addAttribute("departmentApplications", departmentApplications);

        // HOLIDAY ACCOUNT
        final Optional<Account> account = accountService.getHolidaysAccount(year, application.getPerson());
        if (account.isPresent()) {
            final Account acc = account.get();
            final Optional<Account> accountNextYear = accountService.getHolidaysAccount(year + 1, application.getPerson());

            final VacationDaysLeft vacationDaysLeft = vacationDaysService.getVacationDaysLeft(acc, accountNextYear);
            model.addAttribute("vacationDaysLeft", vacationDaysLeft);

            final LocalDate now = LocalDate.now(clock);
            final LocalDate expiryDate = acc.getExpiryDate();
            final BigDecimal expiredRemainingVacationDays = vacationDaysLeft.getExpiredRemainingVacationDays(now, expiryDate);
            model.addAttribute("expiredRemainingVacationDays", expiredRemainingVacationDays);
            model.addAttribute("doRemainingVacationDaysExpire", acc.doRemainingVacationDaysExpire());
            model.addAttribute("expiryDate", expiryDate);

            model.addAttribute("account", acc);
            model.addAttribute("isBeforeExpiryDate", now.isBefore(expiryDate));
        }

        // Signed in person is allowed to manage
        final boolean isDepartmentHeadOfPerson = departmentService.isDepartmentHeadAllowedToManagePerson(signedInUser, application.getPerson());
        final boolean isSecondStageAuthorityOfPerson = departmentService.isSecondStageAuthorityAllowedToManagePerson(signedInUser, application.getPerson());
        final boolean requiresApproval = application.getVacationType().isRequiresApproval();

        model.addAttribute("isAllowedToAllowWaitingApplication", isAllowedToAllowWaitingApplication(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson));
        model.addAttribute("isAllowedToAllowTemporaryAllowedApplication", isAllowedToAllowTemporaryAllowedApplication(application, signedInUser, isSecondStageAuthorityOfPerson));

        model.addAttribute("isAllowedToRejectApplication", isAllowedToRejectApplication(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson));

        model.addAttribute("isAllowedToRevokeApplication", isAllowedToRevokeApplication(application, signedInUser, requiresApproval));
        model.addAttribute("isAllowedToCancelApplication", isAllowedToCancelApplication(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson));
        model.addAttribute("isAllowedToCancelDirectlyApplication", isAllowedToCancelDirectlyApplication(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson, requiresApproval));
        model.addAttribute("isAllowedToStartCancellationRequest", isAllowedToStartCancellationRequest(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson, requiresApproval));

        model.addAttribute("isAllowedToDeclineCancellationRequest", isAllowedToDeclineCancellationRequest(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson));

        model.addAttribute("isAllowedToEditApplication", isAllowedToEditApplication(application, signedInUser));
        model.addAttribute("isAllowedToRemindApplication", isAllowedToRemindApplication(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson));
        model.addAttribute("isAllowedToReferApplication", isAllowedToReferApplication(application, signedInUser, isDepartmentHeadOfPerson, isSecondStageAuthorityOfPerson));

        model.addAttribute("isDepartmentHeadOfPerson", isDepartmentHeadOfPerson);
        model.addAttribute("isSecondStageAuthorityOfPerson", isSecondStageAuthorityOfPerson);
        model.addAttribute("isBoss", signedInUser.hasRole(BOSS));
        model.addAttribute("isOffice", signedInUser.hasRole(OFFICE));

        // UNSPECIFIC ATTRIBUTES
        model.addAttribute("selectedYear", year);
        model.addAttribute("currentYear", Year.now(clock).getValue());
        model.addAttribute("action", requireNonNullElse(action, ""));
        model.addAttribute("shortcut", shortcut);
    }
}
