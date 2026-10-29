package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.view.settings.contact.ContactManagementPanelView;
import org.church.steward.ui.view.settings.fiscalyear.FiscalYearManagementPanelView;

@UiView
public class SettingsView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");

    /** 회계 연도 관리 탭의 위치. 사이드바의 회계 연도 관리 버튼으로 바로 이동할 수 있도록 항상 첫 번째에 둔다. */
    private static final int FISCAL_YEAR_TAB_INDEX = 0;

    private final JTabbedPane tabbedPane;
    private final FiscalYearManagementPanelView fiscalYearManagementPanelView;
    private final ContactManagementPanelView contactManagementPanelView;
    private final PasswordChangePanelView passwordChangePanelView;
    private final UserManagementPanelView userManagementPanelView;
    private final UserSessionHolder userSessionHolder;
    private boolean tabsInitialized = false;

    public SettingsView(FiscalYearManagementPanelView fiscalYearManagementPanelView,
            ContactManagementPanelView contactManagementPanelView,
            PasswordChangePanelView passwordChangePanelView,
            UserManagementPanelView userManagementPanelView,
            UserSessionHolder userSessionHolder) {
        this.tabbedPane = new JTabbedPane();
        this.fiscalYearManagementPanelView = fiscalYearManagementPanelView;
        this.contactManagementPanelView = contactManagementPanelView;
        this.passwordChangePanelView = passwordChangePanelView;
        this.userManagementPanelView = userManagementPanelView;
        this.userSessionHolder = userSessionHolder;

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일) : 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout());

        tabbedPane.setBackground(COLOR_BACKGROUND);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Component selectedComponent = tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
                if (selectedComponent instanceof MenuActivatable menuActivatable) {
                    menuActivatable.onMenuSelected();
                }
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initializeTabs() {
        // 회계 연도 관리 탭은 FISCAL_YEAR_TAB_INDEX에 맞춰 항상 가장 먼저 추가한다.
        tabbedPane.addTab("회계 연도 관리", fiscalYearManagementPanelView);
        tabbedPane.addTab("관계자(봉헌자/수취인) 관리", contactManagementPanelView);
        tabbedPane.addTab("비밀번호 변경", passwordChangePanelView);
        if (userSessionHolder.getLoggedInUser().isAdmin()) {
            tabbedPane.addTab("사용자 관리", userManagementPanelView);
        }
        tabsInitialized = true;
    }

    @Override
    public void onMenuSelected() {
        if (!tabsInitialized) {
            initializeTabs();
        }
        Component selectedComponent = tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
        if (selectedComponent instanceof MenuActivatable menuActivatable) {
            menuActivatable.onMenuSelected();
        }
    }

    /**
     * 회계 연도 관리 탭을 보여준다. 설정 메뉴가 선택된 뒤({@link #onMenuSelected()})에 호출해야 한다. 다른 탭을 보고
     * 있었다면 탭이 바뀌면서 회계 연도 관리 화면이 데이터를 불러온다.
     */
    public void showFiscalYearManagementTab() {
        if (!tabsInitialized) {
            initializeTabs();
        }
        tabbedPane.setSelectedIndex(FISCAL_YEAR_TAB_INDEX);
    }

}
