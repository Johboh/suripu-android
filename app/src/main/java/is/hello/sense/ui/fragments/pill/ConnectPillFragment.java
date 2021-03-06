package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.bluetooth.PillDfuInteractor;
import is.hello.sense.bluetooth.PillPeripheral;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.SenseCache;
import rx.functions.Action0;

public class ConnectPillFragment extends PillHardwareFragment implements OnBackPressedInterceptor {

    @Inject
    DevicesInteractor devicesPresenter;
    @Inject
    PillDfuInteractor pillDfuPresenter;
    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    SenseCache.FirmwareCache firmwareCache;

    private static final String DEFAULT_DEBUG_URL = "https://s3.amazonaws.com/hello-firmware/kodobannin/mobile/pill.bin";

    private DiagramVideoView diagram;
    private ProgressBar activityIndicator;
    private TextView activityStatus;
    private Button retryButton;
    private Button skipButton;

    //region lifecycle
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!bluetoothStack.isEnabled()) {
            cancel(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pair_pill, container, false);
        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_pair_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_pair_pill_retry);
        this.skipButton = (Button) view.findViewById(R.id.fragment_pair_pill_skip);
        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_pair_pill_diagram);
        ((TextView)view.findViewById(R.id.fragment_pair_pill_title)).setText(R.string.title_connect_sleep_pill);

        this.skipButton.setText(R.string.action_cancel);
        Views.setTimeOffsetOnClickListener(retryButton, ignored -> searchForPill());
        Views.setTimeOffsetOnClickListener(skipButton, ignored -> onCancel());
        this.toolbar = OnboardingToolbar.of(this, view)
                                        .setOnHelpClickListener(this::help)
                                        .setWantsBackButton(false)
                                        .setWantsHelpButton(false);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(devicesPresenter.devices,
                         this::bindDevices,
                         this::presentError);
        bindAndSubscribe(pillDfuPresenter.sleepPill,
                         this::shouldShowConfirmationDialog,
                         this::presentError);
        searchForPill();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diagram != null) {
            diagram.destroy();
        }
        this.retryButton.setOnClickListener(null);
        this.retryButton = null;
        this.skipButton.setOnClickListener(null);
        this.skipButton = null;
        this.diagram = null;
    }

    @Override
    void onLocationPermissionGranted(final boolean isGranted) {
        if (isGranted) {
            searchForPill();
        }
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if(retryButton.getVisibility() == View.VISIBLE) {
            onCancel();
        }
        return true;
    }

    private void searchForPill() {
        retryButton.post(() -> {
            if (isLocationPermissionGranted()) {
                updateUI(false);
                setStatus(R.string.label_searching_for_pill);
                devicesPresenter.update();
            } else {
                updateUI(true);
                requestLocationPermission();
            }
        });
    }

    private void bindDevices(@NonNull final Devices devices) {
        final SleepPillDevice sleepPillDevice = devices.getSleepPill();
        //intend to allow any pill found nearby to be updated in debug mode
        if(BuildConfig.DEBUG && (sleepPillDevice == null || !sleepPillDevice.shouldUpdate())){
            Log.d(getClass().getName(), "using " + DEFAULT_DEBUG_URL + " for firmwareCache url");
            firmwareCache.setUrlLocation(DEFAULT_DEBUG_URL);
        } else if (sleepPillDevice == null) {
            cancel(false);
            return;
        } else if (sleepPillDevice.shouldUpdate()){
            assert sleepPillDevice.firmwareUpdateUrl != null;
            firmwareCache.setUrlLocation(sleepPillDevice.firmwareUpdateUrl);
            pillDfuPresenter.setDeviceId(sleepPillDevice.deviceId);
        }
        pillDfuPresenter.update();
    }

    private void updateUI(final boolean onError){
        if(onError){
            diagram.suspendPlayback(true);
        } else {
            diagram.startPlayback();
        }
        toolbar.setVisible(onError);
        toolbar.setWantsHelpButton(onError);
        final int visibleOnError = onError ? View.VISIBLE : View.GONE;
        final int hiddenOnError = onError ? View.GONE : View.VISIBLE;
        skipButton.setVisibility(visibleOnError);
        retryButton.setVisibility(visibleOnError);
        activityStatus.setVisibility(hiddenOnError);
        activityIndicator.setVisibility(hiddenOnError);
    }

    private void presentError(@NonNull final Throwable e) {
        retryButton.post(() -> {
            updateUI(true);

            @StringRes final int title = R.string.error_sleep_pill_title_update_missing;
            @StringRes final int message = R.string.error_sleep_pill_message_update_missing;
            final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
            final ErrorDialogFragment.Builder errorDialogBuilder = getErrorDialogFragmentBuilder(e, title, message, helpUriString);
            errorDialogBuilder.withOperation(StringRef.from(R.string.connect_pill_fragment_operation).toString());

            errorDialogBuilder
                    .build()
                    .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

            Log.e(getTag(), "presentError: ", e);

        });
    }
    //endregion

    private void onCancel(){
        pillDfuPresenter.reset();
        cancel(false);
    }

    private void pillFound() {
        setStatus(R.string.message_sleep_pill_connected);
        diagram.suspendPlayback(true);
        activityStatus.post(() -> activityIndicator.setActivated(true));
        activityStatus.postDelayed(() -> getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null),
                                   LoadingDialogFragment.DURATION_DONE_MESSAGE);
    }

    public void shouldShowConfirmationDialog(@NonNull final PillPeripheral pillPeripheral){
        if(BuildConfig.DEBUG){
            showConfirmationDialog(pillPeripheral.getName(),
                                   this::pillFound,
                                   () -> {
                                       updateUI(true);
                                       pillDfuPresenter.reset();
                                   });
        } else {
            pillFound();
        }
    }

    public void showConfirmationDialog(final String deviceName,
                               final Action0 positiveAction,
                               final Action0 negativeAction){
        final SenseAlertDialog.Builder dialog = new SenseAlertDialog.Builder()
                .setTitle(R.string.debug_title_confirm_sense_pair)
                .setMessage(getString(R.string.debug_message_confirm_pill_update_fmt, deviceName))
                .setPositiveButton(android.R.string.ok, positiveAction::call)
                .setNegativeButton(android.R.string.cancel, negativeAction::call)
                .setCancelable(false);

        dialog.build(getActivity()).show();
    }

    private void setStatus(@StringRes final int text) {
        activityStatus.post(() -> activityStatus.setText(text));
    }
}
