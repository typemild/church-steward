package org.church.steward.ui.view.datamanagement.importing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.label.SubTitleLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;

/**
 * 가져오기를 단계별로 진행하는 패널의 틀. 제목, 단계 표시, 단계 화면, 알림 문구, 이전/다음 버튼을 놓고
 * 단계 이동과 시작/취소/종료를 맡는다. 단계마다 무엇을 하는지는 하위 클래스가 정한다.
 * <p>
 * 다이얼로그가 아니라 탭 안에 놓인다. 가져오는 도중에 다른 메뉴에서 기존 데이터를 확인하거나 고칠 수 있어야 하기
 * 때문이다(모달 다이얼로그는 메뉴 이동을 막는다). 다른 메뉴에 다녀오면 {@link #onReturned}가 불린다.
 */
public abstract class AbstractImportPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final String title;
    private final String[] stepNames;
    private final Runnable onClosed;

    private final SubTitleLabel titleLabel = new SubTitleLabel(" ");
    private final FieldLabel stepLabel = new FieldLabel(" ", ComponentSize.MEDIUM);
    private final SubFieldLabel noticeLabel = new SubFieldLabel(" ", ComponentSize.MEDIUM);
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final SecondaryButton cancelButton = new SecondaryButton("가져오기 취소", ComponentSize.MEDIUM);
    private final SecondaryButton previousButton = new SecondaryButton("이전", ComponentSize.MEDIUM);
    private final PrimaryButton nextButton = new PrimaryButton("다음", ComponentSize.MEDIUM);
    private final JProgressBar progressBar = new JProgressBar();

    private int stepCount = 0;
    private int step = 0;
    private boolean inProgress = false;
    /** {@link #runTask}로 실행 중인 작업의 수. 매핑을 연달아 바꾸면 분석이 겹칠 수 있어 개수로 센다. */
    private int runningTaskCount = 0;

    /**
     * @param title     화면 제목과 알림 창의 제목에 쓸 이름 (예: "예산 가져오기")
     * @param stepNames 단계의 이름. {@link #addStep}으로 단계 화면을 같은 순서로 넣어야 한다. 마지막 단계는
     *                  반영이다.
     * @param onClosed  가져오기를 마쳤거나 취소해서 이 패널을 닫아야 할 때 실행할 동작
     */
    protected AbstractImportPanel(String title, String[] stepNames, Runnable onClosed) {
        this.title = title;
        this.stepNames = stepNames;
        this.onClosed = onClosed;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // 진행 상황(제목과 단계)은 둥근 테두리로 둘러 아래의 단계 화면과 구분한다.
        RoundedPanel headerPanel = new RoundedPanel(new BorderLayout(0, 6));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(stepLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        cardPanel.setOpaque(false);
        add(cardPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(previousButton);
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(nextButton);

        // 진행 표시는 취소 버튼과 이전/다음 버튼 사이에 두고, 버튼 높이만큼 늘어나지 않게 세로 가운데에 놓는다.
        progressBar.setIndeterminate(true);
        progressBar.setForeground(UIColors.PRIMARY_BACKGROUND);
        progressBar.setVisible(false);
        JPanel progressPanel = new JPanel(new GridBagLayout());
        progressPanel.setOpaque(false);
        GridBagConstraints progressConstraints = new GridBagConstraints();
        progressConstraints.fill = GridBagConstraints.HORIZONTAL;
        progressConstraints.weightx = 1.0;
        progressPanel.add(progressBar, progressConstraints);

        JPanel navigationPanel = new JPanel(new BorderLayout(12, 6));
        navigationPanel.setOpaque(false);
        noticeLabel.setForeground(UIColors.ERROR_WARNING);
        navigationPanel.add(noticeLabel, BorderLayout.NORTH);
        navigationPanel.add(cancelButton, BorderLayout.WEST);
        navigationPanel.add(progressPanel, BorderLayout.CENTER);
        navigationPanel.add(buttonPanel, BorderLayout.EAST);
        add(navigationPanel, BorderLayout.SOUTH);

        cancelButton.addActionListener(_ -> cancel());
        previousButton.addActionListener(_ -> showStep(step - 1));
        nextButton.addActionListener(_ -> onNext(step));
    }

    /**
     * 진행 중인 가져오기의 상태.
     */
    protected abstract ImportSession getSession();

    /**
     * 가져오기를 새로 시작할 때 단계 화면들을 처음 상태로 되돌린다. 세션은 이미 비워져 있다.
     */
    protected abstract void onStarted();

    /**
     * 주어진 단계에서 "다음"(마지막 단계에서는 "가져오기 실행")을 누를 수 있는지 여부.
     */
    protected abstract boolean canProceed(int step);

    /**
     * 주어진 단계에서 "다음"을 눌렀을 때 할 일. 검증하고 {@link #showStep}으로 다음 단계를 보여주거나,
     * 마지막 단계이면 반영한다.
     */
    protected abstract void onNext(int step);

    /**
     * 다른 메뉴에 다녀와 이 패널이 다시 보일 때 할 일. 그 사이에 데이터가 바뀌었을 수 있으므로,
     * 분석 결과를 보고 있는 단계라면 현재 데이터로 다시 분석해야 한다.
     */
    protected abstract void onReturnedToStep(int step);

    /**
     * 단계 화면을 순서대로 넣는다. 하위 클래스의 생성자에서 호출한다.
     */
    protected void addStep(JComponent stepPanel) {
        cardPanel.add(stepPanel, String.valueOf(stepCount));
        stepCount++;
    }

    /**
     * 주어진 회계 연도로 가져오기를 새로 시작한다.
     *
     * @param fiscalYear 가져올 회계 연도. 관계자처럼 회계 연도에 속하지 않는 대상이면 null
     */
    public void start(FiscalYearDto fiscalYear) {
        getSession().reset();
        getSession().setFiscalYear(fiscalYear);
        inProgress = true;
        setNotice(null);
        onStarted();
        showStep(0);
    }

    /**
     * 진행 중인 가져오기가 있는지 여부.
     */
    public boolean isInProgress() {
        return inProgress;
    }

    /**
     * 다른 메뉴에 다녀온 뒤에 호출한다. 가져오던 회계 연도가 삭제되었으면 가져오기를 취소하고,
     * 아니면 회계 연도의 최신 정보를 받아들인 뒤 하위 클래스가 다시 분석하게 한다.
     * 회계 연도 없이 시작한 가져오기는 회계 연도를 확인하지 않고 바로 다시 분석하게 한다.
     *
     * @param fiscalYears 방금 다시 읽은 회계 연도 목록
     */
    public void onReturned(List<FiscalYearDto> fiscalYears) {
        if (!inProgress) {
            return;
        }
        if (getSession().getFiscalYear() == null) {
            onReturnedToStep(step);
            return;
        }

        FiscalYearDto current = null;
        for (FiscalYearDto fiscalYear : fiscalYears) {
            if (fiscalYear.getId().equals(getSession().getFiscalYear().getId())) {
                current = fiscalYear;
            }
        }
        if (current == null) {
            showMessage("가져오던 회계 연도가 삭제되어 가져오기를 취소합니다.", JOptionPane.WARNING_MESSAGE);
            close();
            return;
        }
        getSession().setFiscalYear(current);
        updateHeader();
        onReturnedToStep(step);
    }

    protected int getStep() {
        return step;
    }

    /**
     * 마지막 단계(백업 및 반영)의 번호.
     */
    protected int getLastStep() {
        return stepNames.length - 1;
    }

    /**
     * 분석이나 반영처럼 시간이 걸리는 작업을 백그라운드에서 실행한다. 작업하는 동안 진행 표시를 보여주고
     * 가져오기 취소, 이전, 다음 버튼을 끈다. 작업이 끝나면 진행 표시를 감추고 버튼을 단계에 맞게 되돌린 뒤 결과를 넘긴다.
     */
    protected <T> void runTask(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        runningTaskCount++;
        updateBusyState();
        updateNavigation();
        AsyncTask.run(nextButton, work,
                result -> {
                    finishTask();
                    onSuccess.accept(result);
                },
                err -> {
                    finishTask();
                    onError.accept(err);
                });
    }

    private void finishTask() {
        runningTaskCount--;
        updateBusyState();
        updateNavigation();
    }

    private void updateBusyState() {
        boolean busy = runningTaskCount > 0;
        progressBar.setVisible(busy);
        cancelButton.setEnabled(!busy);
    }

    protected void showStep(int newStep) {
        step = newStep;
        cardLayout.show(cardPanel, String.valueOf(step));
        updateHeader();
        updateNavigation();
    }

    /**
     * 단계와 세션의 상태에 맞게 이전/다음 버튼을 켜고 끈다. 세션의 상태가 바뀌면 호출해야 한다.
     */
    protected void updateNavigation() {
        boolean busy = runningTaskCount > 0;
        previousButton.setEnabled(!busy && step > 0);
        nextButton.setText(step == getLastStep() ? "가져오기 실행" : "다음");
        nextButton.setEnabled(!busy && canProceed(step));
    }

    /**
     * 단계 화면 아래에 알림 문구를 보여준다. null이면 지운다.
     */
    protected void setNotice(String notice) {
        noticeLabel.setText(notice == null ? " " : notice);
    }

    protected void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    /**
     * 반영에 실패했을 때의 공통 처리.
     *
     * @param onBackupOutdated 백업한 뒤에 데이터가 바뀌어 백업을 다시 받아야 할 때 할 일. 받아 둔 백업은 이미 버린
     *                         뒤다.
     * @param onStateChanged   분석 결과가 화면에 보여준 것과 달라져 다시 분석해야 할 때 할 일
     */
    protected void handleCommitError(Throwable err, Runnable onBackupOutdated, Runnable onStateChanged) {
        if (err instanceof BackupOutdatedException) {
            getSession().clearBackup();
            updateNavigation();
            showMessage(err.getMessage(), JOptionPane.WARNING_MESSAGE);
            onBackupOutdated.run();
        } else if (err instanceof ImportStateChangedException) {
            showMessage(err.getMessage(), JOptionPane.WARNING_MESSAGE);
            onStateChanged.run();
        } else {
            showMessage("가져오지 못했습니다: " + err.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancel() {
        int answer = JOptionPane.showConfirmDialog(this, "진행 중인 가져오기를 취소할까요?\n선택한 파일과 열 지정이 사라집니다.", title,
                JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            close();
        }
    }

    /**
     * 가져오기를 끝내고 패널을 닫는다. 읽어 둔 파일 내용은 버린다.
     */
    protected void close() {
        inProgress = false;
        getSession().reset();
        onClosed.run();
    }

    private void updateHeader() {
        FiscalYearDto fiscalYear = getSession().getFiscalYear();
        titleLabel.setText(fiscalYear == null ? title : title + " - 회계 연도: " + fiscalYear.getName());

        StringBuilder steps = new StringBuilder("<html>");
        for (int i = 0; i < stepNames.length; i++) {
            if (i > 0) {
                steps.append("&nbsp;&nbsp;›&nbsp;&nbsp;");
            }
            String name = (i + 1) + ". " + stepNames[i];
            steps.append(i == step ? "<b>" + name + "</b>" : name);
        }
        stepLabel.setText(steps.append("</html>").toString());
    }

}
