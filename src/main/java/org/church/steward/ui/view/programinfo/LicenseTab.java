package org.church.steward.ui.view.programinfo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableModel;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.panel.CardPanel;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

/**
 * 오픈소스 라이선스 탭. 위에 라이브러리 목록을, 아래에 고른 라이브러리의 라이선스 정보를 둥근 카드로 보여준다. 고른 라이브러리가
 * 없으면(선택을 풀면) 카드에 안내 문구를 보여준다. 카드는 항상 보이고 높이도 바뀌지 않으므로 목록 영역의 크기가 변하지 않는다.
 */
@Slf4j
public class LicenseTab extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 화면 가장자리의 여백. 프로그램 소개 탭과 같다. */
    private static final int EDGE_PADDING = 30;
    /** 목록과 정보 카드 사이의 간격 */
    private static final int SECTION_GAP = 15;
    /** 정보 카드의 안쪽 여백 */
    private static final int DETAIL_PADDING_VERTICAL = 10;
    private static final int DETAIL_PADDING_HORIZONTAL = 16;

    /** 정보 카드에서 번갈아 보여줄 화면의 이름 */
    private static final String PLACEHOLDER_CARD = "placeholder";
    private static final String DETAIL_CARD = "detail";

    private final DefaultTableModel tableModel;
    private final ListTable licenseTable;
    private final JEditorPane licenseDetailPane;
    /** 고른 라이브러리가 없을 때의 안내와 라이브러리 정보를 번갈아 보여준다. */
    private final CardLayout detailLayout = new CardLayout();
    private final JPanel licenseDetailCard;
    private List<OpenSourceLicenseDto> currentLicenses;

    public LicenseTab() {
        super(new BorderLayout(0, SECTION_GAP));
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(EDGE_PADDING, EDGE_PADDING, EDGE_PADDING, EDGE_PADDING));

        String[] columnNames = { "라이브러리", "버전", "라이선스" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        licenseTable = new ListTable(tableModel);
        licenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        licenseTable.getTableHeader()
                .setResizingAllowed(false);
        // 마우스와 키보드 어느 쪽으로 골라도, 선택을 풀어도 아래 정보를 맞춘다.
        licenseTable.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        showLicenseDetail();
                    }
                });
        add(new JScrollPane(licenseTable), BorderLayout.CENTER);

        licenseDetailPane = new JEditorPane();
        licenseDetailPane.setContentType("text/html");
        licenseDetailPane.setEditable(false);
        licenseDetailPane.setOpaque(false);
        licenseDetailPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop()
                            .browse(e.getURL()
                                    .toURI());
                } catch (Exception ex) {
                    log.error("Failed to open license link", ex);
                }
            }
        });
        SubFieldLabel placeholder = new SubFieldLabel("목록에서 라이브러리를 선택하면 라이선스 정보가 표시됩니다.",
                SwingConstants.CENTER, ComponentSize.MEDIUM);

        licenseDetailCard = new CardPanel(detailLayout);
        licenseDetailCard.setBorder(BorderFactory.createEmptyBorder(DETAIL_PADDING_VERTICAL,
                DETAIL_PADDING_HORIZONTAL, DETAIL_PADDING_VERTICAL, DETAIL_PADDING_HORIZONTAL));
        licenseDetailCard.add(placeholder, PLACEHOLDER_CARD);
        licenseDetailCard.add(licenseDetailPane, DETAIL_CARD);
        fixDetailHeight();
        add(licenseDetailCard, BorderLayout.SOUTH);
    }

    /**
     * 정보 카드의 높이를 정보가 가장 많을 때(링크 두 개가 모두 있을 때)에 맞춰 고정한다. 고른 라이브러리나 안내 문구에 따라 카드 높이가
     * 바뀌면 목록 영역이 늘었다 줄었다 하며 고른 행이 가려질 수 있기 때문이다.
     */
    private void fixDetailHeight() {
        licenseDetailPane.setText(toHtml("라이브러리", "1.0", "라이선스", "https://", "https://"));
        int contentHeight = licenseDetailPane.getPreferredSize().height;
        Insets insets = licenseDetailCard.getInsets();
        licenseDetailCard.setPreferredSize(new Dimension(0, contentHeight + insets.top + insets.bottom));
        licenseDetailPane.setText("");
    }

    public void updateLicenses(List<OpenSourceLicenseDto> licenses) {
        this.currentLicenses = licenses;
        tableModel.setRowCount(0);
        for (OpenSourceLicenseDto dto : licenses) {
            tableModel.addRow(new Object[] { dto.getLibraryName(), dto.getVersion(), dto.getLicenseType() });
        }
    }

    /**
     * 고른 라이브러리의 정보를 보여준다. 고른 라이브러리가 없으면 안내 문구를 보여준다. 카드의 높이는 바뀌지 않는다.
     */
    private void showLicenseDetail() {
        int row = licenseTable.getSelectedRow();
        if (row == -1 || currentLicenses == null) {
            detailLayout.show(licenseDetailCard, PLACEHOLDER_CARD);
            return;
        }
        OpenSourceLicenseDto selected = currentLicenses.get(licenseTable.convertRowIndexToModel(row));
        licenseDetailPane.setText(toHtml(selected.getLibraryName(), selected.getVersion(), selected.getLicenseType(),
                selected.getLicenseUrl(), selected.getProjectUrl()));
        licenseDetailPane.setCaretPosition(0);
        detailLayout.show(licenseDetailCard, DETAIL_CARD);
    }

    /**
     * 라이브러리 정보를 HTML로 만든다. 주소가 없는 링크는 빼고 만든다.
     */
    private static String toHtml(String libraryName, String version, String licenseType, String licenseUrl,
            String projectUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:SansSerif;'>");
        html.append("<h3>")
                .append(libraryName)
                .append(" (")
                .append(version)
                .append(")</h3>");
        html.append("<p><b>라이선스:</b> ")
                .append(licenseType)
                .append("</p>");

        if (licenseUrl != null && !licenseUrl.isEmpty()) {
            html.append("<p><a href='")
                    .append(licenseUrl)
                    .append("'>[라이선스 원문 보기]</a></p>");
        }

        if (projectUrl != null && !projectUrl.isEmpty()) {
            html.append("<p><a href='")
                    .append(projectUrl)
                    .append("'>[프로젝트 홈페이지 방문]</a></p>");
        }

        html.append("</body></html>");
        return html.toString();
    }
}
