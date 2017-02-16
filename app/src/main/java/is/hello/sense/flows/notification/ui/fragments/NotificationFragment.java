package is.hello.sense.flows.notification.ui.fragments;


import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import is.hello.sense.R;
import is.hello.sense.flows.notification.ui.views.NotificationView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.util.Analytics;

public class NotificationFragment extends PresenterFragment<NotificationView> {
    @Override
    public void initializeSenseView() {
        senseView = new NotificationView(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu,
                                    final MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save) {
            Toast.makeText(getActivity(), "Pressed Save", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_NOTIFICATIONS, null);
        }
    }
}
