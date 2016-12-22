package is.hello.sense.ui.fragments.onboarding;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.util.Analytics;

import static android.app.Activity.RESULT_OK;

public class OnboardingSmartAlarmFragment extends SenseFragment {
    private OnboardingSimpleStepView view;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_FIRST_ALARM, null);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.onboarding_title_smart_alarm)
                .setSubheadingText(R.string.onboarding_info_smart_alarm)
                .setDiagramVideo(Uri.parse(getString(R.string.diagram_onboarding_smart_alarm)))
                .setDiagramImage(R.drawable.onboarding_smart_alarm)
                .setPrimaryButtonText(R.string.action_set_smart_alarm_now)
                .setPrimaryOnClickListener(ignored -> complete(true))
                .setSecondaryButtonText(R.string.action_do_later)
                .setSecondaryOnClickListener(ignored -> complete(false))
                .hideToolbar();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(this.view != null) {
            this.view.destroy();
            this.view = null;
        }
    }

    public void complete(final boolean withAlarm) {
        if(withAlarm){
            ((OnboardingActivity) getActivity()).showSetAlarmDetail();
        } else{
            getFragmentNavigation().flowFinished(this, RESULT_OK, null);
        }
    }
}
