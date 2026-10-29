package org.church.steward.ui.view.datamanagement;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.church.steward.domain.fiscalyear.controller.FiscalYearController;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.datamanagement.ExportOptionsDialog.ExportOptions;
import org.church.steward.ui.view.main.FiscalYearComboRenderer;
import org.church.steward.ui.view.main.FiscalYearSelector;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 관리의 "관계자", "예산", "수입/지출" 탭이 함께 쓰는 틀. 회계 연도 선택란과 내보내기(파일로 내보내기, 빈 양식
 * 내려받기)를 제공하고, 무엇을 내보낼지는 하위 클래스가 정한다.
 * <p>
 * 회계 연도는 사이드바의 선택과 독립이다. 처음 들어올 때의 초기값만 사이드바의 선택을 따르고, 그 뒤로는 이 탭에서 고른 회계 연도를
 * 유지한다. 관계자처럼 회계 연도에 속하지 않는 대상의 탭은 회계 연도 선택란을 두지 않으며, 이때 회계 연도를 받는 메서드에는 null이
 * 넘어간다.
 */
@Slf4j
public abstract class AbstractTransferPanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FILE_NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int FISCAL_YEAR_COMBO_WIDTH = 260;
    private static final String CARD_HOME = "HOME";
    private static final String CARD_IMPORT = "IMPORT";

    private final String targetName;
    private final String fileNamePrefix;
    /** 회계 연도에 속하는 대상인지 여부. false이면 회계 연도 선택란을 두지 않는다. */
    private final boolean usesFiscalYear;
    private final FiscalYearController fiscalYearController;
    private final FiscalYearSelector fiscalYearSelector;

    /**
     * 회계 연도 선택란. 폭만 고정하고 높이는 렌더러가 그리는 두 줄(이름, 기간)에 맞춘다. 높이까지 고정하면 목록을 불러오기 전(항목이 없을
     * 때)의 한 줄 높이로 굳어 글자가 잘린다.
     */
    private final StyledComboBox<FiscalYearDto> fiscalYearCombo = new StyledComboBox<>(ComponentSize.MEDIUM) {
        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(FISCAL_YEAR_COMBO_WIDTH, super.getPreferredSize().height);
        }
    };
    private final PrimaryButton exportButton;
    private final SecondaryButton templateButton;
    private final SecondaryButton importButton = new SecondaryButton("가져오기", ComponentSize.MEDIUM);

    private final CardLayout cardLayout = new CardLayout();
    /** 가져오기 화면. 처음 가져오기를 시작할 때 하위 클래스가 넘겨준다. */
    private JComponent importPanel;

    /** 이 탭에서 선택된 회계 연도의 ID. 아직 목록을 불러오지 않았으면 null이다. */
    private Long selectedFiscalYearId;
    /** 마지막으로 파일을 저장한 폴더. 처음에는 null이어서 파일 선택 창이 기본 폴더에서 열린다. */
    private File lastDirectory;

    /**
     * 회계 연도에 속하는 대상의 탭을 만든다.
     *
     * @param targetName        다루는 대상의 이름. 제목과 안내 문구에 쓴다 (예: "예산", "수입/지출")
     * @param fileNamePrefix    저장할 파일 이름의 앞부분. 파일 이름에 쓸 수 없는 글자를 넣지 않는다 (예: "수입지출")
     * @param exportDescription 내보내기 영역에 보여줄 설명
     * @param importDescription 가져오기 영역에 보여줄 설명
     */
    protected AbstractTransferPanelView(String targetName, String fileNamePrefix, String exportDescription,
            String importDescription, FiscalYearController fiscalYearController,
            FiscalYearSelector fiscalYearSelector) {
        this(targetName, fileNamePrefix, exportDescription, importDescription, true, fiscalYearController,
                fiscalYearSelector);
    }

    /**
     * 회계 연도에 속하지 않는 대상(관계자)의 탭을 만든다. 회계 연도 선택란이 없다.
     *
     * @param targetName        다루는 대상의 이름. 제목과 안내 문구에 쓴다 (예: "관계자")
     * @param fileNamePrefix    저장할 파일 이름의 앞부분. 파일 이름에 쓸 수 없는 글자를 넣지 않는다
     * @param exportDescription 내보내기 영역에 보여줄 설명
     * @param importDescription 가져오기 영역에 보여줄 설명
     */
    protected AbstractTransferPanelView(String targetName, String fileNamePrefix, String exportDescription,
            String importDescription) {
        this(targetName, fileNamePrefix, exportDescription, importDescription, false, null, null);
    }

    private AbstractTransferPanelView(String targetName, String fileNamePrefix, String exportDescription,
            String importDescription, boolean usesFiscalYear, FiscalYearController fiscalYearController,
            FiscalYearSelector fiscalYearSelector) {
        this.targetName = targetName;
        this.fileNamePrefix = fileNamePrefix;
        this.usesFiscalYear = usesFiscalYear;
        this.fiscalYearController = fiscalYearController;
        this.fiscalYearSelector = fiscalYearSelector;

        // 탭의 첫 화면과 가져오기 화면을 번갈아 보여준다.
        setLayout(cardLayout);
        setBackground(UIColors.BACKGROUND);

        JPanel homePanel = new JPanel(new BorderLayout(0, 25));
        homePanel.setBackground(UIColors.BACKGROUND);
        homePanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        TitleLabel titleLabel = new TitleLabel(targetName + " 가져오기/내보내기");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        homePanel.add(titleLabel, BorderLayout.NORTH);

        fiscalYearCombo.setRenderer(new FiscalYearComboRenderer());
        exportButton = new PrimaryButton("내보내기", ComponentSize.MEDIUM);
        templateButton = new SecondaryButton("양식 내려받기", ComponentSize.MEDIUM);
        // 회계 연도를 쓰는 탭은 회계 연도 목록을 불러온 뒤에 켠다.
        exportButton.setEnabled(!usesFiscalYear);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        JPanel fiscalYearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fiscalYearPanel.setOpaque(false);
        FieldLabel fiscalYearLabel = new FieldLabel("회계 연도", ComponentSize.MEDIUM, true);
        fiscalYearLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        fiscalYearPanel.add(fiscalYearLabel);
        fiscalYearPanel.add(fiscalYearCombo);

        JPanel exportButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exportButtonPanel.setOpaque(false);
        exportButtonPanel.add(exportButton);

        // 양식은 가져올 데이터를 정리할 때 쓰는 것이므로 가져오기 버튼 옆에 둔다.
        JPanel importButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        importButtonPanel.setOpaque(false);
        importButtonPanel.add(importButton);
        importButtonPanel.add(Box.createHorizontalStrut(12));
        importButtonPanel.add(templateButton);
        importButton.setEnabled(!usesFiscalYear);

        int row = 0;
        if (usesFiscalYear) {
            addRow(contentPanel, row++, fiscalYearPanel, 25);
        }
        addRow(contentPanel, row++, new SectionHeaderLabel("내보내기"), 8);
        addRow(contentPanel, row++, new DescriptionLabel(exportDescription), 15);
        addRow(contentPanel, row++, exportButtonPanel, 25);
        addRow(contentPanel, row++, new SectionHeaderLabel("가져오기"), 8);
        addRow(contentPanel, row++, new DescriptionLabel(importDescription), 15);
        addRow(contentPanel, row++, importButtonPanel, 0);

        // 남는 공간을 아래쪽에 두어 내용을 위로 붙인다.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        homePanel.add(contentPanel, BorderLayout.CENTER);
        add(homePanel, CARD_HOME);

        importButton.addActionListener(_ -> {
            FiscalYearDto fiscalYear = getSelectedFiscalYear();
            if (!usesFiscalYear || fiscalYear != null) {
                startImport(fiscalYear);
            }
        });
        fiscalYearCombo.addActionListener(_ -> onFiscalYearSelected());
        exportButton.addActionListener(_ -> startExport());
        templateButton.addActionListener(_ -> startTemplateExport());
    }

    /**
     * 내용 영역에 한 행을 왼쪽에 붙여 전체 폭으로 놓는다.
     */
    private void addRow(JPanel panel, int row, Component component, int bottomGap) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, bottomGap, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(component, gbc);
    }

    /**
     * 선택한 회계 연도의 데이터를 파일로 내보낸다. 백그라운드 스레드에서 호출된다.
     *
     * @param fiscalYearId 선택한 회계 연도의 ID. 회계 연도를 쓰지 않는 탭이면 null
     * @return 내보낸 건수
     */
    protected abstract int export(Long fiscalYearId, Path target, SpreadsheetFormat format, String password);

    /**
     * 머리글만 있는 빈 양식을 파일로 저장한다. 백그라운드 스레드에서 호출된다.
     */
    protected abstract void exportTemplate(Path target, SpreadsheetFormat format);

    /**
     * 선택한 회계 연도로 가져오기를 시작한다. {@link #showImportPanel}로 가져오기 화면을 띄운다.
     *
     * @param fiscalYear 선택한 회계 연도. 회계 연도를 쓰지 않는 탭이면 null
     */
    protected abstract void startImport(FiscalYearDto fiscalYear);

    /**
     * 이 탭에 들어올 때마다 호출된다. 회계 연도를 쓰는 탭은 회계 연도 목록을 다시 읽은 뒤에 호출되고, 쓰지 않는 탭은 빈 목록으로 바로
     * 호출된다. 가져오기를 진행하다가 다른 메뉴에 다녀온 경우, 하위 클래스가 여기서 현재 데이터로 다시 분석하게 한다.
     */
    protected abstract void onTabActivated(List<FiscalYearDto> fiscalYears);

    /**
     * 진행 중인 가져오기가 있는지 여부. 프로그램을 끝낼 때 확인을 받는 데 쓴다.
     */
    public abstract boolean isImportInProgress();

    /**
     * 탭의 첫 화면 대신 가져오기 화면을 보여준다.
     */
    protected void showImportPanel(JComponent panel) {
        if (importPanel != panel) {
            if (importPanel != null) {
                remove(importPanel);
            }
            importPanel = panel;
            add(panel, CARD_IMPORT);
        }
        cardLayout.show(this, CARD_IMPORT);
    }

    /**
     * 가져오기 화면을 닫고 탭의 첫 화면으로 돌아간다.
     */
    protected void showHomePanel() {
        cardLayout.show(this, CARD_HOME);
    }

    /**
     * 회계 연도 목록을 다시 읽는다. 다른 메뉴에서 회계 연도를 추가, 수정, 삭제했을 수 있기 때문이다.
     */
    @Override
    public void onMenuSelected() {
        if (!usesFiscalYear) {
            onTabActivated(List.of());
            return;
        }
        AsyncTask.run(fiscalYearCombo,
                fiscalYearController::listFiscalYears,
                this::applyFiscalYears,
                err -> {
                    log.error("Failed to load fiscal years", err);
                    JOptionPane.showMessageDialog(this, "회계 연도 목록을 불러오는데 실패했습니다.");
                });
    }

    /**
     * 이 탭에서 고른 회계 연도를 유지하되, 아직 고른 적이 없거나 그 회계 연도가 삭제되었으면 사이드바에서 선택된 회계 연도를, 그것도 없으면
     * 가장 최근 회계 연도(목록의 첫 번째)를 선택한다.
     */
    private void applyFiscalYears(List<FiscalYearDto> fiscalYears) {
        FiscalYearDto toSelect = find(fiscalYears, selectedFiscalYearId);
        if (toSelect == null) {
            toSelect = find(fiscalYears, fiscalYearSelector.getSelectedFiscalYearId());
        }
        if (toSelect == null && !fiscalYears.isEmpty()) {
            toSelect = fiscalYears.getFirst();
        }

        fiscalYearCombo.setModel(new DefaultComboBoxModel<>(fiscalYears.toArray(new FiscalYearDto[0])));
        fiscalYearCombo.setSelectedItem(toSelect);
        onFiscalYearSelected();
        onTabActivated(fiscalYears);
    }

    private FiscalYearDto find(List<FiscalYearDto> fiscalYears, Long fiscalYearId) {
        for (FiscalYearDto fiscalYear : fiscalYears) {
            if (fiscalYear.getId()
                    .equals(fiscalYearId)) {
                return fiscalYear;
            }
        }
        return null;
    }

    private void onFiscalYearSelected() {
        FiscalYearDto fiscalYear = getSelectedFiscalYear();
        selectedFiscalYearId = fiscalYear == null ? null : fiscalYear.getId();
        exportButton.setEnabled(fiscalYear != null);
        // 마감된 회계 연도는 내보내기만 할 수 있다.
        importButton.setEnabled(fiscalYear != null && !fiscalYear.isClosed());
        importButton.setToolTipText(fiscalYear != null && fiscalYear.isClosed() ? "마감된 회계 연도에는 가져올 수 없습니다."
                : null);
    }

    /**
     * 이 탭에서 선택된 회계 연도. 회계 연도가 하나도 없거나 회계 연도를 쓰지 않는 탭이면 null이다.
     */
    protected FiscalYearDto getSelectedFiscalYear() {
        return (FiscalYearDto) fiscalYearCombo.getSelectedItem();
    }

    private void startExport() {
        FiscalYearDto fiscalYear = getSelectedFiscalYear();
        if (usesFiscalYear && fiscalYear == null) {
            return;
        }

        String title = targetName + " 내보내기";
        Optional<ExportOptions> options = new ExportOptionsDialog(SwingUtilities.getWindowAncestor(this), title, true)
                .showDialog();
        if (options.isEmpty()) {
            return;
        }
        SpreadsheetFormat format = options.get().format();
        String password = options.get().password();

        String fiscalYearPart = fiscalYear == null ? "" : toFileName(fiscalYear.getName()) + "_";
        String defaultName = fileNamePrefix + "_" + fiscalYearPart + LocalDate.now().format(FILE_NAME_DATE_FORMAT);
        File file = chooseTargetFile(title, defaultName, format);
        if (file == null) {
            return;
        }

        Long fiscalYearId = fiscalYear == null ? null : fiscalYear.getId();
        AsyncTask.run(exportButton,
                () -> export(fiscalYearId, file.toPath(), format, password),
                count -> JOptionPane.showMessageDialog(this,
                        String.format("%s %,d건을 내보냈습니다.\n%s", targetName, count, file.getAbsolutePath())),
                err -> {
                    log.error("Failed to export", err);
                    JOptionPane.showMessageDialog(this, "내보내기에 실패했습니다: " + err.getMessage(), title,
                            JOptionPane.ERROR_MESSAGE);
                });
    }

    private void startTemplateExport() {
        String title = targetName + " 양식 내려받기";
        Optional<ExportOptions> options = new ExportOptionsDialog(SwingUtilities.getWindowAncestor(this), title,
                false).showDialog();
        if (options.isEmpty()) {
            return;
        }
        SpreadsheetFormat format = options.get().format();

        File file = chooseTargetFile(title, fileNamePrefix + "_양식", format);
        if (file == null) {
            return;
        }

        AsyncTask.run(templateButton,
                () -> {
                    exportTemplate(file.toPath(), format);
                    return null;
                },
                _ -> JOptionPane.showMessageDialog(this, "양식을 저장했습니다.\n" + file.getAbsolutePath()),
                err -> {
                    log.error("Failed to export template", err);
                    JOptionPane.showMessageDialog(this, "양식을 저장하지 못했습니다: " + err.getMessage(), title,
                            JOptionPane.ERROR_MESSAGE);
                });
    }

    /**
     * 저장할 파일을 고르게 한다.
     *
     * @param defaultName 미리 채워 둘 파일 이름 (확장자 제외)
     * @return 고른 파일. 취소했거나 기존 파일을 덮어쓰지 않기로 했으면 null
     */
    private File chooseTargetFile(String title, String defaultName, SpreadsheetFormat format) {
        String extension = "." + format.getExtension();

        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(format.getLabel() + " (*" + extension + ")",
                format.getExtension()));
        chooser.setSelectedFile(new File(lastDirectory, defaultName + extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName()
                .toLowerCase()
                .endsWith(extension)) {
            file = new File(file.getParentFile(), file.getName() + extension);
        }
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(this, "같은 이름의 파일이 이미 있습니다. 덮어쓸까요?\n" + file.getName(),
                    title, JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        lastDirectory = file.getParentFile();
        return file;
    }

    /**
     * 회계 연도 이름에서 파일 이름에 쓸 수 없는 글자를 밑줄로 바꾼다.
     */
    private static String toFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

}
