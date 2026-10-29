package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import org.church.steward.domain.contact.entity.ContactType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관계자 가져오기의 입력: 파일에서 읽은 행과, 그 열들을 관계자의 어느 항목으로 읽을지에 대한 지정. 열은 0부터 세는 위치로 가리킨다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactImportRequestDto {

    /** 머리글 아래의 데이터 행. 칸의 값은 글자, 정수, 소수, 날짜 또는 null이다. */
    private List<List<Object>> rows;

    /** 첫 데이터 행이 파일에서 몇 번째 행인지 (1부터). 화면에 행 번호를 보여주는 데 쓴다. */
    private int firstRowNumber;

    /** 파일 전체의 구분. 구분을 열에서 읽으면 null */
    private ContactType fixedType;

    /** 구분("개인", "업체" 등)이 적힌 열. 파일 전체의 구분을 정했으면 null */
    private Integer typeColumn;

    /** 이름이 적힌 열 */
    private Integer nameColumn;

    /** 비고가 적힌 열. 없으면 null */
    private Integer remarksColumn;

    /** 상태("숨김"/"표시")가 적힌 열. 없으면 null이며 모두 표시로 가져온다. */
    private Integer hiddenColumn;

}
