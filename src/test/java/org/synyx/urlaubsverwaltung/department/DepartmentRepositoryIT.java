package org.synyx.urlaubsverwaltung.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.synyx.urlaubsverwaltung.TestContainersBase;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DepartmentRepositoryIT extends TestContainersBase {

    @Autowired
    private PersonService personService;
    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository sut;

    @Test
    void ensureGetDepartmentsPersonHasAccessTo() {

        final Person savedPersonOne = personService.save(new Person("sam", "smith", "sam", "smith@example.org"));

        final Department memberDepartment = new Department();
        memberDepartment.setName("member department");
        memberDepartment.setMembers(List.of(savedPersonOne));
        final Department savedMemberDepartment = departmentService.create(memberDepartment);

        final Department dhDepartment = new Department();
        dhDepartment.setName("dh department");
        dhDepartment.setDepartmentHeads(List.of(savedPersonOne));
        final Department savedDhDepartment = departmentService.create(dhDepartment);

        final Department ssaDepartment = new Department();
        ssaDepartment.setName("ssa department");
        ssaDepartment.setSecondStageAuthorities(List.of(savedPersonOne));
        final Department savedSsaDepartment = departmentService.create(ssaDepartment);

        assertThat(sut.findByDepartmentHeadsContainingOrSecondStageAuthoritiesContainingOrMembersContaining(savedPersonOne, savedPersonOne.getId())).hasSize(3);
    }
}
