package org.church.steward.ui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;

import org.church.steward.ui.component.field.CompositeInputField;

/**
 * 다이얼로그의 키보드 동작과 닫는 절차를 맡는다.
 * <ul>
 * <li>Esc, 창의 닫기 버튼, 취소 버튼은 모두 {@link #requestClose()}를 거친다. 취소 버튼이 꺼져 있으면 닫지
 * 않고, 입력한 내용이 바뀌었으면 닫을지 묻는다.</li>
 * <li>입력란에서 Enter를 누르면 다음 입력란으로 옮기고, 마지막 입력란이면 주 버튼을 누른다. 꺼진 입력란은 건너뛴다.
 * 여러 줄 입력란({@link JTextArea})도 같으며, 줄바꿈은 Shift+Enter로 하고 Tab은 포커스를 옮긴다.</li>
 * <li>이 다이얼로그가 띄운 창(예산 선택 다이얼로그, 날짜 피커의 달력 등)이 닫히면 마지막으로 쓰던 입력란에 포커스를 되돌려
 * Enter와 Esc를 이어서 쓸 수 있게 한다.</li>
 * <li>등록한 입력란에는 포커스 테두리를 보인다({@link FocusIndicator}).</li>
 * </ul>
 * 입력란은 화면의 순서대로 등록해야 한다.
 */
public final class DialogKeyHandler {

    private static final String CLOSE_ACTION_KEY = "dialogKeyHandler.close";
    private static final String ENTER_ACTION_KEY = "dialogKeyHandler.enter";
    private static final String PRIMARY_ACTION_KEY = "dialogKeyHandler.primary";
    private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final KeyStroke SHIFT_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);

    /**
     * 등록한 입력란.
     *
     * @param component 화면에 배치한 컴포넌트
     * @param target    그 안에서 포커스를 받는 컴포넌트. 컴포넌트 하나로 된 입력란이면 component와 같다.
     */
    private record InputField(JComponent component, JComponent target) {

        boolean isAvailable() {
            return component.isEnabled() && target.isEnabled() && target.isShowing() && target.isFocusable();
        }
    }

    private final JDialog dialog;
    private final List<InputField> fields = new ArrayList<>();

    private AbstractButton cancelButton;
    private AbstractButton primaryButton;
    private Supplier<?> stateSupplier;
    private Object initialState;
    private InputField lastFocusedField;

    private DialogKeyHandler(JDialog dialog) {
        this.dialog = dialog;

        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ESCAPE, CLOSE_ACTION_KEY);
        dialog.getRootPane()
                .getActionMap()
                .put(CLOSE_ACTION_KEY, new AbstractAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        requestClose();
                    }
                });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // 초기값은 다이얼로그를 만든 뒤 띄우기 전에 채우므로, 띄운 시점의 값을 기준으로 삼는다.
                if (stateSupplier != null) {
                    initialState = stateSupplier.get();
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                requestClose();
            }
        });
        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                Window opposite = e.getOppositeWindow();
                // 메시지 창이 닫힌 뒤에는 그 창을 띄운 코드가 포커스를 옮기므로(입력 오류가 난 입력란 등) 건드리지 않는다.
                if (opposite == null || opposite.getOwner() != dialog || isMessageDialog(opposite)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> restoreFieldFocus());
            }
        });
    }

    /**
     * 다이얼로그에 Esc와 닫기 버튼 처리를 붙인다. 입력란과 버튼은 반환된 객체에 이어서 등록한다.
     */
    public static DialogKeyHandler install(JDialog dialog) {
        return new DialogKeyHandler(dialog);
    }

    /**
     * 취소 버튼과 주 버튼을 지정한다. 취소 버튼에는 {@link #requestClose()}를 붙이므로, 취소 버튼에 따로 닫는 동작을
     * 붙이지 않아야 한다. 버튼을 새로 만들었으면 다시 호출한다.
     *
     * @param cancelButton  취소 버튼. 꺼져 있는 동안은 Esc와 창의 닫기 버튼으로도 닫히지 않는다. 없으면 null
     * @param primaryButton 마지막 입력란에서 Enter를 누르면 누를 버튼. 없으면 null
     */
    public DialogKeyHandler setButtons(AbstractButton cancelButton, AbstractButton primaryButton) {
        this.cancelButton = cancelButton;
        this.primaryButton = primaryButton;
        if (cancelButton != null) {
            cancelButton.addActionListener(_ -> requestClose());
        }
        return this;
    }

    /**
     * 입력란을 등록한다. Enter를 누르면 다음 입력란으로 옮기고, 마지막 입력란이면 주 버튼을 누른다.
     *
     * @param field 화면에 배치한 입력란. {@link CompositeInputField}이면 그 입력 컴포넌트가 포커스를 받는다.
     */
    public DialogKeyHandler addField(JComponent field) {
        return addField(field, null);
    }

    /**
     * 입력란을 등록한다.
     *
     * @param field       화면에 배치한 입력란. {@link CompositeInputField}이면 그 입력 컴포넌트가 포커스를
     *                    받는다.
     * @param enterAction Enter를 눌렀을 때 먼저 실행할 동작. true를 반환하면 Enter를 처리한 것으로 보고 다음
     *                    입력란으로 옮기지 않는다.
     */
    public DialogKeyHandler addField(JComponent field, BooleanSupplier enterAction) {
        InputField inputField = register(field);
        if (inputField.target() instanceof JTextArea textArea) {
            useTabAndShiftEnter(textArea);
        }
        inputField.target()
                .getInputMap(JComponent.WHEN_FOCUSED)
                .put(ENTER, ENTER_ACTION_KEY);
        inputField.target()
                .getActionMap()
                .put(ENTER_ACTION_KEY, new EnterAction(inputField, enterAction));
        return this;
    }

    /**
     * 다이얼로그 어디서든 Enter를 누르면 주 버튼을 누르게 한다. 입력란 없이 목록에서 고르기만 하는 다이얼로그(예산 선택 등)에
     * 쓰며, 입력란을 등록한 다이얼로그에는 쓰지 않는다.
     *
     * @param tables Enter를 자신이 쓰는(다음 행으로 이동, 또는 ListTable의 행 더블클릭 처리기) 표. 이 표에
     *               포커스가 있을 때 누른 Enter도 주 버튼을 누르게 바꾼다. 같은 키를 덮어쓰므로 표에 행 더블클릭
     *               처리기를 등록한 뒤에 호출해야 한다.
     */
    public DialogKeyHandler pressPrimaryOnEnter(JComponent... tables) {
        AbstractAction pressPrimary = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // 꺼진 버튼은 doClick해도 아무 일도 일어나지 않는다.
                if (primaryButton != null) {
                    primaryButton.doClick();
                }
            }
        };

        dialog.getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ENTER, PRIMARY_ACTION_KEY);
        dialog.getRootPane()
                .getActionMap()
                .put(PRIMARY_ACTION_KEY, pressPrimary);
        // 표의 Enter는 표에 포커스가 있을 때 창 전체의 키보다 먼저 처리되므로 표에서 직접 바꾼다.
        for (JComponent table : tables) {
            table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .put(ENTER, PRIMARY_ACTION_KEY);
            table.getActionMap()
                    .put(PRIMARY_ACTION_KEY, pressPrimary);
        }
        return this;
    }

    /**
     * 입력한 내용이 바뀌었는지 판단할 값을 지정한다. 다이얼로그를 띄운 시점의 값과 닫으려는 시점의 값이 다르면
     * 닫기 전에 확인을 받는다.
     *
     * @param stateSupplier 입력값을 담은 객체를 만드는 함수. 반환값은 {@link Object#equals}로 비교한다.
     */
    public DialogKeyHandler trackChanges(Supplier<?> stateSupplier) {
        this.stateSupplier = stateSupplier;
        return this;
    }

    /**
     * 다이얼로그를 닫는다. 취소 버튼이 꺼져 있으면(작업 중) 닫지 않고, 입력한 내용이 바뀌었으면 닫을지 묻는다.
     */
    public void requestClose() {
        if (cancelButton != null && !cancelButton.isEnabled()) {
            return;
        }
        if (isModified()) {
            int answer = JOptionPane.showConfirmDialog(dialog, "저장하지 않은 내용이 있습니다.\n저장하지 않고 닫을까요?",
                    dialog.getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dialog.dispose();
    }

    private boolean isModified() {
        return stateSupplier != null && !Objects.equals(initialState, stateSupplier.get());
    }

    private InputField register(JComponent field) {
        JComponent target = field instanceof CompositeInputField composite ? composite.getInputComponent() : field;
        InputField inputField = new InputField(field, target);
        fields.add(inputField);

        // 입력/수정 다이얼로그의 입력란에만 포커스 테두리를 보인다. 테두리는 입력란에 따라 바깥(관계자 입력란)이나
        // 안쪽(날짜 피커의 입력란)에 있으므로 둘 다 켠다.
        FocusIndicator.show(field);
        FocusIndicator.show(target);

        // 입력란 안의 버튼(날짜 피커의 달력 버튼 등)을 눌러도 그 입력란을 쓴 것으로 본다.
        trackFocus(field, new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                lastFocusedField = inputField;
            }
        });
        return inputField;
    }

    /**
     * 여러 줄 입력란도 다른 입력란처럼 Tab으로 포커스를 옮기게 하고, 줄바꿈은 Shift+Enter로 하게 한다. Enter는
     * {@link #addField}가 다음 입력란으로 옮기는 동작으로 바꾼다.
     */
    private static void useTabAndShiftEnter(JTextArea textArea) {
        // JTextArea는 Tab을 글자로 넣으려고 Ctrl+Tab만 포커스 이동 키로 쓴다.
        // null이면 창의 기본 키(Tab, Shift+Tab)를 따른다.
        textArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        textArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(SHIFT_ENTER, DefaultEditorKit.insertBreakAction);
    }

    private static void trackFocus(Component component, FocusListener listener) {
        component.addFocusListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                trackFocus(child, listener);
            }
        }
    }

    private void moveToNextField(InputField current) {
        for (int i = fields.indexOf(current) + 1; i < fields.size(); i++) {
            InputField next = fields.get(i);
            if (next.isAvailable()) {
                next.target()
                        .requestFocusInWindow();
                return;
            }
        }
        // 꺼진 버튼은 doClick해도 아무 일도 일어나지 않으므로, 저장하는 중에 다시 저장되지 않는다.
        if (primaryButton != null) {
            primaryButton.doClick();
        }
    }

    /**
     * 이 다이얼로그가 띄운 창이 닫힌 뒤, 포커스가 입력란 밖(입력란 안의 버튼 등)에 있으면 마지막으로 쓰던 입력란으로 옮긴다.
     */
    private void restoreFieldFocus() {
        if (lastFocusedField == null || !dialog.isFocused() || !lastFocusedField.isAvailable()) {
            return;
        }
        Component focusOwner = dialog.getFocusOwner();
        for (InputField field : fields) {
            if (field.target() == focusOwner) {
                return;
            }
        }
        lastFocusedField.target()
                .requestFocusInWindow();
    }

    private static boolean isMessageDialog(Window window) {
        return window instanceof JDialog messageDialog && messageDialog.getContentPane()
                .getComponentCount() == 1
                && messageDialog.getContentPane()
                        .getComponent(0) instanceof JOptionPane;
    }

    private final class EnterAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private final InputField field;
        private final BooleanSupplier enterAction;

        EnterAction(InputField field, BooleanSupplier enterAction) {
            this.field = field;
            this.enterAction = enterAction;
        }

        /**
         * 콤보박스의 목록이 열려 있으면 Enter는 항목 선택이므로 콤보박스에 맡긴다. 꺼진 동작은 다음 키 바인딩으로 넘어간다.
         */
        @Override
        public boolean isEnabled() {
            return !(field.target() instanceof JComboBox<?> comboBox && comboBox.isPopupVisible());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // 한글을 조합하는 중에 Enter를 누르면 조합한 글자가 확정되기 전에 이 동작이 실행될 수 있다.
            // 글자가 확정된 뒤에 옮기도록 한 박자 늦춘다.
            SwingUtilities.invokeLater(() -> {
                if (enterAction != null && enterAction.getAsBoolean()) {
                    return;
                }
                moveToNextField(field);
            });
        }
    }
}
