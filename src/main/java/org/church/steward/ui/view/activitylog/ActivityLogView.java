package org.church.steward.ui.view.activitylog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.church.steward.domain.activitylog.controller.ActivityLogController;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

@UiView
public class ActivityLogView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final ActivityLogController activityLogController;

    // 1. Filter Components
    private final StyledDatePicker startDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);
    private final StyledDatePicker endDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);
    private final StyledComboBox<ActivityType> allActivityTypeCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final JTextField userIdField = new StyledTextField(10, ComponentSize.MEDIUM);
    private final JTextField targetNameField = new StyledTextField(10, ComponentSize.MEDIUM);
    private final PrimaryButton searchButton = new PrimaryButton("조회", ComponentSize.MEDIUM);

    // 2. Table Components
    private final ActivityLogTableComponent logTableComponent = new ActivityLogTableComponent(this);
    private final JPanel tableContainer = new JPanel(new BorderLayout());
    private final JPanel tableHeaderPanel = new JPanel(new BorderLayout());
    private final SubFieldLabel totalCountLabel = new SubFieldLabel("총 : 0건", ComponentSize.SMALL);

    // 3. Pagination Components
    private final StyledComboBox<Integer> paginationPageCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final IconButton leftIconBtn = new IconButton(FontAwesomeSolid.ANGLE_LEFT, ComponentSize.MEDIUM);
    private final IconButton rightIconBtn = new IconButton(FontAwesomeSolid.ANGLE_RIGHT, ComponentSize.MEDIUM);
    private final SubFieldLabel totalPagesLabel = new SubFieldLabel("", ComponentSize.MEDIUM);
    private final JPanel paginationControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    private final StyledComboBox<Integer> pageSizeCombo = new StyledComboBox<>(
            new Integer[] { 10, 20, 50, 100 },
            ComponentSize.SMALL);

    // State
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private boolean isUpdatingPagination = false;

    ActivityLogView(ActivityLogController activityLogController) {
        this.activityLogController = activityLogController;

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일) : 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setBorder(BorderFactory.createEmptyBorder(30, 25, 25, 30));

        setLayout(new BorderLayout(10, 10));

        initComponents();
        layoutComponents();
        setupEvents();
    }

    private void initComponents() {
        // 1. Filter Components Initialization
        allActivityTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ActivityType type) {
                    setText(type.getLabel());
                } else if (value == null) {
                    setText("전체");
                }
                return this;
            }
        });
        // 첫 항목 null은 "전체"
        final DefaultComboBoxModel<ActivityType> activityTypeModel = new DefaultComboBoxModel<>();
        activityTypeModel.addElement(null);
        activityTypeModel.addAll(Arrays.asList(ActivityType.values()));
        allActivityTypeCombo.setModel(activityTypeModel);

        // 2. Table Components Initialization
        tableHeaderPanel.setBackground(UIColors.BACKGROUND);
        tableContainer.add(tableHeaderPanel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);

        JPanel rightInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        rightInfoPanel.setOpaque(false);
        JLabel pageSizeLabel = new SubFieldLabel("표시 개수 :  ", ComponentSize.SMALL);
        rightInfoPanel.add(pageSizeLabel);
        rightInfoPanel.add(pageSizeCombo);

        pageSizeCombo.setSelectedItem(10);

        infoPanel.add(totalCountLabel, BorderLayout.WEST);
        infoPanel.add(rightInfoPanel, BorderLayout.EAST);

        tableHeaderPanel.setBackground(UIColors.BACKGROUND);
        tableHeaderPanel.add(infoPanel, BorderLayout.NORTH);

        tableContainer.add(tableHeaderPanel, BorderLayout.NORTH);
        tableContainer.add(logTableComponent, BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        // 3. Pagination Components Initialization
        paginationControlPanel.setOpaque(false);

        JPanel navGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        navGroup.setOpaque(false);
        navGroup.add(leftIconBtn);
        navGroup.add(paginationPageCombo);
        navGroup.add(totalPagesLabel);
        navGroup.add(rightIconBtn);

        JPanel paginationPanel = new JPanel(new GridBagLayout());
        paginationPanel.setOpaque(false);
        paginationPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));
        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.gridx = 0;
        gbcCenter.gridy = 0;
        gbcCenter.anchor = GridBagConstraints.CENTER;
        paginationPanel.add(navGroup, gbcCenter);

        add(paginationPanel, BorderLayout.SOUTH);
    }

    private void layoutComponents() {
        // 전체 배경색 설정
        setBackground(UIColors.BACKGROUND);

        // 1. 상단 필터 영역
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);

        // 날짜 기간 선택을 위한 패널 생성
        JPanel dateRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dateRangePanel.setOpaque(false);

        // 시작 일자 피커
        dateRangePanel.add(startDatePicker);
        startDatePicker.setDate(LocalDate.now().minusWeeks(1));

        // "~" 구분자
        FieldLabel separator = new FieldLabel("  ~  ", ComponentSize.MEDIUM);
        dateRangePanel.add(separator);

        // 종료 일자 피커
        dateRangePanel.add(endDatePicker);
        endDatePicker.setDate(LocalDate.now());

        // 검색 조건
        addFilterItem(filterPanel, 0, FontAwesomeSolid.LIST_ALT, "활동 유형", allActivityTypeCombo, 0);
        addFilterItem(filterPanel, 0, FontAwesomeSolid.CALENDAR_ALT, "조회 기간", dateRangePanel, 2);
        addFilterItem(filterPanel, 1, FontAwesomeSolid.DATABASE, "정보 유형", targetNameField, 0);
        addFilterItem(filterPanel, 1, FontAwesomeSolid.USER, "사용자 ID", userIdField, 2);

        // 조회 버튼
        GridBagConstraints btnGbc = new GridBagConstraints();
        btnGbc.weightx = 0.0;
        btnGbc.weighty = 0.0;
        btnGbc.insets = new Insets(5, 0, 5, 0);
        btnGbc.gridx = 4;
        btnGbc.gridy = 0;
        btnGbc.gridheight = 2;
        btnGbc.anchor = GridBagConstraints.CENTER;
        btnGbc.fill = GridBagConstraints.BOTH;
        searchButton.setPreferredSize(new Dimension(70, 0));
        searchButton.setMinimumSize(new Dimension(70, 0));
        filterPanel.add(searchButton, btnGbc);

        add(filterPanel, BorderLayout.NORTH);
    }

    private void addFilterItem(JPanel panel, int y, FontAwesomeSolid icon, String labelText, Component comp, int x) {
        GridBagConstraints gbc;

        // 레이블 추가
        FieldLabel label = new FieldLabel(labelText, icon, SwingConstants.LEFT, ComponentSize.MEDIUM);

        // 레이블 패널 배치
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 7);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0.0;
        panel.add(label, gbc);

        // 입력 컴포넌트 배치
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 20);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = x + 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(comp, gbc);
    }

    private void setupEvents() {
        searchButton.addActionListener(_ -> loadData(0));

        leftIconBtn.addActionListener(_ -> {
            if (currentPage > 0) {
                loadData(currentPage - 1);
            }
        });
        rightIconBtn.addActionListener(_ -> {
            if (currentPage < totalPages - 1) {
                loadData(currentPage + 1);
            }
        });

        pageSizeCombo.addActionListener(_ -> {
            pageSize = (Integer) pageSizeCombo.getSelectedItem();
            loadData(0);
        });

        paginationPageCombo.addActionListener(_ -> {
            if (isUpdatingPagination) {
                return;
            }
            Integer selectedPage = (Integer) paginationPageCombo.getSelectedItem();
            if (selectedPage != null) {
                loadData(selectedPage - 1);
            }
        });
    }

    private void loadData(int page) {
        this.currentPage = page;

        // 람다식 내부에서 사용하기 위해 final 변수로 선언
        final LocalDateTime start;
        if (startDatePicker.getDate() != null) {
            start = startDatePicker.getDate().atStartOfDay();
        } else {
            start = null;
        }

        final LocalDateTime end;
        if (endDatePicker.getDate() != null) {
            end = endDatePicker.getDate().atTime(23, 59, 59);
        } else {
            end = null;
        }

        final String userId = userIdField.getText().isEmpty() ? null : userIdField.getText();
        final ActivityType type = (ActivityType) allActivityTypeCombo.getSelectedItem();
        final String target = targetNameField.getText().isEmpty() ? null : targetNameField.getText();

        AsyncTask.run(this,
                () -> {
                    return activityLogController.getLogs(start, end, userId, type, target, page, pageSize);
                },
                pageResult -> {
                    logTableComponent.getTableModel().setData(pageResult.getContent());
                    this.totalPages = pageResult.getTotalPages();

                    // totalCountLabel 갱신
                    totalCountLabel.setText("총 : " + pageResult.getTotalElements() + "건");

                    // 기존 pageInfoLabel 대신 paginationPageCombo와 totalPagesLabel 업데이트
                    updatePaginationUI(page, totalPages);
                },
                error -> {
                    JOptionPane.showMessageDialog(this, "데이터를 불러오는 중 오류가 발생했습니다: "
                            + error.getMessage());
                });
    }

    public void showDetail(ActivityLogResponseDto dto) {
        new ActivityLogDetailDialog(this, dto).setVisible(true);
    }

    private void updatePaginationUI(int currentPage, int totalPages) {
        isUpdatingPagination = true;
        try {
            // 콤보박스 모델 업데이트
            DefaultComboBoxModel<Integer> model = new DefaultComboBoxModel<>();
            for (int i = 1; i <= totalPages; i++) {
                model.addElement(i);
            }
            paginationPageCombo.setModel(model);

            // 현재 페이지 선택 시, 이미 동일한 페이지가 선택되어 있는 경우 액션 리스너가 트리거되지 않도록 체크
            int targetIndex = Math.min(currentPage, totalPages - 1);
            if (paginationPageCombo.getSelectedIndex() != targetIndex) {
                paginationPageCombo.setSelectedIndex(targetIndex);
            }
        } finally {
            isUpdatingPagination = false;
        }

        // 전체 페이지 표시
        totalPagesLabel.setText("/ " + totalPages);
    }

    @Override
    public void onMenuSelected() {
        loadData(0);
    }
}
