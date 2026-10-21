package org.church.steward.ui.view.dashboard;

import java.awt.BorderLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.ui.component.label.SectionHeaderLabel;

/**
 * 예산 집행 현황 카드.
 * 지출 예산과 수입 예산 중 한쪽의 진행 현황을 보여주며, 제목 줄의 전환 버튼으로 바꾼다.
 * 마우스가 카드 위에 없는 동안에는 일정 시간마다 지출과 수입을 번갈아 보여준다.
 */
class BudgetProgressCard extends DashboardCard {

    private static final long serialVersionUID = 1L;

    private static final int HEADER_GAP = 10;
    /** 지출과 수입을 자동으로 전환하는 간격(밀리초) */
    private static final int AUTO_SWITCH_INTERVAL = 10_000;

    private final BudgetProgressList list = new BudgetProgressList();
    private final BudgetTypeToggle toggle = new BudgetTypeToggle(this::onTypeSelectedByUser);
    private final Timer autoSwitchTimer = new Timer(AUTO_SWITCH_INTERVAL, _ -> switchTypeAutomatically());

    BudgetProgressCard() {
        super(new BorderLayout(0, HEADER_GAP));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(new SectionHeaderLabel("예산 집행 현황"), BorderLayout.WEST);
        header.add(toggle, BorderLayout.EAST);

        // 예산이 많으면 카드 안에서 세로로 스크롤한다.
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport()
                .setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        list.setType(toggle.getSelectedType());
    }

    void setData(List<BudgetProgressDto> progressList) {
        list.setData(progressList);
        scrollToTop();
        // 새로 조회한 내용을 전환 간격만큼은 그대로 보여준다.
        autoSwitchTimer.restart();
    }

    /**
     * 화면에 붙어 있는 동안에만 자동 전환 타이머를 돌린다.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        autoSwitchTimer.start();
    }

    @Override
    public void removeNotify() {
        autoSwitchTimer.stop();
        super.removeNotify();
    }

    private void onTypeSelectedByUser(BudgetType type) {
        showType(type);
        // 직접 고른 직후에 곧바로 자동 전환되지 않게 한다.
        autoSwitchTimer.restart();
    }

    /**
     * 다른 구분으로 넘어간다. 카드가 보이지 않거나, 마우스가 카드 위에 있거나, 넘어갈 구분에 예산이 없으면 그대로 둔다.
     */
    private void switchTypeAutomatically() {
        BudgetType nextType = toggle.getSelectedType() == BudgetType.EXPENSE ? BudgetType.INCOME : BudgetType.EXPENSE;
        if (!isShowing() || isPointerInside() || !list.hasItems(nextType)) {
            return;
        }
        toggle.setSelectedType(nextType);
        showType(nextType);
    }

    private void showType(BudgetType type) {
        list.setType(type);
        scrollToTop();
    }

    private void scrollToTop() {
        list.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }

    /**
     * 마우스 포인터가 카드 위에 있는지 여부. 카드 안의 어느 컴포넌트 위에 있든 같은 결과를 내도록 화면 좌표로 판단한다.
     */
    private boolean isPointerInside() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return false;
        }
        Point location = pointerInfo.getLocation();
        SwingUtilities.convertPointFromScreen(location, this);
        return contains(location);
    }
}
