package org.church.steward.ui.view.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.fiscalyear.controller.FiscalYearController;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import lombok.extern.slf4j.Slf4j;

/**
 * 사이드바 상단의 회계 연도 선택란. 회계 연도에 영향을 받는 화면(대시보드, 수입/지출 관리, 보고서, 예산 관리)이 함께 쓰는 회계
 * 연도를 보관한다. 선택한 회계 연도는 사용자 설정에 저장되어 다음 실행 때 초기값으로 쓰인다. 레이블 오른쪽의 버튼은 설정의 회계 연도
 * 관리 화면으로 이동한다.
 */
@UiView
@Slf4j
public class FiscalYearSelector extends JPanel {

    private static final long serialVersionUID = 1L;

    private final FiscalYearController fiscalYearController;

    private final StyledComboBox<FiscalYearDto> fiscalYearCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final List<Runnable> changeListeners = new ArrayList<>();

    /** 회계 연도 관리 버튼을 눌렀을 때 실행할 동작 */
    private Runnable manageAction = () -> {
    };
    /** 현재 선택된 회계 연도의 ID. 아직 불러오지 않았으면 null이다. */
    private Long selectedFiscalYearId;
    /** 마지막으로 불러온 회계 연도 목록. 회계 연도의 이름, 기간, 마감 여부를 알려줄 때 쓴다. */
    private List<FiscalYearDto> fiscalYears = List.of();
    private boolean updatingFiscalYears = false;

    /**
     * 회계 연도 목록과 사용자 설정에 저장된 회계 연도의 ID.
     */
    private record LoadResult(List<FiscalYearDto> fiscalYears, Long savedFiscalYearId) {
    }

    public FiscalYearSelector(FiscalYearController fiscalYearController) {
        this.fiscalYearController = fiscalYearController;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIColors.SIDEBAR);
        // 기간까지 두 줄로 보여주려면 폭이 빠듯하므로 좌우 여백을 메뉴 버튼(15px)보다 조금 줄인다.
        setBorder(BorderFactory.createEmptyBorder(5, 12, 15, 12));
        setAlignmentX(LEFT_ALIGNMENT);

        FieldLabel label = new FieldLabel("회계 연도", FontAwesomeSolid.CALENDAR, SwingConstants.LEFT,
                ComponentSize.MEDIUM, true);
        label.setForeground(UIColors.TEXT_LIGHT);
        if (label.getIcon() instanceof FontIcon fontIcon) {
            fontIcon.setIconColor(UIColors.TEXT_LIGHT);
        }

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(label, BorderLayout.CENTER);
        headerPanel.add(createManageButton(), BorderLayout.EAST);

        fiscalYearCombo.setRenderer(new FiscalYearComboRenderer());

        add(headerPanel, BorderLayout.NORTH);
        add(fiscalYearCombo, BorderLayout.CENTER);

        fiscalYearCombo.addActionListener(_ -> onFiscalYearSelected());
    }

    /**
     * 회계 연도 관리 화면으로 이동하는 버튼을 만든다. 공용 IconButton은 밝은 배경에 맞춰져 있으므로, 사이드바에 맞게 아이콘 색과
     * 마우스를 올렸을 때의 배경색을 바꾼다.
     */
    private IconButton createManageButton() {
        IconButton button = new IconButton(FontAwesomeSolid.COG, ComponentSize.SMALL);
        button.setIcon(FontIcon.of(FontAwesomeSolid.COG, UIFonts.FONT_SIZE_SMALL, UIColors.TEXT_LIGHT));
        button.setToolTipText("회계 연도 관리");
        // IconButton이 등록한 리스너 다음에 실행되어 배경색을 덮어쓴다.
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(UIColors.SIDEBAR_HOVER);
            }
        });
        button.addActionListener(_ -> manageAction.run());
        return button;
    }

    /**
     * 사이드바(BoxLayout)에서 세로로 늘어나지 않도록 높이를 선호 높이로 제한한다.
     */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /**
     * 현재 선택된 회계 연도의 ID를 반환한다. 아직 불러오지 않았으면 null을 반환한다.
     */
    public Long getSelectedFiscalYearId() {
        return selectedFiscalYearId;
    }

    /**
     * 현재 선택된 회계 연도의 이름, 기간, 마감 여부 등 상세 정보를 반환한다. 아직 불러오지 않았으면 null을 반환한다.
     */
    public FiscalYearDto getSelectedFiscalYear() {
        return getFiscalYear(selectedFiscalYearId);
    }

    /**
     * 회계 연도의 이름, 기간, 마감 여부 등 상세 정보를 반환한다. 목록에 없는 회계 연도이면 null을 반환한다.
     */
    public FiscalYearDto getFiscalYear(Long fiscalYearId) {
        for (FiscalYearDto fiscalYear : fiscalYears) {
            if (fiscalYear.getId()
                    .equals(fiscalYearId)) {
                return fiscalYear;
            }
        }
        return null;
    }

    /**
     * 선택된 회계 연도가 바뀌었을 때 실행할 동작을 등록한다. 목록을 다시 불러온 결과로 바뀐 경우에도 실행된다. 선택된 회계 연도는 그대로이고
     * 그 이름, 기간, 마감 여부만 바뀐 경우에는 실행되지 않으므로, 각 화면은 {@code onMenuSelected()}에서
     * {@link #getSelectedFiscalYear()}를 다시 읽어야 한다.
     */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    /**
     * 회계 연도 관리 버튼을 눌렀을 때 실행할 동작을 지정한다.
     */
    public void setManageAction(Runnable manageAction) {
        this.manageAction = manageAction;
    }

    /**
     * 회계 연도 목록을 다시 불러온다. 회계 연도를 추가, 수정, 삭제하거나 마감 또는 마감 취소한 뒤 호출해야 한다. 기존 선택을 유지하되,
     * 아직 선택한 적이 없거나 선택된 회계 연도가 삭제되었으면 사용자 설정에 저장된 회계 연도를 선택한다.
     */
    public void reload() {
        AsyncTask.run(fiscalYearCombo,
                () -> new LoadResult(fiscalYearController.listFiscalYears(),
                        fiscalYearController.getSelectedFiscalYearId()),
                this::applyLoadResult,
                err -> {
                    log.error("Failed to load fiscal years", err);
                    JOptionPane.showMessageDialog(this, "회계 연도 목록을 불러오는데 실패했습니다.");
                });
    }

    private void applyLoadResult(LoadResult result) {
        fiscalYears = result.fiscalYears();

        Long previous = selectedFiscalYearId;
        Long toSelect = previous != null && getFiscalYear(previous) != null ? previous
                : result.savedFiscalYearId();

        updatingFiscalYears = true;
        try {
            fiscalYearCombo.setModel(new DefaultComboBoxModel<>(fiscalYears.toArray(new FiscalYearDto[0])));
            fiscalYearCombo.setSelectedItem(getFiscalYear(toSelect));
        } finally {
            updatingFiscalYears = false;
        }

        selectedFiscalYearId = toSelect;
        updateToolTip();
        if (!Objects.equals(toSelect, previous)) {
            notifyChanged();
        }
    }

    private void onFiscalYearSelected() {
        if (updatingFiscalYears) {
            return;
        }
        FiscalYearDto fiscalYear = (FiscalYearDto) fiscalYearCombo.getSelectedItem();
        if (fiscalYear == null || fiscalYear.getId()
                .equals(selectedFiscalYearId)) {
            return;
        }
        Long fiscalYearId = fiscalYear.getId();
        selectedFiscalYearId = fiscalYearId;
        updateToolTip();
        notifyChanged();

        AsyncTask.run(fiscalYearCombo,
                () -> {
                    fiscalYearController.updateSelectedFiscalYearId(fiscalYearId);
                    return null;
                },
                _ -> {
                },
                err -> {
                    log.error("Failed to save selected fiscal year", err);
                    JOptionPane.showMessageDialog(this, "선택한 회계 연도를 저장하지 못했습니다.");
                });
    }

    /**
     * 선택란이 좁아 이름이나 기간이 잘릴 수 있으므로, 선택된 회계 연도의 전체 정보를 툴팁으로 보여준다.
     */
    private void updateToolTip() {
        FiscalYearDto fiscalYear = getSelectedFiscalYear();
        if (fiscalYear == null) {
            fiscalYearCombo.setToolTipText(null);
            return;
        }
        String toolTip = String.format("회계 연도: %s (%s ~ %s)", fiscalYear.getName(), fiscalYear.getStartDate(),
                fiscalYear.getEndDate());
        if (fiscalYear.isClosed()) {
            toolTip += " - 마감";
        }
        fiscalYearCombo.setToolTipText(toolTip);
    }

    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }
}
