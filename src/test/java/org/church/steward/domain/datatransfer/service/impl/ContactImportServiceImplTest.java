package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.contact.service.ContactService;
import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactImportServiceImplTest {

    private static final long BACKUP_LOG_ID = 10L;
    private static final String FILE_NAME = "관계자.csv";

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private ContactService contactService;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ContactImportServiceImpl contactImportService;

    @Captor
    private ArgumentCaptor<ContactDto> contactCaptor;

    private final Contact hong = Contact.builder()
            .id(10L)
            .type(ContactType.INDIVIDUAL)
            .name("홍길동")
            .isHidden(false)
            .build();

    /**
     * 파일 내용: 새 관계자 두 명과 구분을 읽을 수 없는 한 명.
     * 열 구성은 "구분 | 이름 | 비고 | 상태"다.
     */
    private ContactImportRequestDto createRequest() {
        return ContactImportRequestDto.builder()
                .rows(List.of(
                        Arrays.asList("개인", "김찬양", "1구역", null),
                        Arrays.asList("기타", "무명", null, "숨김"),
                        Arrays.asList("교인", "이믿음", null, null)))
                .firstRowNumber(2)
                .typeColumn(0)
                .nameColumn(1)
                .remarksColumn(2)
                .hiddenColumn(3)
                .build();
    }

    /**
     * 파일의 첫 행과 같은 관계자. 분석한 뒤에 다른 메뉴에서 등록된 상황을 흉내 낸다.
     */
    private Contact createdKim() {
        return Contact.builder()
                .id(11L)
                .type(ContactType.INDIVIDUAL)
                .name("김찬양")
                .remarks("1구역")
                .build();
    }

    private ContactImportAnalysisDto analyzeCurrentData() {
        when(contactRepository.findAll()).thenReturn(List.of(hong));
        return contactImportService.analyze(createRequest());
    }

    private Set<String> selectableKeys(ContactImportAnalysisDto analysis) {
        return analysis.getItems()
                .stream()
                .filter(ContactImportItemDto::isSelectable)
                .map(ContactImportItemDto::getKey)
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("등록된 관계자와 견주어 분석한 결과를 반환한다")
        void analyze_CurrentData_ReturnsAnalysis() {
            // Given
            when(contactRepository.findAll()).thenReturn(List.of(hong));

            // When
            ContactImportAnalysisDto analysis = contactImportService.analyze(createRequest());

            // Then
            assertThat(analysis.getItems())
                    .extracting(ContactImportItemDto::getName, ContactImportItemDto::isSelectable)
                    .containsExactly(
                            tuple("김찬양", true),
                            tuple("무명", true),
                            tuple("이믿음", false));
        }
    }

    @Nested
    @DisplayName("importContacts 테스트")
    class ImportContactsTest {

        @Test
        @DisplayName("고른 행마다 구분, 이름, 비고, 숨김 여부로 관계자를 등록한다")
        void importContacts_SelectedRows_CreatesContacts() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);

            // When
            contactImportService.importContacts(createRequest(), selectableKeys(analysis), analysis.getSignature(),
                    BACKUP_LOG_ID, FILE_NAME);

            // Then
            verify(contactService, times(2)).createContactWithoutLog(contactCaptor.capture());
            assertThat(contactCaptor.getAllValues())
                    .extracting(ContactDto::getType, ContactDto::getName, ContactDto::getRemarks, ContactDto::isHidden)
                    .containsExactly(
                            tuple(ContactType.INDIVIDUAL, "김찬양", "1구역", false),
                            tuple(ContactType.OTHER, "무명", null, true));
        }

        @Test
        @DisplayName("등록한 건수를 반환한다")
        void importContacts_SelectedRows_ReturnsCreatedCount() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);

            // When
            ImportResultDto result = contactImportService.importContacts(createRequest(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            assertThat(result.getCreatedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("활동 기록에 회계 연도 없이 가져오기 한 건으로 남기고, 추가한 건수를 요약에 적는다")
        void importContacts_SelectedRows_LogsOneImportWithoutFiscalYear() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            List<ActivityLogFieldDto> description = List.of(ActivityLogFieldDto.builder()
                    .name("이름")
                    .value("김찬양")
                    .build());
            when(contactService.createContactWithoutLog(any()))
                    .thenReturn(Contact.builder()
                            .build());
            when(activityLogService.describeCreation(any(Contact.class))).thenReturn(description);

            // When
            contactImportService.importContacts(createRequest(), selectableKeys(analysis), analysis.getSignature(),
                    BACKUP_LOG_ID, FILE_NAME);

            // Then
            verify(activityLogService).logImport(eq("관계자"), isNull(), eq(FILE_NAME), eq("추가 2건"),
                    eq(List.of(description, description)));
        }

        @Test
        @DisplayName("백업한 뒤에 활동 기록이 새로 생겼으면 반영하지 않고 백업을 다시 받게 한다")
        void importContacts_BackupOutdated_Failure() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID + 1);

            // When & Then
            assertThatThrownBy(() -> contactImportService.importContacts(createRequest(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(BackupOutdatedException.class);
        }

        @Test
        @DisplayName("분석한 뒤에 데이터가 바뀌어 분석 결과가 달라졌으면 반영하지 않는다")
        void importContacts_AnalysisChanged_Failure() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            // 분석한 뒤에 다른 메뉴에서 같은 관계자 "김찬양"을 등록한 상황
            when(contactRepository.findAll()).thenReturn(List.of(hong, createdKim()));

            // When & Then
            assertThatThrownBy(() -> contactImportService.importContacts(createRequest(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(ImportStateChangedException.class);
        }

        @Test
        @DisplayName("분석 결과가 달라졌으면 관계자를 하나도 등록하지 않는다")
        void importContacts_AnalysisChanged_CreatesNothing() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            when(contactRepository.findAll()).thenReturn(List.of(hong, createdKim()));

            // When
            catchThrowable(() -> contactImportService.importContacts(createRequest(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME));

            // Then
            verify(contactService, never()).createContactWithoutLog(any());
        }

        @Test
        @DisplayName("오류인 행을 고르면 반영하지 않는다")
        void importContacts_ErrorRowSelected_Failure() {
            // Given
            ContactImportAnalysisDto analysis = analyzeCurrentData();
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            Set<String> errorRow = Set.of(analysis.getItems().getLast().getKey());

            // When & Then
            assertThatThrownBy(() -> contactImportService.importContacts(createRequest(), errorRow,
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가져올 수 없는 행이 선택되었습니다: 4행");
        }

        @Test
        @DisplayName("가져올 행을 하나도 고르지 않으면 반영하지 않는다")
        void importContacts_NothingSelected_Failure() {
            // Given

            // When & Then
            assertThatThrownBy(() -> contactImportService.importContacts(createRequest(), Set.of(), "signature",
                    BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가져올 관계자를 선택해주세요.");
        }
    }

}
