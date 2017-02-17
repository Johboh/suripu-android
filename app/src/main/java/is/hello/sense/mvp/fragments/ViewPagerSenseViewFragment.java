package is.hello.sense.mvp.fragments;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;

import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.ui.fragments.SoundsSenseViewFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.ViewPagerPresenter;
import is.hello.sense.mvp.view.ViewPagerSenseView;
import is.hello.sense.util.NotTested;

/**
 * Any class Fragment that wants to host fragments should extend this.
 */
@NotTested
public abstract class ViewPagerSenseViewFragment extends ControllerSenseViewFragment<ViewPagerSenseView>
        implements ViewPagerPresenter,
        ViewPager.OnPageChangeListener,
        HomeActivity.ScrollUp {

    //region Instance Fields
    /**
     * Used to hold state of the scroll view.
     */
    private boolean isScrolling = false;
    /**
     * Position before the user began scrolling.
     */
    private int lastPosition = 0;
    /**
     * Position the user is currently at ( used if scrolling)
     */
    private int currentlyScrolledPosition = 0;

    /**
     * So we don't have to do null checks.
     */
    private final FabListener emptyListener = new EmptyFabListener();

    /**
     * Provided for telling this Fragment to update the Fab.
     */
    private final NotificationListener notificationListener = new HelperNotificationListener();

    private BaseViewPagerPresenterDelegate viewPagerDelegate;
    //endregion

    //region ControllerPresenterFragment
    @Override
    public final void initializeSenseView() {
        if (this.senseView == null) {
            this.viewPagerDelegate = newViewPagerDelegateInstance();

            this.senseView = new ViewPagerSenseView(this,
                                                    this::onFabClicked);
        }
    }

    @CallSuper
    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (shouldAddViewPagerListener()) {
            senseView.addViewPagerListener(this);
        }
    }

    @CallSuper
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shouldAddViewPagerListener()) {
            if (this.senseView != null) {
                this.senseView.removeViewPagerListener(this);
            }
        }
        super.onDestroyView();
    }

    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        final StaticFragmentAdapter.Controller controller = getCurrentController();
        if (controller == null || !controller.hasPresenterView()) {
            return;
        }
        controller.setVisibleToUser(isVisible);
        if (shouldAddViewPagerListener()) {
            getCurrentFabListener().setNotificationListener(this.notificationListener);
        }
    }

    //endregion

    //region ViewPager.ViewPagePresenter
    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        // should never happen but lets be safe.
        if (this.viewPagerDelegate == null) {
            return new StaticFragmentAdapter.Item[0];
        }
        return this.viewPagerDelegate.getViewPagerItems();
    }

    @Override
    public int getStartingItemPosition() {
        // should never happen but lets be safe.
        if (this.viewPagerDelegate == null) {
            return BaseViewPagerPresenterDelegate.DEFAULT_STARTING_ITEM_POSITION;
        }
        return this.viewPagerDelegate.getStartingItemPosition();
    }

    @Override
    public int getOffscreenPageLimit() {
        if (this.viewPagerDelegate == null) {
            return BaseViewPagerPresenterDelegate.DEFAULT_OFFSCREEN_PAGE_LIMIT;
        }
        return this.viewPagerDelegate.getOffscreenPageLimit();
    }
    //endregion

    //region HomeActivity.ScrollUp
    @Override
    public void scrollUp() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof HomeActivity.ScrollUp) {
            ((HomeActivity.ScrollUp) fragment).scrollUp();
        }
    }
    //endregion

    //region Methods

    /**
     * Override this with your own view pager delegate.
     *
     * @return the view pager delegate to be used.
     */
    @NonNull
    protected abstract BaseViewPagerPresenterDelegate newViewPagerDelegateInstance();

    /**
     * Override this to use a different fragment manager
     *
     * @return false for {@link #getFragmentManager()} or true for {@link #getChildFragmentManager()}
     */
    protected boolean useChildFragmentManager() {
        return true;
    }

    /**
     * @return true will add this class's ViewPager.OnPageScrollListener. Should be false unless the
     * Fab is needed. See {@link SoundsSenseViewFragment} for
     * example.
     */
    protected boolean shouldAddViewPagerListener() {
        return false;
    }

    public FragmentManager getDesiredFragmentManager() {
        return useChildFragmentManager() ? getChildFragmentManager() : getFragmentManager();
    }

    @Nullable
    public Fragment getCurrentFragment() {
        if (this.senseView == null) {
            return null;
        }
        return this.senseView.getCurrentFragment();
    }


    /**
     * Will ignore if scrolling.
     *
     * @param ignored ignored
     */
    private void onFabClicked(@NonNull final View ignored) {
        if (isScrolling) {
            return;
        }
        getCurrentFabListener().onFabClick();
    }

    /**
     * Quick update. Will call {@link #updateFab(float, int)} with positionOffset equal to 1.
     *
     * @param position position to update.
     */
    private void updateFab(final int position) {
        updateFab(1f, position);
    }

    /**
     * Update the Fab UI based on the FabListener settings provided by the fragment at given position
     * If the fragment at this position doesn't implement {@link FabListener} the Fab will be set to
     * {@link View#GONE}.
     *
     * @param positionOffset should be <= 0 and <= 1. Will scale the fab button size based on this value.
     * @param position       position of Fragment to get FabListener for.
     */
    private void updateFab(final float positionOffset, final int position) {
        if (positionOffset < 0 || positionOffset > 1f) { // Hit the edge of the screen. Ignore.
            return;
        }
        final Fragment fragment = this.senseView.getFragmentAtPos(position);
        if (fragment instanceof FabListener) {
            final FabListener fabListener = (FabListener) fragment;
            if (!fabListener.hasNotificationListener()) {
                fabListener.setNotificationListener(notificationListener);
            }
            if (fabListener.shouldShowFab()) {
                this.senseView.setFabRotating(fabListener.shouldFabRotate());
                this.senseView.setFabSizeAndAlpha(Math.min(1f, Math.abs(1f - positionOffset * 2)));
                this.senseView.setFabResource(fabListener.getFabDrawableRes());
                this.senseView.setFabVisible(true);
                return;
            }
        }
        this.senseView.setFabSizeAndAlpha(0);
        this.senseView.setFabVisible(false);
    }

    @Nullable
    private StaticFragmentAdapter.Controller getCurrentController() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof StaticFragmentAdapter.Controller) {
            return (StaticFragmentAdapter.Controller) fragment;
        }
        return null;
    }


    @NonNull
    private FabListener getCurrentFabListener() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof FabListener) {
            return ((FabListener) fragment);
        }
        return this.emptyListener;
    }

    //endregion

    //region ViewPager.OnPageChangeListener

    /**
     * @param position             As soon as you start scrolling left it becomes the position of
     *                             the view left of it. If you're scrolling right it will remain the
     *                             position of the current view. Having only two views makes it
     *                             difficult to tell.
     * @param positionOffset       Some value between 0 and 1 based on how far you've scrolled. Can
     *                             be some super large/small number of your finger is at the edge of
     *                             the screen.
     * @param positionOffsetPixels ignored
     */
    @Override
    public void onPageScrolled(final int position,
                               final float positionOffset,
                               final int positionOffsetPixels) {

        if (isScrolling) {
            final int positionToUse;
            if (position < lastPosition) { // A. Scrolling left.

                if (positionOffset < .5f) { // B. We're closer to the previous fragment than current.

                    if (lastPosition == 0) { // C. Nothing left of it. Use this.
                        positionToUse = lastPosition;

                    } else { // C. Use fragment left of current.
                        positionToUse = lastPosition - 1;
                    }

                } else { // B. We're closer to the current fragment than previous
                    positionToUse = lastPosition;
                }


            } else { // A. Scrolling right.

                if (positionOffset >= .5f) { // B. We're closer to the next fragment than current.

                    if (lastPosition == this.senseView.getAdapterChildCount() - 1) { // C. Nothing right of it. Use this.
                        positionToUse = lastPosition;

                    } else { // C. Use fragment right of current.
                        positionToUse = lastPosition + 1;
                    }

                } else {// B. We're closer to the current fragment than next
                    positionToUse = lastPosition;
                }
            }

            currentlyScrolledPosition = positionToUse;
            updateFab(positionOffset, positionToUse);
        } else {
            lastPosition = position;
        }
    }

    @Override
    public void onPageSelected(final int position) {
        updateFab(position);
    }

    /**
     * Used to track when the user begins/stops scrolling.
     *
     * @param state provided by {@link ViewPager}
     */
    @Override
    public void onPageScrollStateChanged(final int state) {
        this.isScrolling = state == ViewPager.SCROLL_STATE_DRAGGING;
        if (!isScrolling) {
            currentlyScrolledPosition = this.senseView.getCurrentItemPosition();
            updateFab(currentlyScrolledPosition);
        }
    }
    //endregion

    private final class HelperNotificationListener implements NotificationListener {

        @Override
        public void notifyChange() {
            if (ViewPagerSenseViewFragment.this.senseView != null && !isScrolling) {
                updateFab(ViewPagerSenseViewFragment.this.senseView.getCurrentItemPosition());
            }
        }
    }

    private final class EmptyFabListener implements FabListener {
        @Override
        public void onFabClick() {
            // do nothing
        }

        @Override
        public boolean shouldShowFab() {
            return false;
        }

        @Override
        public int getFabDrawableRes() {
            return 0;
        }

        @Override
        public void setNotificationListener(@NonNull final NotificationListener notificationListener) {
            // do nothing
        }

        @Override
        public boolean hasNotificationListener() {
            return true;
        }

        @Override
        public boolean shouldFabRotate() {
            return false;
        }
    }

    public interface FabListener {
        void onFabClick();

        boolean shouldShowFab();

        @DrawableRes
        int getFabDrawableRes();

        void setNotificationListener(@NonNull NotificationListener notificationListener);

        boolean hasNotificationListener();

        boolean shouldFabRotate();

    }

    public interface NotificationListener {
        void notifyChange();
    }


}