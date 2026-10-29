package org.church.steward.ui.view.main;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 선택된 회계 연도가 마감되어 조회만 할 수 있음을 알리는 안내. 예산 관리와 수입/지출 관리, 그 상세 보기 다이얼로그가 같은 모양과
 * 문구로 쓴다. 처음에는 숨겨 두고, 화면이 마감 여부에 따라 보이거나 숨긴다.
 */
public class ClosedFiscalYearNoticeLabel extends FieldLabel {

    private static final long serialVersionUID = 1L;

    public ClosedFiscalYearNoticeLabel() {
        super("마감된 회계 연도입니다. 조회만 할 수 있습니다.", FontAwesomeSolid.LOCK, SwingConstants.LEFT,
                ComponentSize.MEDIUM);
        setForeground(UIColors.ERROR_WARNING);
        if (getIcon() instanceof FontIcon fontIcon) {
            fontIcon.setIconColor(UIColors.ERROR_WARNING);
        }
        setVisible(false);
    }

    /**
     * 상세 보기 다이얼로그의 맨 위(BorderLayout.NORTH)에 놓을 안내를 만든다. 안내는 보이는 상태이며, 좌우 여백은 다이얼로그의
     * 입력 폼(25px)에 맞춘다. 안내와 첫 입력란 사이의 간격은 아래에 오는 폼의 여백이 만들므로, 다이얼로그는 안내가 차지한 높이만큼 폼의
     * 위아래 여백을 줄여 전체 높이를 수정 다이얼로그와 비슷하게 맞춘다.
     */
    public static JPanel createDialogNotice() {
        ClosedFiscalYearNoticeLabel label = new ClosedFiscalYearNoticeLabel();
        label.setVisible(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIColors.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 25, 0, 25));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
