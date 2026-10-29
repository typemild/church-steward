package org.church.steward.domain.fiscalyear.entity;

import java.time.LocalDate;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.transformer.BooleanTypeTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회계 연도. 달력의 해와 무관하게 이름과 기간(시작일~종료일)으로 정의하므로 한 해에 여러 개를 둘 수 있다(예: "2025년 상반기").
 * 기간은 다른 회계 연도와 겹치지 않으며, 수입/지출은 날짜가 자신이 속한 회계 연도의 기간 안에 있어야 한다. 마감하면 그 시점의 총
 * 수입, 총 지출, 차액을 저장하고, 마감을 취소할 때까지 회계 연도와 그에 속한 예산, 수입/지출을 바꿀 수 없다.
 */
@Entity
@Table(name = "FISCAL_YEAR")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Loggable(name = "회계 연도")
public class FiscalYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = FiscalYearConstraints.NAME_MAX_LENGTH)
    @LoggableField(name = "이름", isIdentifier = true)
    private String name;

    @Column(name = "start_date", nullable = false)
    @LoggableField(name = "시작일", format = "yyyy-MM-dd")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @LoggableField(name = "종료일", format = "yyyy-MM-dd")
    private LocalDate endDate;

    @Column(nullable = false)
    @LoggableField(name = "마감", transformer = BooleanTypeTransformer.class)
    private boolean closed;

    /** 마감 시점의 총 수입. 마감하지 않았으면 null이다. */
    @Column(name = "total_income")
    @LoggableField(name = "총 수입", format = "#,###원")
    private Long totalIncome;

    /** 마감 시점의 총 지출. 마감하지 않았으면 null이다. */
    @Column(name = "total_expense")
    @LoggableField(name = "총 지출", format = "#,###원")
    private Long totalExpense;

    /** 마감 시점의 차액(총 수입 - 총 지출). 마감하지 않았으면 null이다. */
    @Column
    @LoggableField(name = "차액", format = "#,###원")
    private Long balance;

    public void update(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * 회계 연도를 마감하고 마감 시점의 총 수입, 총 지출, 차액을 기록한다.
     */
    public void close(long totalIncome, long totalExpense) {
        this.closed = true;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = totalIncome - totalExpense;
    }

    /**
     * 마감을 취소한다. 마감할 때 기록한 금액은 지운다.
     */
    public void reopen() {
        this.closed = false;
        this.totalIncome = null;
        this.totalExpense = null;
        this.balance = null;
    }

    /**
     * 날짜가 회계 연도의 기간 안에 있는지 확인한다. 시작일과 종료일도 기간에 포함된다.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

}
