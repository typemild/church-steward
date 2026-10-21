package org.church.steward.ui.view.dashboard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.dashboard.controller.DashboardController;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.FiscalYearSummaryDto;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.BadgeLabel;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.component.table.PaddedCellRenderer;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;
import org.church.steward.ui.view.main.FiscalYearSelector;
import org.church.steward.util.NumberUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 선택된 회계 연도의 재정 현황을 한 화면에 보여준다.
 * 위에서부터 요약 카드(총 수입, 총 지출, 차액), 월별 수입·지출 차트와 예산 집행 현황, 최근 내역을 배치한다.
 */
@UiView
@Slf4j
public class DashboardView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "재정 현황";
    /** 회계 연도 배지의 크기. 폭은 글자의 길이에 맞추되 이 범위를 벗어나지 않게 하며, 넘치는 글자는 말줄임표로 줄인다. */
    private static final int FISCAL_YEAR_BADGE_HEIGHT = 22;
    private static final int FISCAL_YEAR_BADGE_PADDING = 24;
    private static final int FISCAL_YEAR_BADGE_MIN_WIDTH = 60;
    private static final int FISCAL_YEAR_BADGE_MAX_WIDTH = 400;
    private static final int TITLE_BADGE_GAP = 10;

    /** 최근 내역 테이블의 폰트. 컬럼이 많아 기본 크기보다 1pt 작게 쓴다. */
    private static final Font TRANSACTION_FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN,
            UIFonts.FONT_SIZE_BASE - 1);

    /** 카드와 카드 사이의 간격 */
    private static final int CARD_GAP = 14;
    private static final int CARD_HEADER_GAP = 10;

    /** 요약 카드 아래 영역을 차트 줄과 최근 내역이 나눠 갖는 높이 비율 */
    private static final double CHART_ROW_WEIGHT = 0.55;
    private static final double RECENT_ROW_WEIGHT = 0.45;
    /** 차트 줄을 월별 차트와 예산 집행 현황이 나눠 갖는 폭 비율 */
    private static final double TREND_CARD_WEIGHT = 0.58;
    private static final double BUDGET_CARD_WEIGHT = 0.42;

    private final DashboardController controller;
    private final FiscalYearSelector fiscalYearSelector;

    /** 제목 옆에서 조회 중인 회계 연도의 이름과 기간을 알려주는 배지. 첫 조회가 끝나기 전에는 숨겨 둔다. */
    private final BadgeLabel fiscalYearBadge = new BadgeLabel("", UIColors.CHIP_TEXT, UIColors.CHIP_BACKGROUND,
            ComponentSize.SMALL);

    private final SummaryCard incomeCard = new SummaryCard("총 수입", FontAwesomeSolid.PLUS_CIRCLE, UIColors.INCOME);
    private final SummaryCard expenseCard = new SummaryCard("총 지출", FontAwesomeSolid.MINUS_CIRCLE,
            UIColors.EXPENSE);
    private final SummaryCard balanceCard = new SummaryCard("차액", FontAwesomeSolid.EQUALS, UIColors.BALANCE);

    private final MonthlyTrendChart monthlyTrendChart = new MonthlyTrendChart();
    private final BudgetProgressCard budgetProgressCard = new BudgetProgressCard();

    private final RecentTransactionTableModel transactionTableModel = new RecentTransactionTableModel();

    /** 가장 최근에 보낸 조회 요청의 순번. 늦게 도착한 이전 요청의 결과를 버리는 데 쓴다. */
    private long loadSequence = 0;

    public DashboardView(DashboardController controller, FiscalYearSelector fiscalYearSelector) {
        this.controller = controller;
        this.fiscalYearSelector = fiscalYearSelector;

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일): 720 x 600
        Dimension minSize = new Dimension(720, 600);
        setPreferredSize(minSize);
        setMinimumSize(minSize);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setBackground(UIColors.BACKGROUND);
        setOpaque(true);

        initComponents();
    }

    /**
     * 사이드바에서 선택된 회계 연도로 조회한다. 이 화면이 보이는 동안 회계 연도가 바뀌어도 호출된다.
     */
    @Override
    public void onMenuSelected() {
        loadData();
    }

    private void initComponents() {
        JPanel summaryRow = new JPanel(new GridLayout(1, 3, CARD_GAP, 0));
        summaryRow.setOpaque(false);
        summaryRow.add(incomeCard);
        summaryRow.add(expenseCard);
        summaryRow.add(balanceCard);

        JPanel topPanel = new JPanel(new BorderLayout(0, CARD_GAP));
        topPanel.setOpaque(false);
        topPanel.add(createTitlePanel(), BorderLayout.NORTH);
        topPanel.add(summaryRow, BorderLayout.CENTER);

        // 요약 카드 아래의 남는 높이를 차트 줄과 최근 내역이 정해진 비율로 나눠 갖는다.
        JPanel bodyPanel = new JPanel(new GridBagLayout());
        bodyPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;

        gbc.gridy = 0;
        gbc.weighty = CHART_ROW_WEIGHT;
        gbc.insets = new Insets(0, 0, CARD_GAP, 0);
        bodyPanel.add(createChartRow(), gbc);

        gbc.gridy = 1;
        gbc.weighty = RECENT_ROW_WEIGHT;
        gbc.insets = new Insets(0, 0, 0, 0);
        bodyPanel.add(createRecentTransactionsCard(), gbc);

        setLayout(new BorderLayout(0, CARD_GAP));
        add(topPanel, BorderLayout.NORTH);
        add(bodyPanel, BorderLayout.CENTER);
    }

    /**
     * 화면 제목 줄을 만든다. 아이콘이 붙은 제목 옆에 회계 연도 배지를 둔다.
     */
    private JPanel createTitlePanel() {
        TitleLabel titleLabel = new TitleLabel(TITLE, FontAwesomeSolid.CHART_LINE, SwingConstants.LEADING);

        // 이름은 사용자가 입력한 글자이므로 HTML로 해석되지 않게 한다.
        fiscalYearBadge.putClientProperty("html.disable", Boolean.TRUE);
        fiscalYearBadge.setVisible(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.add(titleLabel);
        panel.add(Box.createHorizontalStrut(TITLE_BADGE_GAP));
        panel.add(fiscalYearBadge);
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private JPanel createChartRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        ignorePreferredSize(row);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.weightx = TREND_CARD_WEIGHT;
        gbc.insets = new Insets(0, 0, 0, CARD_GAP);
        row.add(createMonthlyTrendCard(), gbc);

        gbc.gridx = 1;
        gbc.weightx = BUDGET_CARD_WEIGHT;
        gbc.insets = new Insets(0, 0, 0, 0);
        ignorePreferredSize(budgetProgressCard);
        row.add(budgetProgressCard, gbc);

        return row;
    }

    private JPanel createMonthlyTrendCard() {
        DashboardCard card = new DashboardCard(new BorderLayout(0, CARD_HEADER_GAP));
        ignorePreferredSize(card);
        card.add(createCardHeader("월별 수입·지출", MonthlyTrendChart.createLegend()), BorderLayout.NORTH);
        card.add(monthlyTrendChart, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRecentTransactionsCard() {
        JTable table = new JTable(transactionTableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                if (row == -1 || column == -1) {
                    return null;
                }
                return transactionTableModel.getTooltipText(row, convertColumnIndexToModel(column));
            }
        };
        table.setFillsViewportHeight(true);
        table.setRowHeight(Math.round(TRANSACTION_FONT.getSize() * 2.3f));
        table.setFont(TRANSACTION_FONT);
        table.getTableHeader()
                .setFont(TRANSACTION_FONT);
        table.setBackground(UIColors.CARD_BACKGROUND);
        // 세로 구분선 없이 행 사이에만 옅은 선을 둔다.
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(UIColors.CHART_GRID);
        table.setIntercellSpacing(new Dimension(0, 1));
        // 보기만 하는 목록이므로 행을 선택할 수 없게 한다.
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.getTableHeader()
                .setReorderingAllowed(false);

        // 날짜를 "월/일"로 줄여 남은 폭을 예산, 봉헌자/수취인, 내역, 비고에 나눠 준다.
        setupColumn(table, RecentTransactionTableModel.COLUMN_DATE, 70, new PaddedCellRenderer(SwingConstants.CENTER));
        setupColumn(table, RecentTransactionTableModel.COLUMN_TYPE, 50, new PaddedCellRenderer(SwingConstants.CENTER));
        setupColumn(table, RecentTransactionTableModel.COLUMN_BUDGET, 105,
                new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(table, RecentTransactionTableModel.COLUMN_CONTACT, 105,
                new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(table, RecentTransactionTableModel.COLUMN_DESCRIPTION, 140,
                new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(table, RecentTransactionTableModel.COLUMN_AMOUNT, 90, new SignedAmountCellRenderer());
        setupColumn(table, RecentTransactionTableModel.COLUMN_REMARKS, 85,
                new PaddedCellRenderer(SwingConstants.LEADING));

        // 가로 스크롤 없이 컬럼 폭을 테이블 폭에 맞춘다.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport()
                .setBackground(UIColors.CARD_BACKGROUND);

        DashboardCard card = new DashboardCard(new BorderLayout(0, CARD_HEADER_GAP));
        ignorePreferredSize(card);
        card.add(createCardHeader("최근 내역", null), BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private void setupColumn(JTable table, int column, int preferredWidth, TableCellRenderer renderer) {
        TableColumn tableColumn = table.getColumnModel()
                .getColumn(column);
        tableColumn.setPreferredWidth(preferredWidth);
        tableColumn.setCellRenderer(renderer);
    }

    /**
     * 카드의 제목 줄을 만든다.
     *
     * @param trailing 제목 줄의 오른쪽 끝에 둘 컴포넌트. 없으면 null
     */
    private JPanel createCardHeader(String title, JComponent trailing) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(new SectionHeaderLabel(title), BorderLayout.WEST);
        if (trailing != null) {
            header.add(trailing, BorderLayout.EAST);
        }
        return header;
    }

    /**
     * 컴포넌트가 내용의 크기와 상관없이 GridBagLayout의 비율(weight)대로만 공간을 나눠 갖게 한다.
     * 선호 크기를 그대로 두면 내용이 큰 쪽이 공간을 더 가져가고, 선호 크기의 합이 화면보다 커지면 배치가 무너진다.
     */
    private void ignorePreferredSize(JComponent component) {
        component.setPreferredSize(new Dimension(0, 0));
    }

    /**
     * 배지에 회계 연도의 이름과 기간을 "2026년 / 2026-01-01 ~ 2026-12-31"처럼 적고, 폭을 글자의 길이에 맞춘다.
     */
    private void showFiscalYearBadge(String fiscalYearName, LocalDate startDate, LocalDate endDate) {
        String text = fiscalYearName + " / " + startDate + " ~ " + endDate;
        int textWidth = fiscalYearBadge.getFontMetrics(fiscalYearBadge.getFont())
                .stringWidth(text);
        int width = Math.clamp(textWidth + FISCAL_YEAR_BADGE_PADDING, FISCAL_YEAR_BADGE_MIN_WIDTH,
                FISCAL_YEAR_BADGE_MAX_WIDTH);
        Dimension size = new Dimension(width, FISCAL_YEAR_BADGE_HEIGHT);

        fiscalYearBadge.setText(text);
        fiscalYearBadge.setToolTipText(text);
        fiscalYearBadge.setPreferredSize(size);
        fiscalYearBadge.setMaximumSize(size);
        fiscalYearBadge.setVisible(true);
        fiscalYearBadge.revalidate();
    }

    private void loadData() {
        FiscalYearDto fiscalYear = fiscalYearSelector.getSelectedFiscalYear();
        if (fiscalYear == null) {
            return;
        }

        final Long fiscalYearId = fiscalYear.getId();
        final String fiscalYearName = fiscalYear.getName();
        final LocalDate startDate = fiscalYear.getStartDate();
        final LocalDate endDate = fiscalYear.getEndDate();
        final long sequence = ++loadSequence;

        AsyncTask.run(this, () -> {
            FiscalYearSummaryDto summary = controller.getFiscalYearSummary(fiscalYearId);
            List<BudgetProgressDto> progress = controller.getBudgetProgress(fiscalYearId);
            List<MonthlyTrendDto> trend = controller.getMonthlyTrend(fiscalYearId);
            List<RecentTransactionDto> transactions = controller.getRecentTransactions(fiscalYearId);
            return new DashboardData(summary, progress, trend, transactions);
        }, data -> {
            if (sequence != loadSequence) {
                return;
            }
            showFiscalYearBadge(fiscalYearName, startDate, endDate);
            updateSummary(data.summary(), data.progress());
            monthlyTrendChart.setData(data.trend());
            budgetProgressCard.setData(data.progress());
            transactionTableModel.setTransactions(data.transactions());
        }, err -> {
            if (sequence != loadSequence) {
                return;
            }
            log.error("Failed to load dashboard data", err);
            JOptionPane.showMessageDialog(this, "데이터를 로딩하는 중 오류가 발생했습니다.");
        });
    }

    private void updateSummary(FiscalYearSummaryDto summary, List<BudgetProgressDto> progressList) {
        long income = summary.getTotalIncome();
        long expense = summary.getTotalExpense();
        long balance = summary.getCurrentBalance();

        incomeCard.setAmount(income);
        incomeCard.setBudgetProgress(income, sumBudgetedAmount(progressList, BudgetType.INCOME));

        expenseCard.setAmount(expense);
        expenseCard.setBudgetProgress(expense, sumBudgetedAmount(progressList, BudgetType.EXPENSE));

        balanceCard.setAmount(balance);
        balanceCard.setAmountColor(SummaryCard.balanceColor(balance));
        balanceCard.setNote(createBalanceNote(income, balance));
    }

    /**
     * 구분이 같은 최상위 예산의 예산 금액을 모두 더한다.
     */
    private long sumBudgetedAmount(List<BudgetProgressDto> progressList, BudgetType type) {
        long total = 0;
        for (BudgetProgressDto progress : progressList) {
            if (progress.getType() == type) {
                total += progress.getBudgetedAmount();
            }
        }
        return total;
    }

    private String createBalanceNote(long income, long balance) {
        if (balance < 0) {
            return "지출이 수입보다 많습니다";
        }
        if (income <= 0) {
            return null;
        }
        return String.format("수입의 %.1f%%", balance * 100.0 / income);
    }

    private record DashboardData(FiscalYearSummaryDto summary, List<BudgetProgressDto> progress,
            List<MonthlyTrendDto> trend, List<RecentTransactionDto> transactions) {
    }

    /**
     * 최근 내역 테이블의 모델. 금액은 수입이면 양수, 지출이면 음수로 내보낸다.
     */
    private static final class RecentTransactionTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        static final int COLUMN_DATE = 0;
        static final int COLUMN_TYPE = 1;
        static final int COLUMN_BUDGET = 2;
        static final int COLUMN_CONTACT = 3;
        static final int COLUMN_DESCRIPTION = 4;
        static final int COLUMN_AMOUNT = 5;
        static final int COLUMN_REMARKS = 6;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

        private static final String[] COLUMN_NAMES = { "일자", "구분", "예산", "봉헌자/수취인", "내역", "금액", "비고" };

        private transient List<RecentTransactionDto> transactions = List.of();

        void setTransactions(List<RecentTransactionDto> transactions) {
            this.transactions = List.copyOf(transactions);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return transactions.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RecentTransactionDto transaction = transactions.get(rowIndex);
            return switch (columnIndex) {
            case COLUMN_DATE -> transaction.getDate()
                    .format(DATE_FORMATTER);
            case COLUMN_TYPE -> transaction.getType();
            case COLUMN_BUDGET -> transaction.getCategory();
            case COLUMN_CONTACT -> FinancialRecordFormats.joinNames(transaction.getContactNames());
            case COLUMN_DESCRIPTION -> transaction.getDescription();
            case COLUMN_AMOUNT -> isExpense(transaction) ? -transaction.getAmount() : transaction.getAmount();
            case COLUMN_REMARKS -> transaction.getRemarks();
            default -> null;
            };
        }

        /**
         * 셀이 좁아 글자가 잘릴 수 있는 컬럼의 전체 내용을 툴팁으로 돌려준다.
         */
        String getTooltipText(int rowIndex, int columnIndex) {
            RecentTransactionDto transaction = transactions.get(rowIndex);
            String text = switch (columnIndex) {
            // 셀에는 "월/일"만 보이므로 연도까지 포함한 날짜를 알려준다.
            case COLUMN_DATE -> transaction.getDate()
                    .toString();
            case COLUMN_BUDGET -> transaction.getCategory();
            case COLUMN_CONTACT -> FinancialRecordFormats.joinNames(transaction.getContactNames());
            case COLUMN_DESCRIPTION -> transaction.getDescription();
            case COLUMN_AMOUNT -> NumberUtils.toKoreanAmount(transaction.getAmount());
            case COLUMN_REMARKS -> transaction.getRemarks();
            default -> null;
            };
            return text == null || text.isBlank() ? null : text;
        }

        private boolean isExpense(RecentTransactionDto transaction) {
            return FinancialRecordType.EXPENSE.getLabel()
                    .equals(transaction.getType());
        }
    }

    /**
     * 금액을 부호와 함께("+53,000", "-8,500") 수입/지출 색으로 표시하는 셀 렌더러
     */
    private static final class SignedAmountCellRenderer extends PaddedCellRenderer {

        private static final long serialVersionUID = 1L;

        private SignedAmountCellRenderer() {
            super(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Long amount) {
                setText(String.format("%+,d", amount));
                setForeground(amount < 0 ? UIColors.EXPENSE : UIColors.INCOME);
            }
            return this;
        }
    }
}
