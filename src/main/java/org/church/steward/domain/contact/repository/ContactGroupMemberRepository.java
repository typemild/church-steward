package org.church.steward.domain.contact.repository;

import java.util.List;
import java.util.Set;

import org.church.steward.domain.contact.entity.ContactGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactGroupMemberRepository extends JpaRepository<ContactGroupMember, Long> {

    List<ContactGroupMember> findAllByContactId(Long contactId);

    long countByContactGroupId(Long contactGroupId);

    @Query("select m.contact.id from ContactGroupMember m where m.contactGroup.id = :contactGroupId")
    Set<Long> findContactIdsByContactGroupId(@Param("contactGroupId") Long contactGroupId);

    List<ContactGroupMember> findAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAsc(Long contactGroupId);
}
