package is.hello.sense.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

public abstract class BasePairSensePresenter<T extends BasePairSensePresenter.Output> extends BaseHardwarePresenter<T> {

    protected static final String OPERATION_LINK_ACCOUNT = "Linking account";
    protected static final String ARG_HAS_LINKED_ACCOUNT = "hasLinkedAccount";

    private boolean linkedAccount = false;

    private final ApiService apiService;
    protected final UserFeaturesInteractor userFeaturesInteractor;
    private final PairSenseInteractor pairSenseInteractor;

    public BasePairSensePresenter(final HardwareInteractor hardwareInteractor,
                                  final UserFeaturesInteractor userFeaturesInteractor,
                                  final ApiService apiService,
                                  final PairSenseInteractor pairSenseInteractor) {
        super(hardwareInteractor);
        this.userFeaturesInteractor = userFeaturesInteractor;
        this.apiService = apiService;
        this.pairSenseInteractor = pairSenseInteractor;
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle state = new Bundle();
        state.putBoolean(ARG_HAS_LINKED_ACCOUNT, linkedAccount);
        return state;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        linkedAccount = savedState.getBoolean(ARG_HAS_LINKED_ACCOUNT);
    }

    public abstract String getOnCreateAnalyticsEvent();

    protected abstract void presentError(Throwable e, String operation);

    @StringRes
    public int getPairingRes(){
        return pairSenseInteractor.getPairingRes();
    }

    @StringRes
    public int getFinishedRes(){
        return pairSenseInteractor.getFinishedRes();
    }

    protected boolean shouldContinueFlow(){
        return pairSenseInteractor.shouldContinueFlow();
    }

    protected boolean shouldResetOnPairSuccess(){
        return pairSenseInteractor.shouldClearPeripheral();
    }

    protected Observable<SensePeripheral> getObservableSensePeripheral() {
        return pairSenseInteractor.closestPeripheral();
    }

    protected String getOnFinishAnalyticsEvent(){
        return pairSenseInteractor.getOnFinishedAnalyticsEvent();
    }

    protected void sendOnFinishedAnalytics() {
        Analytics.trackEvent(getOnFinishAnalyticsEvent(), null);
    }

    @StringRes
    public int getLinkedAccountErrorTitleRes(){
        return pairSenseInteractor.getLinkedAccountErrorTitleRes();
    }

    protected void onPairSuccess() {
        if (shouldResetOnPairSuccess()) {
            hardwareInteractor.reset();
        }
        if (shouldContinueFlow()) {
            view.finishFlowWithResult(Activity.RESULT_OK);
        } else {
            view.finishActivity();
        }
    }

    public void checkLinkedAccount() {
        if (linkedAccount) {
            finishUpOperations();
        } else {
            showBlockingActivity(R.string.title_linking_account);

            requestLinkAccount();
        }
    }

    protected void updateLinkedAccount() {
        this.linkedAccount = true;
        finishUpOperations();
    }

    protected void requestLinkAccount() {
        bindAndSubscribe(pairSenseInteractor.linkAccount(),
                         ignored -> updateLinkedAccount(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not link Sense to account", error);
                             presentError(error, OPERATION_LINK_ACCOUNT);
                         });
    }

    protected boolean hasPeripheralPair() {
        Analytics.setSenseId(hardwareInteractor.getDeviceId());
        if (hardwareInteractor.isBonded()) {
            showBlockingActivity(R.string.title_clearing_bond);
            return true;
        } else {
            showBlockingActivity(getPairingRes());
            return false;
        }
    }

    protected boolean hasConnectivity(final ConnectProgress status) {
        if (status == ConnectProgress.CONNECTED) {
            showBlockingActivity(R.string.title_checking_connectivity);
            return true;
        } else {
            showBlockingActivity(Styles.getConnectStatusMessage(status));
            return false;
        }
    }

    public void finishUpOperations() {
        setDeviceTimeZone();
    }

    private void setDeviceTimeZone() {
        showBlockingActivity(R.string.title_setting_time_zone);

        final SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(getClass().getSimpleName(), "Time zone updated.");

                             pushDeviceData();
                         },
                         e -> presentError(e, "Updating time zone"));
    }

    private void pushDeviceData() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(hardwareInteractor.pushData(),
                         ignored -> getDeviceFeatures(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push Sense data, ignoring.", error);
                             getDeviceFeatures();
                         });
    }

    private void getDeviceFeatures() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(userFeaturesInteractor.storeFeaturesInPrefs(),
                         ignored -> onFinished(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not get features from Sense, ignoring.", error);
                             onFinished();
                         });
    }

    private void onFinished() {
        hideAllActivityForSuccess(getFinishedRes(),
                                  () -> {
                                      sendOnFinishedAnalytics();
                                      onPairSuccess();
                                  },
                                  e -> {
                                      presentError(e, "Turning off LEDs");
                                  });
    }



    public interface Output extends BaseOutput {


    }
}