package org.church.steward.ui.view.financialrecord.contact;

/**
 * {@link ContactChipField}에 입력된 텍스트로 관계자 자동 완성 목록을 보여주는 역할. 목록에서 관계자가 선택되면 구현체가
 * {@link ContactChipField#addContact}를 호출한다.
 */
public interface ContactSuggester {

    /**
     * 키워드에 해당하는 자동 완성 목록을 조회해서 보여준다.
     *
     * @param keyword 앞뒤 공백이 제거된, 비어 있지 않은 입력 텍스트
     */
    void suggest(String keyword);

    /**
     * 자동 완성 목록을 닫는다.
     */
    void hide();

    /**
     * 자동 완성 목록이 화면에 보이는 상태인지 반환한다.
     */
    boolean isShowing();

    /**
     * 입력 필드에서 눌린 키를 자동 완성 목록 조작(이동, 선택, 닫기)에 사용한다.
     *
     * @param keyCode {@link java.awt.event.KeyEvent}의 키 코드
     * @return 키를 처리했으면 true. 이 경우 입력 필드는 해당 키를 무시한다.
     */
    boolean handleKey(int keyCode);
}
