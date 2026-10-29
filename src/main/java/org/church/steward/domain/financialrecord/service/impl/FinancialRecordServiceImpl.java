package org.church.steward.domain.financialrecord.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.contact.service.ContactGroupService;
import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordConstraints;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.mapper.FinancialRecordMapper;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.financialrecord.service.FinancialRecordService;
import org.church.steward.domain.financialrecord.specification.FinancialRecordSpecification;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordRepository financialRecordRepository;
    private final BudgetRepository budgetRepository;
    private final ContactRepository contactRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final FinancialRecordMapper financialRecordMapper;
    private final ActivityLogService activityLogService;
    private final EntitySnapshotCapturer entitySnapshotCapturer;
    private final ContactGroupService contactGroupService;
    private static final long MAX_AMOUNT = 100_000_000_000L; // 1,000억
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, FinancialRecord::getDate,
            FinancialRecord::getId);

    @Override
    @Transactional(readOnly = true)
    public List<FinancialRecordResponseDto> listFinancialRecords(FinancialRecordSearchRequestDto request) {
        List<Long> budgetIds = null;
        if (request.getBudgetId() != null) {
            budgetIds = budgetRepository.findAllDescendantIds(request.getBudgetId());
        }

        return financialRecordRepository.findAll(FinancialRecordSpecification.searchBy(request, budgetIds),
                LATEST_FIRST)
                .stream()
                .map(financialRecordMapper::toResponseDto)
                .filter(record -> matchesAmount(record, request))
                .collect(Collectors.toList());
    }

    @Override
    public FinancialRecordResponseDto createFinancialRecord(FinancialRecordRequestDto dto) {
        FinancialRecord financialRecord = createFinancialRecordWithoutLog(dto);
        activityLogService.logCreation(financialRecord);
        return financialRecordMapper.toResponseDto(financialRecord);
    }

    @Override
    public FinancialRecord createFinancialRecordWithoutLog(FinancialRecordRequestDto dto) {
        FinancialRecordType type = dto.getType();
        if (type == null) {
            throw new IllegalArgumentException("Invalid Financial Record Type");
        }
        validateAmount(dto.getAmount());
        validateTexts(dto.getDescription(), dto.getRemarks());
        validateContacts(type, dto.getContactIds());
        FiscalYear fiscalYear = findFiscalYear(dto.getFiscalYearId());
        validateNotClosed(fiscalYear);
        validateDate(fiscalYear, dto.getDate());
        Budget budget = budgetRepository.findById(dto.getBudgetId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Budget ID"));
        if (budget.hasChildren()) {
            throw new IllegalArgumentException("하위 예산이 있는 예산에는 " + type.getLabel() + "을 등록할 수 없습니다.");
        }
        validateBudgetType(budget, type);
        validateBudgetFiscalYear(budget, fiscalYear);

        FinancialRecord financialRecord = FinancialRecord.builder()
                .type(type)
                .fiscalYear(fiscalYear)
                .date(dto.getDate())
                .budget(budget)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .remarks(dto.getRemarks())
                .build();

        Set<Long> contactIdSet = new HashSet<>(dto.getContactIds());
        findContacts(contactIdSet).forEach(financialRecord::addContact);

        financialRecordRepository.save(financialRecord);
        registerContactGroup(type, contactIdSet);
        return financialRecord;
    }

    @Override
    public FinancialRecordResponseDto updateFinancialRecord(Long id, FinancialRecordRequestDto dto) {
        validateAmount(dto.getAmount());
        validateTexts(dto.getDescription(), dto.getRemarks());
        FinancialRecord financialRecord = financialRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Financial Record not found"));
        // 구분은 바꿀 수 없으므로 요청의 구분은 보지 않고 기존 내역의 구분을 따른다.
        FinancialRecordType type = financialRecord.getType();
        validateContacts(type, dto.getContactIds());

        // 내역이 지금 속한 회계 연도와 옮겨 갈 회계 연도가 모두 마감되지 않아야 한다.
        validateNotClosed(financialRecord.getFiscalYear());
        FiscalYear fiscalYear = findFiscalYear(dto.getFiscalYearId());
        validateNotClosed(fiscalYear);
        validateDate(fiscalYear, dto.getDate());
        Budget budget = budgetRepository.findById(dto.getBudgetId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Budget ID"));
        if (budget.hasChildren()) {
            throw new IllegalArgumentException("하위 예산이 있는 예산으로는 " + type.getLabel() + "을 변경할 수 없습니다.");
        }
        validateBudgetType(budget, type);
        validateBudgetFiscalYear(budget, fiscalYear);
        Set<Long> contactIdSet = new HashSet<>(dto.getContactIds());
        List<Contact> contacts = findContacts(contactIdSet);

        EntitySnapshot<FinancialRecord> oldSnapshot = entitySnapshotCapturer.capture(financialRecord);

        financialRecord.update(fiscalYear, dto.getDate(), budget, dto.getDescription(), dto.getAmount(),
                dto.getRemarks());
        financialRecord.replaceContacts(contacts);

        financialRecordRepository.save(financialRecord);
        activityLogService.logUpdate(oldSnapshot, financialRecord);
        registerContactGroup(type, contactIdSet);
        return financialRecordMapper.toResponseDto(financialRecord);
    }

    @Override
    public void updateFinancialRecordsBudget(List<Long> ids, Long budgetId) {
        List<FinancialRecord> financialRecords = findFinancialRecords(ids);
        if (budgetId == null) {
            throw new IllegalArgumentException("예산을 선택해주세요.");
        }
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Budget ID"));

        // 모두 검증한 뒤에 바꾼다.
        for (FinancialRecord financialRecord : financialRecords) {
            FinancialRecordType type = financialRecord.getType();
            validateNotClosed(financialRecord.getFiscalYear());
            if (budget.hasChildren()) {
                throw new IllegalArgumentException("하위 예산이 있는 예산으로는 " + type.getLabel() + "을 변경할 수 없습니다.");
            }
            validateBudgetType(budget, type);
            validateBudgetFiscalYear(budget, financialRecord.getFiscalYear());
        }

        for (FinancialRecord financialRecord : financialRecords) {
            EntitySnapshot<FinancialRecord> oldSnapshot = entitySnapshotCapturer.capture(financialRecord);
            financialRecord.changeBudget(budget);
            financialRecordRepository.save(financialRecord);
            activityLogService.logUpdate(oldSnapshot, financialRecord);
        }
    }

    @Override
    public void updateFinancialRecordsContacts(List<Long> ids, List<Long> contactIds) {
        List<FinancialRecord> financialRecords = findFinancialRecords(ids);

        // 모두 검증한 뒤에 바꾼다.
        for (FinancialRecord financialRecord : financialRecords) {
            validateNotClosed(financialRecord.getFiscalYear());
            validateContacts(financialRecord.getType(), contactIds);
        }
        Set<Long> contactIdSet = new HashSet<>(contactIds);
        List<Contact> contacts = findContacts(contactIdSet);

        boolean hasIncome = false;
        for (FinancialRecord financialRecord : financialRecords) {
            EntitySnapshot<FinancialRecord> oldSnapshot = entitySnapshotCapturer.capture(financialRecord);
            financialRecord.replaceContacts(contacts);
            financialRecordRepository.save(financialRecord);
            activityLogService.logUpdate(oldSnapshot, financialRecord);
            hasIncome |= financialRecord.getType() == FinancialRecordType.INCOME;
        }
        // 바꾼 관계자가 모두 같으므로 그룹은 한 번만 확인한다.
        if (hasIncome) {
            registerContactGroup(FinancialRecordType.INCOME, contactIdSet);
        }
    }

    @Override
    public void deleteFinancialRecords(List<Long> ids) {
        List<FinancialRecord> financialRecords = findFinancialRecords(ids);
        for (FinancialRecord financialRecord : financialRecords) {
            validateNotClosed(financialRecord.getFiscalYear());
        }

        for (FinancialRecord financialRecord : financialRecords) {
            financialRecordRepository.delete(financialRecord);
            activityLogService.logDeletion(financialRecord);
        }
    }

    /**
     * 여러 건을 한 번에 처리할 내역들을 찾는다. 하나라도 찾지 못하면(다른 곳에서 이미 삭제한 경우 등) 모두 처리하지 않는다.
     */
    private List<FinancialRecord> findFinancialRecords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("선택된 내역이 없습니다.");
        }
        Set<Long> idSet = new HashSet<>(ids);
        List<FinancialRecord> financialRecords = financialRecordRepository.findAllById(idSet);
        if (financialRecords.size() != idSet.size()) {
            throw new IllegalArgumentException("이미 삭제되었거나 찾을 수 없는 내역이 있습니다. 다시 조회해주세요.");
        }
        return financialRecords;
    }

    /**
     * 예산의 구분은 내역의 구분과 같아야 한다. 수입에는 수입 예산만, 지출에는 지출 예산만 쓸 수 있다.
     * 등록(가져오기 포함), 수정, 예산 일괄 변경이 모두 검증한다.
     */
    private void validateBudgetType(Budget budget, FinancialRecordType type) {
        BudgetType expectedType = type == FinancialRecordType.INCOME ? BudgetType.INCOME : BudgetType.EXPENSE;
        if (budget.getType() != expectedType) {
            throw new IllegalArgumentException(type.getLabel() + "에는 " + type.getLabel() + " 예산만 선택할 수 있습니다.");
        }
    }

    private List<Contact> findContacts(Set<Long> contactIds) {
        List<Contact> contacts = contactRepository.findAllById(contactIds);
        if (contacts.size() != contactIds.size()) {
            throw new IllegalArgumentException("Invalid Contact ID(s) provided.");
        }
        return contacts;
    }

    /**
     * 수입의 봉헌자가 2명 이상이면, 같은 구성원의 그룹이 없는 경우 그룹으로 등록한다.
     * 다음 수입 입력 시 자동 완성에서 그룹을 골라 구성원 전체를 한 번에 입력할 수 있게 하기 위함이다.
     * 지출은 그룹 기능을 쓰지 않는다.
     */
    private void registerContactGroup(FinancialRecordType type, Set<Long> contactIds) {
        if (type != FinancialRecordType.INCOME || contactIds.size() < 2) {
            return;
        }
        contactGroupService.createGroupIfAbsent(new ArrayList<>(contactIds));
    }

    private void validateAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0원 이상이어야 합니다.");
        }
        if (amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("금액이 허용된 범위를 초과했습니다. (최대 금액 : 1,000억)");
        }
    }

    /**
     * 내용과 비고는 비워 둘 수 있고, 적으면 칼럼 길이 이내여야 한다.
     */
    private void validateTexts(String description, String remarks) {
        if (description != null && description.length() > FinancialRecordConstraints.DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "내용은 " + FinancialRecordConstraints.DESCRIPTION_MAX_LENGTH + "자 이내로 입력해주세요.");
        }
        if (remarks != null && remarks.length() > FinancialRecordConstraints.REMARKS_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "비고는 " + FinancialRecordConstraints.REMARKS_MAX_LENGTH + "자 이내로 입력해주세요.");
        }
    }

    private FiscalYear findFiscalYear(Long fiscalYearId) {
        if (fiscalYearId == null) {
            throw new IllegalArgumentException("Invalid Fiscal Year ID");
        }
        return fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Fiscal Year ID"));
    }

    /**
     * 마감된 회계 연도의 수입/지출은 추가, 수정, 삭제할 수 없다.
     */
    private void validateNotClosed(FiscalYear fiscalYear) {
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감된 회계 연도의 수입/지출은 변경할 수 없습니다.");
        }
    }

    /**
     * 예산은 내역과 같은 회계 연도에 속해야 한다.
     */
    private void validateBudgetFiscalYear(Budget budget, FiscalYear fiscalYear) {
        if (!budget.getFiscalYear()
                .getId()
                .equals(fiscalYear.getId())) {
            throw new IllegalArgumentException("다른 회계 연도의 예산은 선택할 수 없습니다.");
        }
    }

    /**
     * 날짜는 회계 연도의 기간(시작일과 종료일 포함) 안에 있어야 한다.
     */
    private void validateDate(FiscalYear fiscalYear, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜를 입력해주세요.");
        }
        if (!fiscalYear.contains(date)) {
            throw new IllegalArgumentException(String.format("날짜가 \"%s\" 회계 연도의 기간(%s ~ %s)을 벗어났습니다.",
                    fiscalYear.getName(), fiscalYear.getStartDate(), fiscalYear.getEndDate()));
        }
    }

    /**
     * 수입은 봉헌자가 1명 이상, 지출은 수취인이 정확히 1명이어야 한다.
     */
    private void validateContacts(FinancialRecordType type, List<Long> contactIds) {
        int contactCount = contactIds == null ? 0 : new HashSet<>(contactIds).size();
        if (type == FinancialRecordType.INCOME && contactCount < 1) {
            throw new IllegalArgumentException("봉헌자는 한 명 이상이어야 합니다.");
        }
        if (type == FinancialRecordType.EXPENSE && contactCount != 1) {
            throw new IllegalArgumentException("수취인은 한 명이어야 합니다.");
        }
    }

    private boolean matchesAmount(FinancialRecordResponseDto record, FinancialRecordSearchRequestDto request) {
        if (request.getMinAmount() != null && record.getAmount() < request.getMinAmount()) {
            return false;
        }
        if (request.getMaxAmount() != null && record.getAmount() > request.getMaxAmount()) {
            return false;
        }
        return true;
    }
}
