package org.church.steward.domain.financialrecord.repository;

import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialRecordContactRepository extends JpaRepository<FinancialRecordContact, Long> {
    boolean existsByContactId(Long contactId);
}
