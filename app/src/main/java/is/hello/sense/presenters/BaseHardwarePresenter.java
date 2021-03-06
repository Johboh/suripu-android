package is.hello.sense.presenters;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.util.Logger;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Contains methods for wrapping loading dialog fragments Runnables
 * with {@link is.hello.sense.util.StateSafeExecutor#}
 *
 * @param <T>
 */
public abstract class BaseHardwarePresenter<T extends BaseOutput>
        extends BasePresenter<T> {

    protected HardwareInteractor hardwareInteractor;

    public BaseHardwarePresenter(final HardwareInteractor hardwareInteractor) {
        super();
        this.hardwareInteractor = hardwareInteractor;
        addInteractor(hardwareInteractor);
    }

    @CallSuper
    @Override
    public void onDetach(){
        hardwareInteractor = null;
    }

    protected void showHardwareActivity(@NonNull final Runnable onCompletion,
                                        @NonNull final Action1<Throwable> onError) {
        bindAndSubscribe(hardwareInteractor.runLedAnimation(SenseLedAnimation.BUSY),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when showing hardware activity.", e);
                             onError.call(e);
                         });
    }

    protected void hideHardwareActivity(@NonNull final Runnable onCompletion,
                                        @Nullable final Action1<Throwable> onError) {
        if (hardwareInteractor.isConnected()) {
            bindAndSubscribe(hardwareInteractor.runLedAnimation(SenseLedAnimation.TRIPPY),
                             ignored -> onCompletion.run(),
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Error occurred when hiding hardware activity.", e);
                                 if (onError != null) {
                                     onError.call(e);
                                 } else {
                                     onCompletion.run();
                                 }
                             });
        } else {
            stateSafeExecutor.execute(onCompletion);
        }
    }

    protected void completeHardwareActivity(@NonNull final Runnable onCompletion) {
        bindAndSubscribe(hardwareInteractor.runLedAnimation(SenseLedAnimation.STOP),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when completing hardware activity", e);

                             onCompletion.run();
                         });
    }

    protected void hideAllActivityForSuccess(@NonNull final Runnable onCompletion,
                                             @NonNull final Action1<Throwable> onError) {
        hideHardwareActivity(() -> hideBlockingActivity(true, onCompletion),
                             e -> hideBlockingActivity(false, () -> onError.call(e)));
    }

    public void hideAllActivityForFailure(@NonNull final Runnable onCompletion) {
        final Runnable next = () -> hideBlockingActivity(false, onCompletion);
        hideHardwareActivity(next, ignored -> next.run());
    }

    protected void hideAllActivityForSuccess(@StringRes final int messageRes,
                                             @NonNull final Runnable onCompletion,
                                             @NonNull final Action1<Throwable> onError) {
        hideHardwareActivity(() -> hideBlockingActivity(messageRes, onCompletion),
                             e -> hideBlockingActivity(false, () -> onError.call(e)));
    }

    public Subscription provideBluetoothEnabledSubscription(final Action1<Boolean> action) {
        return subscribe(hardwareInteractor.bluetoothEnabled, action, Functions.LOG_ERROR);
    }
}
