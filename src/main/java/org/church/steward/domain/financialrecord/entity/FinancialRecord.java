package org.church.steward.domain.financialrecord.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.util.DynamicLoggable;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.transformer.BudgetTransformer;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.financialrecord.transformer.FinancialRecordContactListTransformer;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.transformer.FiscalYearTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입 또는 지출 내역. 구분({@link FinancialRecordType})은 생성할 때 정해지며 이후 바꿀 수 없다.
 * 관계자는 수입이면 봉헌자(1명 이상), 지출이면 수취인(1명)이다.
 */
@Entity
@Table(name = "FINANCIAL_RECORD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Loggable(name = "수입/지출")
public class FinancialRecord implements DynamicLoggable {

    private static final String DATE_FIELD_NAME = "일자";
    private static final String CONTACTS_FIELD_NAME = "관계자";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private FinancialRecordType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    @LoggableField(name = "회계 연도", isIdentifier = true, transformer = FiscalYearTransformer.class)
    private FiscalYear fiscalYear;

    @Column(nullable = false)
    @LoggableField(name = DATE_FIELD_NAME, isIdentifier = true, format = "yyyy-MM-dd")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    @LoggableField(name = "예산", isIdentifier = true, transformer = BudgetTransformer.class)
    private Budget budget;

    @Column(length = FinancialRecordConstraints.DESCRIPTION_MAX_LENGTH)
    @LoggableField(name = "내역", isIdentifier = true)
    private String description;

    @Column(nullable = false)
    @LoggableField(name = "금액", format = "#,###원")
    private long amount;

    @Column(length = FinancialRecordConstraints.REMARKS_MAX_LENGTH)
    @LoggableField(name = "비고")
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "financialRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @LoggableField(name = CONTACTS_FIELD_NAME, isIdentifier = true, transformer = FinancialRecordContactListTransformer.class)
    private List<FinancialRecordContact> contacts = new ArrayList<>();

    // 편의 메소드: 내역에 관계자를 추가할 때 사용
    public void addContact(Contact contact) {
        if (this.contacts.stream().anyMatch(c -> c.getContact().equals(contact))) {
            return; // 이미 추가된 관계자라면 무시
        }

        this.contacts.add(new FinancialRecordContact(null, this, contact, contact.getName()));
    }

    public void update(FiscalYear fiscalYear, LocalDate date, Budget budget, String description, long amount,
            String remarks) {
        this.fiscalYear = fiscalYear;
        this.date = date;
        this.budget = budget;
        this.description = description;
        this.amount = amount;
        this.remarks = remarks;
    }

    /**
     * 활동 기록에는 구분에 따라 "수입" 또는 "지출"로 남긴다.
     */
    @Override
    public String getLoggableName() {
        return type.getLabel();
    }

    /**
     * 활동 기록에는 구분에 따라 일자를 "수입일"/"지출일", 관계자를 "봉헌자"/"수취인"으로 남긴다.
     */
    @Override
    public String getLoggableFieldName(String declaredName) {
        return switch (declaredName) {
        case DATE_FIELD_NAME -> type.getLabel() + "일";
        case CONTACTS_FIELD_NAME -> type == FinancialRecordType.INCOME ? "봉헌자" : "수취인";
        default -> declaredName;
        };
    }

}
