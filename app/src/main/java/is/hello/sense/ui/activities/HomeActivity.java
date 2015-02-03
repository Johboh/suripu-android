package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.seismic.ShakeDetector;

import net.hockeyapp.android.UpdateManager;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.EnumSet;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineNavigatorFragment;
import is.hello.sense.ui.fragments.UndersideFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.widget.FragmentPageView;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.RateLimitingShakeListener;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class HomeActivity
        extends InjectionActivity
        implements FragmentPageView.Adapter<TimelineFragment>, FragmentPageView.OnTransitionObserver<TimelineFragment>, SlidingLayersView.OnInteractionListener, TimelineNavigatorFragment.OnTimelineDateSelectedListener
{
    public static final String EXTRA_IS_NOTIFICATION = HomeActivity.class.getName() + ".EXTRA_IS_NOTIFICATION";
    public static final String EXTRA_SHOW_UNDERSIDE = HomeActivity.class.getName() + ".EXTRA_SHOW_UNDERSIDE";

    private final PresenterContainer presenterContainer = new PresenterContainer();

    @Inject ApiService apiService;
    @Inject DevicesPresenter devicesPresenter;

    private long lastUpdated = Long.MAX_VALUE;

    private RelativeLayout rootContainer;
    private SlidingLayersView slidingLayersView;
    private FragmentPageView<TimelineFragment> viewPager;

    private @Nullable View deviceAlert;

    private boolean isFirstActivityRun;
    private boolean showUnderside;

    private @Nullable SensorManager sensorManager;
    private @Nullable ShakeDetector shakeDetector;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        presenterContainer.addPresenter(devicesPresenter);

        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.showUnderside = false;
            this.lastUpdated = savedInstanceState.getLong("lastUpdated");
            presenterContainer.onRestoreState(savedInstanceState);
        } else {
            this.showUnderside = getWillShowUnderside();
            if (NotificationRegistration.shouldRegister(this)) {
                new NotificationRegistration(this).register();
            }
        }

        devicesPresenter.update();


        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            this.shakeDetector = new ShakeDetector(new RateLimitingShakeListener(() -> {
                Intent intent = new Intent(this, DebugActivity.class);
                startActivity(intent);
            }));
        }


        this.rootContainer = (RelativeLayout) findViewById(R.id.activity_home_container);


        // noinspection unchecked
        this.viewPager = (FragmentPageView<TimelineFragment>) findViewById(R.id.activity_home_view_pager);
        viewPager.setFragmentManager(getFragmentManager());
        viewPager.setAdapter(this);
        viewPager.setOnTransitionObserver(this);
        if (viewPager.getCurrentFragment() == null) {
            TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight());
            viewPager.setCurrentFragment(fragment);
        }


        this.slidingLayersView = (SlidingLayersView) findViewById(R.id.activity_home_sliding_layers);
        slidingLayersView.setOnInteractionListener(this);
        slidingLayersView.setGestureInterceptingChild(viewPager);


        //noinspection PointlessBooleanExpression,ConstantConditions
        if (!BuildConfig.DEBUG && BuildConfig.DEBUG_SCREEN_ENABLED) {
            UpdateManager.register(this, getString(R.string.build_hockey_id));
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Observable<Intent> onLogOut = fromLocalBroadcast(getApplicationContext(), new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        bindAndSubscribe(onLogOut,
                         ignored -> {
                             Toast.makeText(getApplicationContext(), R.string.error_session_invalidated, Toast.LENGTH_SHORT).show();

                             startActivity(new Intent(this, OnboardingActivity.class));
                             finish();
                         },
                         Functions.LOG_ERROR);

        if (isFirstActivityRun && !getIntent().getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false)) {
            bindAndSubscribe(devicesPresenter.devices.take(1),
                             this::bindDevices,
                             this::devicesUnavailable);
        }

        checkInForUpdates();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("lastUpdated", lastUpdated);
        presenterContainer.onSaveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (shakeDetector != null && sensorManager != null) {
            shakeDetector.start(sensorManager);
        }

        if (!BuildConfig.DEBUG) {
            UpdateManager.register(this, getString(R.string.build_hockey_id));
        }

        if (showUnderside) {
            slidingLayersView.openWithoutAnimation();
            this.showUnderside = false;
        }

        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS && !isCurrentFragmentLastNight()) {
            Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
            TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight());
            viewPager.setCurrentFragment(fragment);


            Fragment navigatorFragment = getFragmentManager().findFragmentByTag(TimelineNavigatorFragment.TAG);
            if (navigatorFragment != null) {
                getFragmentManager().popBackStack();
            }
        }

        presenterContainer.onContainerResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (shakeDetector != null) {
            shakeDetector.stop();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            presenterContainer.onContainerDestroyed();
        }
    }

    //endregion


    @Override
    public void onBackPressed() {
        if(slidingLayersView.isOpen()) {
            UndersideFragment undersideFragment = (UndersideFragment) getFragmentManager().findFragmentById(R.id.activity_home_underside_container);
            if (undersideFragment != null && !undersideFragment.isAtStart()) {
                undersideFragment.jumpToStart();
            } else {
                slidingLayersView.close();
            }
        } else {
            super.onBackPressed();
        }
    }


    public boolean getWillShowUnderside() {
        return getIntent().getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false);
    }

    public boolean isCurrentFragmentLastNight() {
        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }


    public void checkInForUpdates() {
        bindAndSubscribe(apiService.checkInForUpdates(new UpdateCheckIn()),
                         response -> {
                             if (response.isNewVersion()) {
                                 AppUpdateDialogFragment dialogFragment = AppUpdateDialogFragment.newInstance(response);
                                 dialogFragment.show(getFragmentManager(), AppUpdateDialogFragment.TAG);
                             }
                         },
                         e -> Logger.error(HomeActivity.class.getSimpleName(), "Could not run update check in", e));
    }


    //region Fragment Adapter

    @Override
    public boolean hasFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return true;
    }

    @Override
    public TimelineFragment getFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().minusDays(1));
    }


    @Override
    public boolean hasFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        DateTime fragmentTime = fragment.getDate();
        return fragmentTime.isBefore(DateFormatter.lastNight().withTimeAtStartOfDay());
    }

    @Override
    public TimelineFragment getFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().plusDays(1));
    }


    @Override
    public void onWillTransitionToFragment(@NonNull FragmentPageView<TimelineFragment> view, @NonNull TimelineFragment fragment) {

    }

    @Override
    public void onDidTransitionToFragment(@NonNull FragmentPageView<TimelineFragment> view, @NonNull TimelineFragment fragment) {
        this.lastUpdated = System.currentTimeMillis();

        fragment.onTransitionCompleted();
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_DATE_CHANGED, null);
    }

    //endregion


    //region Alerts

    public void showDevices() {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(R.string.label_devices), DeviceListFragment.class, null);
        Intent intent = new Intent(this, FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }

    public void bindDevices(@NonNull ArrayList<Device> devices) {
        EnumSet<Device.Type> deviceTypes = Device.getDeviceTypes(devices);
        boolean hasSense = deviceTypes.contains(Device.Type.SENSE);
        boolean hasPill = deviceTypes.contains(Device.Type.PILL);
        if (!hasSense) {
            showDeviceAlert(R.string.alert_title_no_sense, R.string.alert_message_no_sense, this::showDevices);
        } else if (!hasPill) {
            showDeviceAlert(R.string.alert_title_no_pill, R.string.alert_message_no_pill, this::showDevices);
        } else {
            hideDeviceAlert();
        }
    }

    public void devicesUnavailable(Throwable e) {
        Logger.error(getClass().getSimpleName(), "Devices list was unavailable.", e);
        hideDeviceAlert();
    }

    public void showDeviceAlert(@StringRes int titleRes,
                                @StringRes int messageRes,
                                @NonNull Runnable action) {
        if (deviceAlert != null) {
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        this.deviceAlert = inflater.inflate(R.layout.item_bottom_alert, rootContainer, false);

        TextView title = (TextView) deviceAlert.findViewById(R.id.item_bottom_alert_title);
        title.setText(titleRes);

        TextView message = (TextView) deviceAlert.findViewById(R.id.item_bottom_alert_message);
        message.setText(messageRes);

        Button later = (Button) deviceAlert.findViewById(R.id.item_bottom_alert_later);
        Views.setSafeOnClickListener(later, ignored -> hideDeviceAlert());

        Button fixNow = (Button) deviceAlert.findViewById(R.id.item_bottom_alert_fix_now);
        Views.setSafeOnClickListener(fixNow, ignored -> {
            hideDeviceAlert();
            action.run();
        });

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) deviceAlert.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        deviceAlert.setVisibility(View.INVISIBLE);

        Views.observeNextLayout(deviceAlert).subscribe(container -> {
            int alertViewHeight = deviceAlert.getMeasuredHeight();
            int alertViewY = (int) deviceAlert.getY();

            deviceAlert.setY(alertViewY + alertViewHeight);
            deviceAlert.setVisibility(View.VISIBLE);

            animate(deviceAlert)
                    .y(alertViewY)
                    .start();
        });
        rootContainer.post(() -> rootContainer.addView(deviceAlert));
    }

    public void hideDeviceAlert() {
        if (deviceAlert == null) {
            return;
        }

        coordinator.postOnResume(() -> {
            int alertViewHeight = deviceAlert.getMeasuredHeight();
            int alertViewY = (int) deviceAlert.getY();
            animate(deviceAlert)
                    .y(alertViewY + alertViewHeight)
                    .addOnAnimationCompleted(finished -> {
                        rootContainer.removeView(deviceAlert);
                        this.deviceAlert = null;
                    })
                    .start();
        });
    }

    //endregion


    //region Sliding Layers

    public SlidingLayersView getSlidingLayersView() {
        return slidingLayersView;
    }

    @Override
    public void onUserWillPullDownTopView() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_OPENED, null);

        if (getFragmentManager().findFragmentById(R.id.activity_home_underside_container) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_home_underside_container, new UndersideFragment())
                    .commit();
        }

        viewPager.getCurrentFragment().onUserWillPullDownTopView();

        this.isFirstActivityRun = false;
    }

    @Override
    public void onUserDidPushUpTopView() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_CLOSED, null);

        Fragment underside = getFragmentManager().findFragmentById(R.id.activity_home_underside_container);
        if (underside != null) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(underside)
                    .commit();
        }

        viewPager.getCurrentFragment().onUserDidPushUpTopView();
    }

    //endregion


    //region Timeline Navigation

    public void showTimelineNavigator(@NonNull DateTime startDate) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_IN, null);

        ViewGroup undersideContainer = (ViewGroup) findViewById(R.id.activity_home_content_container);

        TimelineNavigatorFragment navigatorFragment = TimelineNavigatorFragment.newInstance(startDate);
        navigatorFragment.show(getFragmentManager(), 0, TimelineNavigatorFragment.TAG);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.executePendingTransactions();

        View view = navigatorFragment.getView();
        if (view == null) {
            throw new IllegalStateException();
        }
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (!navigatorFragment.isAdded() && !isDestroyed()) {
                    animate(viewPager)
                            .zoomInFrom(0.7f)
                            .addOnAnimationCompleted(finished -> {
                                if (finished) {
                                    undersideContainer.removeView(view);
                                }
                            })
                            .start();

                    fragmentManager.removeOnBackStackChangedListener(this);
                }
            }
        });

        undersideContainer.addView(view, 0);

        animate(viewPager)
                .zoomOutTo(View.GONE, 0.7f)
                .start();
    }

    @Override
    public void onTimelineDateSelected(@NonNull DateTime date) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_OUT, null);

        if (!date.equals(viewPager.getCurrentFragment().getDate())) {
            viewPager.setCurrentFragment(TimelineFragment.newInstance(date));
        }
        getFragmentManager().popBackStack();
    }

    //endregion
}
