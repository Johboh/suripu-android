package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.ui.widget.ExtendedViewPager;


@SuppressLint("ViewConstructor")
public class ViewPagerPresenterView extends PresenterView {

    private final ExtendedViewPager viewPager;
    private final TabLayout tabLayout;
    private final FloatingActionButton fab;
    private final Animation fabLoadingAnimation;

    /**
     * @param fragment - Fragment providing initialization settings and callbacks.
     *                 Don't keep a reference to this.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment) {
        super(fragment.getActivity());
        this.viewPager = (ExtendedViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);
        this.tabLayout.setupWithViewPager(this.viewPager);
        this.fabLoadingAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_360);
        this.fabLoadingAnimation.setRepeatCount(Animation.INFINITE);
        this.fab = (FloatingActionButton) findViewById(R.id.view_view_pager_fab);
        createTabsAndPager(fragment);
    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.view_view_pager_view;
    }

    @Override
    public void releaseViews() {
        this.tabLayout.removeAllViews();
        this.tabLayout.removeAllTabs();
        this.viewPager.removeAllViews();
        this.fab.setOnClickListener(null);
    }
    //endregion

    //region methods
    public void unlockViewPager(@NonNull final ViewPagerPresenterFragment fragment) {
        createTabsAndPager(fragment);
        this.viewPager.setScrollingEnabled(true);
    }

    public void lockViewPager(final int startPosition) {
        removeTabs();
        this.viewPager.setScrollingEnabled(false);
        this.viewPager.setCurrentItem(startPosition);
    }

    private void createTabsAndPager(@NonNull final ViewPagerPresenterFragment fragment) {

        final StaticFragmentAdapter.Item[] items = fragment.getViewPagerItems();

        // ViewPager
        final StaticFragmentAdapter adapter =
                new StaticFragmentAdapter(fragment.getDesiredFragmentManager(),
                                          items);
        this.viewPager.setOffscreenPageLimit(fragment.getOffscreenPageLimit());
        this.viewPager.setAdapter(adapter);
        this.viewPager.setEnabled(true);

        // TabLayout
        this.tabLayout.removeAllTabs();
        for (final StaticFragmentAdapter.Item item : items) {
            this.tabLayout.addTab(this.tabLayout.newTab().setText(item.getTitle()));
        }
        final TabLayout.Tab firstTab = this.tabLayout.getTabAt(fragment.getStartingItemPosition());
        if (firstTab != null) {
            firstTab.select();
        }
        setTabLayoutVisible(true);
    }

    private void setTabLayoutVisible(final boolean visible) {
        this.tabLayout.setVisibility(visible ? VISIBLE : GONE);
    }

    private void removeTabs() {
        this.tabLayout.removeAllTabs();
        setTabLayoutVisible(false);
    }

    public void addViewPagerListener(final ViewPager.OnPageChangeListener listener) {
        this.viewPager.addOnPageChangeListener(listener);
    }

    public void removeViewPagerListener(final ViewPager.OnPageChangeListener listener) {
        this.viewPager.removeOnPageChangeListener(listener);
    }

    @Nullable
    public Fragment getCurrentFragment() {
        if (this.viewPager.getAdapter() instanceof StaticFragmentAdapter) {
            return ((StaticFragmentAdapter) this.viewPager.getAdapter()).getFragmentAtPosition(this.viewPager.getCurrentItem());
        }
        return null;
    }

    //endregion

    //region fab methods

    public void setFabSize(final float size) {
        if (size >= 0.5) {
            this.fab.setScaleX(size);
            this.fab.setScaleY(size);
            this.fab.setAlpha(Anime.interpolateFloats(size, 0, 2) - 1);
        } else {
            this.fab.setAlpha(0f);
        }
    }

    public void setFabVisible(final boolean visible) {
        if (visible) {
            this.fab.show();
        } else {
            this.fab.hide();
        }
    }

    public void updateFab(final @DrawableRes int resource,
                          final @Nullable View.OnClickListener listener) {
        this.setFabLoading(false);
        this.fab.setOnClickListener(listener);
        this.fab.setImageResource(resource);
    }

    public void setFabLoading(final boolean loading) {
        this.fab.setClickable(!loading);
        this.fab.setLongClickable(!loading);
        this.fab.setFocusable(!loading);
        if (loading) {
            this.fab.setOnClickListener(null);
            this.fab.setImageResource(R.drawable.sound_loading_icon);
            final Animation currentAnimation = this.fab.getAnimation();
            if (!(fabLoadingAnimation.equals(currentAnimation) && currentAnimation.hasStarted())) {
                this.fab.startAnimation(fabLoadingAnimation);
            }
        } else {
            this.fab.clearAnimation();
        }
    }
    //endregion

}

