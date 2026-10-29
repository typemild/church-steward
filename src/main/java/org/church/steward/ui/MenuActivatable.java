package org.church.steward.ui;

/**
 * 메뉴가 선택되었을 때의 동작을 정의하는 인터페이스다.
 */
public interface MenuActivatable {
    /**
     * 메뉴가 선택되었을 때 호출된다. 구현체는 이 메소드 내에서 데이터를 새로 고칠지, 혹은 최초 1회만 로딩할지를 스스로 결정할 수 있다.
     */
    void onMenuSelected();
}
