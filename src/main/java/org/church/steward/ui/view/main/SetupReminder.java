package org.church.steward.ui.view.main;

import java.awt.Window;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.util.AsyncTask;

import lombok.extern.slf4j.Slf4j;

/**
 * 수입/지출 관리에 들어올 때 수입/지출을 입력하기 전에 준비해야 할 것이 빠져 있으면 안내 팝업을 띄우고 그 메뉴로 바로 이동하게 한다.
 * 안내하는 상황은 두 가지다.
 * <ol>
 * <li>오늘이 속한 회계 연도가 없음: 회계 연도 관리로 이동한다.</li>
 * <li>회계 연도에 수입 예산이나 지출 예산이 없음: 사이드바의 회계 연도를 그 회계 연도로 바꾼 뒤 예산 관리로 이동한다.</li>
 * </ol>
 * 두 상황에 모두 해당하면 회계 연도를 먼저 만들어야 하므로 1만 안내한다. 마감된 회계 연도는 2의 대상에서 뺀다(예산 관리와 수입/지출
 * 관리에 이미 마감 안내가 있다).
 * <p>
 * 같은 안내는 프로그램을 다시 시작하기 전까지 한 번만 보여준다(1은 한 번, 2는 회계 연도마다 한 번). 보여준 기록은 메모리에만 둔다.
 */
@UiView
@Slf4j
public class SetupReminder {

    private final FiscalYearSelector fiscalYearSelector;
    private final BudgetController budgetController;

    /** 회계 연도 관리로 이동하는 동작 */
    private Runnable fiscalYearManagementAction = () -> {
    };
    /** 예산 관리로 이동하는 동작. 사이드바의 회계 연도는 이 동작을 실행하기 전에 바꿔 둔다. */
    private Runnable budgetManagementAction = () -> {
    };

    /** 오늘이 속한 회계 연도가 없다는 안내를 보여주었는지 여부 */
    private boolean fiscalYearReminderShown = false;
    /** 예산이 없다는 안내를 보여준 회계 연도의 ID */
    private final Set<Long> budgetReminderShownIds = new HashSet<>();

    public SetupReminder(FiscalYearSelector fiscalYearSelector, BudgetController budgetController) {
        this.fiscalYearSelector = fiscalYearSelector;
        this.budgetController = budgetController;
    }

    /**
     * 바로가기 버튼이 실행할 동작을 지정한다.
     *
     * @param fiscalYearManagementAction 회계 연도 관리로 이동하는 동작
     * @param budgetManagementAction     예산 관리로 이동하는 동작
     */
    public void setShortcutActions(Runnable fiscalYearManagementAction, Runnable budgetManagementAction) {
        this.fiscalYearManagementAction = fiscalYearManagementAction;
        this.budgetManagementAction = budgetManagementAction;
    }

    /**
     * 수입/지출 관리 메뉴에 들어올 때 확인한다. 오늘이 속한 회계 연도가 있으면 사이드바에서 선택한 회계 연도의 예산을 확인한다. 로그인
     * 직후에는 확인하지 않는다(첫 화면인 대시보드는 입력하는 곳이 아니다).
     */
    public void checkSelectedFiscalYear(Window owner) {
        if (findFiscalYearOfToday() == null) {
            remindFiscalYear(owner);
            return;
        }
        FiscalYearDto selected = fiscalYearSelector.getSelectedFiscalYear();
        if (selected != null) {
            checkBudgets(owner, selected);
        }
    }

    /**
     * 오늘이 기간에 드는 회계 연도를 찾는다. 회계 연도의 기간은 겹치지 않으므로 많아야 하나다. 마감된 회계 연도도 회계 연도가 있는 것으로
     * 본다.
     */
    private FiscalYearDto findFiscalYearOfToday() {
        LocalDate today = LocalDate.now();
        for (FiscalYearDto fiscalYear : fiscalYearSelector.getFiscalYears()) {
            if (!today.isBefore(fiscalYear.getStartDate()) && !today.isAfter(fiscalYear.getEndDate())) {
                return fiscalYear;
            }
        }
        return null;
    }

    private void remindFiscalYear(Window owner) {
        if (fiscalYearReminderShown) {
            return;
        }
        fiscalYearReminderShown = true;

        String heading = "오늘 날짜가 포함된 회계 연도가 없습니다.";
        String message = "오늘 날짜로 수입/지출을 입력하려면 새 회계 연도를 추가해야 합니다.";
        SetupReminderDialog dialog = new SetupReminderDialog(owner, heading, message, "회계 연도 관리로 이동");
        dialog.setVisible(true);
        if (dialog.isShortcutSelected()) {
            fiscalYearManagementAction.run();
        }
    }

    private void checkBudgets(Window owner, FiscalYearDto fiscalYear) {
        final Long fiscalYearId = fiscalYear.getId();
        if (fiscalYear.isClosed() || budgetReminderShownIds.contains(fiscalYearId)) {
            return;
        }
        // 창 전체를 잠그면 화면의 조회와 겹쳐 입력이 막히므로, 회계 연도 선택란만 잠근다.
        AsyncTask.run(fiscalYearSelector,
                () -> budgetController.getMissingBudgetTypes(fiscalYearId),
                missingTypes -> {
                    // 확인하는 동안 같은 회계 연도의 안내가 먼저 떴을 수 있다.
                    if (missingTypes.isEmpty() || budgetReminderShownIds.contains(fiscalYearId)) {
                        return;
                    }
                    remindBudgets(owner, fiscalYear, missingTypes);
                },
                err -> log.error("Failed to check budgets of fiscal year {}", fiscalYearId, err));
    }

    private void remindBudgets(Window owner, FiscalYearDto fiscalYear, List<BudgetType> missingTypes) {
        budgetReminderShownIds.add(fiscalYear.getId());

        List<String> labels = missingTypes.stream()
                .map(BudgetType::getLabel)
                .toList();
        String heading = fiscalYear.getName() + " 회계 연도에 " + String.join(" 예산과 ", labels) + " 예산이 없습니다.";
        String message = String.join("과 ", labels) + "을 입력하려면 예산을 등록해야 합니다.";
        SetupReminderDialog dialog = new SetupReminderDialog(owner, heading, message, "예산 관리로 이동");
        dialog.setVisible(true);
        if (dialog.isShortcutSelected()) {
            fiscalYearSelector.select(fiscalYear.getId());
            budgetManagementAction.run();
        }
    }
}
