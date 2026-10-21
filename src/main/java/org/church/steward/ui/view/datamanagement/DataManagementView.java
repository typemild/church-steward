package org.church.steward.ui.view.datamanagement;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.util.UIColors;

/**
 * 데이터 관리 화면. 전체 데이터 백업/복원, 예산과 수입/지출의 가져오기/내보내기를 탭으로 묶는다.
 */
@UiView
public class DataManagementView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane tabbedPane;

    public DataManagementView(BackupRestorePanelView backupRestorePanelView,
            BudgetTransferPanelView budgetTransferPanelView,
            FinancialRecordTransferPanelView financialRecordTransferPanelView) {
        this.tabbedPane = new JTabbedPane();

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일): 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout());

        tabbedPane.setBackground(UIColors.BACKGROUND);
        tabbedPane.addTab("백업/복원", backupRestorePanelView);
        tabbedPane.addTab("예산", budgetTransferPanelView);
        tabbedPane.addTab("수입/지출", financialRecordTransferPanelView);
        tabbedPane.addChangeListener(_ -> activateSelectedTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public void onMenuSelected() {
        activateSelectedTab();
    }

    /**
     * 진행 중인 가져오기가 있는지 여부. 가져오기의 진행 상태는 저장되지 않으므로, 프로그램을 끝내기 전에 확인을 받는 데 쓴다.
     */
    public boolean isImportInProgress() {
        for (Component tab : tabbedPane.getComponents()) {
            if (tab instanceof AbstractTransferPanelView transferPanel && transferPanel.isImportInProgress()) {
                return true;
            }
        }
        return false;
    }

    private void activateSelectedTab() {
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof MenuActivatable menuActivatable) {
            menuActivatable.onMenuSelected();
        }
    }

}
