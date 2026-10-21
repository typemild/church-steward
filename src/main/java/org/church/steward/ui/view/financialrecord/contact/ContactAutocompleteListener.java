package org.church.steward.ui.view.financialrecord.contact;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;

/**
 * 계층형 자동 완성 목록({@link ContactAutocompletePopup})에서 일어난 사용자 동작을 받는 리스너
 */
public interface ContactAutocompleteListener {

    /** 관계자 한 명이 선택됨 */
    void onContactSelected(ContactAutocompleteDto contact);

    /** 그룹이 선택됨. 그룹의 구성원 전체를 입력 대상으로 한다. */
    void onGroupSelected(GroupInfoDto group);

    /** "신규 추가" 항목이 선택됨 */
    void onNewContactRequested();

    /** 관계자 항목의 연필(수정) 아이콘이 클릭됨 */
    void onContactEditRequested(ContactAutocompleteDto contact);

    /** 관계자 항목의 휴지통(삭제) 아이콘이 클릭됨 */
    void onContactDeleteRequested(ContactAutocompleteDto contact);

    /** 그룹 항목의 휴지통(삭제) 아이콘이 클릭됨 */
    void onGroupDeleteRequested(GroupInfoDto group);
}
