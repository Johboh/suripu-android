package is.hello.sense.flows.home.ui.fragments;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.fragments.ViewPagerPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class SoundsPresenterFragment extends ViewPagerPresenterFragment {

    //region ViewPagerPresenterFragment
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new SoundsViewPagerPresenterDelegate(getResources());
    }
    //endRegion

    @Override
    protected boolean shouldAddViewPagerListener() {
        return true;
    }
}