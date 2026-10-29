package org.church.steward.ui.view.report;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.report.controller.ReportController;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSettingConstraints;
import org.church.steward.domain.report.entity.ReportType;
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
import org.church.steward.ui.view.report.layout.ReportAutoFitter;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.output.ReportPdfExporter;
import org.church.steward.ui.view.report.output.ReportPrinter;
import org.church.steward.ui.view.report.viewer.ReportViewer;
import org.church.steward.ui.view.report.viewer.ReportViewerState;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 보고서 화면. 보고서 종류를 고르면 사이드바에서 선택된 회계 연도의 보고서를 미리보기로 보여주고, 미리보기에 보이는 보고서를 그대로
 * 인쇄하거나 PDF로 저장한다.
 * <p>
 * 글꼴, 글자 크기, 행 간격, 한 페이지에 맞춤과 미리보기 설정은 사용자가 보고서마다 정하며, 바꿀 때마다 저장한다. 서식을 바꾸면
 * 데이터를 다시 조회하지 않고 조회해 둔 데이터로 다시 배치한다.
 */
@UiView
@Slf4j
public class ReportingView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 8;
    private static final String PDF_EXTENSION = ".pdf";
    /** 설정을 바꾼 뒤 저장하기까지 기다리는 시간(ms). 휠로 배율을 바꿀 때처럼 잇따른 변경은 한 번에 저장한다. */
    private static final int SAVE_DELAY = 500;

    private final ReportController reportController;
    private final FiscalYearSelector fiscalYearSelector;

    private final StyledComboBox<ReportType> typeCombo = new StyledComboBox<>(ReportType.values(),
            ComponentSize.MEDIUM);
    /** 주간 보고서의 대상 주간. 주간 보고서에서만 보여준다. */
    private final WeekSelectorPanel weekSelector = new WeekSelectorPanel(this::load);
    private final PrimaryButton searchButton = new PrimaryButton("조회", ComponentSize.MEDIUM);
    private final SecondaryButton pdfButton = new SecondaryButton("PDF 저장", ComponentSize.MEDIUM);
    private final SecondaryButton printButton = new SecondaryButton("인쇄", ComponentSize.MEDIUM);
    private final ReportFormatBar formatBar = new ReportFormatBar(new FormatListener());
    private final ReportViewer viewer = new ReportViewer();

    /** 보고서별 설정. 처음 조회할 때 읽어 두고, 바꾸면 여기서 바꾼 뒤 저장한다. */
    private final Map<ReportType, ReportSettingDto> settings = new EnumMap<>(ReportType.class);
    /** 아직 저장하지 않은 설정의 보고서 */
    private final Set<ReportType> unsavedTypes = EnumSet.noneOf(ReportType.class);
    private final Timer saveTimer = new Timer(SAVE_DELAY, _ -> saveSettings());

    /** 가장 최근에 보낸 조회 요청의 순번. 늦게 도착한 이전 요청의 결과를 버리는 데 쓴다. */
    private long loadSequence = 0;
    /** 가장 최근에 시작한 다시 배치의 순번. 조회가 끝날 때도 올려서 그 전에 시작한 다시 배치의 결과를 버린다. */
    private long composeSequence = 0;
    /** 미리보기에 보이는 보고서. 아직 조회하지 않았거나 조회에 실패했으면 null이다. */
    private LoadedReport loadedReport;
    /** 마지막으로 PDF를 저장한 폴더. 다음에 저장할 때 같은 폴더에서 시작한다. */
    private File lastPdfDirectory;
    /** 설정을 저장하지 못했다고 이미 알렸는지 여부. 같은 안내를 거듭 띄우지 않는다. */
    private boolean saveFailureNotified;

    /**
     * 조회한 보고서와 그 조회 조건. 인쇄하거나 저장할 때 화면의 선택이 아니라 실제로 보이는 보고서를 기준으로 삼기 위해 함께 보관한다.
     *
     * @param startDate 주간 보고서의 시작일. 그 밖의 보고서에서는 null
     * @param endDate   주간 보고서의 종료일. 그 밖의 보고서에서는 null
     * @param composer  조회한 데이터를 서식에 따라 배치하는 함수. 서식을 바꾸면 이것으로 다시 배치한다.
     * @param fits      한 페이지에 담겼는지 여부. 한 페이지에 맞춤일 때만 의미가 있다.
     */
    private record LoadedReport(ReportType type, Long fiscalYearId, LocalDate startDate, LocalDate endDate,
            Function<ReportStyle, ReportDocument> composer, ReportDocument document, boolean fits) {

        LoadedReport withComposed(Composed composed) {
            return new LoadedReport(type, fiscalYearId, startDate, endDate, composer, composed.document(),
                    composed.fits());
        }
    }

    /**
     * 서식을 적용해 배치한 결과
     */
    private record Composed(ReportDocument document, boolean fits) {
    }

    /**
     * 조회 결과. 그 보고서의 설정을 처음 읽었으면 함께 돌려준다.
     */
    private record LoadResult(ReportSettingDto setting, LoadedReport report) {
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
        add(viewer, BorderLayout.CENTER);

        saveTimer.setRepeats(false);

        typeCombo.addActionListener(_ -> {
            updateWeekVisibility();
            load();
        });
        searchButton.addActionListener(_ -> load());
        pdfButton.addActionListener(_ -> savePdf());
        printButton.addActionListener(_ -> print());
        viewer.setStateChangeListener(() -> {
            ReportViewerState state = viewer.getState();
            updateSetting(setting -> setting.toBuilder()
                    .thumbnailsVisible(state.thumbnailsVisible())
                    .viewMode(state.viewMode())
                    .zoomMode(state.zoomMode())
                    .zoomPercent(state.zoomPercent())
                    .build(), false);
        });

        setLoadedReport(null, false);
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
        // 서식은 그 아래 줄이다.
        JPanel toolbar = new JPanel(new BorderLayout(0, 10));
        toolbar.setOpaque(false);
        toolbar.add(buttonRow, BorderLayout.NORTH);
        toolbar.add(weekSelector, BorderLayout.CENTER);
        toolbar.add(formatBar, BorderLayout.SOUTH);
        updateWeekVisibility();
        return toolbar;
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
            setLoadedReport(null, false);
            return;
        }

        // 주간 보고서만 기간을 쓴다. 주간은 수입/지출 관리의 "주간"과 같이 일요일부터 토요일까지이며, 회계 연도의 기간을 벗어난 부분은 빠진다.
        final LocalDate startDate = type.isWeekly() ? weekSelector.getStartDate() : null;
        final LocalDate endDate = type.isWeekly() ? weekSelector.getEndDate() : null;
        final ReportSettingDto cachedSetting = settings.get(type);

        // 글자 폭을 재서 페이지를 나누는 배치 작업도 화면이 멈추지 않도록 조회와 함께 백그라운드에서 한다.
        AsyncTask.run(viewer,
                () -> {
                    ReportSettingDto setting = cachedSetting != null ? cachedSetting
                            : reportController.getReportSetting(type);
                    Function<ReportStyle, ReportDocument> composer = fetch(type, fiscalYearId, startDate, endDate);
                    Composed composed = compose(composer, setting);
                    return new LoadResult(setting, new LoadedReport(type, fiscalYearId, startDate, endDate, composer,
                            composed.document(), composed.fits()));
                },
                result -> {
                    if (sequence != loadSequence) {
                        return;
                    }
                    // 이 조회보다 먼저 시작한 다시 배치는 이전 보고서의 것이므로 결과를 버린다.
                    composeSequence++;
                    settings.putIfAbsent(type, result.setting());
                    ReportSettingDto setting = settings.get(type);
                    viewer.setState(toViewerState(setting));
                    setLoadedReport(result.report(), false);
                    // 조회하는 동안 서식을 바꿨으면 바뀐 서식으로 다시 배치한다.
                    if (setting != result.setting()) {
                        recompose();
                    }
                },
                err -> {
                    if (sequence != loadSequence) {
                        return;
                    }
                    setLoadedReport(null, false);
                    log.error("Failed to load report", err);
                    JOptionPane.showMessageDialog(this, "보고서를 불러오는데 실패했습니다: " + err.getMessage());
                });
    }

    /**
     * 보고서의 데이터를 조회하고, 그 데이터를 서식에 따라 배치하는 함수를 돌려준다.
     *
     * @param startDate 주간 보고서의 시작일. 그 밖의 보고서에서는 쓰지 않는다.
     * @param endDate   주간 보고서의 종료일. 그 밖의 보고서에서는 쓰지 않는다.
     */
    private Function<ReportStyle, ReportDocument> fetch(ReportType type, Long fiscalYearId, LocalDate startDate,
            LocalDate endDate) {
        return switch (type) {
        case WEEKLY_SETTLEMENT -> {
            WeeklySettlementReportDto report = reportController.getWeeklySettlementReport(fiscalYearId, startDate,
                    endDate);
            yield style -> WeeklySettlementReportComposer.compose(report, style);
        }
        case INCOME_BUDGET_PLAN, EXPENSE_BUDGET_PLAN -> {
            BudgetType budgetType = type == ReportType.INCOME_BUDGET_PLAN ? BudgetType.INCOME : BudgetType.EXPENSE;
            BudgetPlanReportDto report = reportController.getBudgetPlanReport(fiscalYearId, budgetType);
            yield style -> BudgetPlanReportComposer.compose(report, style);
        }
        case ANNUAL_SETTLEMENT -> {
            AnnualSettlementReportDto report = reportController.getAnnualSettlementReport(fiscalYearId);
            yield style -> AnnualSettlementReportComposer.compose(report, style);
        }
        case INCOME_SETTLEMENT_DETAIL, EXPENSE_SETTLEMENT_DETAIL -> {
            BudgetType budgetType = type == ReportType.INCOME_SETTLEMENT_DETAIL ? BudgetType.INCOME
                    : BudgetType.EXPENSE;
            SettlementDetailReportDto report = reportController.getSettlementDetailReport(fiscalYearId, budgetType);
            yield style -> SettlementDetailReportComposer.compose(report, style);
        }
        };
    }

    /**
     * 설정의 서식으로 배치한다. 한 페이지에 맞춤이 켜져 있으면 저장된 글자 크기와 행 간격은 쓰지 않고 기본 서식에서 시작해 한 페이지에 담길
     * 때까지 줄인다 ({@link ReportAutoFitter}). 꺼져 있으면 저장된 서식을 쓰되, 글자 크기는 가장 작은 글자가 허용
     * 범위를 벗어나지 않게 맞춘다.
     */
    private static Composed compose(Function<ReportStyle, ReportDocument> composer, ReportSettingDto setting) {
        if (setting.isAutoFit()) {
            ReportAutoFitter.Result result = ReportAutoFitter.fit(composer, setting.getFontFamily());
            return new Composed(result.document(), result.fits());
        }

        ReportStyle style = new ReportStyle(setting.getFontFamily(), setting.getFontSizeStep(),
                setting.getLineSpacing());
        ReportDocument document = composer.apply(style);

        int step = Math.max(document.getMinFontSizeStep(),
                Math.min(document.getMaxFontSizeStep(), style.fontSizeStep()));
        if (step != style.fontSizeStep()) {
            document = composer.apply(style.withFontSizeStep(step));
        }
        return new Composed(document, true);
    }

    /**
     * 보이는 보고서를 지금 설정의 서식으로 다시 배치한다. 데이터는 다시 조회하지 않으며 보던 페이지를 유지한다.
     */
    private void recompose() {
        final LoadedReport current = loadedReport;
        if (current == null) {
            return;
        }
        final ReportSettingDto setting = settings.get(current.type());
        final long loadSnapshot = loadSequence;
        final long sequence = ++composeSequence;

        AsyncTask.run(viewer,
                () -> compose(current.composer(), setting),
                composed -> {
                    if (loadSnapshot != loadSequence || sequence != composeSequence) {
                        return;
                    }
                    setLoadedReport(current.withComposed(composed), true);
                },
                err -> {
                    if (loadSnapshot != loadSequence || sequence != composeSequence) {
                        return;
                    }
                    log.error("Failed to compose report", err);
                    JOptionPane.showMessageDialog(this, "보고서를 배치하지 못했습니다: " + err.getMessage());
                });
    }

    /**
     * 보이는 보고서의 설정을 바꾸고 저장을 예약한다.
     *
     * @param recompose 서식이 바뀌어 다시 배치해야 하는지 여부. 미리보기 설정만 바꿨으면 false
     */
    private void updateSetting(UnaryOperator<ReportSettingDto> change, boolean recompose) {
        LoadedReport current = loadedReport;
        if (current == null) {
            return;
        }
        ReportSettingDto setting = settings.get(current.type());
        if (setting == null) {
            return;
        }

        settings.put(current.type(), change.apply(setting));
        unsavedTypes.add(current.type());
        saveTimer.restart();

        if (recompose) {
            recompose();
        }
    }

    /**
     * 저장하지 않은 설정을 저장한다. 저장은 화면을 막지 않으며, 실패해도 화면의 설정은 그대로 쓴다.
     */
    private void saveSettings() {
        for (ReportType type : unsavedTypes) {
            final ReportSettingDto setting = settings.get(type);
            AsyncTask.run(formatBar,
                    () -> {
                        reportController.updateReportSetting(setting);
                        return null;
                    },
                    _ -> {
                    },
                    err -> {
                        log.error("Failed to save report setting: {}", type, err);
                        if (!saveFailureNotified) {
                            saveFailureNotified = true;
                            JOptionPane.showMessageDialog(this, "보고서 설정을 저장하지 못했습니다: " + err.getMessage());
                        }
                    });
        }
        unsavedTypes.clear();
    }

    private static ReportViewerState toViewerState(ReportSettingDto setting) {
        return new ReportViewerState(setting.isThumbnailsVisible(), setting.getViewMode(), setting.getZoomMode(),
                setting.getZoomPercent());
    }

    /**
     * 서식 줄에서 바꾼 값을 설정에 반영하고 다시 배치한다.
     * <p>
     * 글자 크기와 행 간격은 지금 보이는 문서의 값에서 한 단계씩 바꾸고, 허용 범위를 벗어나지 않게 한다. 사람이 직접 조정하면 한 페이지에
     * 맞춤을 끈다. 이때 보이던 값(자동으로 줄인 값)에서 이어서 조정하도록, 조정하지 않은 쪽 값도 보이던 값으로 함께 저장한다.
     */
    private final class FormatListener implements ReportFormatBar.Listener {

        @Override
        public void onFontFamilyChanged(ReportFontFamily fontFamily) {
            updateSetting(setting -> setting.toBuilder()
                    .fontFamily(fontFamily)
                    .build(), true);
        }

        @Override
        public void onFontSizeChanged(int direction) {
            LoadedReport current = loadedReport;
            if (current == null) {
                return;
            }
            ReportDocument document = current.document();
            ReportStyle shown = document.getStyle();
            int step = Math.max(document.getMinFontSizeStep(), Math.min(document.getMaxFontSizeStep(),
                    shown.fontSizeStep() + direction));
            adjustManually(step, shown.lineSpacingPercent());
        }

        @Override
        public void onLineSpacingChanged(int direction) {
            LoadedReport current = loadedReport;
            if (current == null) {
                return;
            }
            ReportStyle shown = current.document()
                    .getStyle();
            int lineSpacing = Math.max(ReportSettingConstraints.LINE_SPACING_MIN,
                    Math.min(ReportSettingConstraints.LINE_SPACING_MAX,
                            shown.lineSpacingPercent() + direction * ReportSettingConstraints.LINE_SPACING_STEP));
            adjustManually(shown.fontSizeStep(), lineSpacing);
        }

        private void adjustManually(int fontSizeStep, int lineSpacing) {
            updateSetting(setting -> setting.toBuilder()
                    .fontSizeStep(fontSizeStep)
                    .lineSpacing(lineSpacing)
                    .autoFit(false)
                    .build(), true);
        }

        /**
         * 이 보고서의 글꼴, 글자 크기, 행 간격, 한 페이지에 맞춤(꺼짐)을 기본값으로 되돌리고 바로 저장한다. 미리보기 설정(페이지 목록, 보기
         * 유형, 배율)은 그대로 둔다.
         */
        @Override
        public void onReset() {
            updateSetting(setting -> setting.toBuilder()
                    .fontFamily(ReportSettingConstraints.DEFAULT_FONT_FAMILY)
                    .fontSizeStep(ReportSettingConstraints.DEFAULT_FONT_SIZE_STEP)
                    .lineSpacing(ReportSettingConstraints.DEFAULT_LINE_SPACING)
                    .autoFit(ReportSettingConstraints.DEFAULT_AUTO_FIT)
                    .build(), true);
            saveTimer.stop();
            saveSettings();
        }

        @Override
        public void onAutoFitChanged(boolean autoFit) {
            updateSetting(setting -> setting.toBuilder()
                    .autoFit(autoFit)
                    .build(), true);
        }
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
     * 미리보기에 보일 보고서를 바꾼다. 보고서가 없으면 인쇄와 PDF 저장, 서식 조정을 막는다.
     *
     * @param keepPage 보던 페이지를 그대로 보여줄지 여부. 서식만 바꿔 다시 배치한 경우에 쓴다.
     */
    private void setLoadedReport(LoadedReport report, boolean keepPage) {
        loadedReport = report;
        ReportDocument document = report != null ? report.document() : null;
        viewer.setDocument(document, keepPage);
        pdfButton.setEnabled(report != null);
        printButton.setEnabled(report != null);
        formatBar.update(report != null ? settings.get(report.type()) : null, document,
                report == null || report.fits());
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
