package org.church.steward.ui.view.financialrecord;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.financialrecord.controller.FinancialRecordController;
import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.budget.BudgetDisplayOrder;
import org.church.steward.ui.view.financialrecord.budget.BudgetTabPanel;
import org.church.steward.ui.view.financialrecord.itemized.ItemizedTabPanel;
import org.church.steward.ui.view.main.FiscalYearSelector;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;

import lombok.extern.slf4j.Slf4j;

@UiView
@Slf4j
public class FinancialRecordView extends JPanel implements MenuActivatable, FinancialRecordActionHandler {

    private static final long serialVersionUID = 1L;
    /** 여러 건을 고르는 방법. Swing의 표는 맥에서 Ctrl 대신 Command 키로 한 건씩 더 고른다. */
    private static final String MULTI_SELECT_GUIDE = SystemInfo.isMacOS
            ? "Command(⌘) 또는 Shift를 누른 채 클릭하면 여러 건을 선택할 수 있습니다."
            : "Ctrl 또는 Shift를 누른 채 클릭하면 여러 건을 선택할 수 있습니다.";

    private final FinancialRecordController financialRecordController;
    private final BudgetController budgetController;
    private final ContactController contactController;
    private final ContactGroupController contactGroupController;
    private final FiscalYearSelector fiscalYearSelector;

    private final FinancialRecordSearchPanel searchPanel;
    private final FinancialSummaryPanel summaryPanel = new FinancialSummaryPanel();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final BudgetTabPanel budgetTabPanel;
    private final ItemizedTabPanel itemizedTabPanel;

    /** 마지막으로 조회한 회계 연도의 ID. 사이드바에서 회계 연도가 바뀌었는지 판단하는 데 쓴다. */
    private Long lastFiscalYearId;
    /** 가장 최근에 보낸 조회 요청의 순번. 늦게 도착한 이전 요청의 결과를 버리는 데 쓴다. */
    private long searchSequence = 0;
    /** 마지막 추가 다이얼로그에서 확정한 입력값. 아직 추가한 적이 없거나 회계 연도가 바뀌면 null이다. */
    private LastAddInput lastAddInput;

    /**
     * 추가 다이얼로그에서 확정한 구분, 날짜, 예산. 다음 추가 다이얼로그의 초기값으로 쓴다.
     */
    private record LastAddInput(FinancialRecordType type, LocalDate date, Long budgetId, String budgetPath) {
    }

    /**
     * 조회 결과. 수입/지출 내역과 예산 ID별 표시 위치.
     */
    private record SearchResult(List<FinancialRecordResponseDto> records, Map<Long, Integer> budgetPositions) {
    }

    public FinancialRecordView(FinancialRecordController financialRecordController,
            BudgetController budgetController, ContactController contactController,
            ContactGroupController contactGroupController, FiscalYearSelector fiscalYearSelector) {
        this.financialRecordController = financialRecordController;
        this.budgetController = budgetController;
        this.contactController = contactController;
        this.contactGroupController = contactGroupController;
        this.fiscalYearSelector = fiscalYearSelector;
        this.searchPanel = new FinancialRecordSearchPanel(budgetController, contactController,
                fiscalYearSelector::getSelectedFiscalYearId, this::search);
        this.budgetTabPanel = new BudgetTabPanel(this);
        this.itemizedTabPanel = new ItemizedTabPanel(this);

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일): 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setBackground(UIColors.BACKGROUND);
        setLayout(new BorderLayout(0, 10));

        layoutComponents();
    }

    private void layoutComponents() {
        // [1행] ~ [2행] 검색 조건, 총합
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(summaryPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // [3행] 탭
        tabbedPane.setBackground(UIColors.BACKGROUND);
        tabbedPane.addTab("예산별 보기", budgetTabPanel);
        tabbedPane.addTab("건별 보기", itemizedTabPanel);
        // 두 탭의 표 모두 여러 건을 고를 수 있으므로 탭 머리글 오른쪽에 고르는 방법을 한 번만 안내한다.
        SubFieldLabel multiSelectGuide = new SubFieldLabel(MULTI_SELECT_GUIDE, SwingConstants.RIGHT,
                ComponentSize.SMALL) {
            private static final long serialVersionUID = 1L;

            @Override
            public void addNotify() {
                super.addNotify();
                // FlatLaf가 trailing component를 감싸는 패널은 불투명하므로, 탭 영역의 배경이 그대로 보이도록 투명하게 한다.
                if (getParent() instanceof JComponent parent) {
                    parent.setOpaque(false);
                }
            }
        };
        multiSelectGuide.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, multiSelectGuide);
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * 사이드바에서 선택된 회계 연도로 조회한다. 이 화면이 보이는 동안 회계 연도가 바뀌어도 호출된다.
     * 회계 연도는 다른 화면에 있는 동안에도 바뀔 수 있으므로, 마지막으로 조회한 회계 연도와 비교해 바뀌었는지 판단한다.
     */
    @Override
    public void onMenuSelected() {
        FiscalYearDto fiscalYear = fiscalYearSelector.getSelectedFiscalYear();
        Long fiscalYearId = fiscalYear == null ? null : fiscalYear.getId();
        if (!Objects.equals(fiscalYearId, lastFiscalYearId)) {
            // 예산은 회계 연도에 속하므로 회계 연도가 바뀌면 예산 조건을 해제한다.
            searchPanel.clearBudget();
            // 추가 다이얼로그의 마지막 입력값도 회계 연도가 바뀌면 이어 쓰지 않는다.
            lastAddInput = null;
            lastFiscalYearId = fiscalYearId;
        }
        // 조회 기간은 회계 연도의 기간을 벗어날 수 없다. 회계 연도가 그대로여도 회계 연도 관리에서 기간을 고쳤을 수 있으므로 매번 맞춘다.
        if (fiscalYear != null) {
            searchPanel.setFiscalYearPeriod(fiscalYear.getStartDate(), fiscalYear.getEndDate());
        }
        // 마감된 회계 연도의 내역은 조회만 할 수 있다. 마감 여부도 회계 연도 관리에서 바뀔 수 있으므로 매번 맞춘다.
        boolean editable = fiscalYear != null && !fiscalYear.isClosed();
        budgetTabPanel.setEditable(editable);
        itemizedTabPanel.setEditable(editable);
        search();
    }

    private void search() {
        search(null);
    }

    /**
     * 조회한 뒤 지정한 내역이 결과에 있으면 두 탭에서 그 내역을 선택하고 보이도록 스크롤한다.
     * 새로 추가한 내역은 정렬에 따라 목록의 아래쪽에 놓일 수 있기 때문이다.
     *
     * @param revealRecordId 보여줄 내역의 ID. null이면 조회만 한다.
     */
    private void search(Long revealRecordId) {
        final FinancialRecordSearchRequestDto request = searchPanel.getSearchRequest();
        final long sequence = ++searchSequence;

        AsyncTask.run(tabbedPane,
                () -> loadRecords(request),
                result -> {
                    if (sequence != searchSequence) {
                        return;
                    }
                    summaryPanel.update(result.records());
                    budgetTabPanel.setRecords(result.records(), result.budgetPositions());
                    itemizedTabPanel.setRecords(result.records());
                    if (revealRecordId != null) {
                        budgetTabPanel.revealRecord(revealRecordId);
                        itemizedTabPanel.revealRecord(revealRecordId);
                    }
                },
                err -> {
                    if (sequence != searchSequence) {
                        return;
                    }
                    log.error("Failed to load financial records", err);
                    JOptionPane.showMessageDialog(this, "수입/지출 내역을 불러오는데 실패했습니다: " + err.getMessage());
                });
    }

    @Override
    public void addRecord(FinancialRecordType type, Long budgetId, String budgetPath) {
        FiscalYearDto fiscalYear = fiscalYearSelector.getSelectedFiscalYear();
        if (fiscalYear == null) {
            JOptionPane.showMessageDialog(this, "회계 연도를 먼저 선택해주세요.");
            return;
        }

        // 구분이 정해지지 않았으면 마지막으로 추가한 구분, 그것도 없으면 수입을 기본값으로 두고 다이얼로그에서 바꿀 수 있게 한다.
        FinancialRecordType initialType = type;
        if (initialType == null) {
            initialType = lastAddInput != null ? lastAddInput.type() : FinancialRecordType.INCOME;
        }
        FinancialRecordEntryDialog dialog = new FinancialRecordEntryDialog(SwingUtilities.getWindowAncestor(this),
                type != null ? type.getLabel() + " 추가" : "수입/지출 추가", budgetController, contactController,
                contactGroupController, fiscalYear, initialType, type == null);
        if (lastAddInput != null) {
            dialog.setDate(lastAddInput.date());
        }
        // 호출한 쪽에서 지정한 예산이 우선이다. 마지막 예산은 구분이 같을 때만 이어 쓴다.
        if (budgetId != null) {
            dialog.setBudget(budgetId, budgetPath);
        } else if (lastAddInput != null && lastAddInput.type() == initialType) {
            dialog.setBudget(lastAddInput.budgetId(), lastAddInput.budgetPath());
        }
        dialog.enableContinuousEntry();
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        final FinancialRecordType confirmedType = dialog.getFinancialRecordType();
        lastAddInput = new LastAddInput(confirmedType, dialog.getDate(), dialog.getBudgetId(), dialog.getBudgetPath());
        final FinancialRecordRequestDto request = dialog.toRequest();
        final boolean continueRequested = dialog.isContinueRequested();
        final Long confirmedBudgetId = dialog.getBudgetId();
        final String confirmedBudgetPath = dialog.getBudgetPath();

        AsyncTask.run(tabbedPane,
                () -> financialRecordController.createFinancialRecord(request),
                created -> {
                    search(created.getId());
                    if (continueRequested) {
                        // 연속 입력: 추가에 성공한 뒤에만 방금 확정한 구분, 날짜, 예산으로 다이얼로그를 다시 연다.
                        // 구분과 날짜는 lastAddInput으로 이어지고, 예산은 호출한 쪽이 처음 지정한 것 대신 방금 확정한 것을 쓴다.
                        SwingUtilities.invokeLater(() -> addRecord(type, confirmedBudgetId, confirmedBudgetPath));
                    }
                },
                err -> {
                    log.error("Failed to add financial record", err);
                    JOptionPane.showMessageDialog(this, "추가 실패: " + err.getMessage());
                });
    }

    @Override
    public void editRecord(FinancialRecordResponseDto record) {
        final FinancialRecordType type = record.getType();
        final Long id = record.getId();

        FiscalYearDto fiscalYear = fiscalYearSelector.getFiscalYear(record.getFiscalYearId());
        if (fiscalYear == null) {
            JOptionPane.showMessageDialog(this, "내역이 속한 회계 연도를 찾을 수 없습니다.");
            return;
        }

        FinancialRecordEntryDialog dialog = new FinancialRecordEntryDialog(SwingUtilities.getWindowAncestor(this),
                type.getLabel() + " 수정", budgetController, contactController, contactGroupController,
                fiscalYear, type, false);
        dialog.setRecord(record);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        final FinancialRecordRequestDto request = dialog.toRequest();

        AsyncTask.run(tabbedPane,
                () -> financialRecordController.updateFinancialRecord(id, request),
                _ -> search(),
                err -> {
                    log.error("Failed to edit financial record", err);
                    JOptionPane.showMessageDialog(this, "수정 실패: " + err.getMessage());
                });
    }

    @Override
    public void deleteRecords(List<FinancialRecordResponseDto> records) {
        int confirm = JOptionPane.showConfirmDialog(this, "선택한 " + records.size() + "건의 내역을 삭제하시겠습니까?", "삭제 확인",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        final List<FinancialRecordResponseDto> targets = List.copyOf(records);
        AsyncTask.run(tabbedPane,
                () -> {
                    for (FinancialRecordResponseDto target : targets) {
                        financialRecordController.deleteFinancialRecord(target.getId());
                    }
                    return null;
                },
                _ -> search(),
                err -> {
                    log.error("Failed to delete financial records", err);
                    JOptionPane.showMessageDialog(this, "삭제 실패: " + err.getMessage());
                    // 여러 건 중 일부만 삭제되었을 수 있으므로 목록을 다시 불러온다.
                    search();
                });
    }

    /**
     * 수입/지출 내역을 조회한다. 각 내역의 예산명은 최상위부터 나열한 전체 예산명("예산A > 예산B > 예산C")으로 온다.
     * "예산별 보기" 탭이 예산을 정렬하는 데 쓸 예산의 표시 위치도 함께 조회한다.
     * 백그라운드 스레드에서 실행된다.
     */
    private SearchResult loadRecords(FinancialRecordSearchRequestDto request) {
        List<FinancialRecordResponseDto> records = financialRecordController.listFinancialRecords(request);
        return new SearchResult(records, loadBudgetPositions(records));
    }

    /**
     * 조회된 내역이 속한 회계 연도의 예산을 모두 불러와 예산 ID별 표시 위치를 만든다.
     */
    private Map<Long, Integer> loadBudgetPositions(List<FinancialRecordResponseDto> records) {
        Set<Long> fiscalYearIds = records.stream()
                .map(FinancialRecordResponseDto::getFiscalYearId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<BudgetDto> budgets = new ArrayList<>();
        for (Long fiscalYearId : fiscalYearIds) {
            for (BudgetType budgetType : BudgetType.values()) {
                budgets.addAll(budgetController.getBudgetsByFiscalYearAndType(fiscalYearId, budgetType));
            }
        }
        return BudgetDisplayOrder.positionsOf(budgets);
    }
}
