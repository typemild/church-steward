package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예산 가져오기의 입력: 파일에서 읽은 행과, 그 열들을 예산의 어느 항목으로 읽을지에 대한 지정. 열은 0부터 세는 위치로 가리킨다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetImportRequestDto {

    /** 머리글 아래의 데이터 행. 칸의 값은 글자, 정수, 소수, 날짜 또는 null이다. */
    private List<List<Object>> rows;

    /** 첫 데이터 행이 파일에서 몇 번째 행인지 (1부터). 화면에 행 번호를 보여주는 데 쓴다. */
    private int firstRowNumber;

    /** 파일 전체의 구분. 구분을 열에서 읽으면 null */
    private BudgetType fixedType;

    /** 구분("수입"/"지출")이 적힌 열. 파일 전체의 구분을 정했으면 null */
    private Integer typeColumn;

    /** 단계별 예산명이 적힌 열들 (최상위 단계부터). 전체 경로가 한 열에 적혀 있으면 빈 목록 */
    @Builder.Default
    private List<Integer> levelColumns = List.of();

    /** "예산A > 예산B"처럼 전체 경로가 적힌 열. 단계별 열을 쓰면 null */
    private Integer pathColumn;

    /** 금액이 적힌 열 */
    private Integer amountColumn;

    /** 단계별 열에서 빈 칸(병합된 셀 포함)이 윗 행의 값을 이어받게 할지 여부 */
    private boolean fillDown;

    /** 이미 있는 예산의 금액이 파일과 다를 때 파일의 값으로 갱신할지 여부 */
    private boolean updateExistingAmounts;

}
