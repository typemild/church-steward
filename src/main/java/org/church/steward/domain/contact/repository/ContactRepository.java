package org.church.steward.domain.contact.repository;

import java.util.List;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByNameContainingOrderByNameAsc(String name, Pageable pageable);

    List<Contact> findByNameContainingAndIsHiddenFalseOrderByNameAsc(String name, Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE " +
            "(:type IS NULL OR c.type = :type) AND " +
            "(:name IS NULL OR c.name LIKE concat('%', :name, '%')) AND " +
            "(:remarks IS NULL OR c.remarks LIKE concat('%', :remarks, '%')) AND " +
            "(:includeHidden = true OR c.isHidden = false) ORDER BY c.name ASC")
    List<Contact> searchContacts(@Param("type") ContactType type,
            @Param("name") String name,
            @Param("remarks") String remarks,
            @Param("includeHidden") Boolean includeHidden);
}
