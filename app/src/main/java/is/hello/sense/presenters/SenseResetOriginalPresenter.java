package is.hello.sense.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.interactors.hardware.SenseResetOriginalInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class SenseResetOriginalPresenter
        extends BasePresenter<SenseResetOriginalPresenter.Output> {

    private final CurrentSenseInteractor currentSenseInteractor;
    private SenseResetOriginalInteractor interactor;

    public SenseResetOriginalPresenter(final SenseResetOriginalInteractor interactor,
                                       final CurrentSenseInteractor resetInteractor) {
        this.interactor = interactor;
        this.currentSenseInteractor = resetInteractor;
        addInteractor(interactor);
    }

    @Override
    public void onDetach() {
        interactor = null;
    }

    public void navigateToHelp() {
        execute( () -> view.showHelpUri(UserSupport.HelpStep.RESET_ORIGINAL_SENSE));
    }

    public void onStart() {
        showBlockingActivity(R.string.dialog_sense_reset_original, this::startOperation);
    }

    void startOperation() {
        if(currentSenseInteractor.getCurrentSense() == null){
            onError(new SenseNotFoundError());
            return;
        }
        bindAndSubscribe(interactor.discoverPeripheralForDevice(currentSenseInteractor.getCurrentSense()),
                         ignore -> this.checkBond(),
                         this::onError);
    }

    void onOperationComplete() {
        hideBlockingActivity(true, () -> {
            currentSenseInteractor.senseDevice.forget();
            interactor.reset();
            view.finishFlow();
        });
    }

    void onSkip() {
        showBlockingActivity(R.string.empty, this::onOperationComplete);
    }

    public void onError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
            builder.withMessage(StringRef.from(R.string.error_factory_reset_original_sense_message));
            view.showErrorDialog(builder);
            view.showRetry(R.string.action_skip,
                           this::onSkip);
        });

    }

    public void checkBond(){
        logEvent("checkBond");
        if(interactor.isBonded()){
            bindAndSubscribe(interactor.clearBond(),
                             ignore -> checkConnection(),
                             this::onError);
        } else {
            checkConnection();
        }
    }

    private void checkConnection(){
        logEvent("checkConnection");
        if(interactor.isConnected()) {
            factoryResetOperation();
        } else {
            bindAndSubscribe(interactor.connectToPeripheral()
                                               .filter(ConnectProgress.CONNECTED::equals),
                             ignore -> this.factoryResetOperation(),
                             this::onError);
        }
    }

    private void factoryResetOperation() {
        bindAndSubscribe(interactor.unsafeFactoryReset(),
                         ignored -> this.onOperationComplete(),
                         this::onError);
    }

    public interface Output extends BaseOutput {

        void showRetry(@StringRes int retryRes,
                       @NonNull final Runnable onClickRunnable);
    }
}
