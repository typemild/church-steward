package org.church.steward.ui.view.report;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.report.controller.ReportController;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportType;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.main.FiscalYearSelector;
import org.church.steward.ui.view.report.composer.AnnualSettlementReportComposer;
import org.church.steward.ui.view.report.composer.BudgetPlanReportComposer;
import org.church.steward.ui.view.report.composer.SettlementDetailReportComposer;
import org.church.steward.ui.view.report.composer.WeeklySettlementReportComposer;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.output.ReportPdfExporter;
import org.church.steward.ui.view.report.output.ReportPrinter;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 보고서 화면. 보고서 종류를 고르면 사이드바에서 선택된 회계 연도의 보고서를 미리보기로 보여주고,
 * 미리보기에 보이는 보고서를 그대로 인쇄하거나 PDF로 저장한다.
 */
@UiView
@Slf4j
public class ReportingView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 8;
    private static final String PDF_EXTENSION = ".pdf";

    private final ReportController reportController;
    private final FiscalYearSelector fiscalYearSelector;

    private final StyledComboBox<ReportType> typeCombo = new StyledComboBox<>(ReportType.values(),
            ComponentSize.MEDIUM);
    /** 주간 보고서의 대상 주간. 주간 보고서에서만 보여준다. */
    private final WeekSelectorPanel weekSelector = new WeekSelectorPanel(this::load);
    private final PrimaryButton searchButton = new PrimaryButton("조회", ComponentSize.MEDIUM);
    private final SecondaryButton pdfButton = new SecondaryButton("PDF 저장", ComponentSize.MEDIUM);
    private final SecondaryButton printButton = new SecondaryButton("인쇄", ComponentSize.MEDIUM);
    private final ReportPreviewPanel previewPanel = new ReportPreviewPanel();
    private final JScrollPane previewScrollPane = new JScrollPane(previewPanel);

    /** 가장 최근에 보낸 조회 요청의 순번. 늦게 도착한 이전 요청의 결과를 버리는 데 쓴다. */
    private long loadSequence = 0;
    /** 미리보기에 보이는 보고서. 아직 조회하지 않았거나 조회에 실패했으면 null이다. */
    private LoadedReport loadedReport;
    /** 마지막으로 PDF를 저장한 폴더. 다음에 저장할 때 같은 폴더에서 시작한다. */
    private File lastPdfDirectory;

    /**
     * 조회한 보고서와 그 조회 조건. 인쇄하거나 저장할 때 화면의 선택이 아니라 실제로 보이는 보고서를 기준으로 삼기 위해 함께 보관한다.
     *
     * @param startDate 주간 보고서의 시작일. 그 밖의 보고서에서는 null
     * @param endDate   주간 보고서의 종료일. 그 밖의 보고서에서는 null
     */
    private record LoadedReport(ReportType type, Long fiscalYearId, LocalDate startDate, LocalDate endDate,
            ReportDocument document) {
    }

    public ReportingView(ReportController reportController, FiscalYearSelector fiscalYearSelector) {
        this.reportController = reportController;
        this.fiscalYearSelector = fiscalYearSelector;

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일) : 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setBackground(UIColors.BACKGROUND);
        setLayout(new BorderLayout(0, 10));

        add(createToolbar(), BorderLayout.NORTH);
        add(createPreviewPane(), BorderLayout.CENTER);

        typeCombo.addActionListener(_ -> {
            updateWeekVisibility();
            load();
        });
        searchButton.addActionListener(_ -> load());
        pdfButton.addActionListener(_ -> savePdf());
        printButton.addActionListener(_ -> print());

        setLoadedReport(null);
    }

    private JPanel createToolbar() {
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ReportType type) {
                    setText(type.getLabel());
                }
                return this;
            }
        });

        JPanel conditionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        conditionPanel.setOpaque(false);
        conditionPanel.add(new FieldLabel("보고서", FontAwesomeSolid.FILE_ALT, SwingConstants.LEFT,
                ComponentSize.MEDIUM));
        conditionPanel.add(Box.createHorizontalStrut(GAP));
        conditionPanel.add(typeCombo);
        conditionPanel.add(Box.createHorizontalStrut(GAP));
        conditionPanel.add(searchButton);

        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        outputPanel.setOpaque(false);
        outputPanel.add(pdfButton);
        outputPanel.add(Box.createHorizontalStrut(GAP));
        outputPanel.add(printButton);

        JPanel buttonRow = new JPanel(new BorderLayout(GAP, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(conditionPanel, BorderLayout.WEST);
        buttonRow.add(outputPanel, BorderLayout.EAST);

        // 주간은 한 줄에 다 들어가지 않으므로 아랫줄에 따로 둔다. 숨겨져 있는 동안에는 줄과 간격이 함께 사라진다.
        JPanel toolbar = new JPanel(new BorderLayout(0, 10));
        toolbar.setOpaque(false);
        toolbar.add(buttonRow, BorderLayout.CENTER);
        toolbar.add(weekSelector, BorderLayout.SOUTH);
        updateWeekVisibility();
        return toolbar;
    }

    private JScrollPane createPreviewPane() {
        previewScrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        previewScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        previewScrollPane.getVerticalScrollBar()
                .setUnitIncrement(24);
        previewScrollPane.getViewport()
                .setBackground(UIColors.REPORT_PREVIEW_BACKGROUND);
        return previewScrollPane;
    }

    /**
     * 사이드바에서 선택된 회계 연도로 조회한다. 이 화면이 보이는 동안 회계 연도가 바뀌어도 호출된다.
     */
    @Override
    public void onMenuSelected() {
        // 주간은 회계 연도의 기간을 벗어날 수 없다. 회계 연도가 그대로여도 회계 연도 관리에서 기간을 고쳤을 수 있으므로 매번 맞춘다.
        FiscalYearDto fiscalYear = fiscalYearSelector.getSelectedFiscalYear();
        if (fiscalYear != null) {
            weekSelector.setLimits(fiscalYear.getStartDate(), fiscalYear.getEndDate());
        }
        load();
    }

    private void load() {
        final Long fiscalYearId = fiscalYearSelector.getSelectedFiscalYearId();
        final ReportType type = (ReportType) typeCombo.getSelectedItem();
        final long sequence = ++loadSequence;
        if (fiscalYearId == null || type == null) {
            setLoadedReport(null);
            return;
        }

        // 주간 보고서만 기간을 쓴다. 주간은 수입/지출 관리의 "주간"과 같이 일요일부터 토요일까지이며, 회계 연도의 기간을 벗어난 부분은 빠진다.
        final LocalDate startDate = type.isWeekly() ? weekSelector.getStartDate() : null;
        final LocalDate endDate = type.isWeekly() ? weekSelector.getEndDate() : null;

        // 글자 폭을 재서 쪽을 나누는 배치 작업도 화면이 멈추지 않도록 조회와 함께 백그라운드에서 한다.
        AsyncTask.run(previewScrollPane,
                () -> new LoadedReport(type, fiscalYearId, startDate, endDate,
                        compose(type, fiscalYearId, startDate, endDate)),
                report -> {
                    if (sequence != loadSequence) {
                        return;
                    }
                    setLoadedReport(report);
                    previewPanel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
                },
                err -> {
                    if (sequence != loadSequence) {
                        return;
                    }
                    setLoadedReport(null);
                    log.error("Failed to load report", err);
                    JOptionPane.showMessageDialog(this, "보고서를 불러오는데 실패했습니다: " + err.getMessage());
                });
    }

    /**
     * 보고서의 데이터를 조회해 쪽으로 배치한다.
     *
     * @param startDate 주간 보고서의 시작일. 그 밖의 보고서에서는 쓰지 않는다.
     * @param endDate   주간 보고서의 종료일. 그 밖의 보고서에서는 쓰지 않는다.
     */
    private ReportDocument compose(ReportType type, Long fiscalYearId, LocalDate startDate, LocalDate endDate) {
        return switch (type) {
        case WEEKLY_SETTLEMENT -> WeeklySettlementReportComposer.compose(
                reportController.getWeeklySettlementReport(fiscalYearId, startDate, endDate));
        case INCOME_BUDGET_PLAN -> BudgetPlanReportComposer.compose(
                reportController.getBudgetPlanReport(fiscalYearId, BudgetType.INCOME));
        case EXPENSE_BUDGET_PLAN -> BudgetPlanReportComposer.compose(
                reportController.getBudgetPlanReport(fiscalYearId, BudgetType.EXPENSE));
        case ANNUAL_SETTLEMENT -> AnnualSettlementReportComposer.compose(
                reportController.getAnnualSettlementReport(fiscalYearId));
        case INCOME_SETTLEMENT_DETAIL -> SettlementDetailReportComposer.compose(
                reportController.getSettlementDetailReport(fiscalYearId, BudgetType.INCOME));
        case EXPENSE_SETTLEMENT_DETAIL -> SettlementDetailReportComposer.compose(
                reportController.getSettlementDetailReport(fiscalYearId, BudgetType.EXPENSE));
        };
    }

    /**
     * 주간은 주간 보고서에서만 쓰므로 그때만 보여준다.
     */
    private void updateWeekVisibility() {
        ReportType type = (ReportType) typeCombo.getSelectedItem();
        weekSelector.setVisible(type != null && type.isWeekly());
        revalidate();
    }

    /**
     * 미리보기에 보일 보고서를 바꾼다. 보고서가 없으면 인쇄와 PDF 저장을 막는다.
     */
    private void setLoadedReport(LoadedReport report) {
        loadedReport = report;
        previewPanel.setDocument(report != null ? report.document() : null);
        pdfButton.setEnabled(report != null);
        printButton.setEnabled(report != null);
    }

    private void print() {
        final LoadedReport report = loadedReport;
        if (report == null) {
            return;
        }

        final PrinterJob job;
        try {
            job = ReportPrinter.showDialog(report.document());
        } catch (RuntimeException e) {
            log.error("Failed to prepare printing", e);
            JOptionPane.showMessageDialog(this, "인쇄를 시작하지 못했습니다: " + e.getMessage());
            return;
        }
        if (job == null) {
            return;
        }

        AsyncTask.run(printButton,
                () -> {
                    // 도중에 취소한 인쇄는 기록하지 않는다.
                    if (ReportPrinter.print(job)) {
                        logOutput(report, ReportOutputType.PRINT);
                    }
                    return null;
                },
                _ -> {
                },
                err -> {
                    log.error("Failed to print report", err);
                    JOptionPane.showMessageDialog(this, "인쇄에 실패했습니다: " + err.getMessage());
                });
    }

    private void savePdf() {
        final LoadedReport report = loadedReport;
        if (report == null) {
            return;
        }

        final File file = choosePdfFile(report);
        if (file == null) {
            return;
        }
        lastPdfDirectory = file.getParentFile();

        AsyncTask.run(pdfButton,
                () -> {
                    ReportPdfExporter.export(report.document(), file.toPath());
                    logOutput(report, ReportOutputType.PDF);
                    return null;
                },
                _ -> JOptionPane.showMessageDialog(this, "PDF 파일로 저장했습니다.\n" + file.getAbsolutePath()),
                err -> {
                    log.error("Failed to save report as PDF", err);
                    JOptionPane.showMessageDialog(this, "PDF 저장에 실패했습니다: " + err.getMessage());
                });
    }

    /**
     * 저장할 PDF 파일을 고르게 한다. 파일 이름은 보고서 제목으로 미리 채워 둔다.
     *
     * @return 고른 파일. 취소했거나 기존 파일을 덮어쓰지 않기로 했으면 null
     */
    private File choosePdfFile(LoadedReport report) {
        JFileChooser chooser = new JFileChooser(lastPdfDirectory);
        chooser.setDialogTitle("PDF 저장");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF 문서 (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File(lastPdfDirectory, toFileName(report.document()
                .getTitle()) + PDF_EXTENSION));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName()
                .toLowerCase()
                .endsWith(PDF_EXTENSION)) {
            file = new File(file.getParentFile(), file.getName() + PDF_EXTENSION);
        }
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(this, "같은 이름의 파일이 이미 있습니다. 덮어쓸까요?\n" + file.getName(),
                    "PDF 저장", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return file;
    }

    /**
     * 보고서 제목에서 파일 이름에 쓸 수 없는 글자("수입/지출"의 빗금 등)를 밑줄로 바꾼다.
     */
    private static String toFileName(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void logOutput(LoadedReport report, ReportOutputType outputType) {
        reportController.logOutput(report.type(), outputType, report.fiscalYearId(), report.startDate(),
                report.endDate());
    }
}
