package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.ToggleButton;

import com.zendesk.logger.Logger;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.AppSettingsFragment;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsFragment;
import is.hello.sense.flows.home.ui.fragments.SoundsFragment;
import is.hello.sense.flows.home.ui.fragments.TrendsFragment;
import is.hello.sense.flows.home.ui.fragments.VoiceFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.widget.SelectorView;

/**
 * Will eventually replace {@link HomeActivity}
 */

public class NewHomeActivity extends ScopedInjectionActivity
        implements SelectorView.OnSelectionChangedListener,
        FragmentNavigation{

    private static final String KEY_CURRENT_ITEM_INDEX = NewHomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static final int DEFAULT_ITEM_INDEX = 2;
    private SelectorView bottomSelectorView;
    private FragmentNavigationDelegate fragmentNavigationDelegate;
    //todo these are the tags FragmentNavigationDelegate uses when making transactions
    // heavy dependence on fragment.class.getSimpleName()
    private final String TIMELINE_TAG = "RoomConditionsFragment";
    private final String TRENDS_TAG = "TrendsFragment";
    private final String HOME_TAG = "VoiceFragment";
    private final String SOUNDS_TAG = "SoundsFragment";
    private final String CONDITIONS_TAG = "AppSettingsFragment";
    private int currentItemIndex;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_home);
        restoreState(savedInstanceState);
        this.fragmentNavigationDelegate = new FragmentNavigationDelegate(this,
                                                                         R.id.activity_new_home_backside_container,
                                                                         stateSafeExecutor);
        this.bottomSelectorView = (SelectorView) findViewById(R.id.activity_new_home_bottom_selector_view);
        initSelector(bottomSelectorView);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, currentItemIndex);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(this.bottomSelectorView != null){
            this.bottomSelectorView.setOnSelectionChangedListener(null);
        }
        if(fragmentNavigationDelegate != null) {
            fragmentNavigationDelegate.onDestroy();
        }
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {

        setCurrentItemIndex(newSelectionIndex);

        final String tag = (String) this.bottomSelectorView.getButtonTagAt(newSelectionIndex);
        final Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment != getTopFragment()) {
            pushFragment(fragment, null, false); //todo save most recent 3 fragments in backstack
        } else if (fragment == null) {
            pushFragment(createFragmentFromTag(tag), null, false);
        } else {
            Logger.d(NewHomeActivity.class.getName(), fragment + " is already visible");
        }
    }

    private void restoreState(@Nullable final Bundle savedInstanceState) {
        if(savedInstanceState != null){
            this.currentItemIndex = savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX, DEFAULT_ITEM_INDEX);
        } else {
            this.currentItemIndex = DEFAULT_ITEM_INDEX;
        }
    }

    private Fragment createFragmentFromTag(@NonNull final String tag) {
        final Fragment fragment;

        switch (tag){
            case TIMELINE_TAG:
                //todo depends on BackSideFragment to call update onResume()
                fragment = new RoomConditionsFragment();
                break;
            case TRENDS_TAG:
                //todo depends on BackSideFragment to call update onResume()
                fragment = new TrendsFragment();
                break;
            case SOUNDS_TAG:
                fragment = new SoundsFragment();
                break;
            case CONDITIONS_TAG:
                fragment = new AppSettingsFragment();
                break;
            case HOME_TAG:
            default:
                //todo unless dependencies on old HomeActivity are removed from InsightFragment
                // HomeFragment can't be instantiated here
                fragment = new VoiceFragment();
        }
        return fragment;
    }

    @Override
    public void pushFragment(@NonNull final Fragment fragment,
                             @Nullable final String title,
                             final boolean wantsBackStackEntry) {
        this.fragmentNavigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                              @Nullable final String title,
                                              final boolean wantsBackStackEntry) {
        this.fragmentNavigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        this.fragmentNavigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent result) {
        //todo
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return fragmentNavigationDelegate.getTopFragment();
    }

    private void initSelector(@NonNull final SelectorView selectorView) {
        selectorView.setButtonLayoutParams(new SelectorView.LayoutParams(0, SelectorView.LayoutParams.MATCH_PARENT, 1));
        //todo update icons and order
        final @DrawableRes int[] inactiveIcons = {
                R.drawable.backside_icon_currently,
                R.drawable.backside_icon_trends,
                R.drawable.backside_icon_insights,
                R.drawable.backside_icon_sounds,
                R.drawable.backside_icon_settings,
        };
        final @DrawableRes int[] activeIcons = {
                R.drawable.backside_icon_currently_active,
                R.drawable.backside_icon_trends_active,
                R.drawable.backside_icon_insights_active,
                R.drawable.backside_icon_sounds_active,
                R.drawable.backside_icon_settings_active,
        };
        final String[] tags = {
                TIMELINE_TAG,
                TRENDS_TAG,
                HOME_TAG,
                SOUNDS_TAG,
                CONDITIONS_TAG
        };
        for (int i = 0; i < inactiveIcons.length; i++) {
            final SpannableString inactiveContent = createIconSpan("empty",
                                                                   inactiveIcons[i]);
            final SpannableString activeContent = createIconSpan("empty",
                                                                 activeIcons[i]);
            final ToggleButton button = selectorView.addOption(activeContent,
                                                               inactiveContent,
                                                               false);
            button.setPadding(0, 0, 0, 0);
        }
        selectorView.setButtonTags((Object[]) tags);
        selectorView.setOnSelectionChangedListener(this);
        selectorView.setSelectedIndex(getCurrentItemIndex());
        onSelectionChanged(getCurrentItemIndex());
    }

    private SpannableString createIconSpan(@NonNull final String title,
                                           @DrawableRes final int icon) {
        final SpannableString spannableString = new SpannableString(title);
        final ImageSpan imageSpan = new ImageSpan(this, icon);
        spannableString.setSpan(imageSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    private int getCurrentItemIndex() {
        return this.currentItemIndex;
    }

    public void setCurrentItemIndex(final int currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
    }
}
