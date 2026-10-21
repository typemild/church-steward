package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.datatransfer.dto.BudgetImportAction;
import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetImportAnalyzerTest {

    /**
     * 열 구성이 "구분 | 1단계 | 2단계 | 금액"인 파일의 열 지정. 데이터는 파일의 2행부터 시작한다.
     */
    private BudgetImportRequestDto.BudgetImportRequestDtoBuilder levelRequest(List<List<Object>> rows) {
        return BudgetImportRequestDto.builder()
                .rows(rows)
                .firstRowNumber(2)
                .typeColumn(0)
                .levelColumns(List.of(1, 2))
                .amountColumn(3);
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private BudgetImportAnalysisDto analyze(BudgetImportRequestDto request) {
        return BudgetImportAnalyzer.analyze(request, List.of(), _ -> false);
    }

    private Budget existingBudget(Long id, BudgetType type, String name, Long amount, Budget parent) {
        Budget budget = Budget.builder()
                .id(id)
                .type(type)
                .name(name)
                .amount(amount)
                .parent(parent)
                .children(new ArrayList<>())
                .build();
        if (parent != null) {
            parent.getChildren().add(budget);
        }
        return budget;
    }

    private BudgetImportItemDto find(BudgetImportAnalysisDto analysis, String... levelNames) {
        return analysis.getItems()
                .stream()
                .filter(item -> item.getLevelNames().equals(List.of(levelNames)))
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("단계별 열에서 예산의 계층을 읽고, 없는 예산은 모두 추가 대상으로 본다")
        void analyze_LevelColumns_ReadsHierarchyAsNewBudgets() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", null, null),
                    row("수입", "헌금", "십일조", 1000L),
                    row("수입", "헌금", "감사헌금", 300L),
                    row("지출", "운영비", null, 500L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getRowNumber, BudgetImportItemDto::getType,
                            BudgetImportItemDto::getLevelNames, BudgetImportItemDto::getAmount,
                            BudgetImportItemDto::isLeaf, BudgetImportItemDto::getStatus,
                            BudgetImportItemDto::getAction)
                    .containsExactly(
                            tuple(2, BudgetType.INCOME, List.of("헌금"), 1300L, false, ImportItemStatus.OK,
                                    BudgetImportAction.CREATE),
                            tuple(3, BudgetType.INCOME, List.of("헌금", "십일조"), 1000L, true, ImportItemStatus.OK,
                                    BudgetImportAction.CREATE),
                            tuple(4, BudgetType.INCOME, List.of("헌금", "감사헌금"), 300L, true, ImportItemStatus.OK,
                                    BudgetImportAction.CREATE),
                            tuple(5, BudgetType.EXPENSE, List.of("운영비"), 500L, true, ImportItemStatus.OK,
                                    BudgetImportAction.CREATE));
        }

        @Test
        @DisplayName("하위 예산은 상위 예산인 건의 식별자를 가리킨다")
        void analyze_ChildBudget_PointsToParentKey() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", null, null),
                    row("수입", "헌금", "십일조", 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(find(analysis, "헌금", "십일조").getParentKey()).isEqualTo(find(analysis, "헌금").getKey());
        }

        @Test
        @DisplayName("하위 예산의 경로에만 나오는 상위 예산은 행 번호 없이 하위 예산보다 먼저 만든다")
        void analyze_ParentWithoutOwnRow_CreatesImplicitParentFirst() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", "십일조", 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getRowNumber, BudgetImportItemDto::getLevelNames)
                    .containsExactly(
                            tuple(null, List.of("헌금")),
                            tuple(2, List.of("헌금", "십일조")));
        }

        @Test
        @DisplayName("전체 경로가 적힌 열을 '>'로 나누어 계층을 읽는다")
        void analyze_PathColumn_SplitsPathIntoLevels() {
            // Given
            BudgetImportRequestDto request = BudgetImportRequestDto.builder()
                    .rows(List.of(row("헌금 > 십일조", 1000L)))
                    .firstRowNumber(2)
                    .fixedType(BudgetType.INCOME)
                    .pathColumn(0)
                    .amountColumn(1)
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getType, BudgetImportItemDto::getLevelNames)
                    .containsExactly(
                            tuple(BudgetType.INCOME, List.of("헌금")),
                            tuple(BudgetType.INCOME, List.of("헌금", "십일조")));
        }

        @Test
        @DisplayName("빈 칸을 이어받게 하면 구분과 상위 단계의 빈 칸은 윗 행의 값을 따른다")
        void analyze_FillDown_InheritsBlankCellsFromRowAbove() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", "십일조", 1000L),
                    row(null, null, "감사헌금", 300L)))
                    .fillDown(true)
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(find(analysis, "헌금", "감사헌금"))
                    .extracting(BudgetImportItemDto::getType, BudgetImportItemDto::getStatus)
                    .containsExactly(BudgetType.INCOME, ImportItemStatus.OK);
        }

        @Test
        @DisplayName("빈 칸을 이어받게 해도 그 위 단계가 윗 행과 다르면 이어받지 않고 오류로 본다")
        void analyze_FillDownWithDifferentUpperLevel_DoesNotInherit() {
            // Given
            BudgetImportRequestDto request = BudgetImportRequestDto.builder()
                    .rows(List.of(
                            row("헌금", "주일헌금", "1부", 100L),
                            row("기타수입", null, "이자", 200L)))
                    .firstRowNumber(2)
                    .fixedType(BudgetType.INCOME)
                    .levelColumns(List.of(0, 1, 2))
                    .amountColumn(3)
                    .fillDown(true)
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems().getLast())
                    .extracting(BudgetImportItemDto::getRowNumber, BudgetImportItemDto::getStatus,
                            BudgetImportItemDto::getMessage)
                    .containsExactly(3, ImportItemStatus.ERROR, "2단계의 예산명이 비어 있습니다.");
        }

        @Test
        @DisplayName("빈 칸을 이어받게 하지 않으면 상위 단계가 빈 행은 오류다")
        void analyze_BlankUpperLevelWithoutFillDown_Error() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", "십일조", 1000L),
                    row("수입", null, "감사헌금", 300L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems().getLast())
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR, "1단계의 예산명이 비어 있습니다.");
        }

        @Test
        @DisplayName("예산명이 하나도 없는 행은 건으로 만들지 않고 건너뛴 행으로 센다")
        void analyze_RowWithoutNames_CountsAsBlankRow() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", null, 1000L),
                    row(null, null, null, null),
                    row("수입", null, null, 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis)
                    .extracting(result -> result.getItems().size(), BudgetImportAnalysisDto::getBlankRowCount)
                    .containsExactly(1, 2);
        }

        @Test
        @DisplayName("금액은 숫자 셀과 '1,000,000원' 같은 글자를 모두 읽고, 빈 칸은 0으로 본다")
        void analyze_AmountFormats_ParsesNumbersAndText() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "십일조", null, 1000L),
                    row("수입", "감사헌금", null, " 1,000,000원 "),
                    row("수입", "기타", null, null)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getAmount)
                    .containsExactly(1000L, 1000000L, 0L);
        }

        @Test
        @DisplayName("금액을 읽을 수 없거나 음수인 행은 오류다")
        void analyze_InvalidAmount_Error() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "십일조", null, "천원"),
                    row("수입", "감사헌금", null, -100L),
                    row("수입", "기타", null, 10.5)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getMessage)
                    .containsExactly(
                            tuple(ImportItemStatus.ERROR, "금액을 읽을 수 없습니다: 천원"),
                            tuple(ImportItemStatus.ERROR, "금액은 0 이상이어야 합니다."),
                            tuple(ImportItemStatus.ERROR, "금액을 읽을 수 없습니다: 10.5"));
        }

        @Test
        @DisplayName("구분이 비어 있거나 수입/지출이 아닌 행은 오류다")
        void analyze_InvalidType_Error() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row(null, "십일조", null, 1000L),
                    row("기타", "감사헌금", null, 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getMessage)
                    .containsExactly(
                            tuple(ImportItemStatus.ERROR, "구분이 비어 있습니다."),
                            tuple(ImportItemStatus.ERROR, "구분을 알 수 없습니다: 기타"));
        }

        @Test
        @DisplayName("예산명에 '<', '>'가 있거나 50자를 넘는 행은 오류다")
        void analyze_InvalidName_Error() {
            // Given
            String longName = "가".repeat(51);
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금<일반>", null, 1000L),
                    row("수입", longName, null, 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getMessage)
                    .containsExactly(
                            tuple(ImportItemStatus.ERROR, "예산명에는 '<', '>' 문자를 사용할 수 없습니다: 헌금<일반>"),
                            tuple(ImportItemStatus.ERROR, "예산명이 50자를 넘습니다: " + longName));
        }

        @Test
        @DisplayName("파일 안에서 같은 예산이 두 번 나오면 두 번째 행이 오류다")
        void analyze_DuplicatePathInFile_SecondRowIsError() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "십일조", null, 1000L),
                    row("수입", "십일조", null, 2000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getRowNumber, BudgetImportItemDto::getStatus,
                            BudgetImportItemDto::getMessage)
                    .containsExactly(
                            tuple(2, ImportItemStatus.OK, ""),
                            tuple(3, ImportItemStatus.ERROR, "파일 안에서 같은 예산이 두 번 나옵니다. (2행)"));
        }

        @Test
        @DisplayName("이름이 같아도 구분이 다르면 다른 예산이다")
        void analyze_SameNameDifferentType_AreDifferentBudgets() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "기타", null, 1000L),
                    row("지출", "기타", null, 2000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getStatus)
                    .containsExactly(ImportItemStatus.OK, ImportItemStatus.OK);
        }

        @Test
        @DisplayName("상위 예산에 적힌 금액이 하위 예산의 합계와 다르면 경고하고 하위 예산의 합계를 쓴다")
        void analyze_ParentAmountDiffersFromChildren_Warning() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", null, 2000L),
                    row("수입", "헌금", "십일조", 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(find(analysis, "헌금"))
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getAction,
                            BudgetImportItemDto::getAmount, BudgetImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.WARNING, BudgetImportAction.CREATE, 1000L,
                            "파일에 적힌 금액(2,000원)이 하위 예산의 합계(1,000원)와 다릅니다. 하위 예산의 합계로 저장합니다.");
        }

        @Test
        @DisplayName("이미 있는 예산과 금액까지 같으면 건너뛴다")
        void analyze_ExistingBudgetWithSameAmount_Skipped() {
            // Given
            Budget tithe = existingBudget(1L, BudgetType.INCOME, "십일조", 1000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "십일조", null, 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(tithe), _ -> false);

            // Then
            assertThat(find(analysis, "십일조"))
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getAction,
                            BudgetImportItemDto::getExistingBudgetId, BudgetImportItemDto::isSelectable)
                    .containsExactly(ImportItemStatus.SKIPPED, BudgetImportAction.NONE, 1L, false);
        }

        @Test
        @DisplayName("이미 있는 예산의 금액이 다르면 기본으로는 건너뛰고 기존 금액을 알려준다")
        void analyze_ExistingBudgetWithDifferentAmount_SkippedByDefault() {
            // Given
            Budget tithe = existingBudget(1L, BudgetType.INCOME, "십일조", 1000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "십일조", null, 3000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(tithe), _ -> false);

            // Then
            assertThat(find(analysis, "십일조"))
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getAction,
                            BudgetImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.SKIPPED, BudgetImportAction.NONE,
                            "이미 있는 예산입니다. 금액이 파일과 다릅니다. (기존 1,000원)");
        }

        @Test
        @DisplayName("금액을 갱신하도록 하면 금액이 다른 기존 예산은 금액 갱신 대상이 된다")
        void analyze_ExistingBudgetWithUpdateOption_UpdateAmount() {
            // Given
            Budget tithe = existingBudget(1L, BudgetType.INCOME, "십일조", 1000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "십일조", null, 3000L)))
                    .updateExistingAmounts(true)
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(tithe), _ -> false);

            // Then
            assertThat(find(analysis, "십일조"))
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getAction,
                            BudgetImportItemDto::getAmount, BudgetImportItemDto::isSelectable)
                    .containsExactly(ImportItemStatus.OK, BudgetImportAction.UPDATE_AMOUNT, 3000L, true);
        }

        @Test
        @DisplayName("이미 있는 상위 예산 아래의 없는 하위 예산만 추가 대상으로 본다")
        void analyze_NewChildUnderExistingParent_OnlyChildIsCreated() {
            // Given
            Budget offering = existingBudget(1L, BudgetType.INCOME, "헌금", null, null);
            Budget tithe = existingBudget(2L, BudgetType.INCOME, "십일조", 1000L, offering);
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "헌금", "십일조", 1000L),
                    row("수입", "헌금", "감사헌금", 300L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(offering, tithe),
                    _ -> false);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getLevelNames, BudgetImportItemDto::getAction)
                    .containsExactly(
                            tuple(List.of("헌금"), BudgetImportAction.NONE),
                            tuple(List.of("헌금", "십일조"), BudgetImportAction.NONE),
                            tuple(List.of("헌금", "감사헌금"), BudgetImportAction.CREATE));
        }

        @Test
        @DisplayName("내역이 없는 기존 말단 예산에 하위 예산이 생기면 금액이 바뀐다고 경고한다")
        void analyze_ChildUnderExistingLeafWithoutRecords_WarnsOnParent() {
            // Given
            Budget offering = existingBudget(1L, BudgetType.INCOME, "헌금", 5000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "헌금", "십일조", 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(offering), _ -> false);

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getAction)
                    .containsExactly(
                            tuple(ImportItemStatus.WARNING, BudgetImportAction.NONE),
                            tuple(ImportItemStatus.OK, BudgetImportAction.CREATE));
        }

        @Test
        @DisplayName("내역이 있는 기존 말단 예산 아래로 들어가는 예산은 오류다")
        void analyze_ChildUnderExistingLeafWithRecords_Error() {
            // Given
            Budget offering = existingBudget(1L, BudgetType.INCOME, "헌금", 5000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "헌금", "십일조", 1000L)))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(offering),
                    budgetId -> budgetId.equals(1L));

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getStatus, BudgetImportItemDto::getMessage)
                    .containsExactly(
                            tuple(ImportItemStatus.ERROR, "수입/지출 내역이 있는 예산이라 하위 예산을 추가할 수 없습니다."),
                            tuple(ImportItemStatus.ERROR, "상위 예산에 오류가 있어 가져올 수 없습니다."));
        }

        @Test
        @DisplayName("같은 입력과 같은 데이터로 분석하면 요약 값이 같다")
        void analyze_SameInput_ProducesSameSignature() {
            // Given
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "십일조", null, 1000L)))
                    .build();

            // When
            String first = analyze(request).getSignature();
            String second = analyze(request).getSignature();

            // Then
            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("그 사이에 같은 예산이 등록되어 분석 결과가 달라지면 요약 값도 달라진다")
        void analyze_DataChanged_ProducesDifferentSignature() {
            // Given
            Budget tithe = existingBudget(1L, BudgetType.INCOME, "십일조", 1000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(row("수입", "십일조", null, 1000L)))
                    .build();

            // When
            String before = analyze(request).getSignature();
            String after = BudgetImportAnalyzer.analyze(request, List.of(tithe), _ -> false).getSignature();

            // Then
            assertThat(after).isNotEqualTo(before);
        }

        @Test
        @DisplayName("예산명, 구분, 금액의 열을 지정하지 않으면 분석하지 않는다")
        void analyze_MissingColumnMapping_Failure() {
            // Given
            BudgetImportRequestDto request = BudgetImportRequestDto.builder()
                    .rows(List.of())
                    .fixedType(BudgetType.INCOME)
                    .amountColumn(1)
                    .build();

            // When & Then
            assertThatThrownBy(() -> analyze(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명이 있는 열을 지정해주세요.");
        }
    }

    @Nested
    @DisplayName("isSelectable 테스트")
    class IsSelectableTest {

        @Test
        @DisplayName("분석 결과에서 고를 수 있는 건은 추가하거나 갱신할 것이 있고 오류가 없는 건뿐이다")
        void isSelectable_MixedItems_OnlyActionableWithoutError() {
            // Given
            Budget tithe = existingBudget(1L, BudgetType.INCOME, "십일조", 1000L, null);
            BudgetImportRequestDto request = levelRequest(List.of(
                    row("수입", "십일조", null, 1000L),
                    row("수입", "감사헌금", null, 300L),
                    row("수입", "기타", null, "천원")))
                    .build();

            // When
            BudgetImportAnalysisDto analysis = BudgetImportAnalyzer.analyze(request, List.of(tithe), _ -> false);

            // Then
            Set<Integer> selectableRows = Set.copyOf(analysis.getItems()
                    .stream()
                    .filter(BudgetImportItemDto::isSelectable)
                    .map(BudgetImportItemDto::getRowNumber)
                    .toList());
            assertThat(selectableRows).containsExactly(3);
        }
    }

}
