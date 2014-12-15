package is.hello.sense.ui.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.util.Logger;

public class FragmentNavigationActivity extends SenseActivity implements FragmentNavigation, FragmentManager.OnBackStackChangedListener {
    public static final String EXTRA_DEFAULT_TITLE = FragmentNavigationActivity.class.getName() + ".EXTRA_DEFAULT_TITLE";
    public static final String EXTRA_FRAGMENT_CLASS = FragmentNavigationActivity.class.getName() + ".EXTRA_FRAGMENT_CLASS";
    public static final String EXTRA_FRAGMENT_ARGUMENTS = FragmentNavigationActivity.class.getName() + ".EXTRA_FRAGMENT_ARGUMENTS";

    private boolean wantsTitleUpdates = true;

    public static Bundle getArguments(@NonNull String defaultTitle,
                                      @NonNull Class<? extends Fragment> fragmentClass,
                                      @Nullable Bundle fragmentArguments) {
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_DEFAULT_TITLE, defaultTitle);
        arguments.putString(EXTRA_FRAGMENT_CLASS, fragmentClass.getName());
        arguments.putParcelable(EXTRA_FRAGMENT_ARGUMENTS, fragmentArguments);
        return arguments;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_navigation);

        getFragmentManager().addOnBackStackChangedListener(this);

        if (savedInstanceState == null) {
            //noinspection ConstantConditions
            getActionBar().setTitle(getDefaultTitle());

            if (getIntent().hasExtra(EXTRA_FRAGMENT_CLASS)) {
                try {
                    String className = getIntent().getStringExtra(EXTRA_FRAGMENT_CLASS);
                    //noinspection unchecked
                    Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) Class.forName(className);
                    Fragment fragment = fragmentClass.newInstance();
                    fragment.setArguments(getIntent().getParcelableExtra(EXTRA_FRAGMENT_ARGUMENTS));
                    showFragment(fragment, getDefaultTitle(), false);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    Logger.warn(getClass().getSimpleName(), "Could not create fragment", e);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showFragment(@NonNull Fragment fragment,
                             @Nullable String title,
                             boolean wantsBackStackEntry) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        if (getTopFragment() == null) {
            transaction.add(R.id.activity_fragment_navigation_container, fragment, tag);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.activity_fragment_navigation_container, fragment, tag);
        }

        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
    }

    @Override
    public void onBackStackChanged() {
        if (getWantsTitleUpdates()) {
            int entryCount = getFragmentManager().getBackStackEntryCount();
            if (entryCount > 0) {
                FragmentManager.BackStackEntry entry = getFragmentManager().getBackStackEntryAt(entryCount - 1);
                //noinspection ConstantConditions
                getActionBar().setTitle(entry.getBreadCrumbTitle());
            } else {
                //noinspection ConstantConditions
                getActionBar().setTitle(getDefaultTitle());
            }
        }
    }

    protected @Nullable Fragment getTopFragment() {
        return getFragmentManager().findFragmentById(R.id.activity_fragment_navigation_container);
    }

    protected @Nullable String getDefaultTitle() {
        return getIntent().getStringExtra(EXTRA_DEFAULT_TITLE);
    }

    public boolean getWantsTitleUpdates() {
        return wantsTitleUpdates;
    }

    public void setWantsTitleUpdates(boolean wantsTitleUpdates) {
        this.wantsTitleUpdates = wantsTitleUpdates;
    }
}
