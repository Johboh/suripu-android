package is.hello.sense.flows.expansions.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.expansions.ExpansionsAuthView;
import is.hello.sense.ui.widget.CustomWebViewClient;

public class ExpansionsAuthFragment extends PresenterFragment<ExpansionsAuthView>
implements CustomWebViewClient.Listener{

    @Inject
    ApiSessionManager sessionManager;

    public static final String EXTRA_INIT_URL = ExpansionsAuthFragment.class.getName() + "EXTRA_INIT_URL";
    public static final String EXTRA_COMPLETE_URL = ExpansionsAuthFragment.class.getName() + "EXTRA_COMPLETE_URL";
    private String initUrl;
    private String completeUrl;

    public static ExpansionsAuthFragment newInstance(@NonNull final String initialUrl,
                                                     @NonNull final String completionUrl) {

        final Bundle args = new Bundle();
        args.putString(ExpansionsAuthFragment.EXTRA_INIT_URL, initialUrl);
        args.putString(ExpansionsAuthFragment.EXTRA_COMPLETE_URL, completionUrl);
        final ExpansionsAuthFragment fragment = new ExpansionsAuthFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Intent newIntent(@NonNull final String initialUrl,
                                   @NonNull final String completionUrl){
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_INIT_URL, initialUrl);
        intent.putExtra(EXTRA_COMPLETE_URL, completionUrl);
        return intent;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if(arguments != null){
            this.initUrl = arguments.getString(EXTRA_INIT_URL);
            this.completeUrl = arguments.getString(EXTRA_COMPLETE_URL);
        } else {
            //todo remove when done testing
            this.initUrl = "https://home.nest.com/login/oauth2?client_id=a023a014-65eb-447e-92cc-3e693dbf4a94&state=STATE";
            this.completeUrl = "https://www.hello.is";
        }
    }

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            final CustomWebViewClient client = new CustomWebViewClient(initUrl,
                                                                       completeUrl);
            client.setListener(this);
            presenterView = new ExpansionsAuthView(
                    getActivity(),
                    client
            );
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Map<String, String> headers = new HashMap<>(1);
        if(sessionManager.hasSession()) {
            headers.put("Authorization", "Bearer " + sessionManager.getAccessToken());
        }
        presenterView.loadlInitialUrl(headers);
        presenterView.showProgress(true);
    }

    @Override
    public void onInitialUrlLoaded() {
        presenterView.showProgress(false);
    }

    @Override
    public void onCompletionUrlLoaded() {
        presenterView.showProgress(false);
        finishFlow();
    }

    @Override
    public void onOtherUrlLoaded(){
        presenterView.showProgress(false);
    }
}