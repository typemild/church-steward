package org.church.steward.ui.util;

import javax.swing.Icon;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 작업이 끝났음을 잠시 알리는 표시의 공용 모양. 복사 버튼이 잠시 체크로 바뀌는
 * 것({@code IconButton#showSuccess})과 설정을 저장한 뒤 입력란 옆에 체크를 띄우는 것이 같은 아이콘, 색, 시간을
 * 쓰도록 한곳에 둔다.
 */
public final class SuccessFeedback {

    /** 완료 표시를 보여 주는 시간(ms) */
    public static final int DURATION_MILLIS = 2000;

    private static final FontAwesomeSolid ICON = FontAwesomeSolid.CHECK;

    private SuccessFeedback() {
    }

    /**
     * 완료 표시 아이콘(초록 체크)을 만든다.
     *
     * @param size 아이콘 크기(px)
     */
    public static Icon createIcon(int size) {
        return FontIcon.of(ICON, size, UIColors.SUCCESS);
    }
}
