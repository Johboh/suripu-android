package is.hello.sense.ui.fragments.onboarding;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Analytics;

public class HaveSenseReadyFragment extends SenseFragment {
    private OnboardingSimpleStepView view;
    //region Lifecycle


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_START, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_have_sense_ready)
                .setSubheadingText(R.string.info_have_sense_ready)
                .setDiagramImage(R.drawable.onboarding_sense_grey)
                .setDiagramEdgeToEdge(false)
                .setToolbarWantsBackButton(true)
                .setPrimaryButtonText(R.string.action_pair_your_sense)
                .setPrimaryOnClickListener(this::pairSense)
                .setSecondaryButtonText(R.string.action_buy_sense)
                .setSecondaryOnClickListener(this::showBuySense);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.view != null) {
            this.view.destroy();
            this.view = null;
        }
    }

    //endregion


    //region Actions

    public void pairSense(@NonNull View sender) {
        finishFlow();
    }

    public void showBuySense(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_SENSE, null);
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.ORDER_URL));
    }

    //endregion
}
