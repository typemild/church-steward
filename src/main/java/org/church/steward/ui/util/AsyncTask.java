package org.church.steward.ui.util;

import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingWorker;

public class AsyncTask {

    private AsyncTask() {
        // 인스턴스화 방지
    }

    public static <T> void run(Component lockTarget, Supplier<T> backgroundWork, Consumer<T> onSuccess,
            Consumer<Throwable> onError) {
        // 작업이 끝나면 기본 커서가 아니라 원래 커서로 되돌린다. 기본 커서로 되돌리면 버튼의 손 모양 커서처럼 컴포넌트에
        // 지정해 둔 커서가 사라진다. 지정된 커서가 없었으면(부모의 커서를 따르고 있었으면) null로 되돌린다.
        final Cursor originalCursor = lockTarget.isCursorSet() ? lockTarget.getCursor() : null;
        // 이미 대기 커서이면 같은 컴포넌트로 먼저 시작한 작업이 아직 끝나지 않은 것이다. 이때의 대기 커서를 원래 커서로
        // 알고 되돌리면 대기 커서가 그대로 남으므로, 커서는 먼저 시작한 작업이 되돌리게 둔다.
        final boolean alreadyWaiting = originalCursor != null && originalCursor.getType() == Cursor.WAIT_CURSOR;

        lockTarget.setEnabled(false);
        lockTarget.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return backgroundWork.get();
            }

            @Override
            protected void done() {
                lockTarget.setEnabled(true);
                if (!alreadyWaiting) {
                    lockTarget.setCursor(originalCursor);
                }
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    // doInBackground에서 발생한 실제 예외를 추출하여 전달
                    onError.accept(e.getCause() != null ? e.getCause() : e);
                } catch (InterruptedException e) {
                    // 작업 중단 시 발생
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                } catch (Exception e) {
                    // 기타 예외 처리
                    onError.accept(e);
                }
            }
        }.execute();
    }
}
