package org.church.steward.ui.component.field;

import javax.swing.JComponent;

/**
 * 여러 컴포넌트로 이루어진 입력란(날짜 피커, 예산 선택 박스, 관계자 칩 입력란 등). 키보드로 입력란을 옮겨 다닐 때 포커스를 줄
 * 컴포넌트를 알려준다.
 */
public interface CompositeInputField {

    /**
     * 포커스를 받아 키 입력을 처리하는 컴포넌트를 반환한다.
     */
    JComponent getInputComponent();
}
