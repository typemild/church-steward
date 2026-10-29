package org.church.steward.ui.view.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.activitylog.ActivityLogView;
import org.church.steward.ui.view.budget.BudgetManagementView;
import org.church.steward.ui.view.dashboard.DashboardView;
import org.church.steward.ui.view.datamanagement.DataManagementView;
import org.church.steward.ui.view.financialrecord.FinancialRecordView;
import org.church.steward.ui.view.programinfo.ProgramInfoView;
import org.church.steward.ui.view.report.ReportingView;
import org.church.steward.ui.view.settings.SettingsView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import lombok.extern.slf4j.Slf4j;

@UiView
@Slf4j
public class MainView extends JFrame {

    private static final long serialVersionUID = 1L;

    /** 회계 연도에 영향을 받는 화면. 사이드바에서 회계 연도 선택란 바로 아래에 묶어 표시한다. */
    private static final Set<String> FISCAL_YEAR_CARDS = Set.of("DASHBOARD", "INCOME_EXPENSE", "REPORTING",
            "BUDGET");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel sidebarPanel = new JPanel();
    private final Map<String, MenuActivatable> activatableViews = new HashMap<>();
    private final Map<String, JButton> menuButtons = new HashMap<>();

    private final UserSessionHolder userSessionHolder;
    private final FiscalYearSelector fiscalYearSelector;
    private final DashboardView dashboardView;
    private final FinancialRecordView incomeExpenseView;
    private final ReportingView reportingView;
    private final BudgetManagementView budgetManagementView;
    private final SettingsView settingsView;
    private final DataManagementView dataManagementView;
    private final ActivityLogView activityLogView;
    private final ProgramInfoView programInfoView;
    private JButton selectedButton = null; // 현재 선택된 버튼을 추적하기 위함
    private String selectedCardName = null; // 현재 표시 중인 화면의 카드 이름

    public MainView(UserSessionHolder userSessionHolder, FiscalYearSelector fiscalYearSelector,
            DashboardView dashboardView, FinancialRecordView incomeExpenseView, ReportingView reportingView,
            BudgetManagementView budgetManagementView, SettingsView settingsView,
            DataManagementView dataManagementView, ActivityLogView activityLogView,
            ProgramInfoView programInfoView) {
        this.userSessionHolder = userSessionHolder;
        this.fiscalYearSelector = fiscalYearSelector;
        this.dashboardView = dashboardView;
        this.incomeExpenseView = incomeExpenseView;
        this.reportingView = reportingView;
        this.budgetManagementView = budgetManagementView;
        this.settingsView = settingsView;
        this.dataManagementView = dataManagementView;
        this.activityLogView = activityLogView;
        this.programInfoView = programInfoView;

        setTitle("청지기 : 교회 재정 관리");
        AppIcons.apply(this);
        // 진행 중인 가져오기는 저장되지 않으므로, 창을 닫을 때 확인을 받은 뒤에 끝낸다.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (dataManagementView.isImportInProgress()) {
                    int answer = JOptionPane.showConfirmDialog(MainView.this,
                            "진행 중인 가져오기가 있습니다. 프로그램을 끝내면 가져오던 내용이 사라집니다.\n프로그램을 끝낼까요?", "청지기",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (answer != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                log.info("Exiting by user request");
                System.exit(0);
            }
        });
        Dimension frameSize = new Dimension(900, 600);
        setSize(frameSize);
        setMinimumSize(frameSize);
        setLocationRelativeTo(null);

        // 사이드바 설정: 너비를 180px로 고정
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        sidebarPanel.setBackground(UIColors.SIDEBAR);
        sidebarPanel.setPreferredSize(new Dimension(180, 0));

        // 메인 레이아웃 설정
        setLayout(new BorderLayout());
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void showMainView() {
        // 회계 연도 선택란과 회계 연도에 영향을 받는 메뉴
        sidebarPanel.add(fiscalYearSelector);
        JButton dashboardButton = createMenuButton("대시보드", "DASHBOARD", FontAwesomeSolid.CHART_LINE);
        createMenuButton("수입/지출 관리", "INCOME_EXPENSE", FontAwesomeSolid.WALLET);
        createMenuButton("보고서", "REPORTING", FontAwesomeSolid.FILE_ALT);
        createMenuButton("예산 관리", "BUDGET", FontAwesomeSolid.COINS);

        addMenuSeparator();

        boolean isAdmin = userSessionHolder.getLoggedInUser().isAdmin();

        // 회계 연도와 무관한 메뉴
        createMenuButton("설정", "SETTINGS", FontAwesomeSolid.COG);
        if (isAdmin) {
            createMenuButton("데이터 관리", "DATA_MANAGEMENT", FontAwesomeSolid.DATABASE);
        }
        createMenuButton("활동 기록", "ACTIVITY_LOG", FontAwesomeSolid.HISTORY);
        createMenuButton("프로그램 정보", "PROGRAM_INFO", FontAwesomeSolid.INFO_CIRCLE);

        // 남은 영역을 빈 공간으로 채우기 위해 Glue 추가
        sidebarPanel.add(Box.createVerticalGlue());

        // 컨텐츠 패널에 각 뷰 추가
        addActivatableView(dashboardView, "DASHBOARD");
        addActivatableView(incomeExpenseView, "INCOME_EXPENSE");
        addActivatableView(reportingView, "REPORTING");
        addActivatableView(budgetManagementView, "BUDGET");
        addActivatableView(settingsView, "SETTINGS");
        if (isAdmin) {
            addActivatableView(dataManagementView, "DATA_MANAGEMENT");
        }
        addActivatableView(activityLogView, "ACTIVITY_LOG");
        addActivatableView(programInfoView, "PROGRAM_INFO");

        // 첫 화면을 대시보드로 설정
        cardLayout.show(contentPanel, "DASHBOARD");
        selectedCardName = "DASHBOARD";

        // 대시보드 버튼을 선택된 상태로 표시
        if (dashboardButton != null) {
            dashboardButton.setBackground(UIColors.PRIMARY_BACKGROUND);
            dashboardButton.setForeground(UIColors.PRIMARY_TEXT);
            updateIconColor(dashboardButton, UIColors.PRIMARY_TEXT);
            selectedButton = dashboardButton;
        }

        // 회계 연도를 불러오면 변경 알림이 와서 첫 화면(대시보드)의 데이터를 조회한다.
        fiscalYearSelector.addChangeListener(this::onFiscalYearChanged);
        fiscalYearSelector.setManageAction(this::showFiscalYearManagement);
        fiscalYearSelector.reload();

        setVisible(true);
    }

    /**
     * 설정 메뉴의 회계 연도 관리 화면으로 이동한다. 사이드바의 회계 연도 관리 버튼을 눌렀을 때 실행된다.
     */
    private void showFiscalYearManagement() {
        selectMenu("SETTINGS");
        settingsView.showFiscalYearManagementTab();
    }

    /**
     * 회계 연도가 바뀌면 현재 화면이 회계 연도에 영향을 받는 화면일 때만 다시 조회한다. 보이지 않는 화면은 메뉴를 선택할 때 바뀐 회계
     * 연도로 조회한다.
     */
    private void onFiscalYearChanged() {
        if (selectedCardName == null || !FISCAL_YEAR_CARDS.contains(selectedCardName)) {
            return;
        }
        MenuActivatable activatable = activatableViews.get(selectedCardName);
        if (activatable != null) {
            activatable.onMenuSelected();
        }
    }

    private JButton createMenuButton(String text, String cardName, FontAwesomeSolid icon) {
        JButton button = new JButton(text, FontIcon.of(icon, Math.round(UIFonts.FONT_SIZE_BASE * 1.4f)));

        // 버튼 스타일링: 세련된 플랫 디자인 적용
        button.setFont(new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_BASE));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
        button.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 기본 상태 (비활성)
        button.setBackground(UIColors.SIDEBAR);
        button.setForeground(UIColors.TEXT_LIGHT);
        updateIconColor(button, UIColors.TEXT_LIGHT);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button != selectedButton) {
                    button.setBackground(UIColors.SIDEBAR_HOVER);
                    button.setForeground(UIColors.PRIMARY_TEXT);
                    updateIconColor(button, UIColors.PRIMARY_TEXT);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button != selectedButton) {
                    button.setBackground(UIColors.SIDEBAR);
                    button.setForeground(UIColors.TEXT_LIGHT);
                    updateIconColor(button, UIColors.TEXT_LIGHT);
                }
            }
        });

        button.addActionListener(_ -> selectMenu(cardName));
        menuButtons.put(cardName, button);

        sidebarPanel.add(button);
        // 버튼 사이의 간격 유지
        sidebarPanel.add(Box.createVerticalStrut(5));
        return button;
    }

    /**
     * 메뉴를 선택한다. 메뉴 버튼을 선택된 모양으로 바꾸고, 해당 화면을 보여준 뒤 화면에 선택되었음을 알린다.
     */
    private void selectMenu(String cardName) {
        JButton button = menuButtons.get(cardName);
        if (button == null) {
            return;
        }

        // 이전 선택된 버튼 스타일 초기화
        if (selectedButton != null) {
            resetButtonStyle(selectedButton);
        }

        // 현재 선택된 버튼 스타일 적용 (활성화 상태)
        button.setBackground(UIColors.PRIMARY_BACKGROUND);
        button.setForeground(UIColors.PRIMARY_TEXT);
        updateIconColor(button, UIColors.PRIMARY_TEXT);
        selectedButton = button;

        cardLayout.show(contentPanel, cardName);
        selectedCardName = cardName;

        MenuActivatable activatable = activatableViews.get(cardName);
        if (activatable != null) {
            activatable.onMenuSelected();
        }
    }

    /**
     * 회계 연도에 영향을 받는 메뉴와 나머지 메뉴를 나누는 구분선을 추가한다.
     */
    private void addMenuSeparator() {
        JPanel line = new JPanel();
        line.setBackground(UIColors.TEXT_SUB);
        line.setPreferredSize(new Dimension(0, 1));

        // 구분선이 사이드바 양끝에 닿지 않도록 좌우 여백을 둔다.
        JPanel separator = new JPanel(new BorderLayout());
        separator.setBackground(UIColors.SIDEBAR);
        separator.setBorder(BorderFactory.createEmptyBorder(8, 15, 13, 15));
        separator.add(line, BorderLayout.CENTER);
        separator.setAlignmentX(LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, separator.getPreferredSize().height));

        sidebarPanel.add(separator);
    }

    private void addActivatableView(JComponent view, String cardName) {
        contentPanel.add(view, cardName);
        if (view instanceof MenuActivatable activatable) {
            activatableViews.put(cardName, activatable);
        }
    }

    private void resetButtonStyle(JButton button) {
        button.setBackground(UIColors.SIDEBAR);
        button.setForeground(UIColors.TEXT_LIGHT);
        updateIconColor(button, UIColors.TEXT_LIGHT);
    }

    private void updateIconColor(JButton button, Color color) {
        if (button.getIcon() instanceof FontIcon fontIcon) {
            fontIcon.setIconColor(color);
        }
    }
}
