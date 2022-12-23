package org.synyx.urlaubsverwaltung.person.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.UnknownPersonException;
import org.synyx.urlaubsverwaltung.security.SessionService;

import static org.synyx.urlaubsverwaltung.person.web.PersonPermissionsMapper.mapToPersonPermissionsDto;
import static org.synyx.urlaubsverwaltung.person.web.PersonPermissionsMapper.merge;
import static org.synyx.urlaubsverwaltung.security.SecurityRules.IS_OFFICE;

@Controller
@RequestMapping("/web")
public class PersonPermissionsViewController {

    private final PersonService personService;
    private final DepartmentService departmentService;
    private final PersonPermissionsDtoValidator validator;
    private final SessionService sessionService;

    @Autowired
    public PersonPermissionsViewController(PersonService personService, DepartmentService departmentService, PersonPermissionsDtoValidator validator, SessionService sessionService) {
        this.personService = personService;
        this.departmentService = departmentService;
        this.validator = validator;
        this.sessionService = sessionService;
    }

    @PreAuthorize(IS_OFFICE)
    @GetMapping("/person/{personId}/permissions")
    public String showPersonPermissionsAndNotifications(@PathVariable("personId") Long personId, Model model) throws UnknownPersonException {

        final Person person = personService.getPersonByID(personId).orElseThrow(() -> new UnknownPersonException(personId));

        model.addAttribute("person", mapToPersonPermissionsDto(person));
        model.addAttribute("departments", departmentService.getManagedDepartmentsOfDepartmentHead(person));
        model.addAttribute("secondStageDepartments", departmentService.getManagedDepartmentsOfSecondStageAuthority(person));

        return "thymeleaf/person/person_permissions";
    }

    @PreAuthorize(IS_OFFICE)
    @PostMapping("/person/{personId}/permissions")
    public String editPersonPermissionsAndNotifications(@PathVariable("personId") Long personId,
                                                        @ModelAttribute("person") PersonPermissionsDto personPermissionsDto, Errors errors,
                                                        RedirectAttributes redirectAttributes) throws UnknownPersonException {

        validator.validate(personPermissionsDto, errors);

        if (errors.hasErrors()) {
            return "thymeleaf/person/person_permissions";
        }

        final Person person = personService.getPersonByID(personId).orElseThrow(() -> new UnknownPersonException(personId));
        personService.update(merge(person, personPermissionsDto));

        sessionService.markSessionToReloadAuthorities(person.getUsername());

        redirectAttributes.addFlashAttribute("updateSuccess", true);
        return "redirect:/web/person/" + personId;
    }
}
