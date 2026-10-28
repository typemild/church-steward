package org.church.steward.domain.budget.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.budget.transformer.BudgetAmountTransformer;
import org.church.steward.domain.budget.transformer.BudgetTransformer;
import org.church.steward.domain.budget.transformer.BudgetTypeTransformer;
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
import lombok.Setter;

@Entity
@Table(name = "BUDGET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Loggable(name = "예산")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    @LoggableField(name = "회계 연도", isIdentifier = true, transformer = FiscalYearTransformer.class)
    private FiscalYear fiscalYear;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    @LoggableField(name = "유형", isIdentifier = true, transformer = BudgetTypeTransformer.class)
    private BudgetType type;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Budget> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @LoggableField(name = "상위 예산", isIdentifier = true, transformer = BudgetTransformer.class)
    private Budget parent;

    @Setter
    @Column(nullable = false, length = BudgetConstraints.NAME_MAX_LENGTH)
    @LoggableField(name = "예산명", isIdentifier = true)
    private String name;

    @Setter
    @Column(nullable = true)
    @LoggableField(name = "예산", transformer = BudgetAmountTransformer.class, format = "#,###원")
    private Long amount; // 자식 건 존재 시 null이 됨

    @Setter
    @Column(nullable = false)
    private int sortOrder;

    public long getTotalAmount() {
        if (children != null && !children.isEmpty()) {
            long total = 0;
            for (Budget child : children) {
                total += child.getTotalAmount();
            }

            return total;
        }

        return amount != null ? amount : 0L;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public String getHierarchyName() {
        List<String> names = new ArrayList<>();
        Budget current = this;
        while (current != null) {
            names.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(names);
        return String.join(" > ", names);
    }

}
