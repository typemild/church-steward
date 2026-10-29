package org.church.steward.domain.activitylog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.groups.Tuple;
import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.dto.ActivityLogSearchRequestDto;
import org.church.steward.domain.activitylog.entity.ActivityLog;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.church.steward.domain.activitylog.mapper.ActivityLogMapper;
import org.church.steward.domain.activitylog.repository.ActivityLogRepository;
import org.church.steward.domain.activitylog.service.impl.ActivityLogServiceImpl;
import org.church.steward.domain.activitylog.util.DynamicLoggable;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.infrastructure.security.LoggedInUser;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceImplTest {

    @Mock
    private UserSessionHolder userSessionHolder;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ActivityLogMapper activityLogMapper;

    @Captor
    private ArgumentCaptor<List<ActivityLogFieldDto>> fieldsCaptor;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    @Loggable(name = "테스트 엔터티")
    static class TestEntity {
        @LoggableField(name = "ID", isIdentifier = true)
        private Long id;
        @LoggableField(name = "이름")
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 구분에 따라 활동 기록의 논리명이 달라지는 엔터티
     */
    @Loggable(name = "동적 엔터티")
    static class DynamicTestEntity implements DynamicLoggable {
        @LoggableField(name = "일자", isIdentifier = true)
        private String date;
        @LoggableField(name = "금액")
        private String amount;

        private final String kind;

        public DynamicTestEntity(String kind, String date, String amount) {
            this.kind = kind;
            this.date = date;
            this.amount = amount;
        }

        @Override
        public String getLoggableName() {
            return kind;
        }

        @Override
        public String getLoggableFieldName(String declaredName) {
            return "일자".equals(declaredName) ? kind + "일" : declaredName;
        }
    }

    private void mockLoggedInUser(String userId) {
        when(userSessionHolder.getLoggedInUser()).thenReturn(new LoggedInUser(userId, UserRole.USER));
    }

    private void mockNoLoggedInUser() {
        when(userSessionHolder.getLoggedInUser()).thenReturn(null);
    }

    /**
     * 서비스가 매퍼에 넘긴 항목 목록을 (논리명, 변경 전 값, 값, 변경 여부)로 검증한다.
     */
    private void assertCapturedFields(Tuple... expected) {
        assertThat(fieldsCaptor.getValue())
                .extracting(ActivityLogFieldDto::getName, ActivityLogFieldDto::getOldValue,
                        ActivityLogFieldDto::getValue, ActivityLogFieldDto::isChanged)
                .containsExactly(expected);
    }

    @Nested
    @DisplayName("logCreation 테스트")
    class LogCreationTest {

        @Test
        @DisplayName("logCreation_유효한 엔터티_모든 항목이 변경 전 값 없이 기록된다")
        void logCreation_validEntity_recordsAllFields() {
            // Given
            mockLoggedInUser("test-user");
            TestEntity entity = new TestEntity(1L, "테스트");

            // When
            activityLogService.logCreation(entity);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.CREATE), eq("테스트 엔터티"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("ID", null, "1", false),
                    tuple("이름", null, "테스트", false));
        }

        @Test
        @DisplayName("logCreation_유효한 엔터티_매퍼가 만든 활동 기록이 저장된다")
        void logCreation_validEntity_savesMappedLog() {
            // Given
            mockLoggedInUser("test-user");
            ActivityLog mappedLog = ActivityLog.builder()
                    .build();
            when(activityLogMapper.toEntity(any(), any(), any(), any())).thenReturn(mappedLog);

            // When
            activityLogService.logCreation(new TestEntity(1L, "테스트"));

            // Then
            verify(activityLogRepository).save(mappedLog);
        }

        @Test
        @DisplayName("logCreation_로그인되지 않은 사용자_SYSTEM으로 기록된다")
        void logCreation_noUser_recordsAsSystem() {
            // Given
            mockNoLoggedInUser();
            TestEntity entity = new TestEntity(1L, "테스트");

            // When
            activityLogService.logCreation(entity);

            // Then
            verify(activityLogMapper).toEntity(eq("(비로그인)"), eq(ActivityType.CREATE), any(), any());
        }

        @Test
        @DisplayName("논리명이 동적으로 정해지는 엔터티는 엔터티가 반환한 논리명으로 대상과 필드를 기록한다")
        void logCreation_dynamicLoggable_usesNamesFromEntity() {
            // Given
            mockLoggedInUser("test-user");
            DynamicTestEntity entity = new DynamicTestEntity("지출", "2025-03-02", "1000");

            // When
            activityLogService.logCreation(entity);

            // Then
            verify(activityLogMapper).toEntity(any(), any(), eq("지출"), fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("지출일", null, "2025-03-02", false),
                    tuple("금액", null, "1000", false));
        }
    }

    @Nested
    @DisplayName("logUpdate 테스트")
    class LogUpdateTest {

        @Test
        @DisplayName("logUpdate_필드 값이 변경된 경우_변경 전 값과 함께 기록된다")
        void logUpdate_changedFields_recordsChanges() {
            // Given
            mockLoggedInUser("test-user");
            TestEntity newEntity = new TestEntity(1L, "새이름");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("ID")).thenReturn("1");
            when(snapshot.getFieldValue("이름")).thenReturn("옛날이름");

            // When
            activityLogService.logUpdate(snapshot, newEntity);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.UPDATE), eq("테스트 엔터티"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("ID", null, "1", false),
                    tuple("이름", "옛날이름", "새이름", true));
        }

        @Test
        @DisplayName("logUpdate_값이 변경된 항목이 없는 경우_활동 기록을 만들지 않는다")
        void logUpdate_noChangedFields_doesNotCreateLog() {
            // Given
            TestEntity newEntity = new TestEntity(1L, "이름");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("ID")).thenReturn("1");
            when(snapshot.getFieldValue("이름")).thenReturn("이름");

            // When
            activityLogService.logUpdate(snapshot, newEntity);

            // Then
            verify(activityLogMapper, never()).toEntity(any(), any(), any(), any());
        }

        @Test
        @DisplayName("logUpdate_값이 변경된 항목이 없는 경우_활동 기록을 저장하지 않는다")
        void logUpdate_noChangedFields_doesNotSaveLog() {
            // Given
            TestEntity newEntity = new TestEntity(1L, "이름");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("ID")).thenReturn("1");
            when(snapshot.getFieldValue("이름")).thenReturn("이름");

            // When
            activityLogService.logUpdate(snapshot, newEntity);

            // Then
            verify(activityLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("logUpdate_식별자만 변경된 경우_식별자의 변경 전 값과 함께 기록된다")
        void logUpdate_onlyIdentifierChanged_recordsChange() {
            // Given
            mockLoggedInUser("test-user");
            TestEntity newEntity = new TestEntity(2L, "이름");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("ID")).thenReturn("1");
            when(snapshot.getFieldValue("이름")).thenReturn("이름");

            // When
            activityLogService.logUpdate(snapshot, newEntity);

            // Then
            verify(activityLogMapper).toEntity(any(), any(), any(), fieldsCaptor.capture());
            assertCapturedFields(tuple("ID", "1", "2", true));
        }

        @Test
        @DisplayName("logUpdate_로그인되지 않은 사용자_SYSTEM으로 기록된다")
        void logUpdate_noUser_recordsAsSystem() {
            // Given
            mockNoLoggedInUser();
            TestEntity newEntity = new TestEntity(1L, "새이름");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);

            // When
            activityLogService.logUpdate(snapshot, newEntity);

            // Then
            verify(activityLogMapper).toEntity(eq("(비로그인)"), eq(ActivityType.UPDATE), any(), any());
        }

        @Test
        @DisplayName("논리명이 동적으로 정해지는 엔터티는 선언된 논리명으로 스냅샷을 조회하고 엔터티가 반환한 논리명으로 기록한다")
        void logUpdate_dynamicLoggable_looksUpByDeclaredNameAndUsesNamesFromEntity() {
            // Given
            mockLoggedInUser("test-user");
            DynamicTestEntity newEntity = new DynamicTestEntity("수입", "2025-03-02", "2000");
            @SuppressWarnings("unchecked")
            EntitySnapshot<DynamicTestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("일자")).thenReturn("2025-03-01");
            when(snapshot.getFieldValue("금액")).thenReturn("2000");

            // When
            activityLogService.logUpdate(snapshot, newEntity);

            // Then
            verify(activityLogMapper).toEntity(any(), any(), eq("수입"), fieldsCaptor.capture());
            assertCapturedFields(tuple("수입일", "2025-03-01", "2025-03-02", true));
        }
    }

    @Nested
    @DisplayName("logClosing 테스트")
    class LogClosingTest {

        @Test
        @DisplayName("logClosing_필드 값이 변경된 경우_마감 유형으로 식별자와 변경된 항목이 기록된다")
        void logClosing_changedFields_recordsChangesAsClose() {
            // Given
            mockLoggedInUser("test-user");
            TestEntity closedEntity = new TestEntity(1L, "마감 후");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("ID")).thenReturn("1");
            when(snapshot.getFieldValue("이름")).thenReturn("마감 전");

            // When
            activityLogService.logClosing(snapshot, closedEntity);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.CLOSE), eq("테스트 엔터티"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("ID", null, "1", false),
                    tuple("이름", "마감 전", "마감 후", true));
        }

        @Test
        @DisplayName("logClosing_유효한 엔터티_매퍼가 만든 활동 기록이 저장된다")
        void logClosing_validEntity_savesMappedLog() {
            // Given
            mockLoggedInUser("test-user");
            ActivityLog mappedLog = ActivityLog.builder()
                    .build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(activityLogMapper.toEntity(any(), any(), any(), any())).thenReturn(mappedLog);

            // When
            activityLogService.logClosing(snapshot, new TestEntity(1L, "마감 후"));

            // Then
            verify(activityLogRepository).save(mappedLog);
        }
    }

    @Nested
    @DisplayName("logReopening 테스트")
    class LogReopeningTest {

        @Test
        @DisplayName("logReopening_필드 값이 변경된 경우_마감 취소 유형으로 식별자와 변경된 항목이 기록된다")
        void logReopening_changedFields_recordsChangesAsReopen() {
            // Given
            mockLoggedInUser("test-user");
            TestEntity reopenedEntity = new TestEntity(1L, "마감 취소 후");
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(snapshot.getFieldValue("ID")).thenReturn("1");
            when(snapshot.getFieldValue("이름")).thenReturn("마감 취소 전");

            // When
            activityLogService.logReopening(snapshot, reopenedEntity);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.REOPEN), eq("테스트 엔터티"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("ID", null, "1", false),
                    tuple("이름", "마감 취소 전", "마감 취소 후", true));
        }

        @Test
        @DisplayName("logReopening_유효한 엔터티_매퍼가 만든 활동 기록이 저장된다")
        void logReopening_validEntity_savesMappedLog() {
            // Given
            mockLoggedInUser("test-user");
            ActivityLog mappedLog = ActivityLog.builder()
                    .build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> snapshot = mock(EntitySnapshot.class);
            when(activityLogMapper.toEntity(any(), any(), any(), any())).thenReturn(mappedLog);

            // When
            activityLogService.logReopening(snapshot, new TestEntity(1L, "마감 취소 후"));

            // Then
            verify(activityLogRepository).save(mappedLog);
        }
    }

    @Nested
    @DisplayName("logDeletion 테스트")
    class LogDeletionTest {

        @Test
        @DisplayName("logDeletion_유효한 엔터티_식별자 정보만 기록된다")
        void logDeletion_validEntity_includesIdentifier() {
            // Given
            mockLoggedInUser("test-user");
            TestEntity entity = new TestEntity(1L, "테스트");

            // When
            activityLogService.logDeletion(entity);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.DELETE), eq("테스트 엔터티"),
                    fieldsCaptor.capture());
            assertCapturedFields(tuple("ID", null, "1", false));
        }

        @Test
        @DisplayName("logDeletion_로그인되지 않은 사용자_SYSTEM으로 기록된다")
        void logDeletion_noUser_recordsAsSystem() {
            // Given
            mockNoLoggedInUser();
            TestEntity entity = new TestEntity(1L, "테스트");

            // When
            activityLogService.logDeletion(entity);

            // Then
            verify(activityLogMapper).toEntity(eq("(비로그인)"), eq(ActivityType.DELETE), any(), any());
        }

        @Test
        @DisplayName("논리명이 동적으로 정해지는 엔터티는 엔터티가 반환한 논리명으로 대상과 식별자를 기록한다")
        void logDeletion_dynamicLoggable_usesNamesFromEntity() {
            // Given
            mockLoggedInUser("test-user");
            DynamicTestEntity entity = new DynamicTestEntity("지출", "2025-03-02", "1000");

            // When
            activityLogService.logDeletion(entity);

            // Then
            verify(activityLogMapper).toEntity(any(), any(), eq("지출"), fieldsCaptor.capture());
            assertCapturedFields(tuple("지출일", null, "2025-03-02", false));
        }
    }

    @Nested
    @DisplayName("logReportPrint 테스트")
    class LogReportPrintTest {

        @Test
        @DisplayName("logReportPrint_유효한 파라미터_보고서 정보가 올바르게 기록된다")
        void logReportPrint_validParams_recordsCorrectly() {
            // Given
            mockLoggedInUser("test-user");
            String reportType = "회계 연도 수입/지출 결산 보고서";
            String target = "2026년";

            // When
            activityLogService.logReportPrint(reportType, target, "PDF 저장");

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.PRINT), eq(reportType),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("유형", null, "회계 연도 수입/지출 결산 보고서", false),
                    tuple("대상", null, "2026년", false),
                    tuple("출력 방식", null, "PDF 저장", false));
        }

        @Test
        @DisplayName("logReportPrint_로그인되지 않은 사용자_SYSTEM으로 기록된다")
        void logReportPrint_noUser_recordsAsSystem() {
            // Given
            mockNoLoggedInUser();
            String reportType = "보고서";
            String target = "대상";

            // When
            activityLogService.logReportPrint(reportType, target, "인쇄");

            // Then
            verify(activityLogMapper).toEntity(eq("(비로그인)"), eq(ActivityType.PRINT), any(), any());
        }
    }

    @Nested
    @DisplayName("logBackup 테스트")
    class LogBackupTest {

        @Test
        @DisplayName("logBackup_유효한 파일 이름_백업 유형과 파일 이름이 기록된다")
        void logBackup_validFileName_recordsCorrectly() {
            // Given
            mockLoggedInUser("test-user");

            // When
            activityLogService.logBackup("청지기_백업_20261013_1530.csbackup");

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.BACKUP), eq("전체 데이터"),
                    fieldsCaptor.capture());
            assertCapturedFields(tuple("파일 이름", null, "청지기_백업_20261013_1530.csbackup", false));
        }
    }

    @Nested
    @DisplayName("logRestore 테스트")
    class LogRestoreTest {

        @Test
        @DisplayName("logRestore_유효한 파라미터_복원 유형과 백업 파일의 이름, 백업 일시가 기록된다")
        void logRestore_validParams_recordsCorrectly() {
            // Given
            String userId = "관리자";

            // When
            activityLogService.logRestore(userId, "backup.csbackup", LocalDateTime.of(2026, 10, 13, 15, 30, 5));

            // Then
            verify(activityLogMapper).toEntity(eq("관리자"), eq(ActivityType.RESTORE), eq("전체 데이터"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("파일 이름", null, "backup.csbackup", false),
                    tuple("백업 일시", null, "2026-10-13 15:30:05", false));
        }

        @Test
        @DisplayName("logRestore_로그인 세션 없음_로그인 세션이 아니라 넘겨받은 계정으로 기록된다")
        void logRestore_noLoggedInUser_recordsGivenUserId() {
            // Given
            String userId = "관리자";

            // When
            activityLogService.logRestore(userId, "backup.csbackup", LocalDateTime.of(2026, 10, 13, 15, 30, 5));

            // Then
            verify(activityLogMapper).toEntity(eq("관리자"), eq(ActivityType.RESTORE), any(), any());
        }
    }

    @Nested
    @DisplayName("logExport 테스트")
    class LogExportTest {

        @Test
        @DisplayName("logExport_유효한 파라미터_내보내기 유형과 회계 연도, 파일 이름, 건수가 기록된다")
        void logExport_validParams_recordsCorrectly() {
            // Given
            mockLoggedInUser("test-user");

            // When
            activityLogService.logExport("수입/지출", "2026년", "수입지출.xlsx", 1234);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.EXPORT), eq("수입/지출"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("회계 연도", null, "2026년", false),
                    tuple("파일 이름", null, "수입지출.xlsx", false),
                    tuple("건수", null, "1,234건", false));
        }

        @Test
        @DisplayName("logExport_회계 연도 없음_회계 연도 항목 없이 파일 이름과 건수만 기록된다")
        void logExport_noFiscalYear_recordsWithoutFiscalYear() {
            // Given
            mockLoggedInUser("test-user");

            // When
            activityLogService.logExport("관계자", null, "관계자.xlsx", 24);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.EXPORT), eq("관계자"),
                    fieldsCaptor.capture());
            assertCapturedFields(
                    tuple("파일 이름", null, "관계자.xlsx", false),
                    tuple("건수", null, "24건", false));
        }
    }

    @Nested
    @DisplayName("logImport 테스트")
    class LogImportTest {

        @Test
        @DisplayName("logImport_유효한 파라미터_가져오기 유형으로 요약 항목과 건별 항목 목록이 기록된다")
        void logImport_validParams_recordsSummaryAndItems() {
            // Given
            mockLoggedInUser("test-user");
            List<List<ActivityLogFieldDto>> items = List.of(List.of(ActivityLogFieldDto.builder()
                    .name("예산명")
                    .value("십일조")
                    .build()));

            // When
            activityLogService.logImport("예산", "2026년", "예산.xlsx", "추가 1건, 금액 갱신 0건", items);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.IMPORT), eq("예산"),
                    fieldsCaptor.capture(), eq(items));
            assertCapturedFields(
                    tuple("회계 연도", null, "2026년", false),
                    tuple("파일 이름", null, "예산.xlsx", false),
                    tuple("결과", null, "추가 1건, 금액 갱신 0건", false));
        }

        @Test
        @DisplayName("logImport_회계 연도 없음_회계 연도 항목 없이 파일 이름과 결과만 기록된다")
        void logImport_noFiscalYear_recordsWithoutFiscalYear() {
            // Given
            mockLoggedInUser("test-user");
            List<List<ActivityLogFieldDto>> items = List.of();

            // When
            activityLogService.logImport("관계자", null, "관계자.csv", "추가 3건", items);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.IMPORT), eq("관계자"),
                    fieldsCaptor.capture(), eq(items));
            assertCapturedFields(
                    tuple("파일 이름", null, "관계자.csv", false),
                    tuple("결과", null, "추가 3건", false));
        }
    }

    @Nested
    @DisplayName("describeCreation 테스트")
    class DescribeCreationTest {

        @Test
        @DisplayName("describeCreation_엔터티_생성 로그와 같은 항목 목록을 로그를 남기지 않고 반환한다")
        void describeCreation_entity_returnsFieldsOfCreationLog() {
            // Given
            TestEntity entity = new TestEntity(1L, "새이름");

            // When
            List<ActivityLogFieldDto> fields = activityLogService.describeCreation(entity);

            // Then
            assertThat(fields)
                    .extracting(ActivityLogFieldDto::getName, ActivityLogFieldDto::getValue)
                    .containsExactly(tuple("ID", "1"), tuple("이름", "새이름"));
        }
    }

    @Nested
    @DisplayName("describeUpdate 테스트")
    class DescribeUpdateTest {

        @Test
        @DisplayName("describeUpdate_값이 바뀐 엔터티_식별자와 바뀐 항목만 변경 전 값과 함께 반환한다")
        void describeUpdate_changedEntity_returnsIdentifierAndChangedFields() {
            // Given
            @SuppressWarnings("unchecked")
            EntitySnapshot<TestEntity> oldSnapshot = mock(EntitySnapshot.class);
            when(oldSnapshot.getFieldValue("ID")).thenReturn("1");
            when(oldSnapshot.getFieldValue("이름")).thenReturn("옛날이름");

            // When
            List<ActivityLogFieldDto> fields = activityLogService.describeUpdate(oldSnapshot,
                    new TestEntity(1L, "새이름"));

            // Then
            assertThat(fields)
                    .extracting(ActivityLogFieldDto::getName, ActivityLogFieldDto::getOldValue,
                            ActivityLogFieldDto::getValue, ActivityLogFieldDto::isChanged)
                    .containsExactly(
                            tuple("ID", null, "1", false),
                            tuple("이름", "옛날이름", "새이름", true));
        }
    }

    @Nested
    @DisplayName("getLatestLogId 테스트")
    class GetLatestLogIdTest {

        @Test
        @DisplayName("getLatestLogId_활동 기록이 있음_가장 마지막 활동 기록의 ID를 반환한다")
        void getLatestLogId_logsExist_returnsLatestId() {
            // Given
            ActivityLog latest = ActivityLog.builder()
                    .id(42L)
                    .build();
            when(activityLogRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(latest));

            // When
            long latestLogId = activityLogService.getLatestLogId();

            // Then
            assertThat(latestLogId).isEqualTo(42L);
        }

        @Test
        @DisplayName("getLatestLogId_활동 기록이 없음_0을 반환한다")
        void getLatestLogId_noLogs_returnsZero() {
            // Given
            when(activityLogRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

            // When
            long latestLogId = activityLogService.getLatestLogId();

            // Then
            assertThat(latestLogId).isZero();
        }
    }

    @Nested
    @DisplayName("logLogin 테스트")
    class LogLoginTest {

        @Test
        @DisplayName("logLogin_유효한 사용자_로그인 정보가 올바르게 기록된다")
        void logLogin_validUser_recordsCorrectly() {
            // Given
            mockLoggedInUser("test-user");
            String userId = "test-user";

            // When
            activityLogService.logLogin(userId);

            // Then
            verify(activityLogMapper).toEntity(eq("test-user"), eq(ActivityType.LOGIN), eq("사용자"),
                    fieldsCaptor.capture());
            assertCapturedFields(tuple("사용자 ID", null, "test-user", false));
        }
    }

    @Nested
    @DisplayName("getLogs 테스트")
    class GetLogsTest {

        @Test
        @DisplayName("getLogs_검색 조건이 있는 경우_조회된 활동 기록을 매퍼로 변환해 반환한다")
        void getLogs_withFilters_returnsMappedResults() {
            // Given
            LocalDate now = LocalDate.now();
            ActivityLogSearchRequestDto request = ActivityLogSearchRequestDto.builder()
                    .startDate(now.atStartOfDay())
                    .endDate(now.atTime(23, 59, 59))
                    .userId("test-user")
                    .activityType(ActivityType.CREATE)
                    .targetName("테스트")
                    .build();
            Pageable pageable = PageRequest.of(0, 10);

            ActivityLog log = ActivityLog.builder()
                    .id(1L)
                    .build();
            ActivityLogResponseDto dto = ActivityLogResponseDto.builder()
                    .id(1L)
                    .build();

            Page<ActivityLog> logPage = new PageImpl<>(List.of(log), pageable, 1);
            when(activityLogRepository.findAll(
                    ArgumentMatchers.<Specification<ActivityLog>>any(),
                    ArgumentMatchers.any(Pageable.class)))
                    .thenReturn(logPage);
            when(activityLogMapper.toResponseDto(log)).thenReturn(dto);

            // When
            Page<ActivityLogResponseDto> result = activityLogService.getLogs(request, 0, 10);

            // Then
            assertThat(result.getContent()).containsExactly(dto);
        }

        @Test
        @DisplayName("요청한 페이지 번호와 크기로, 기록 일시의 역순으로 정렬해 조회한다")
        void getLogs_pageAndSize_requestsLatestFirstPage() {
            // Given
            LocalDate now = LocalDate.now();
            ActivityLogSearchRequestDto request = ActivityLogSearchRequestDto.builder()
                    .startDate(now.atStartOfDay())
                    .endDate(now.atTime(23, 59, 59))
                    .build();
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(activityLogRepository.findAll(
                    ArgumentMatchers.<Specification<ActivityLog>>any(),
                    pageableCaptor.capture()))
                    .thenReturn(Page.empty());

            // When
            activityLogService.getLogs(request, 2, 20);

            // Then
            assertThat(pageableCaptor.getValue())
                    .isEqualTo(PageRequest.of(2, 20, Sort.by(Sort.Direction.DESC, ActivityLog::getCreatedAt)));
        }
    }
}
