package org.church.steward.domain.programsetting.service.impl;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.church.steward.domain.programsetting.dto.BudgetViewColumnsDto;
import org.church.steward.domain.programsetting.entity.BudgetViewColumn;
import org.church.steward.domain.programsetting.entity.BudgetViewOption;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.programsetting.entity.ProgramSetting;
import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.church.steward.domain.programsetting.mapper.ProgramSettingMapper;
import org.church.steward.domain.programsetting.repository.BudgetViewColumnRepository;
import org.church.steward.domain.programsetting.repository.BudgetViewOptionRepository;
import org.church.steward.domain.programsetting.repository.ProgramSettingRepository;
import org.church.steward.domain.programsetting.service.ProgramSettingService;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.service.ColumnOrderValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 프로그램 전체 설정. 화면의 기준을 정하는 설정이므로 활동 기록을 남기지 않는다(보고서 설정과 같다).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgramSettingServiceImpl implements ProgramSettingService {

    private final ProgramSettingRepository programSettingRepository;
    private final BudgetViewColumnRepository budgetViewColumnRepository;
    private final BudgetViewOptionRepository budgetViewOptionRepository;
    private final ProgramSettingMapper programSettingMapper;

    @Override
    @Transactional(readOnly = true)
    public DayOfWeek getWeeklyClosingDay() {
        return findOrDefaults().getWeeklyClosingDay();
    }

    @Override
    public void updateWeeklyClosingDay(DayOfWeek weeklyClosingDay) {
        if (weeklyClosingDay == null) {
            throw new IllegalArgumentException("주간 결산 요일을 골라 주세요.");
        }

        ProgramSetting setting = findOrDefaults();
        setting.update(weeklyClosingDay);
        programSettingRepository.save(setting);
    }

    private ProgramSetting findOrDefaults() {
        return programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)
                .orElseGet(ProgramSetting::defaults);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetViewColumnsDto getBudgetViewColumns() {
        Map<BudgetViewTable, List<ReportColumnDto>> tables = new EnumMap<>(BudgetViewTable.class);
        Map<BudgetViewTable, RecordOrder> recordOrders = new EnumMap<>(BudgetViewTable.class);
        Map<BudgetViewTable, Boolean> dateVisible = new EnumMap<>(BudgetViewTable.class);
        for (BudgetViewTable table : BudgetViewTable.values()) {
            tables.put(table, findColumns(table));
            Optional<BudgetViewOption> option = budgetViewOptionRepository.findById(table);
            recordOrders.put(table, option.map(BudgetViewOption::getRecordOrder)
                    .orElse(ProgramSettingConstraints.DEFAULT_RECORD_ORDER));
            dateVisible.put(table, option.map(BudgetViewOption::isDateVisible)
                    .orElse(ProgramSettingConstraints.DEFAULT_DATE_VISIBLE));
        }
        return BudgetViewColumnsDto.builder()
                .tables(tables)
                .recordOrders(recordOrders)
                .dateVisible(dateVisible)
                .build();
    }

    /**
     * 저장된 표의 칸. 저장한 적이 없거나, 저장된 칸이 지금 버전의 칸 구성이나 규칙과 맞지 않으면 기본 순서다.
     */
    private List<ReportColumnDto> findColumns(BudgetViewTable table) {
        List<ReportColumnDto> saved = budgetViewColumnRepository.findAllByTableTypeOrderBySortOrder(table)
                .stream()
                .map(programSettingMapper::toColumnDto)
                .toList();
        if (!saved.isEmpty() && ColumnOrderValidator.findError(table, saved) == null) {
            return saved;
        }

        return table.getDefaultColumns()
                .stream()
                .map(column -> ReportColumnDto.builder()
                        .column(column)
                        .hidden(false)
                        .build())
                .toList();
    }

    @Override
    public void updateBudgetViewColumns(BudgetViewColumnsDto columns) {
        if (columns == null || columns.getTables() == null || columns.getRecordOrders() == null
                || columns.getDateVisible() == null) {
            throw new IllegalArgumentException("예산별 보기의 칸 설정에 빠진 항목이 있습니다.");
        }

        // 모두 검증한 뒤에 저장해서, 한 표라도 걸리면 아무것도 바꾸지 않는다.
        for (BudgetViewTable table : BudgetViewTable.values()) {
            List<ReportColumnDto> tableColumns = columns.getTables()
                    .get(table);
            if (ColumnOrderValidator.hasMissing(tableColumns) || columns.getRecordOrders()
                    .get(table) == null || columns.getDateVisible()
                            .get(table) == null) {
                throw new IllegalArgumentException("예산별 보기의 칸 설정에 빠진 항목이 있습니다.");
            }
            String error = ColumnOrderValidator.findError(table, tableColumns);
            if (error != null) {
                throw new IllegalArgumentException(error);
            }
        }

        for (BudgetViewTable table : BudgetViewTable.values()) {
            saveColumns(table, columns.getTables()
                    .get(table));
            saveOption(table, columns.getRecordOrders()
                    .get(table),
                    columns.getDateVisible()
                            .get(table));
        }
    }

    private void saveOption(BudgetViewTable table, RecordOrder recordOrder, boolean dateVisible) {
        BudgetViewOption option = budgetViewOptionRepository.findById(table)
                .orElseGet(() -> BudgetViewOption.builder()
                        .tableType(table)
                        .build());
        option.update(recordOrder, dateVisible);
        budgetViewOptionRepository.save(option);
    }

    /**
     * 표의 칸을 저장한다. 키(표와 칸)가 같은 행은 고쳐 쓰고, 그 표의 칸 구성에서 빠진 칸의 행은 지운다. 같은 키를 지우고 다시 넣으면
     * Hibernate가 삽입을 삭제보다 먼저 실행해 키가 겹친다.
     */
    private void saveColumns(BudgetViewTable table, List<ReportColumnDto> columns) {
        Map<ReportColumn, BudgetViewColumn> saved = new EnumMap<>(ReportColumn.class);
        for (BudgetViewColumn column : budgetViewColumnRepository.findAllByTableTypeOrderBySortOrder(table)) {
            saved.put(column.getColumnType(), column);
        }

        List<BudgetViewColumn> entities = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            ReportColumnDto column = columns.get(i);
            BudgetViewColumn entity = saved.remove(column.getColumn());
            if (entity == null) {
                entity = BudgetViewColumn.builder()
                        .tableType(table)
                        .columnType(column.getColumn())
                        .build();
            }
            entity.update(i, column.isHidden());
            entities.add(entity);
        }
        budgetViewColumnRepository.deleteAll(saved.values());
        budgetViewColumnRepository.saveAll(entities);
    }
}
