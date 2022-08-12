package org.synyx.urlaubsverwaltung.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.synyx.urlaubsverwaltung.person.Person;

import java.util.List;


/**
 * Repository for {@link DepartmentEntity} entities.
 */
interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer> {

    List<DepartmentEntity> findByDepartmentHeadsAndSecondStageAuthorities(Person departmentHead, Person secondStageAuthority);

    List<DepartmentEntity> findByDepartmentHeads(Person person);

    List<DepartmentEntity> findBySecondStageAuthorities(Person person);

    @Query("SELECT d FROM department d WHERE :person MEMBER OF d.departmentHeads OR :person MEMBER OF d.secondStageAuthorities OR :personId = d.members.person.id")
    List<DepartmentEntity> findByDepartmentHeadsContainingOrSecondStageAuthoritiesContainingOrMembersContaining(@Param("person") Person person, @Param("personId") int personId);

    List<DepartmentEntity> findByMembersPerson(Person person);
}
