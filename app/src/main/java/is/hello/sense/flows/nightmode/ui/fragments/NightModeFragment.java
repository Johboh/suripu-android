package is.hello.sense.flows.nightmode.ui.fragments;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import com.google.android.gms.location.LocationSettingsStatusCodes;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.LocStatus;
import is.hello.sense.api.model.UserLocation;
import is.hello.sense.flows.generic.ui.interactors.LocationInteractor;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.nightmode.ui.views.NightModeLocationPermission;
import is.hello.sense.flows.nightmode.ui.views.NightModeView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Control night mode settings
 */
public class NightModeFragment extends PresenterFragment<NightModeView>
        implements NightModeView.Listener {

    @Inject
    NightModeInteractor nightModeInteractor;

    @Inject
    LocationInteractor locationInteractor;

    private final NightModeLocationPermission nightModeLocationPermission = new NightModeLocationPermission(this);
    private Subscription statusSubscription = Subscriptions.empty();

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new NightModeView(getActivity());
            setInitialMode();
            presenterView.setListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.locationInteractor.start();
        if (hasPresenterView()) {
            final boolean hasLocationPermission = this.nightModeLocationPermission.isGranted();
            final boolean currentModeIsAuto = this.nightModeInteractor.getCurrentMode() == AppCompatDelegate.MODE_NIGHT_AUTO;
            if (!hasLocationPermission && currentModeIsAuto) {
                this.presenterView.setOffMode();
            }
            this.presenterView.setScheduledModeEnabled(hasLocationPermission);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.locationInteractor.stop();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (nightModeLocationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            if (hasPresenterView()) {
                presenterView.setScheduledModeEnabled(true);
            }
        } else {
            nightModeLocationPermission.showEnableInstructionsDialog();
        }
    }

    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NightModeActivity.REQUEST_LOCATION_STATUS) {
            revertMode(); // let user click again
        }
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        statusSubscription.unsubscribe();
    }

    //endregion

    //region NightModeView.Listener
    @Override
    public void offModeSelected() {
        this.statusSubscription.unsubscribe();
        this.setMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Override
    public void onModeSelected() {
        this.statusSubscription.unsubscribe();
        this.setMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    public void scheduledModeSelected() {
        this.statusSubscription.unsubscribe();
        final UserLocation userLocation = this.locationInteractor.getCurrentUserLocation();
        if (userLocation == null) {

            this.locationInteractor.forget();
            this.statusSubscription = this.locationInteractor.statusSubject.subscribe(this::bindLocStatus,
                                                                                      Functions.LOG_ERROR);
            this.locationInteractor.start();
        } else {
            setMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

    @Override
    public boolean onLocationPermissionLinkIntercepted() {
        nightModeLocationPermission.requestPermissionWithDialog();
        return true;
    }
    //endregion

    //region methods
    private void bindLocStatus(@Nullable final LocStatus locStatus) {
        this.statusSubscription.unsubscribe();
        revertMode();
        if (locStatus == null || locStatus.getStatus() == null || LocationSettingsStatusCodes.RESOLUTION_REQUIRED != locStatus.getStatus().getStatusCode()) {
            presentUserLocationError();
        } else {
            try {
                locStatus.getStatus().startResolutionForResult(getActivity(), NightModeActivity.REQUEST_LOCATION_STATUS);
            } catch (final IntentSender.SendIntentException e) {
                presentUserLocationError();
            }
        }
    }

    private void presentUserLocationError() {
        if (presenterView == null) {
            return;
        }
        new SenseAlertDialog.Builder()
                .setTitle(R.string.nightmode_scheduled_error_title)
                .setMessage(R.string.nightmode_scheduled_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .build(getActivity())
                .show();
    }

    private void revertMode() {
        if (nightModeInteractor.getCurrentMode().equals(AppCompatDelegate.MODE_NIGHT_YES)) {
            this.presenterView.setAlwaysOnMode();
        } else {
            this.presenterView.setOffMode();
        }
    }


    private void setInitialMode() {
        if (nightModeInteractor == null) {
            this.presenterView.setOffMode();
            return;
        }
        if (nightModeInteractor.getCurrentMode().equals(AppCompatDelegate.MODE_NIGHT_AUTO) && locationInteractor.getCurrentUserLocation() == null) {
            this.presenterView.setOffMode();
            this.setMode(AppCompatDelegate.MODE_NIGHT_NO);
            return;
        }

        switch (nightModeInteractor.getCurrentMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                presenterView.setOffMode();
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                presenterView.setAlwaysOnMode();
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
                presenterView.setScheduledMode();
                break;
            default:
                presenterView.setOffMode();
        }

    }

    private void setMode(@AppCompatDelegate.NightMode final int mode) {
        if (nightModeInteractor == null || nightModeInteractor.getCurrentMode().equals(mode)) {
            return;
        }
        Analytics.trackNightMode(mode);
        nightModeInteractor.setMode(mode);
        getActivity().recreate();

    }
    //endregion
}
