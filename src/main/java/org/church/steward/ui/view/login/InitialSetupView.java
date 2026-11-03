package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.church.steward.domain.fiscalyear.entity.FiscalYearConstraints;
import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.infrastructure.security.PasswordPolicy.Violation;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.component.label.WrappingDescriptionLabel;
import org.church.steward.ui.component.panel.CardPanel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.login.DatabaseRestoreView.RestoreAction;
import org.church.steward.ui.view.settings.fiscalyear.FiscalYearPeriodField;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 최초 실행 때 프로그램의 초기 설정을 받는 화면이다. 먼저 새로 시작할지 백업에서 복원할지 고르고, 새로 시작하면 관리자 계정, 주간
 * 결산 요일, 첫 회계 연도를 차례로 입력받는다.
 * <p>
 * 시작 방법 선택은 세 단계 앞의 갈림길이므로 단계 표시에 넣지 않고, 그 화면에서는 단계 표시를 숨긴다. 1단계의 "이전"으로 돌아올 수
 * 있다. "백업에서 복원"도 새 창을 띄우지 않고 이 창 안의 화면으로 바꾸며(입력부는 예약 복원 창과 함께 쓰는
 * {@link RestoreFormPanel}), "이전"으로 선택 화면에 돌아온다.
 * <p>
 * 세 단계는 맨 위에 단계 표시({@link SetupStepIndicator})를 두고, 단계마다 제목, 할 일을 알리는 안내 글, 입력란을 담은 카드를 차례로
 * 쌓는다. 단계 사이를 오가도 입력란을 그대로 두므로 입력값이 유지된다.
 * <p>
 * 초기화는 마지막 단계에서 "완료"를 누를 때 한 번에 실행한다. 데이터베이스 파일을 만든 뒤에 실패하면 되돌릴 수 없으므로, 각 단계의
 * 입력값은 다음 단계로 넘어가기 전에 화면에서 검증한다. 중간에 창을 닫으면 아무것도 만들지 않고 취소한 것으로 반환한다.
 */
@Slf4j
public class InitialSetupView extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String ADMIN_ID = "관리자";

    /** 결산 요일의 순서. 프로그램 설정 화면과 같이 일요일부터 적는다. */
    private static final DayOfWeek[] DAYS = { DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY };

    private static final String[] STEP_NAMES = { "관리자 계정", "주간 결산 요일", "회계 연도" };
    private static final int LAST_STEP = STEP_NAMES.length - 1;
    /** 세 단계 앞의 시작 방법 선택. 단계 번호가 아니므로 단계 표시에는 나오지 않는다. */
    private static final int CHOICE_STEP = -1;
    private static final String CHOICE_CARD = "시작 방법";
    /** 시작 방법 선택에서 고른 백업에서 복원. 세 단계와 다른 길이므로 단계 표시에 나오지 않는다. */
    private static final int RESTORE_STEP = -2;
    private static final String RESTORE_CARD = "백업에서 복원";

    private static final int WINDOW_WIDTH = 450;
    /** 높이를 재려고 처음 배치할 때 주는 높이. 어느 단계 화면보다 커야 한다. */
    private static final int MEASURE_HEIGHT = 1000;

    /** 단계 표시와 그 아래 구분선 사이의 간격 */
    private static final int HEADER_BOTTOM_GAP = 12;
    /** 시작 방법 선택 버튼 사이의 간격 */
    private static final int CHOICE_GAP = 12;
    /** 시작 방법 선택 화면 위의 프로그램 아이콘 크기와, 아이콘과 환영 문구 사이의 간격 */
    private static final int WELCOME_ICON_SIZE = 72;
    private static final int WELCOME_ICON_GAP = 16;
    /** 환영 안내와 선택 버튼 사이의 간격 */
    private static final int WELCOME_GAP = 28;
    /** 제목과 안내 글 사이의 간격 */
    private static final int TITLE_GAP = 6;
    /** 안내 글과 카드, 카드와 주의사항 사이의 간격 */
    private static final int SECTION_GAP = 16;
    /** 카드 안의 한 항목(레이블, 입력란, 설명) 사이의 간격 */
    private static final int FIELD_GAP = 16;
    /** 레이블과 입력란, 입력란과 설명 사이의 간격 */
    private static final int LABEL_GAP = 6;
    /** 레이블을 입력란 왼쪽에 둘 때 그 사이의 간격 */
    private static final int SIDE_LABEL_GAP = 12;
    /** 카드 내용 둘레의 여백. 설정 화면의 카드와 같다. */
    private static final int CARD_PADDING = 20;

    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final String ENTER_ACTION_KEY = "initialSetupView.enter";

    /**
     * 최초 실행 화면에서 입력받은 값. 비밀번호는 {@link PasswordPolicy#normalize}로 정리한 값이다.
     */
    public record SetupInput(String adminId, String adminPassword, DayOfWeek weeklyClosingDay, String fiscalYearName,
            LocalDate fiscalYearStartDate, LocalDate fiscalYearEndDate) {
    }

    private final SetupStepIndicator stepIndicator = new SetupStepIndicator(STEP_NAMES);
    /** 단계 표시와 그 아래 구분선. 시작 방법 선택에서는 숨긴다. */
    private final JPanel headerPanel = new JPanel(new BorderLayout());
    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepPanel = new JPanel(stepLayout);

    // 시작 방법 선택
    private final SetupChoiceButton newStartButton = new SetupChoiceButton(FontAwesomeSolid.PLAY_CIRCLE, "새로 시작",
            "청지기를 처음 사용하시는 경우 선택해 주세요.");
    private final SetupChoiceButton restoreChoiceButton = new SetupChoiceButton(FontAwesomeSolid.HISTORY, "백업에서 복원",
            "기존 백업 파일로부터 데이터를 되살립니다.\n백업한 시점의 관리자 비밀번호가 필요합니다.");
    /** 화면별 내용. 화면마다 창 높이를 잴 때 쓴다. */
    private JPanel choiceContent;
    private JPanel restoreContent;
    private final List<JPanel> stepContents = new ArrayList<>();
    /** 시작 방법 선택, 복원, 세 단계의 창 높이. {@link #measureWindowHeights()}가 정한다. */
    private int choiceWindowHeight;
    private int restoreWindowHeight;
    private int stepWindowHeight;
    // 백업에서 복원
    private final RestoreFormPanel restoreForm;

    // 1단계: 관리자 계정
    private final ValidatedPasswordField passwordField = new ValidatedPasswordField(ComponentSize.MEDIUM);
    private final ValidatedPasswordField confirmPasswordField = new ValidatedPasswordField(ComponentSize.MEDIUM);
    // 2단계: 주간 결산 요일
    private final StyledComboBox<DayOfWeek> weeklyClosingDayCombo = new StyledComboBox<>(DAYS,
            ComponentSize.MEDIUM);
    // 3단계: 회계 연도
    private final StyledTextField fiscalYearNameField = new StyledTextField(ComponentSize.MEDIUM);
    private final FiscalYearPeriodField fiscalYearPeriodField = new FiscalYearPeriodField(ComponentSize.MEDIUM);

    private final JProgressBar progressBar = new JProgressBar();
    /** 진행 표시와 버튼 줄. 버튼이 없는 시작 방법 선택에서는 숨긴다(간격만 남지 않게). */
    private final JPanel bottomPanel = new JPanel(new BorderLayout(0, 15));
    private final JPanel navigationPanel = new JPanel(new GridLayout(1, 0, 12, 0));
    private final SecondaryButton previousButton = new SecondaryButton("이전", ComponentSize.LARGE);
    private final PrimaryButton nextButton = new PrimaryButton("다음", ComponentSize.LARGE);
    private Consumer<SetupInput> doSetup;

    /** 지금 보이는 단계(0부터). 시작 방법 선택이면 {@link #CHOICE_STEP}, 복원이면 {@link #RESTORE_STEP} */
    private int step = CHOICE_STEP;
    private boolean isInitializedSuccessfully = false;
    /** 초기화나 복원을 하는 중인지 여부. 그동안은 창을 닫을 수 없다. */
    private boolean busy = false;

    /**
     * @param backupManager 복원할 백업 파일의 정보를 읽는 데 쓴다
     * @param restoreAction 백업에서 복원과 그 기록을 수행하는 동작. 복원하면 초기화가 끝난 것으로 본다.
     */
    public InitialSetupView(BackupManager backupManager, RestoreAction restoreAction) {
        // 0. 다이얼로그 설정
        setModal(true);
        restoreForm = new RestoreFormPanel(backupManager, restoreAction);
        getContentPane().setBackground(UIColors.BACKGROUND);

        JOptionPane.showMessageDialog(this, "프로그램 최초 실행을 감지했습니다.\n초기 설정을 진행합니다.");

        // 1. 컴포넌트 초기화
        weeklyClosingDayCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DayOfWeek day) {
                    setText(day.getDisplayName(TextStyle.FULL, Locale.KOREAN));
                }
                return this;
            }
        });
        weeklyClosingDayCombo.setSelectedItem(DayOfWeek.SUNDAY);

        // 기본값은 예전에 자동으로 만들던 회계 연도와 같다(이름 "{올해}년", 기간 올해 1월 1일 ~ 12월 31일).
        int year = LocalDate.now()
                .getYear();
        fiscalYearNameField.setMaxLength(FiscalYearConstraints.NAME_MAX_LENGTH);
        fiscalYearNameField.setText(year + "년");
        fiscalYearPeriodField.setPeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        // 입력 화면이므로 포커스 테두리를 보인다(DialogKeyHandler를 쓰지 않아 직접 켠다).
        // 날짜 피커는 테두리가 안쪽 입력란에 있으므로 그것도 켠다.
        FocusIndicator.show(passwordField);
        FocusIndicator.show(confirmPasswordField);
        FocusIndicator.show(weeklyClosingDayCombo);
        FocusIndicator.show(fiscalYearNameField);
        for (StyledDatePicker picker : List.of(fiscalYearPeriodField.getStartDatePicker(),
                fiscalYearPeriodField.getEndDatePicker())) {
            FocusIndicator.show(picker);
            FocusIndicator.show(picker.getInputComponent());
        }

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setForeground(UIColors.PRIMARY_BACKGROUND);

        // 2. 레이아웃 구성
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Step Panels
        stepPanel.setOpaque(false);
        choiceContent = createChoiceStep();
        restoreContent = createRestoreStep();
        stepContents.add(createAccountStep());
        stepContents.add(createWeeklyClosingDayStep());
        stepContents.add(createFiscalYearStep());
        stepPanel.add(choiceContent, CHOICE_CARD);
        stepPanel.add(restoreContent, RESTORE_CARD);
        for (int i = 0; i < stepContents.size(); i++) {
            stepPanel.add(stepContents.get(i), STEP_NAMES[i]);
        }

        // Bottom Panel
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        navigationPanel.setOpaque(false);
        bottomPanel.add(navigationPanel, BorderLayout.SOUTH);

        // 단계 표시와 단계 화면(제목부터)을 가로선으로 나눈다. 선과 제목 사이는 mainPanel의 세로 간격이다.
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIColors.BORDER),
                BorderFactory.createEmptyBorder(0, 0, HEADER_BOTTOM_GAP, 0)));
        headerPanel.add(stepIndicator, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(stepPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);

        // 3. 동작 연결
        // 시작 방법 선택 버튼은 Space뿐 아니라 Enter로도 누르고, ↑/↓로 선택을 옮긴다.
        onEnter(newStartButton, newStartButton::doClick);
        onEnter(restoreChoiceButton, restoreChoiceButton::doClick);
        bindArrowKeys(newStartButton, restoreChoiceButton);
        // Enter는 다음 입력란으로 옮기고, 단계의 마지막 입력란이면 "다음"(마지막 단계는 "완료")을 누른다.
        onEnter(passwordField, confirmPasswordField::requestFocusInWindow);
        onEnter(confirmPasswordField, nextButton::doClick);
        onEnter(weeklyClosingDayCombo, nextButton::doClick);
        onEnter(fiscalYearNameField, () -> fiscalYearPeriodField.getStartDatePicker()
                .getInputComponent()
                .requestFocusInWindow());
        onEnter(fiscalYearPeriodField.getStartDatePicker()
                .getInputComponent(),
                () -> fiscalYearPeriodField.getEndDatePicker()
                        .getInputComponent()
                        .requestFocusInWindow());
        onEnter(fiscalYearPeriodField.getEndDatePicker()
                .getInputComponent(), nextButton::doClick);
        // 복원 화면은 비밀번호에서 Enter를 누르면 "복원"을 누른다.
        onEnter(restoreForm.getPasswordField(), nextButton::doClick);

        newStartButton.addActionListener(_ -> showStep(0));
        restoreChoiceButton.addActionListener(_ -> showStep(RESTORE_STEP));
        // 1단계와 복원 화면의 "이전"은 시작 방법 선택으로 돌아간다.
        previousButton.addActionListener(_ -> showStep(step <= 0 ? CHOICE_STEP : step - 1));
        nextButton.addActionListener(_ -> onNext());

        // 초기화나 복원을 하는 중에 창을 닫으면 끝나기 전에 취소한 것으로 반환되므로, 그동안은 닫지 않는다.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!busy) {
                    dispose();
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                focusFirstField();
            }
        });
    }

    /**
     * 초기 설정 화면을 보여준다.
     *
     * @param doSetup 수행할 초기화 작업 로직. 마지막 단계에서 "완료"를 누르면 입력받은 값으로 백그라운드 스레드에서 실행된다.
     * @return 초기화했거나 백업에서 복원했으면 true, 창을 닫아 취소했으면 false
     */
    public boolean showSetupDialog(Consumer<SetupInput> doSetup) {
        this.doSetup = doSetup;
        isInitializedSuccessfully = false;
        this.setTitle("청지기 초기 설정");
        AppIcons.apply(this);
        this.setResizable(false);
        measureWindowHeights();
        showStep(CHOICE_STEP);
        this.setLocationRelativeTo(null);

        this.setVisible(true);
        return isInitializedSuccessfully;
    }

    /**
     * 창의 폭은 고정하고, 높이는 시작 방법 선택, 복원, 세 단계로 나눠 각각의 내용에 맞춘다. 내용의 양이 크게 달라 한 높이로 두면
     * 작은 화면에 빈 곳이 크게 남기 때문이다. 세 단계는 단계를 오갈 때 창이 들썩이지 않도록 셋 중 가장 큰 높이를 함께 쓴다.
     * <p>
     * 줄바꿈하는 글({@link WrappingDescriptionLabel})은 폭이 정해져야 높이를 알 수 있으므로, 먼저 정한 폭으로 한 번 배치한 뒤
     * 선호 높이를 잰다. 화면들은 {@link CardLayout}에 있어 숨은 화면도 함께 배치된다. 창 높이는 "화면마다 다른 둘레(단계 표시와
     * 버튼 줄이 보이는지에 따라 다르다) + 그 화면 내용의 높이"이므로, 화면마다 그 상태로 바꿔 둘레를 잰다.
     */
    private void measureWindowHeights() {
        // 창이 화면에 표시될 준비가 되어야 제목 표시줄을 포함한 창 테두리의 크기를 알 수 있다.
        pack();
        showStep(0);
        // 아직 배치되지 않은 글은 한 줄 높이로 알리므로, 배치에 넉넉한 높이를 준다.
        setSize(WINDOW_WIDTH, MEASURE_HEIGHT);
        validate();
        // 배치되며 정해진 폭으로 줄바꿈 높이가 바뀌었으므로 다시 잰다.
        invalidateTree(getContentPane());
        validate();

        int stepContentHeight = 0;
        for (JPanel content : stepContents) {
            stepContentHeight = Math.max(stepContentHeight, content.getPreferredSize().height);
        }
        stepWindowHeight = measureSurroundingHeight() + stepContentHeight;

        // 복원 화면은 단계 표시가 없고, 선택 화면은 단계 표시와 버튼 줄이 모두 없다.
        showStep(RESTORE_STEP);
        validate();
        restoreWindowHeight = measureSurroundingHeight() + restoreContent.getPreferredSize().height;

        showStep(CHOICE_STEP);
        validate();
        choiceWindowHeight = measureSurroundingHeight() + choiceContent.getPreferredSize().height;
    }

    /**
     * 지금 상태에서 창 높이 중 화면 내용을 뺀 부분(제목 표시줄, 여백, 단계 표시, 버튼 줄)의 높이.
     */
    private int measureSurroundingHeight() {
        return getPreferredSize().height - stepPanel.getPreferredSize().height;
    }

    /**
     * 지금 화면에 맞는 높이로 창을 바꾼다. 화면이 위아래로 튀지 않도록 창의 세로 가운데는 그대로 둔다.
     */
    private void applyWindowHeight() {
        // 높이를 재기 전(재는 중)에는 바꾸지 않는다.
        if (stepWindowHeight == 0) {
            return;
        }
        int height = switch (step) {
        case CHOICE_STEP -> choiceWindowHeight;
        case RESTORE_STEP -> restoreWindowHeight;
        default -> stepWindowHeight;
        };
        if (getHeight() == height) {
            return;
        }
        int y = Math.max(0, getY() - (height - getHeight()) / 2);
        setBounds(getX(), y, WINDOW_WIDTH, height);
        validate();
    }

    /**
     * 시작 방법 선택 버튼 사이를 ↑/↓로 옮기게 한다. 끝에서는 반대쪽 끝으로 넘어간다.
     */
    private static void bindArrowKeys(JComponent... buttons) {
        for (int i = 0; i < buttons.length; i++) {
            JComponent previous = buttons[(i - 1 + buttons.length) % buttons.length];
            JComponent next = buttons[(i + 1) % buttons.length];
            bindKey(buttons[i], KeyEvent.VK_UP, "initialSetupView.previousChoice", previous::requestFocusInWindow);
            bindKey(buttons[i], KeyEvent.VK_DOWN, "initialSetupView.nextChoice", next::requestFocusInWindow);
        }
    }

    private static void bindKey(JComponent target, int keyCode, String actionKey, Runnable action) {
        target.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
        target.getActionMap()
                .put(actionKey, new AbstractAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        action.run();
                    }
                });
    }

    private static void invalidateTree(Component component) {
        component.invalidate();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                invalidateTree(child);
            }
        }
    }

    private JPanel createChoiceStep() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = createStackedConstraints();

        // 프로그램 아이콘과 환영 문구를 가운데에 두어 처음 여는 화면임을 알린다.
        gbc.insets = new Insets(0, 0, WELCOME_ICON_GAP, 0);
        panel.add(new JLabel(AppIcons.createIcon(WELCOME_ICON_SIZE), SwingConstants.CENTER), gbc);
        gbc.insets = new Insets(0, 0, TITLE_GAP, 0);
        panel.add(new TitleLabel("청지기에 오신 것을 환영합니다", SwingConstants.CENTER), gbc);
        gbc.insets = new Insets(0, 0, WELCOME_GAP, 0);
        FieldLabel guideLabel = new FieldLabel("시작할 방법을 선택해 주세요.", SwingConstants.CENTER, ComponentSize.MEDIUM);
        panel.add(guideLabel, gbc);

        gbc.insets = new Insets(0, 0, CHOICE_GAP, 0);
        panel.add(newStartButton, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(restoreChoiceButton, gbc);

        // 창 높이를 이 화면에 맞추므로(measureWindowHeights) 위에 붙여 둔다.
        JPanel content = wrapAtTop(panel);
        // 처음에는 아무 버튼도 선택되지 않도록 화면 자체가 포커스를 받는다. 그대로 두면 창이 첫 버튼에 포커스를 준다.
        // 이 상태에서 ↓는 첫 버튼, ↑는 마지막 버튼을 선택한다.
        content.setFocusable(true);
        bindKey(content, KeyEvent.VK_DOWN, "initialSetupView.firstChoice", newStartButton::requestFocusInWindow);
        bindKey(content, KeyEvent.VK_UP, "initialSetupView.lastChoice", restoreChoiceButton::requestFocusInWindow);
        return content;
    }

    private JPanel createRestoreStep() {
        CardPanel card = createFormCard();
        card.add(restoreForm, createStackedConstraints());

        JPanel panel = createStepContent("백업에서 복원", "백업 파일을 고르고 백업한 시점의 관리자 비밀번호를 입력해 주세요.", card);
        GridBagConstraints noticeGbc = createStackedConstraints();
        noticeGbc.insets = new Insets(SECTION_GAP, 0, 0, 0);
        panel.add(RestoreFormPanel.createWarningNotice(), noticeGbc);
        return wrapAtTop(panel);
    }

    private JPanel createAccountStep() {
        // 관리자 ID는 바꿀 수 없으므로 다른 항목과 같은 모양의 입력란을 꺼 둔다(복원 화면과 같다).
        StyledTextField userIdField = new StyledTextField(ComponentSize.MEDIUM);
        userIdField.setText(ADMIN_ID);
        userIdField.setEditable(false);
        userIdField.setEnabled(false);

        // 항목이 많아 단계 중 가장 높으므로, 레이블을 입력란 왼쪽에 두어 높이를 줄인다(세 단계는 가장 높은 단계에 창 높이를 맞춘다).
        CardPanel card = createFormCard();
        addSideLabeledField(card, "사용자 ID", userIdField);
        addSideLabeledField(card, "비밀번호", passwordField,
                // 비밀번호 변경 화면과 같은 안내다.
                String.format("%d자 이상 %d자 이하로 입력해 주세요. 여러 단어를 이은 문장형 비밀번호를 권장합니다.",
                        PasswordPolicy.MIN_LENGTH, PasswordPolicy.MAX_LENGTH),
                "같은 문자나 문자열의 반복(aaaa, gogogo), 연속된 문자(abcd, 1234), 키보드 배열(qwer, asdf)을 주로 쓴 "
                        + "비밀번호는 쓸 수 없습니다.");
        addSideLabeledField(card, "비밀번호 확인", confirmPasswordField);

        JPanel panel = createStepContent("관리자 계정 설정", "관리자 계정의 비밀번호를 지정해 주세요.", card);
        GridBagConstraints noticeGbc = createStackedConstraints();
        noticeGbc.insets = new Insets(SECTION_GAP, 0, 0, 0);
        panel.add(new WarningNoticePanel("비밀번호를 잊으면 복구할 수 없습니다.\n별도로 기록하여 안전한 곳에 보관하시기 바랍니다."),
                noticeGbc);
        return wrapAtTop(panel);
    }

    /**
     * 카드에 레이블을 왼쪽, 입력란을 오른쪽에 둔 항목을 한 줄 더한다. 설명은 입력란 아래(오른쪽 칸)에 둔다. 레이블 칸의 폭은 가장
     * 긴 레이블에 맞춰진다.
     *
     * @param descriptions 입력란 아래의 회색 설명. 단락마다 하나씩 넘긴다.
     */
    private static void addSideLabeledField(CardPanel card, String label, JComponent field,
            String... descriptions) {
        // 앞 항목의 설명 단락까지 차지한 격자 행 다음에 놓고, 앞에 항목이 있으면 그 사이를 띄운다.
        int gridY = nextGridY(card);
        int top = gridY == 0 ? 0 : FIELD_GAP;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(top, 0, 0, SIDE_LABEL_GAP);
        card.add(new FieldLabel(label, ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = gridY;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(top, 0, 0, 0);
        card.add(field, gbc);

        for (int i = 0; i < descriptions.length; i++) {
            gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = gridY + 1 + i;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(LABEL_GAP, 0, 0, 0);
            card.add(new WrappingDescriptionLabel(descriptions[i], ComponentSize.SMALL), gbc);
        }
    }

    /**
     * 카드의 격자에서 지금까지 놓인 컴포넌트 다음의 빈 행.
     */
    private static int nextGridY(CardPanel card) {
        GridBagLayout layout = (GridBagLayout) card.getLayout();
        int next = 0;
        for (Component component : card.getComponents()) {
            next = Math.max(next, layout.getConstraints(component).gridy + 1);
        }
        return next;
    }

    private JPanel createWeeklyClosingDayStep() {
        CardPanel card = createFormCard();
        GridBagConstraints gbc = createStackedConstraints();
        addField(card, gbc, "주간 결산 요일", weeklyClosingDayCombo,
                "주간은 결산 요일 다음 날부터 결산 요일까지입니다. 주일에 결산하시는 경우 일요일을 선택하시면, "
                        + "월요일부터 주일까지가 한 주간이 됩니다.");
        addDescription(card, gbc, "수입/지출 관리의 주간 조회와 주간 회계 결산 보고서에 쓰이며, "
                + "나중에 설정 > 프로그램 설정에서 바꿀 수 있습니다.");

        return wrapAtTop(createStepContent("주간 결산 요일 설정", "주간 단위로 결산하는 요일을 선택해 주세요.", card));
    }

    private JPanel createFiscalYearStep() {
        CardPanel card = createFormCard();
        GridBagConstraints gbc = createStackedConstraints();
        addField(card, gbc, "이름", fiscalYearNameField, "예: 2026년, 2025년 상반기");
        addField(card, gbc, "기간", fiscalYearPeriodField, "기간은 달력의 한 해와 달라도 됩니다.");
        addDescription(card, gbc, "나중에 설정 > 회계 연도 관리에서 바꾸거나 새 회계 연도를 추가할 수 있습니다.");

        return wrapAtTop(createStepContent("첫 회계 연도 설정", "처음 사용할 회계 연도의 이름과 기간을 지정해 주세요.", card));
    }

    /**
     * 단계 화면의 공통 틀. 제목, 할 일을 알리는 안내 글, 입력란을 담은 카드를 위에서부터 쌓는다. 그 아래에 더 놓을 것은
     * {@link #createStackedConstraints()}로 이어 붙인다.
     */
    private static JPanel createStepContent(String title, String guide, CardPanel card) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = createStackedConstraints();

        gbc.insets = new Insets(0, 0, TITLE_GAP, 0);
        panel.add(new TitleLabel(title, SwingConstants.LEFT), gbc);
        gbc.insets = new Insets(0, 0, SECTION_GAP, 0);
        panel.add(createGuideLabel(guide), gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(card, gbc);
        return panel;
    }

    /**
     * 제목 아래에 그 화면에서 할 일을 알리는 안내 글. 카드 안의 회색 설명과 구분되도록 본문 색으로 적는다.
     */
    private static WrappingDescriptionLabel createGuideLabel(String guide) {
        WrappingDescriptionLabel guideLabel = new WrappingDescriptionLabel(guide, ComponentSize.MEDIUM);
        guideLabel.setForeground(UIColors.TEXT_MAIN);
        return guideLabel;
    }

    /**
     * 입력란을 담는 흰 카드. 항목은 레이블 아래에 입력란을 놓는 방식으로 {@link #addField}로 쌓는다.
     */
    private static CardPanel createFormCard() {
        CardPanel card = new CardPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));
        return card;
    }

    /**
     * 카드에 레이블, 입력란, 설명을 차례로 더한다. 앞에 다른 항목이 있으면 그 사이를 띄운다.
     *
     * @param descriptions 입력란 아래의 회색 설명. 단락마다 하나씩 넘긴다.
     */
    private static void addField(CardPanel card, GridBagConstraints gbc, String label, JComponent field,
            String... descriptions) {
        gbc.insets = new Insets(card.getComponentCount() == 0 ? 0 : FIELD_GAP, 0, LABEL_GAP, 0);
        card.add(new FieldLabel(label, ComponentSize.MEDIUM, true), gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(field, gbc);
        for (String description : descriptions) {
            addDescription(card, gbc, description);
        }
    }

    /**
     * 카드의 마지막 항목 아래에 설명을 한 단락 더한다.
     */
    private static void addDescription(CardPanel card, GridBagConstraints gbc, String description) {
        gbc.insets = new Insets(LABEL_GAP, 0, 0, 0);
        card.add(new WrappingDescriptionLabel(description, ComponentSize.SMALL), gbc);
    }

    /**
     * 컴포넌트를 위에서부터 한 줄에 하나씩, 가로로 꽉 채워 쌓는 배치.
     */
    private static GridBagConstraints createStackedConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        return gbc;
    }

    /**
     * 단계마다 내용의 높이가 다르므로, 위쪽에 붙이고 남는 높이는 비워 둔다.
     */
    private static JPanel wrapAtTop(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.NORTH);
        return wrapper;
    }

    /**
     * 입력란에서 Enter를 누르면 동작을 실행하게 한다. 콤보박스의 목록이 열려 있으면 Enter는 항목 선택이므로 콤보박스에 맡긴다.
     */
    private static void onEnter(JComponent target, Runnable action) {
        target.getInputMap(JComponent.WHEN_FOCUSED)
                .put(ENTER, ENTER_ACTION_KEY);
        target.getActionMap()
                .put(ENTER_ACTION_KEY, new AbstractAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return !(target instanceof JComboBox<?> comboBox && comboBox.isPopupVisible());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 한글을 조합하는 중에 Enter를 누르면 조합한 글자가 확정되기 전에 이 동작이 실행될 수 있다.
                        // 글자가 확정된 뒤에 실행하도록 한 박자 늦춘다.
                        SwingUtilities.invokeLater(action);
                    }
                });
    }

    /**
     * @param newStep 보일 단계(0부터). 시작 방법 선택이면 {@link #CHOICE_STEP}, 복원이면 {@link #RESTORE_STEP}
     */
    private void showStep(int newStep) {
        step = newStep;
        String card = switch (step) {
        case CHOICE_STEP -> CHOICE_CARD;
        case RESTORE_STEP -> RESTORE_CARD;
        default -> STEP_NAMES[step];
        };
        stepLayout.show(stepPanel, card);
        // 단계 표시는 새로 시작의 세 단계에서만 보인다.
        boolean setupStep = step >= 0;
        headerPanel.setVisible(setupStep);
        if (setupStep) {
            stepIndicator.setCurrentStep(step);
        }

        // 시작 방법 선택은 선택 버튼이 곧 다음으로 가는 버튼이므로 아래쪽 버튼을 두지 않는다.
        navigationPanel.removeAll();
        if (step != CHOICE_STEP) {
            navigationPanel.add(previousButton);
            navigationPanel.add(nextButton);
            if (step == RESTORE_STEP) {
                nextButton.setText("복원");
            } else {
                nextButton.setText(step == LAST_STEP ? "완료" : "다음");
            }
        }
        navigationPanel.revalidate();
        navigationPanel.repaint();
        bottomPanel.setVisible(step != CHOICE_STEP);

        applyWindowHeight();
        focusFirstField();
    }

    private void focusFirstField() {
        if (step == RESTORE_STEP) {
            restoreForm.focusFirstField();
            return;
        }
        JComponent field = switch (step) {
        case CHOICE_STEP -> choiceContent;
        case 0 -> passwordField;
        case 1 -> weeklyClosingDayCombo;
        default -> fiscalYearNameField;
        };
        field.requestFocusInWindow();
    }

    private void onNext() {
        if (step == RESTORE_STEP) {
            restoreForm.restore(this::setBusy, () -> {
                isInitializedSuccessfully = true;
                dispose();
            });
            return;
        }

        String error = switch (step) {
        case 0 -> findAccountError();
        case 1 -> weeklyClosingDayCombo.getSelectedItem() == null ? "주간 결산 요일을 선택해 주세요." : null;
        default -> findFiscalYearError();
        };
        if (error != null) {
            JOptionPane.showMessageDialog(this, error);
            return;
        }

        if (step < LAST_STEP) {
            showStep(step + 1);
        } else {
            initialize();
        }
    }

    /**
     * 비밀번호의 규칙과 일치 여부를 확인한다.
     *
     * @return 문제가 있으면 사용자에게 보여줄 메시지, 없으면 null
     */
    private String findAccountError() {
        String password = PasswordPolicy.normalize(new String(passwordField.getPassword()));
        String confirmPassword = PasswordPolicy.normalize(new String(confirmPasswordField.getPassword()));

        List<Violation> result = PasswordPolicy.validate(password, List.of(ADMIN_ID));
        if (!result.isEmpty()) {
            return result.getFirst()
                    .getMessage();
        }
        if (!password.equals(confirmPassword)) {
            return "비밀번호가 일치하지 않습니다.";
        }
        return null;
    }

    /**
     * 회계 연도 추가와 같은 검증을 한다(이름 필수, 길이는 입력란이 막는다). 처음 만드는 회계 연도이므로 겹칠 회계 연도는 없다.
     *
     * @return 문제가 있으면 사용자에게 보여줄 메시지, 없으면 null
     */
    private String findFiscalYearError() {
        if (fiscalYearNameField.getText()
                .trim()
                .isEmpty()) {
            return "회계 연도의 이름을 입력해주세요.";
        }
        return fiscalYearPeriodField.findInputError();
    }

    private void initialize() {
        SetupInput input = new SetupInput(ADMIN_ID, PasswordPolicy.normalize(new String(passwordField.getPassword())),
                (DayOfWeek) weeklyClosingDayCombo.getSelectedItem(),
                fiscalYearNameField.getText()
                        .trim(),
                fiscalYearPeriodField.getStartDate(), fiscalYearPeriodField.getEndDate());

        // 비동기 작업 시작
        setBusy(true);
        AsyncTask.run(this,
                () -> {
                    doSetup.accept(input);
                    return null;
                },
                _ -> {
                    JOptionPane.showMessageDialog(this, "초기 설정이 완료되었습니다.");
                    isInitializedSuccessfully = true;
                    this.dispose();
                },
                err -> {
                    log.error("Failed to initialize database", err);
                    JOptionPane.showMessageDialog(this, "초기 설정 실패: " + err.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    setBusy(false);
                });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        previousButton.setEnabled(!busy);
        nextButton.setEnabled(!busy);
        progressBar.setVisible(busy);
    }

}
