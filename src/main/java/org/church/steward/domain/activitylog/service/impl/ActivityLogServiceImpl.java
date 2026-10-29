package org.church.steward.domain.activitylog.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.dto.ActivityLogSearchRequestDto;
import org.church.steward.domain.activitylog.entity.ActivityLog;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.church.steward.domain.activitylog.mapper.ActivityLogMapper;
import org.church.steward.domain.activitylog.repository.ActivityLogRepository;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.LoggableMetadataExtractor;
import org.church.steward.domain.activitylog.util.LoggableMetadataExtractor.LoggableFieldInfo;
import org.church.steward.infrastructure.security.LoggedInUser;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    /** 백업과 복원 기록의 대상명 */
    private static final String TARGET_ALL_DATA = "전체 데이터";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, ActivityLog::getCreatedAt);

    private final UserSessionHolder userSessionHolder;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogMapper activityLogMapper;

    private String getUserId() {
        LoggedInUser currentUser = userSessionHolder.getLoggedInUser();
        if (currentUser == null) {
            return "(비로그인)";
        }

        return currentUser.id();
    }

    private void log(ActivityType activityType, String targetName, List<ActivityLogFieldDto> fields) {
        ActivityLog activityLog = activityLogMapper.toEntity(getUserId(), activityType, targetName, fields);

        activityLogRepository.save(activityLog);
    }

    private ActivityLogFieldDto field(String name, String value) {
        return ActivityLogFieldDto.builder()
                .name(name)
                .value(value)
                .build();
    }

    private <T> ActivityLogFieldDto field(T entity, LoggableFieldInfo info) {
        return field(LoggableMetadataExtractor.getFieldName(entity, info),
                LoggableMetadataExtractor.getValue(entity, info));
    }

    @Override
    public <T> void logCreation(T entity) {
        log(ActivityType.CREATE, LoggableMetadataExtractor.getLoggableName(entity), describeCreation(entity));
    }

    @Override
    public <T> List<ActivityLogFieldDto> describeCreation(T entity) {
        return LoggableMetadataExtractor.getLoggableFields(entity)
                .stream()
                .map(info -> field(entity, info))
                .toList();
    }

    @Override
    public <T> void logUpdate(EntitySnapshot<T> oldEntitySnapshot, T newEntity) {
        logChange(ActivityType.UPDATE, oldEntitySnapshot, newEntity);
    }

    @Override
    public <T> void logClosing(EntitySnapshot<T> oldEntitySnapshot, T newEntity) {
        logChange(ActivityType.CLOSE, oldEntitySnapshot, newEntity);
    }

    @Override
    public <T> void logReopening(EntitySnapshot<T> oldEntitySnapshot, T newEntity) {
        logChange(ActivityType.REOPEN, oldEntitySnapshot, newEntity);
    }

    /**
     * 식별자와, 스냅샷을 찍은 뒤로 값이 바뀐 항목을 기록한다.
     */
    private <T> void logChange(ActivityType activityType, EntitySnapshot<T> oldEntitySnapshot, T newEntity) {
        log(activityType, LoggableMetadataExtractor.getLoggableName(newEntity),
                describeUpdate(oldEntitySnapshot, newEntity));
    }

    @Override
    public <T> List<ActivityLogFieldDto> describeUpdate(EntitySnapshot<T> oldEntitySnapshot, T newEntity) {
        List<LoggableFieldInfo> fieldInfos = LoggableMetadataExtractor.getLoggableFields(newEntity);

        List<ActivityLogFieldDto> fields = new ArrayList<>();
        for (LoggableFieldInfo info : fieldInfos) {
            // 스냅샷은 선언된 논리명으로 저장되어 있으므로, 조회는 선언된 논리명으로 하고 표시만 바꾼다.
            String fieldName = LoggableMetadataExtractor.getFieldName(newEntity, info);
            String oldValue = oldEntitySnapshot.getFieldValue(info.getName());
            String newValue = LoggableMetadataExtractor.getValue(newEntity, info);

            if (!Objects.equals(oldValue, newValue)) {
                // 값이 변경된 경우에는 변경 전 값도 함께 기록
                fields.add(ActivityLogFieldDto.builder()
                        .name(fieldName)
                        .oldValue(oldValue)
                        .value(newValue)
                        .changed(true)
                        .build());
            } else if (info.isIdentifier()) {
                // 식별자는 값의 변화가 없더라도 무조건 포함
                fields.add(field(fieldName, newValue));
            }
        }

        return fields;
    }

    @Override
    public <T> void logDeletion(T entity) {
        List<ActivityLogFieldDto> fields = LoggableMetadataExtractor.getLoggableFields(entity)
                .stream()
                .filter(LoggableFieldInfo::isIdentifier)
                .map(info -> field(entity, info))
                .toList();

        log(ActivityType.DELETE, LoggableMetadataExtractor.getLoggableName(entity), fields);
    }

    @Override
    public void logReportPrint(String reportType, String target, String outputMethod) {
        log(ActivityType.PRINT, reportType,
                List.of(field("유형", reportType), field("대상", target), field("출력 방식", outputMethod)));
    }

    @Override
    public void logBackup(String fileName) {
        log(ActivityType.BACKUP, TARGET_ALL_DATA, List.of(field("파일 이름", fileName)));
    }

    @Override
    public void logRestore(String userId, String fileName, LocalDateTime backupCreatedAt) {
        List<ActivityLogFieldDto> fields = List.of(field("파일 이름", fileName),
                field("백업 일시", backupCreatedAt.format(DATE_TIME_FORMAT)));
        activityLogRepository.save(activityLogMapper.toEntity(userId, ActivityType.RESTORE, TARGET_ALL_DATA, fields));
    }

    @Override
    public void logExport(String targetName, String fiscalYearName, String fileName, int count) {
        List<ActivityLogFieldDto> fields = fiscalYearFields(fiscalYearName);
        fields.add(field("파일 이름", fileName));
        fields.add(field("건수", String.format("%,d건", count)));
        log(ActivityType.EXPORT, targetName, fields);
    }

    @Override
    public void logImport(String targetName, String fiscalYearName, String fileName, String result,
            List<List<ActivityLogFieldDto>> items) {
        List<ActivityLogFieldDto> fields = fiscalYearFields(fiscalYearName);
        fields.add(field("파일 이름", fileName));
        fields.add(field("결과", result));
        activityLogRepository.save(activityLogMapper.toEntity(getUserId(), ActivityType.IMPORT, targetName, fields,
                items));
    }

    /**
     * 내보내기와 가져오기 기록의 첫 항목인 회계 연도. 관계자처럼 회계 연도에 속하지 않는 대상(null)이면 항목을 두지 않는다. 뒤에 항목을
     * 더 붙일 수 있도록 수정 가능한 목록을 돌려준다.
     */
    private List<ActivityLogFieldDto> fiscalYearFields(String fiscalYearName) {
        List<ActivityLogFieldDto> fields = new ArrayList<>();
        if (fiscalYearName != null) {
            fields.add(field("회계 연도", fiscalYearName));
        }
        return fields;
    }

    @Override
    @Transactional(readOnly = true)
    public long getLatestLogId() {
        return activityLogRepository.findFirstByOrderByIdDesc()
                .map(ActivityLog::getId)
                .orElse(0L);
    }

    @Override
    public void logLogin(String userId) {
        log(ActivityType.LOGIN, "사용자", List.of(field("사용자 ID", userId)));
    }

    @Override
    public Page<ActivityLogResponseDto> getLogs(ActivityLogSearchRequestDto request, int page, int size) {
        Specification<ActivityLog> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 기간 조회 (필수)
            predicates.add(cb.between(root.get("createdAt"), request.getStartDate(), request.getEndDate()));

            // 사용자 ID (선택)
            if (StringUtils.hasText(request.getUserId())) {
                predicates.add(cb.equal(root.get("userId"), request.getUserId()));
            }

            // 행위 유형 (선택)
            if (request.getActivityType() != null) {
                predicates.add(cb.equal(root.get("activityType"), request.getActivityType()));
            }

            // 대상 엔터티 논리명 (선택)
            if (StringUtils.hasText(request.getTargetName())) {
                predicates.add(cb.like(root.get("targetName"), "%" + request.getTargetName() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, LATEST_FIRST);

        return activityLogRepository.findAll(spec, pageable)
                .map(activityLogMapper::toResponseDto);
    }

}
