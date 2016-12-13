package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;

import is.hello.sense.R;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.widget.ExtendedViewPager;


@SuppressLint("ViewConstructor")
public final class ViewPagerPresenterView extends PresenterView {

    private final ExtendedViewPager viewPager;
    private final TabLayout tabLayout;

    /**
     * @param fragment - Fragment providing initialization settings and callbacks.
     *                 Don't keep a reference to this.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment) {
        super(fragment.getActivity());
        this.viewPager = (ExtendedViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);
        this.tabLayout.setupWithViewPager(this.viewPager);
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
        this.viewPager.removeAllViews();
    }
    //endregion

    //region methods

    public void createTabsAndPager(@NonNull final ViewPagerPresenterFragment fragment) {
        final StaticFragmentAdapter.Item[] items = fragment.getViewPagerItems();

        // ViewPager
        final StaticFragmentAdapter adapter =
                new StaticFragmentAdapter(fragment.getDesiredFragmentManager(),
                                                      items);
        this.viewPager.setOffscreenPageLimit(0);
        this.viewPager.setAdapter(adapter);
        this.viewPager.setEnabled(true);

        // TabLayout
        tabLayout.removeAllTabs();
        for (final StaticFragmentAdapter.Item item : items) {
            this.tabLayout.addTab(this.tabLayout.newTab().setText(item.getTitle()));
        }
        final TabLayout.Tab firstTab = this.tabLayout.getTabAt(fragment.getStartingItemPosition());
        if (firstTab != null) {
            firstTab.select();
        }
    }

    public void hideTabLayout() {
        tabLayout.setVisibility(GONE);
    }

    public void lockViewPager(final int position) {
        viewPager.setScrollingEnabled(false);
        viewPager.setCurrentItem(position);
    }

    public void unlockViewPager() {
        viewPager.setScrollingEnabled(true);
    }

    public void hideTabsAfter(final int position) {
        if (position < 0) {
            return;
        }
        for (int i = position + 1; i < tabLayout.getTabCount(); i++) {
            tabLayout.removeTabAt(position);
        }
    }

    //endregion

}

