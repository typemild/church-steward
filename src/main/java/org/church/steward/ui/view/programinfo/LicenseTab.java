package org.church.steward.ui.view.programinfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableModel;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LicenseTab extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable licenseTable;
    private DefaultTableModel tableModel;
    private JEditorPane licenseDetailPane;
    private JScrollPane licenseDetailScrollPane;
    private List<OpenSourceLicenseDto> currentLicenses;

    public LicenseTab() {
        setLayout(new BorderLayout());
        setBackground(UIColors.BACKGROUND);

        // Tab 2: Open Source Licenses
        String[] columnNames = { "라이브러리", "버전", "라이선스" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        licenseTable = new JTable(tableModel);
        licenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        licenseTable.setRowHeight(35);
        licenseTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
        licenseTable.setBackground(Color.WHITE);
        licenseTable.setGridColor(UIColors.BORDER);
        licenseTable.getTableHeader().setResizingAllowed(false);

        licenseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showLicenseDetail();
            }
        });

        // 목록 표와 상세 내용에는 포커스 테두리를 두지 않는다(FlatLaf의 기본 테두리는 포커스를 받으면 색이 바뀐다).
        JScrollPane tableScrollPane = new JScrollPane(licenseTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        add(tableScrollPane, BorderLayout.CENTER);

        // License Detail Pane (Hidden initially)
        licenseDetailPane = new JEditorPane();
        licenseDetailPane.setContentType("text/html");
        licenseDetailPane.setEditable(false);
        licenseDetailPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        licenseDetailScrollPane = new JScrollPane(licenseDetailPane);
        licenseDetailScrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        licenseDetailScrollPane.setVisible(false);
        add(licenseDetailScrollPane, BorderLayout.SOUTH);
        licenseDetailScrollPane.setPreferredSize(new Dimension(0, 200));
    }

    public void updateLicenses(List<OpenSourceLicenseDto> licenses) {
        this.currentLicenses = licenses;
        tableModel.setRowCount(0);
        for (OpenSourceLicenseDto dto : licenses) {
            tableModel.addRow(new Object[] { dto.getLibraryName(), dto.getVersion(), dto.getLicenseType() });
        }
    }

    private void showLicenseDetail() {
        int row = licenseTable.getSelectedRow();
        if (row != -1 && currentLicenses != null) {
            OpenSourceLicenseDto selected = currentLicenses.get(row);

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family:SansSerif;'>");
            html.append("<h3>").append(selected.getLibraryName()).append(" (").append(selected.getVersion())
                    .append(")</h3>");
            html.append("<p><b>라이선스:</b> ").append(selected.getLicenseType()).append("</p>");

            if (selected.getLicenseUrl() != null && !selected.getLicenseUrl().isEmpty()) {
                html.append("<p><a href='").append(selected.getLicenseUrl()).append("'>[라이선스 원문 보기]</a></p>");
            }

            if (selected.getProjectUrl() != null && !selected.getProjectUrl().isEmpty()) {
                html.append("<p><a href='").append(selected.getProjectUrl()).append("'>[프로젝트 홈페이지 방문]</a></p>");
            }

            html.append("</body></html>");

            licenseDetailPane.setText(html.toString());
            licenseDetailScrollPane.setVisible(true);
            revalidate();
            repaint();
        }
    }
}
