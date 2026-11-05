package org.church.steward.domain.report.service.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportApproval;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTableColumn;
import org.church.steward.domain.report.entity.ReportTableOption;
import org.church.steward.domain.report.entity.ReportTemplateConstraints;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.mapper.ReportTemplateMapper;
import org.church.steward.domain.report.repository.ReportApprovalRepository;
import org.church.steward.domain.report.repository.ReportTableColumnRepository;
import org.church.steward.domain.report.repository.ReportTableOptionRepository;
import org.church.steward.domain.report.service.ColumnOrderValidator;
import org.church.steward.domain.report.service.ReportTemplateService;
import org.church.steward.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 보고서 양식. 프로그램 전체 설정이며 모든 사용자가 같은 양식을 쓴다. 보고서의 모양을 정하는 설정이므로 활동 기록을 남기지
 * 않는다(프로그램 설정, 보고서 설정과 같다).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportTemplateServiceImpl implements ReportTemplateService {

    private final ReportApprovalRepository reportApprovalRepository;
    private final ReportTableColumnRepository reportTableColumnRepository;
    private final ReportTableOptionRepository reportTableOptionRepository;
    private final ReportTemplateMapper reportTemplateMapper;

    @Override
    @Transactional(readOnly = true)
    public ReportTemplateDto getReportTemplate(ReportType reportType) {
        ReportApprovalDto approval = null;
        if (reportType.hasApproval()) {
            ReportApproval entity = reportApprovalRepository.findById(reportType)
                    .orElseGet(() -> ReportApproval.defaults(reportType));
            approval = reportTemplateMapper.toApprovalDto(entity);
        }

        Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, Boolean> rateBarVisible = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, RecordOrder> recordOrders = new EnumMap<>(ReportTemplateTable.class);
        for (ReportTemplateTable table : ReportTemplateTable.of(reportType)) {
            tables.put(table, findColumns(table).stream()
                    .map(reportTemplateMapper::toColumnDto)
                    .toList());
            if (table.hasRateBarOption() || table.hasRecordOrderOption()) {
                ReportTableOption option = findOption(table);
                if (table.hasRateBarOption()) {
                    rateBarVisible.put(table, option.isRateBarVisible());
                }
                if (table.hasRecordOrderOption()) {
                    recordOrders.put(table, option.getRecordOrder());
                }
            }
        }

        return ReportTemplateDto.builder()
                .reportType(reportType)
                .approval(approval)
                .tables(tables)
                .rateBarVisible(rateBarVisible)
                .recordOrders(recordOrders)
                .build();
    }

    /**
     * 저장된 표의 칸. 저장한 적이 없거나, 저장된 칸이 지금 버전의 칸 구성이나 규칙과 맞지 않으면 기본 순서다.
     */
    private List<ReportTableColumn> findColumns(ReportTemplateTable table) {
        List<ReportTableColumn> saved = reportTableColumnRepository.findAllByTableTypeOrderBySortOrder(table);
        List<ReportColumnDto> columns = saved.stream()
                .map(reportTemplateMapper::toColumnDto)
                .toList();
        if (!saved.isEmpty() && findColumnError(table, columns) == null) {
            return saved;
        }

        List<ReportTableColumn> defaults = new ArrayList<>();
        for (ReportColumn column : table.getDefaultColumns()) {
            defaults.add(ReportTableColumn.builder()
                    .tableType(table)
                    .columnType(column)
                    .sortOrder(defaults.size())
                    .hidden(false)
                    .build());
        }
        return defaults;
    }

    @Override
    public void updateReportTemplate(ReportTemplateDto template) {
        if (template == null || template.getReportType() == null || template.getTables() == null) {
            throw new IllegalArgumentException("보고서 양식에 빠진 항목이 있습니다.");
        }
        ReportType reportType = template.getReportType();
        if (!reportType.hasTemplate()) {
            throw new IllegalArgumentException("양식을 정할 수 없는 보고서입니다.");
        }

        // 모두 검증한 뒤에 저장해서, 한 곳이라도 걸리면 아무것도 바꾸지 않는다.
        List<String> titles = reportType.hasApproval() ? validateApproval(template.getApproval()) : null;
        for (ReportTemplateTable table : ReportTemplateTable.of(reportType)) {
            String error = findColumnError(table, template.getTables()
                    .get(table));
            if (error != null) {
                throw new IllegalArgumentException(error);
            }
            if (table.hasRateBarOption() && (template.getRateBarVisible() == null || template.getRateBarVisible()
                    .get(table) == null)) {
                throw new IllegalArgumentException("보고서 양식에 빠진 항목이 있습니다.");
            }
            if (table.hasRecordOrderOption() && (template.getRecordOrders() == null || template.getRecordOrders()
                    .get(table) == null)) {
                throw new IllegalArgumentException("보고서 양식에 빠진 항목이 있습니다.");
            }
        }

        if (titles != null) {
            saveApproval(reportType, template.getApproval()
                    .isVisible(), titles);
        }
        for (ReportTemplateTable table : ReportTemplateTable.of(reportType)) {
            saveColumns(table, template.getTables()
                    .get(table));
            if (table.hasRateBarOption() || table.hasRecordOrderOption()) {
                saveOption(table, template);
            }
        }
    }

    /**
     * 결재란을 검증한다.
     *
     * @return 앞뒤 공백을 지우고 빈 직함을 null로 바꾼 직함
     */
    private static List<String> validateApproval(ReportApprovalDto approval) {
        if (approval == null || approval.getTitles() == null) {
            throw new IllegalArgumentException("보고서 양식에 빠진 항목이 있습니다.");
        }
        int cellCount = approval.getTitles()
                .size();
        if (cellCount < ReportTemplateConstraints.APPROVAL_CELL_COUNT_MIN
                || cellCount > ReportTemplateConstraints.APPROVAL_CELL_COUNT_MAX) {
            throw new IllegalArgumentException(String.format("결재란은 %d칸부터 %d칸까지 둘 수 있습니다.",
                    ReportTemplateConstraints.APPROVAL_CELL_COUNT_MIN,
                    ReportTemplateConstraints.APPROVAL_CELL_COUNT_MAX));
        }

        List<String> titles = new ArrayList<>();
        for (String title : approval.getTitles()) {
            String normalized = TextUtils.normalize(title);
            if (normalized != null && normalized.length() > ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH) {
                throw new IllegalArgumentException(String.format("결재란의 직함은 %d자 이내로 입력해 주세요.",
                        ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH));
            }
            titles.add(normalized);
        }
        return titles;
    }

    /**
     * 표의 칸이 규칙에 맞는지 확인한다({@link ColumnOrderValidator#findError}).
     *
     * @return 맞지 않으면 사용자에게 보일 이유, 맞으면 null
     */
    private static String findColumnError(ReportTemplateTable table, List<ReportColumnDto> columns) {
        if (ColumnOrderValidator.hasMissing(columns)) {
            return "보고서 양식에 빠진 항목이 있습니다.";
        }
        return ColumnOrderValidator.findError(table, columns);
    }

    private void saveApproval(ReportType reportType, boolean visible, List<String> titles) {
        ReportApproval approval = reportApprovalRepository.findById(reportType)
                .orElseGet(() -> ReportApproval.defaults(reportType));
        approval.update(visible, titles);
        reportApprovalRepository.save(approval);
    }

    /**
     * 저장된 표 단위 설정. 저장한 적이 없으면 기본 설정이다.
     */
    private ReportTableOption findOption(ReportTemplateTable table) {
        return reportTableOptionRepository.findById(table)
                .orElseGet(() -> ReportTableOption.defaults(table));
    }

    /**
     * 표 단위 설정을 저장한다. 그 표가 정할 수 없는 값은 저장된 값(처음이면 기본값)을 그대로 둔다.
     */
    private void saveOption(ReportTemplateTable table, ReportTemplateDto template) {
        ReportTableOption option = findOption(table);
        boolean rateBarVisible = table.hasRateBarOption() ? template.getRateBarVisible()
                .get(table) : option.isRateBarVisible();
        RecordOrder recordOrder = table.hasRecordOrderOption() ? template.getRecordOrders()
                .get(table) : option.getRecordOrder();
        option.update(rateBarVisible, recordOrder);
        reportTableOptionRepository.save(option);
    }

    /**
     * 표의 칸을 저장한다. 키(표와 칸)가 같은 행은 고쳐 쓰고, 그 표의 칸 구성에서 빠진 칸의 행은 지운다.
     */
    private void saveColumns(ReportTemplateTable table, List<ReportColumnDto> columns) {
        Map<ReportColumn, ReportTableColumn> saved = new EnumMap<>(ReportColumn.class);
        for (ReportTableColumn column : reportTableColumnRepository.findAllByTableTypeOrderBySortOrder(table)) {
            saved.put(column.getColumnType(), column);
        }

        List<ReportTableColumn> entities = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            ReportColumnDto column = columns.get(i);
            ReportTableColumn entity = saved.remove(column.getColumn());
            if (entity == null) {
                entity = ReportTableColumn.builder()
                        .tableType(table)
                        .columnType(column.getColumn())
                        .build();
            }
            entity.update(i, column.isHidden());
            entities.add(entity);
        }
        reportTableColumnRepository.deleteAll(saved.values());
        reportTableColumnRepository.saveAll(entities);
    }
}
