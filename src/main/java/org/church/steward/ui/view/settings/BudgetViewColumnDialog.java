package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.church.steward.domain.programsetting.dto.BudgetViewColumnsDto;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.panel.ColumnOrderPanel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

/**
 * 수입/지출 관리의 "예산별 보기" 칸 설정 다이얼로그. 수입 표와 지출 표의 칸 순서와 숨김, 같은 날짜의 내역끼리의 순서를 탭으로 나눠
 * 정한다. 프로그램 전체 설정이므로 모든 사용자의 화면에 함께 적용된다. 저장은 다이얼로그가 직접 실행하며, 실패하면 닫지 않고 오류를
 * 보여준다.
 */
@Slf4j
class BudgetViewColumnDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** 맨 위 안내 문구와 탭 사이의 간격 */
    private static final int NOTICE_GAP = 12;

    private final Consumer<BudgetViewColumnsDto> saveAction;
    private final Runnable onSaved;

    private final Map<BudgetViewTable, ColumnOrderPanel> columnPanels = new EnumMap<>(BudgetViewTable.class);

    private final PrimaryButton saveButton = new PrimaryButton("저장", ComponentSize.MEDIUM);
    private final SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);

    /**
     * @param columns      지금 저장된 칸 설정
     * @param initialTable 처음에 열어 둘 표의 탭
     * @param saveAction   칸 설정을 저장하는 동작. 백그라운드 스레드에서 실행되며, 저장할 수 없으면 사용자에게 보여줄 메시지를
     *                     담은 예외를 던져야 한다.
     * @param onSaved      저장에 성공한 뒤 실행할 동작
     */
    BudgetViewColumnDialog(Window owner, BudgetViewColumnsDto columns, BudgetViewTable initialTable,
            Consumer<BudgetViewColumnsDto> saveAction, Runnable onSaved) {
        super(owner, "예산별 보기 칸 설정", Dialog.ModalityType.APPLICATION_MODAL);
        this.saveAction = saveAction;
        this.onSaved = onSaved;

        setLayout(new BorderLayout());
        getContentPane().setBackground(UIColors.BACKGROUND);

        // 화면의 표는 한 행이 한 줄이므로 보고서와 달리 숨긴 칸의 값을 따로 적지 않는다.
        List<String> hints = List.of("숨긴 칸의 값은 수정이나 상세 보기에서 확인할 수 있습니다.");
        JTabbedPane tabbedPane = new JTabbedPane();
        for (BudgetViewTable table : BudgetViewTable.values()) {
            ColumnOrderPanel panel = new ColumnOrderPanel(table, columns.getTables()
                    .get(table), false,
                    columns.getRecordOrders()
                            .get(table),
                    hints);
            columnPanels.put(table, panel);
            tabbedPane.addTab(table.getLabel(), panel);
        }
        tabbedPane.setSelectedComponent(columnPanels.get(initialTable));

        // 안내 문구와 탭 사이를 띄워 탭 머리와 붙어 보이지 않게 한다(보고서 양식 설정과 같다).
        JPanel contentPanel = new JPanel(new BorderLayout(0, NOTICE_GAP));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        contentPanel.add(new SubFieldLabel("칸 설정은 모든 사용자의 수입/지출 관리 화면에 함께 적용됩니다.", ComponentSize.SMALL),
                BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        saveButton.addActionListener(_ -> onSave());
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        DialogKeyHandler.install(this)
                .setButtons(cancelButton, saveButton)
                .trackChanges(this::currentColumns);

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 화면에서 정한 칸 설정. 저장하지 않은 내용이 있는지 판단하는 데에도 쓴다.
     */
    private BudgetViewColumnsDto currentColumns() {
        Map<BudgetViewTable, List<ReportColumnDto>> tables = new EnumMap<>(BudgetViewTable.class);
        Map<BudgetViewTable, RecordOrder> recordOrders = new EnumMap<>(BudgetViewTable.class);
        for (Map.Entry<BudgetViewTable, ColumnOrderPanel> entry : columnPanels.entrySet()) {
            tables.put(entry.getKey(), entry.getValue()
                    .getColumns());
            recordOrders.put(entry.getKey(), entry.getValue()
                    .getRecordOrder());
        }
        return BudgetViewColumnsDto.builder()
                .tables(tables)
                .recordOrders(recordOrders)
                .build();
    }

    private void onSave() {
        final BudgetViewColumnsDto columns = currentColumns();
        // 저장하는 동안 저장 버튼을 잠가 두 번 저장되지 않게 한다.
        // 취소 버튼도 잠가 저장 중에는 Esc나 창의 닫기 버튼으로도 닫히지 않게 한다.
        cancelButton.setEnabled(false);
        AsyncTask.run(saveButton,
                () -> {
                    saveAction.accept(columns);
                    return null;
                },
                _ -> {
                    dispose();
                    onSaved.run();
                },
                err -> {
                    cancelButton.setEnabled(true);
                    log.error("Failed to save budget view columns: {}", err.getMessage());
                    JOptionPane.showMessageDialog(this, "저장 실패: " + err.getMessage());
                });
    }
}
