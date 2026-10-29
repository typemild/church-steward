package org.church.steward.ui.view.financialrecord.contact;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.im.InputContext;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.CompositeInputField;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import com.formdev.flatlaf.ui.FlatTextBorder;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

/**
 * 관계자 선택 박스. 텍스트를 입력하면 {@link ContactSuggester}가 자동 완성 목록을 보여주고, 목록에서 선택한 관계자는
 * 칩(chip)으로 바뀐다. 입력 커서는 항상 마지막 칩 뒤에 위치하며, 칩으로 확정되지 않은 텍스트는 포커스를 잃으면 사라진다.
 */
public class ContactChipField extends JPanel implements CompositeInputField {

    private static final long serialVersionUID = 1L;

    private static final int CHIP_GAP = 4;
    private static final int MIN_INPUT_WIDTH = 60;
    private static final int PREFERRED_WIDTH = 150;
    private static final int AUTOCOMPLETE_DELAY_MS = 200;

    private final StyledTextField inputField = new StyledTextField(ComponentSize.MEDIUM);
    private final List<ContactChip> chips = new ArrayList<>();
    private final Timer autocompleteTimer;

    private ContactSuggester suggester;
    /** 입력할 수 있는 최대 관계자 수. 0이면 제한이 없다. */
    private int maxContacts = 0;
    /** 가장 최근에 자동 완성을 요청한 키워드 */
    private String lastSuggestedKeyword = "";
    /** 칩을 보여주기만 하고 입력이나 삭제를 받지 않는지 여부 */
    private boolean readOnly = false;

    public ContactChipField() {
        // 칩과 입력 필드는 doLayout에서 직접 배치한다.
        setLayout(null);
        setBackground(UIColors.TEXT_FIELD_BACKGROUND);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        // 다른 입력란과 같은 FlatLaf 입력란 테두리를 쓴다. 포커스 테두리는 입력/수정 다이얼로그에서만 보이며,
        // DialogKeyHandler가 켜면 안쪽 입력 필드에 포커스가 있을 때 포커스 색으로 그린다.
        // 테두리의 둥근 모서리 밖으로 배경이 보이지 않도록 배경은 paintComponent에서 직접 그린다.
        setOpaque(false);
        FocusIndicator.hide(this);
        setBorder(BorderFactory.createCompoundBorder(
                new FlatTextBorder(),
                BorderFactory.createEmptyBorder(
                        Math.round(UIFonts.FONT_SIZE_BASE * 0.4f),
                        Math.round(UIFonts.FONT_SIZE_BASE * 0.46f),
                        Math.round(UIFonts.FONT_SIZE_BASE * 0.4f),
                        Math.round(UIFonts.FONT_SIZE_BASE * 0.46f))));

        // 테두리와 배경은 이 패널이 대신 그린다.
        inputField.setBorder(BorderFactory.createEmptyBorder());
        inputField.setOpaque(false);
        add(inputField);

        autocompleteTimer = new Timer(AUTOCOMPLETE_DELAY_MS, _ -> requestSuggestions());
        autocompleteTimer.setRepeats(false);

        setupEvents();
    }

    /**
     * 자동 완성 목록을 담당할 구현체를 설정한다.
     */
    public void setSuggester(ContactSuggester suggester) {
        this.suggester = suggester;
    }

    private void setupEvents() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        inputField.getDocument()
                .addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        autocompleteTimer.restart();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        autocompleteTimer.restart();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        autocompleteTimer.restart();
                    }
                });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }
        });

        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // 테두리 색을 바꾸기 위해 패널 전체를 다시 그린다.
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
                if (e.isTemporary()) {
                    return;
                }
                // 칩으로 확정되지 않은 텍스트는 버린다.
                discardInput();
            }
        });
    }

    /**
     * 칩을 보여주기만 하고 입력이나 삭제를 받지 않게 한다. 상세 보기 다이얼로그에서 쓴다. 입력 필드는 포커스를 받지만 글자를 넣을 수 없고,
     * 칩은 앞에서부터 보이도록 입력 필드 자리를 남기지 않는다.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        inputField.setEditable(!readOnly);
        setCursor(Cursor.getPredefinedCursor(readOnly ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
        revalidate();
        repaint();
    }

    private void handleKeyPressed(KeyEvent e) {
        // 입력 필드가 편집 불가여도 키 이벤트는 오므로, Backspace로 칩이 지워지지 않게 여기서 막는다.
        if (readOnly) {
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (inputField.getText()
                    .isEmpty() && !chips.isEmpty()) {
                removeLastChip();
                e.consume();
            }
            return;
        }

        if (suggester == null) {
            return;
        }
        if (suggester.handleKey(e.getKeyCode())) {
            e.consume();
            return;
        }

        // 목록이 닫힌 상태에서 칩으로 확정되지 않은 텍스트가 남아 있으면, Enter는 다음 입력란으로 옮기지 않고 목록을 다시 띄운다.
        if (e.getKeyCode() == KeyEvent.VK_ENTER && !getInputText().isEmpty()) {
            autocompleteTimer.stop();
            requestSuggestions();
            e.consume();
        }
    }

    private void requestSuggestions() {
        if (suggester == null) {
            return;
        }

        String keyword = getInputText();
        if (keyword.isEmpty()) {
            suggester.hide();
            return;
        }
        // 한글 조합 중에 방향키를 누르면 IME가 글자를 확정하면서 텍스트는 그대로인데 문서 변경 이벤트가 발생한다.
        // 이때 다시 조회하면 목록이 깜빡이고 선택 항목이 처음으로 돌아가므로, 같은 키워드의 목록이 떠 있으면 건너뛴다.
        if (suggester.isShowing() && keyword.equals(lastSuggestedKeyword)) {
            return;
        }
        lastSuggestedKeyword = keyword;
        suggester.suggest(keyword);
    }

    /**
     * 입력 중인 텍스트를 지우고 자동 완성 목록을 닫는다.
     */
    private void discardInput() {
        autocompleteTimer.stop();
        if (suggester != null) {
            suggester.hide();
        }
        inputField.setText("");
        autocompleteTimer.stop(); // setText로 다시 시작된 타이머를 멈춘다.
    }

    /**
     * 칩으로 확정되지 않은, 입력 중인 텍스트를 앞뒤 공백 없이 반환한다.
     */
    public String getInputText() {
        return inputField.getText()
                .trim();
    }

    /**
     * 입력 필드에 텍스트를 채운다. 직접 입력한 것과 같이 자동 완성 목록이 뜬다. 찾을 이름을 미리 알고 있을 때(가져오기의 관계자 매핑)
     * 사용하며, 입력 필드가 포커스를 가진 뒤에 호출해야 한다.
     */
    public void setInputText(String text) {
        inputField.setText(text);
    }

    /**
     * 조합 중인 글자(한글 등)를 확정한다. 자동 완성 목록은 포커스를 받지 않으므로 목록을 마우스로 눌러도 조합이 끝나지 않는다. 조합이 남은
     * 채로 다른 창을 띄우면 창을 닫고 돌아왔을 때 IME 상태가 어긋나 다이얼로그가 마우스 클릭을 받지 못하므로, 창을 띄우기 전에 호출해야
     * 한다.
     */
    public void commitComposition() {
        InputContext inputContext = inputField.getInputContext();
        if (inputContext != null) {
            inputContext.endComposition();
        }
    }

    public boolean isInputFocusOwner() {
        return inputField.isFocusOwner();
    }

    public void requestInputFocus() {
        inputField.requestFocusInWindow();
    }

    /**
     * 이름을 입력하는 필드를 반환한다. 자동 완성 목록이 떠 있는 동안의 Enter와 Esc는 이 필드가 먼저 처리한다.
     */
    @Override
    public JComponent getInputComponent() {
        return inputField;
    }

    private boolean containsContact(Long contactId) {
        return chips.stream()
                .anyMatch(chip -> chip.getContactId()
                        .equals(contactId));
    }

    /**
     * 관계자를 현재 이름의 칩으로 추가하고 입력 중인 텍스트를 지운다. 자동 완성으로 새로 입력할 때 사용한다. 이미 추가된 관계자는 무시하며,
     * 최대 관계자 수에 도달한 상태면 가장 오래된 칩을 빼고 추가한다.
     */
    public void addContact(Long contactId, ContactType type, String name, String remarks) {
        ContactChip chip = addChip(contactId, name);
        if (chip != null) {
            chip.setCurrentInfo(type, name, remarks);
        }
    }

    /**
     * 수입/지출 내역에 기록된 관계자를 기록 당시 이름의 칩으로 추가한다. 툴팁에 보여줄 현재 정보는
     * {@link #updateCurrentInfo(Long, ContactType, String, String)}로 따로 설정해야 한다.
     */
    public void addRecordedContact(Long contactId, String recordedName) {
        addChip(contactId, recordedName);
    }

    /**
     * 칩으로 입력된 관계자의 툴팁을 현재 정보로 갱신한다. 칩에 표시된 이름은 바꾸지 않으며, 입력되지 않은 관계자는 무시한다.
     */
    public void updateCurrentInfo(Long contactId, ContactType type, String currentName, String remarks) {
        for (ContactChip chip : chips) {
            if (chip.getContactId()
                    .equals(contactId)) {
                chip.setCurrentInfo(type, currentName, remarks);
            }
        }
    }

    /**
     * @return 추가한 칩. 이미 추가된 관계자면 null.
     */
    private ContactChip addChip(Long contactId, String chipName) {
        discardInput();

        if (containsContact(contactId)) {
            return null;
        }

        while (maxContacts > 0 && chips.size() >= maxContacts) {
            remove(chips.removeFirst());
        }

        ContactChip chip = new ContactChip(contactId, chipName);
        chips.add(chip);
        add(chip);
        revalidate();
        repaint();
        return chip;
    }

    private void removeLastChip() {
        remove(chips.removeLast());
        revalidate();
        repaint();
    }

    /**
     * 입력할 수 있는 최대 관계자 수를 설정한다. 0이면 제한이 없다. 이미 입력된 관계자가 더 많으면 앞쪽부터 지운다.
     */
    public void setMaxContacts(int maxContacts) {
        this.maxContacts = maxContacts;
        while (maxContacts > 0 && chips.size() > maxContacts) {
            remove(chips.removeFirst());
        }
        revalidate();
        repaint();
    }

    /**
     * 모든 칩과 입력 중인 텍스트를 지운다.
     */
    public void clear() {
        for (ContactChip chip : chips) {
            remove(chip);
        }
        chips.clear();
        discardInput();
        revalidate();
        repaint();
    }

    /**
     * 칩으로 확정된 관계자의 ID 목록을 반환한다. 입력 중인 텍스트는 포함되지 않는다.
     */
    public List<Long> getContactIds() {
        return chips.stream()
                .map(ContactChip::getContactId)
                .toList();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int height = inputField.getPreferredSize().height + insets.top + insets.bottom;
        return new Dimension(PREFERRED_WIDTH, height);
    }

    @Override
    public Dimension getMinimumSize() {
        Insets insets = getInsets();
        int width = MIN_INPUT_WIDTH + insets.left + insets.right;
        return new Dimension(width, getPreferredSize().height);
    }

    /**
     * 칩들을 왼쪽부터 한 줄로 배치하고 입력 필드를 그 뒤에 둔다. 칩이 많아 한 줄에 다 들어가지 않으면 앞쪽 칩을 가려서 입력 필드가 항상
     * 보이도록 한다. 읽기 전용이면 입력 필드 자리를 남기지 않고 앞쪽 칩부터 보인다.
     */
    @Override
    public void doLayout() {
        Insets insets = getInsets();
        int availableWidth = getWidth() - insets.left - insets.right;
        int availableHeight = getHeight() - insets.top - insets.bottom;

        int chipsWidth = 0;
        for (ContactChip chip : chips) {
            chipsWidth += chip.getPreferredSize().width + CHIP_GAP;
        }
        int inputWidth = Math.max(readOnly ? 0 : MIN_INPUT_WIDTH, availableWidth - chipsWidth);

        int x = insets.left;
        if (!readOnly) {
            x += Math.min(0, availableWidth - (chipsWidth + inputWidth));
        }
        for (ContactChip chip : chips) {
            Dimension chipSize = chip.getPreferredSize();
            int chipHeight = Math.min(chipSize.height, availableHeight);
            chip.setBounds(x, insets.top + (availableHeight - chipHeight) / 2, chipSize.width, chipHeight);
            x += chipSize.width + CHIP_GAP;
        }
        inputField.setBounds(x, insets.top, inputWidth, availableHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // FlatLaf의 입력란과 같이 포커스 표시 폭만큼 안쪽에 둥근 배경을 그린다.
        Graphics2D g2 = (Graphics2D) g.create();
        FlatUIUtils.setRenderingHints(g2);
        g2.setColor(getBackground());
        FlatUIUtils.paintComponentBackground(g2, 0, 0, getWidth(), getHeight(),
                UIScale.scale((float) UIManager.getInt("Component.focusWidth")),
                UIScale.scale((float) UIManager.getInt("TextComponent.arc")));
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        // 가려진 앞쪽 칩이 테두리와 안쪽 여백 위에 그려지지 않도록 그리기 영역을 제한한다.
        Insets insets = getInsets();
        Graphics clipped = g.create();
        clipped.clipRect(insets.left, insets.top, getWidth() - insets.left - insets.right,
                getHeight() - insets.top - insets.bottom);
        super.paintChildren(clipped);
        clipped.dispose();
    }
}
