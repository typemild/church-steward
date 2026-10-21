package org.church.steward.ui.view.programinfo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.church.steward.domain.programinfo.controller.ProgramInfoController;
import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

@UiView
@Slf4j
public class ProgramInfoView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final ProgramInfoController programInfoController;

    private JTabbedPane tabbedPane;
    private ProgramInfoTab programInfoTab;
    private LicenseTab licenseTab;

    public ProgramInfoView(ProgramInfoController programInfoController) {
        this.programInfoController = programInfoController;

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일): 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UIColors.BACKGROUND);

        programInfoTab = new ProgramInfoTab();
        licenseTab = new LicenseTab();

        tabbedPane.addTab("프로그램 정보", programInfoTab);
        tabbedPane.addTab("오픈소스 라이선스", licenseTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public void onMenuSelected() {
        loadData();
    }

    private void loadData() {
        AsyncTask.run(this, () -> {
            ProgramInfoDto info = programInfoController.getProgramInfo();
            List<OpenSourceLicenseDto> licenses = programInfoController.getOpenSourceLicenses();
            return new Object[] { info, licenses };
        }, result -> {
            ProgramInfoDto info = (ProgramInfoDto) result[0];
            @SuppressWarnings("unchecked")
            List<OpenSourceLicenseDto> licenses = (List<OpenSourceLicenseDto>) result[1];

            programInfoTab.updateInfo(info);
            licenseTab.updateLicenses(licenses);
        }, err -> {
            log.error("Failed to load program information", err);
        });
    }

}
