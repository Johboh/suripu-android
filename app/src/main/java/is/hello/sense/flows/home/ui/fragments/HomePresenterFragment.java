package is.hello.sense.flows.home.ui.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.model.v2.alerts.Category;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.interactors.LastNightInteractor;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.views.HomeTabLayout;
import is.hello.sense.flows.home.ui.views.HomeView;
import is.hello.sense.flows.home.util.HomeFragmentPagerAdapter;
import is.hello.sense.flows.home.util.HomeViewPagerPresenterDelegate;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.Scope;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UnreadStateInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationInteractor;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.SystemAlertDialogFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.util.Logger;

import static is.hello.sense.flows.home.ui.activities.HomeActivity.EXTRA_HOME_NAV_INDEX;
import static is.hello.sense.flows.home.ui.activities.HomeActivity.EXTRA_HOME_SHOW_ALERTS;


public class HomePresenterFragment extends PresenterFragment<HomeView>
        implements
        OnBackPressedInterceptor,
        HomeTabLayout.Listener,
        TimelineFragment.ParentProvider,
        InsightInfoFragment.ParentProvider,
        Alert.ActionHandler {
    public static final String TAG = HomePresenterFragment.class.getSimpleName();
    private HomeFragmentPagerAdapter adapter = null;

    @Inject
    ApiService apiService;
    @Inject
    AlertsInteractor alertsInteractor;
    @Inject
    DeviceIssuesInteractor deviceIssuesInteractor;
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
    @Inject
    NotificationInteractor notificationInteractor;
    @Inject
    NightModeInteractor nightModeInteractor;

    private final HomeViewPagerPresenterDelegate viewPagerDelegate = new HomeViewPagerPresenterDelegate();
    private boolean shouldShowAlerts = true;

    public static HomePresenterFragment newInstance(@Nullable final Bundle args) {
        final HomePresenterFragment fragment = new HomePresenterFragment();
        fragment.setArguments(args);
        return fragment;
    }

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.adapter = createAdapter();
            this.presenterView = new HomeView(getActivity(),
                                              this.viewPagerDelegate.getOffscreenPageLimit(),
                                              this.adapter);
        }
    }


    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(getActivity() instanceof Scope)) {
            throw new IllegalStateException("Activity must implement Scope");
        }
        if (!(getActivity() instanceof OnboardingFlowProvider)) {
            throw new IllegalStateException("Activity must implement OnboardingFlowProvider");
        }
        this.deviceIssuesInteractor.bindScope((Scope) getActivity());
        addInteractor(this.deviceIssuesInteractor);
        addInteractor(this.alertsInteractor);
        addInteractor(this.lastNightInteractor);
        addInteractor(this.unreadStateInteractor);
        addInteractor(this.nightModeInteractor);

        if (savedInstanceState == null) {
            final Bundle args = getArguments();
            if (args != null) {
                this.shouldShowAlerts = args.getBoolean(EXTRA_HOME_SHOW_ALERTS, true);
                this.presenterView.setCurrentItem(args.getInt(EXTRA_HOME_NAV_INDEX,
                                                              this.viewPagerDelegate.getStartingItemPosition()));
            } else {
                this.shouldShowAlerts = true;
                this.presenterView.setCurrentItem(this.viewPagerDelegate.getStartingItemPosition());
            }
        } else {
            this.shouldShowAlerts = false;
        }

        this.presenterView.setTabListener(this);

        if (shouldUpdateDeviceIssues()) {
            bindAndSubscribe(this.deviceIssuesInteractor.topIssue,
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }
        this.alertsInteractor.alert.forget();
        bindAndSubscribe(this.alertsInteractor.alert,
                         this::bindAlert,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.lastNightInteractor.timeline,
                         this::bindLastNightTimeline,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.unreadStateInteractor.hasUnreadItems,
                         this::bindUnreadItems,
                         Functions.LOG_ERROR);
        this.lastNightInteractor.update();
        this.unreadStateInteractor.update();
        checkInForUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.shouldShowAlerts = false;
        if (this.adapter != null) {
            this.adapter.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.adapter != null) {
            this.adapter.onResume();
        }
        this.lastNightInteractor.update();
        if (shouldUpdateAlerts()) {
            this.alertsInteractor.alert.forget();
            this.alertsInteractor.update();
        }
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        this.adapter = null;
    }
//endregion

    //region OnBackPressedInterceptor

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (this.presenterView.isProgressOverlayShowing()) {
            return true;
        }
        defaultBehavior.run();
        return true;
    }

    //endregion

    //region SenseTabLayout.Listener
    @Override
    public void scrollUp(final int fragmentPosition) {
        final Fragment fragment = this.presenterView.getFragmentWithIndex(fragmentPosition);
        if (fragment instanceof HomeActivity.ScrollUp) {
            ((HomeActivity.ScrollUp) fragment).scrollUp();
        }
    }

    @Override
    public void tabChanged(final int fragmentPosition) {
        if (!this.lastNightInteractor.timeline.hasValue()) {
            this.lastNightInteractor.update();
        }
        this.unreadStateInteractor.update();
        this.presenterView.setCurrentItem(fragmentPosition);
        if (fragmentPosition == HomeViewPagerPresenterDelegate.SLEEP_ICON_KEY) {
            jumpToLastNight();
        }
        this.nightModeInteractor.updateIfAuto();
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

    //region Timeline Parent Provider
    @Nullable
    @Override
    public TimelineFragment.Parent getTimelineParent() {
        final Fragment fragment = this.presenterView.getFragmentWithIndex(HomeViewPagerPresenterDelegate.SLEEP_ICON_KEY);
        if (fragment instanceof TimelineFragment.Parent) {
            return (TimelineFragment.Parent) fragment;
        }
        return null;
    }

    //endregion
    //region InsightInfoFragment Parent
    @Nullable
    @Override
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final Fragment parentProvider = this.presenterView.getFragmentWithIndex(this.viewPagerDelegate.FEED_ICON_KEY);
        if (parentProvider instanceof InsightInfoFragment.ParentProvider) {
            return ((InsightInfoFragment.ParentProvider) parentProvider).provideInsightInfoParent();
        } else {
            return null;
        }
    }

    //endregion
    //Alert Actionhandler
    @Override
    public void handleAlert(@NonNull final Category alertCategory) {
        switch (alertCategory) {
            case SENSE_MUTED:
                this.unMuteSense();
                break;
            case REVIEW_ACCOUNTS_PAIRED_TO_SENSE:
                //todo handle
                break;
            case SENSE_NOT_PAIRED:
            case SENSE_NOT_SEEN:
            case SLEEP_PILL_NOT_PAIRED:
            case SLEEP_PILL_NOT_SEEN:
                this.startDevicesActivity();
                break;
            case EXPANSION_UNREACHABLE:
            case UNKNOWN:
                //do nothing
                break;
            default:
                throw new IllegalStateException("unhandled alert category " + alertCategory);
        }
    }


    @VisibleForTesting
    protected void unMuteSense() {
        this.presenterView.showProgressOverlay(true);
        this.voiceSettingsInteractor.setSenseId(this.preferencesInteractor.getString(PreferencesInteractor.PAIRED_SENSE_ID,
                                                                                     VoiceSettingsInteractor.EMPTY_ID));
        track(this.voiceSettingsInteractor.setMuted(false)
                                          .subscribe(Functions.NO_OP,
                                                     e -> {
                                                         this.presenterView.showProgressOverlay(false);
                                                         ErrorDialogFragment.presentError(getActivity(),
                                                                                          e,
                                                                                          R.string.voice_settings_update_error_title);
                                                     },
                                                     () -> this.presenterView.showProgressOverlay(false))
             );
    }

    @VisibleForTesting
    protected void startDevicesActivity() {
        DeviceListFragment.startStandaloneFrom(getActivity());
    }

    //endregion
    //region methods
    public void forwardNotification(@NonNull final Notification notification) {
        if (this.notificationInteractor == null) {
            return;
        }
        this.notificationInteractor.onNext(notification);
    }

    public void showProgressOverlay(final boolean show) {
        this.presenterView.showProgressOverlay(show);
    }


    /**
     * Immediately go to the {@link TimelineFragment} for last night.
     */
    public void jumpToLastNight() {
        final TimelineFragment.Parent parent = getTimelineParent();
        if (parent != null) {
            parent.jumpToLastNight();
        }
    }

    /**
     * @return true to check for updates.
     */
    private boolean shouldUpdateDeviceIssues() {
        return this.shouldShowAlerts && shouldUpdateAlerts();
    }

    /**
     * I'm not sure what this is checking.
     */
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

    /**
     * Check if we're already showing an alert.
     *
     * @return true if an alert is showing.
     */
    private boolean isShowingAlert() {
        return getFragmentManager().findFragmentByTag(BottomAlertDialogFragment.TAG) != null
                || getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null;
    }

    /**
     * Checks if this is a registration flow.
     *
     * @return true if this flow is for a registered user.
     */
    private boolean shouldUpdateAlerts() {
        return ((OnboardingFlowProvider) getActivity()).getOnboardingFlow() != OnboardingActivity.FLOW_REGISTER;
    }

    /**
     * @param alert important information to show the user.
     * @return true when there is an alert to show and none are currently showing.
     */
    private boolean shouldShow(@NonNull final Alert alert) {
        final boolean valid = alert.isValid();
        final boolean existingAlert = this.isShowingAlert();
        switch (alert.getCategory()) {
            case EXPANSION_UNREACHABLE:
                return valid && !existingAlert; // always show valid unreachable alerts whenever we get them
            case SENSE_MUTED:
            default:
                return valid && !existingAlert && this.shouldShowAlerts;
        }
    }


    /**
     * Will tell the view to display an indicator on the FeedTab.
     *
     * @param hasUnreadItems true to show indicator.
     */
    private void bindUnreadItems(final boolean hasUnreadItems) {
        this.presenterView.showUnreadIndicatorOnTab(hasUnreadItems,
                                                    this.viewPagerDelegate.FEED_ICON_KEY);
    }

    /**
     * Will show the alert if necessary.
     *
     * @param alert most important alert to show.
     */
    private void bindAlert(@NonNull final Alert alert) {
        if (shouldShow(alert)) {
            this.localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
            final SystemAlertDialogFragment alertDialogFragment = SystemAlertDialogFragment.newInstance(alert,
                                                                                                        getResources());
            alertDialogFragment.setTargetFragment(this, 0);
            alertDialogFragment.showAllowingStateLoss(getFragmentManager(),
                                                      R.id.view_home_bottom_alert_container,
                                                      BottomAlertDialogFragment.TAG);
        } else if (shouldUpdateDeviceIssues()) {
            this.deviceIssuesInteractor.update();
        }
    }

    /**
     * Show an issue.
     *
     * @param issue important issue to show.
     */
    private void bindDeviceIssue(@NonNull final DeviceIssuesInteractor.Issue issue) {
        if (issue == DeviceIssuesInteractor.Issue.NONE || isShowingAlert() || !this.shouldShowAlerts) {
            return;
        }
        this.shouldShowAlerts = false;
        this.localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
        DeviceIssueDialogFragment.newInstance(issue,
                                              getResources())
                                 .showAllowingStateLoss(getFragmentManager(),
                                                        R.id.view_home_bottom_alert_container,
                                                        DeviceIssueDialogFragment.TAG);
        this.deviceIssuesInteractor.updateLastShown(issue);
    }

    /**
     * Used to render a {@link is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable}
     * based on the users last night of sleep data.
     *
     * @param timeLine for last night.
     */
    private void bindLastNightTimeline(@Nullable final Timeline timeLine) {
        this.presenterView.updateTabWithSleepScore(timeLine,
                                                   HomeViewPagerPresenterDelegate.SLEEP_ICON_KEY);
    }

    public int getCurrentTabPosition() {
        if (this.presenterView == null) {
            return this.viewPagerDelegate.getStartingItemPosition();
        }
        return this.presenterView.getCurrentItem();
    }

    @VisibleForTesting
    @NonNull
    protected HomeFragmentPagerAdapter createAdapter() {
        return new HomeFragmentPagerAdapter(getChildFragmentManager(),
                                            HomeViewPagerPresenterDelegate.SLEEP_ICON_KEY,
                                            viewPagerDelegate.getViewPagerItems());
    }

    public void selectTimelineTab() {
        this.presenterView.setCurrentItem(HomeViewPagerPresenterDelegate.SLEEP_ICON_KEY);
    }

    public void selectTrendsTab() {
        this.presenterView.setCurrentItem(this.viewPagerDelegate.TRENDS_ICON_KEY);
    }

    public void selectFeedTab() {
        this.presenterView.setCurrentItem(this.viewPagerDelegate.FEED_ICON_KEY);
    }

    public void selectSoundTab() {
        this.presenterView.setCurrentItem(this.viewPagerDelegate.SOUNDS_ICON_KEY);
    }

    public void selectConditionsTab() {
        this.presenterView.setCurrentItem(HomeViewPagerPresenterDelegate.CONDITIONS_ICON_KEY);
    }

    //endregion

}
