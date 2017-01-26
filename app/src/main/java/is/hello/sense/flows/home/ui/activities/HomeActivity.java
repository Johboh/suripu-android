package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.interactors.LastNightInteractor;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerFragment;
import is.hello.sense.flows.home.ui.views.SenseTabLayout;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UnreadStateInteractor;
import is.hello.sense.mvp.presenters.HomePresenterFragment;
import is.hello.sense.mvp.presenters.SoundsPresenterFragment;
import is.hello.sense.mvp.presenters.TrendsPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.FabPresenter;
import is.hello.sense.mvp.util.FabPresenterProvider;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionActivity;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.SystemAlertDialogFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

import static android.provider.AlarmClock.ACTION_SHOW_ALARMS;
import static is.hello.sense.util.Logger.info;


public class HomeActivity extends ScopedInjectionActivity
        implements
        Alert.ActionHandler,
        InsightInfoFragment.ParentProvider,
        TimelineFragment.ParentProvider,
        FabPresenterProvider,
        OnboardingFlowProvider,
        SenseTabLayout.Listener {

    private static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";
    private static final String KEY_CURRENT_ITEM_INDEX = HomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static boolean isFirstActivityRun = true; // changed when paused

    @Inject
    ApiService apiService;
    @Inject
    AlertsInteractor alertsInteractor;
    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;
    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    VoiceSettingsInteractor voiceSettingsInteractor;
    @Inject
    LastNightInteractor lastNightInteractor;
    @Inject
    UnreadStateInteractor unreadStateInteractor;
    private final HomeViewPagerDelegate viewPagerDelegate = new HomeViewPagerDelegate();
    private View progressOverlay;
    private SpinnerImageView spinner;
    private ExtendedViewPager extendedViewPager;
    private SenseTabLayout tabLayout;

    public static Intent getIntent(@NonNull final Context context,
                                   @OnboardingActivity.Flow final int fromFlow) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(HomeActivity.EXTRA_ONBOARDING_FLOW, fromFlow);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.deviceIssuesPresenter.bindScope(this);
        addInteractor(this.deviceIssuesPresenter);
        addInteractor(this.alertsInteractor);
        addInteractor(this.lastNightInteractor);
        addInteractor(this.unreadStateInteractor);
        setContentView(R.layout.activity_new_home);
        this.progressOverlay = findViewById(R.id.activity_new_home_progress_overlay);
        this.spinner = (SpinnerImageView) this.progressOverlay.findViewById(R.id.activity_new_home_spinner);
        this.extendedViewPager = (ExtendedViewPager) findViewById(R.id.activity_new_home_extended_view_pager);
        this.extendedViewPager.setScrollingEnabled(false);
        this.extendedViewPager.setFadePageTransformer(true);
        this.extendedViewPager.setOffscreenPageLimit(this.viewPagerDelegate.getOffscreenPageLimit());
        this.tabLayout = (SenseTabLayout) findViewById(R.id.activity_new_home_tab_layout);
        restoreState(savedInstanceState);
        this.tabLayout.setupWithViewPager(this.extendedViewPager);
        this.extendedViewPager.setAdapter(new StaticFragmentAdapter(getFragmentManager(),
                                                                    this.viewPagerDelegate.getViewPagerItems()));
        this.tabLayout.setUpTabs(savedInstanceState == null);
        this.tabLayout.setListener(this);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final IntentFilter loggedOutIntent = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
        final Observable<Intent> onLogOut = Rx.fromLocalBroadcast(getApplicationContext(), loggedOutIntent);
        bindAndSubscribe(onLogOut,
                         ignored -> finish(),
                         Functions.LOG_ERROR);
        if (shouldUpdateDeviceIssues()) {
            bindAndSubscribe(this.deviceIssuesPresenter.topIssue,
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }
        bindAndSubscribe(this.alertsInteractor.alert,
                         this::bindAlert,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.lastNightInteractor.timeline,
                         this.tabLayout::updateSleepScoreTab,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.unreadStateInteractor.hasUnreadItems,
                         this::bindUnreadItems,
                         Functions.LOG_ERROR);
        this.lastNightInteractor.update();
        checkInForUpdates();
    }

    @Override
    public void onBackPressed() {
        if (this.progressOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState, final PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, this.tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.lastNightInteractor.update();
        if (shouldUpdateAlerts()) {
            this.alertsInteractor.update();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isFirstActivityRun = false;
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_SHOW_ALARMS.equals(intent.getAction())) {
            final Properties properties =
                    Analytics.createProperties(Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME,
                                               "ACTION_SHOW_ALARMS");
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            this.tabLayout.selectSoundTab();
        } else if (intent.hasExtra(NotificationReceiver.EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(NotificationReceiver.EXTRA_NOTIFICATION_PAYLOAD));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.tabLayout != null) {
            this.tabLayout.clearOnTabSelectedListeners();
        }
    }

    public void checkInForUpdates() {
        bindAndSubscribe(this.apiService.checkInForUpdates(new UpdateCheckIn()),
                         response -> {
                             if (response.isNewVersion()) {
                                 final AppUpdateDialogFragment dialogFragment =
                                         AppUpdateDialogFragment.newInstance(response);
                                 dialogFragment.show(getFragmentManager(), AppUpdateDialogFragment.TAG);
                             }
                         },
                         e -> Logger.error(HomeActivity.class.getSimpleName(), "Could not run update check in", e));
    }


    private void restoreState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.tabLayout.setCurrentItemIndex(savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX,
                                                                         this.viewPagerDelegate.getStartingItemPosition()));
        } else {
            this.tabLayout.setCurrentItemIndex(this.viewPagerDelegate.getStartingItemPosition());
        }
    }

    //region Onboarding flow provider

    @OnboardingActivity.Flow
    public int getOnboardingFlow() {
        @OnboardingActivity.Flow
        final int flow =
                getIntent().getIntExtra(EXTRA_ONBOARDING_FLOW,
                                        OnboardingActivity.FLOW_NONE);
        return flow;
    }

    //end region

    //region Device Issues and Alerts

    private boolean isShowingAlert() {
        return getFragmentManager().findFragmentByTag(BottomAlertDialogFragment.TAG) != null
                || getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null;
    }

    private boolean shouldUpdateAlerts() {
        return getOnboardingFlow() != OnboardingActivity.FLOW_REGISTER;
    }

    private boolean shouldUpdateDeviceIssues() {
        return isFirstActivityRun && getOnboardingFlow() != OnboardingActivity.FLOW_REGISTER;
    }

    private boolean shouldShow(@NonNull final Alert alert) {
        final boolean valid = alert.isValid();
        final boolean existingAlert = this.isShowingAlert();
        switch (alert.getCategory()) {
            case EXPANSION_UNREACHABLE:
                return valid && !existingAlert; // always show valid unreacahable alerts whenever we get them
            case SENSE_MUTED:
            default:
                return valid && !existingAlert && isFirstActivityRun;
        }
    }

    private void bindUnreadItems(final boolean hasUnreadItems) {
        tabLayout.setHomeabIndicatorVisible(hasUnreadItems);
    }

    private void bindAlert(@NonNull final Alert alert) {
        if (shouldShow(alert)) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
            SystemAlertDialogFragment.newInstance(alert,
                                                  getResources())
                                     .showAllowingStateLoss(getFragmentManager(),
                                                            R.id.activity_new_home_bottom_alert_container,
                                                            BottomAlertDialogFragment.TAG);
        } else if (shouldUpdateDeviceIssues()) {
            this.deviceIssuesPresenter.update();
        }
    }

    private void bindDeviceIssue(@NonNull final DeviceIssuesInteractor.Issue issue) {
        if (issue == DeviceIssuesInteractor.Issue.NONE || this.isShowingAlert()) {
            return;
        }

        localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
        DeviceIssueDialogFragment.newInstance(issue,
                                              getResources())
                                 .showAllowingStateLoss(getFragmentManager(),
                                                        R.id.activity_new_home_bottom_alert_container,
                                                        DeviceIssueDialogFragment.TAG);

        this.deviceIssuesPresenter.updateLastShown(issue);
    }

    //endregion

    //region Alert Action Handler

    @Override
    public void unMuteSense() {
        showProgressOverlay(true);
        this.voiceSettingsInteractor.setSenseId(this.preferencesInteractor.getString(PreferencesInteractor.PAIRED_SENSE_ID,
                                                                                     VoiceSettingsInteractor.EMPTY_ID));
        track(this.voiceSettingsInteractor.setMuted(false)
                                          .subscribe(Functions.NO_OP,
                                                     e -> {
                                                         showProgressOverlay(false);
                                                         ErrorDialogFragment.presentError(this,
                                                                                          e,
                                                                                          R.string.voice_settings_update_error_title);
                                                     },
                                                     () -> showProgressOverlay(false))
             );
    }

    //endregion

    public void showProgressOverlay(final boolean show) {
        this.progressOverlay.post(() -> {
            if (show) {
                this.progressOverlay.bringToFront();
                this.spinner.startSpinning();
                this.progressOverlay.setVisibility(View.VISIBLE);
            } else {
                this.spinner.stopSpinning();
                this.progressOverlay.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public TimelineFragment.Parent getTimelineParent() {
        return (TimelineFragment.Parent) getFragmentWithIndex(SenseTabLayout.SLEEP_ICON_KEY);
    }

    @Override
    public FabPresenter getFabPresenter() {
        return (FabPresenter) getFragmentWithIndex(SenseTabLayout.SOUNDS_ICON_KEY);
    }

    @Nullable
    @Override
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final Fragment parentProvider = getFragmentWithIndex(SenseTabLayout.INSIGHTS_ICON_KEY);
        if (parentProvider instanceof InsightInfoFragment.ParentProvider) {
            return ((InsightInfoFragment.ParentProvider) parentProvider).provideInsightInfoParent();
        } else {
            return null;
        }
    }

    @Nullable
    private Fragment getFragmentWithIndex(final int index) {
        if (extendedViewPager != null && extendedViewPager.getAdapter() instanceof StaticFragmentAdapter) {
            return ((StaticFragmentAdapter) extendedViewPager.getAdapter()).getFragmentAtPosition(index);
        }
        return null;
    }

    //region Notifications

    private void dispatchNotification(@NonNull final Bundle notification) {
        this.stateSafeExecutor.execute(() -> {
            info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            final Notification target = Notification.fromBundle(notification);
            switch (target) {
                case TIMELINE: {
                    this.tabLayout.selectTimelineTab();
                    //todo support scrolling to date.
                    break;
                }
                case SENSOR: {
                    this.tabLayout.selectTimelineTab();
                    break;
                }
                case TRENDS: {
                    this.tabLayout.selectTrendsTab();
                    break;
                }
                case ALARM: {
                    this.tabLayout.selectSoundTab();
                    break;
                }
                case SETTINGS: {
                    //todo start AppSettingsActivity after merging.
                    break;
                }
                case INSIGHTS: {
                    this.tabLayout.selectHomeTab();
                    break;
                }
                case CONDITIONS: {
                    this.tabLayout.selectConditionsTab();
                    break;
                }
            }
        });
    }

    //endregion

    //region SenseTabLayout.Listener
    @Override
    public void scrollUp(final int fragmentPosition) {
        final Fragment fragment = getFragmentWithIndex(fragmentPosition);
        if (fragment instanceof HomeActivity.ScrollUp) {
            ((HomeActivity.ScrollUp) fragment).scrollUp();
        }
    }

    @Override
    public void jumpToLastNight() {
        final TimelineFragment.Parent parent = getTimelineParent();
        if (parent != null) {
            parent.jumpToLastNight();
        }
    }

    @Override
    public void tabChanged(final int fragmentPosition) {
        if (!this.lastNightInteractor.timeline.hasValue()) {
            this.lastNightInteractor.update();
        }
        this.extendedViewPager.setCurrentItem(fragmentPosition);
    }

    @Nullable
    @Override
    public Timeline getCurrentTimeline() {
        if (this.lastNightInteractor.timeline.hasValue()) {
            return this.lastNightInteractor.timeline.getValue();
        }
        return null;
    }
    //endregion


    public interface ScrollUp {
        void scrollUp();
    }

    private static class HomeViewPagerDelegate extends BaseViewPagerPresenterDelegate {

        @NonNull
        @Override
        public StaticFragmentAdapter.Item[] getViewPagerItems() {
            return new StaticFragmentAdapter.Item[]{
                    new StaticFragmentAdapter.Item(TimelinePagerFragment.class, TimelinePagerFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(TrendsPresenterFragment.class, TrendsPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(HomePresenterFragment.class, HomePresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(SoundsPresenterFragment.class, SoundsPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(RoomConditionsPresenterFragment.class, RoomConditionsPresenterFragment.class.getSimpleName())
            };
        }

        @Override
        public int getOffscreenPageLimit() {
            return 4;
        }
    }


}