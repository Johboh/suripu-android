package is.hello.sense.ui.fragments.updating;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;

import javax.inject.Inject;

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import is.hello.sense.R;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class SelectWifiNetworkFragment extends BasePresenterFragment
        implements AdapterView.OnItemClickListener,
        BaseSelectWifiNetworkPresenter.Output,
        OnBackPressedInterceptor {

    private WifiNetworkAdapter networkAdapter;

    private TextView heading;
    private TextView subheading;
    private TextView scanningIndicatorLabel;
    private ProgressBar scanningIndicator;
    private ListView listView;
    private Button rescanButton;
    private OnboardingToolbar toolbar;
    private View otherNetworkView;
    private View macAddressContainer;
    private TextView macAddress;
    private TextView copyMacAddress;

    @Inject
    BaseSelectWifiNetworkPresenter presenter;


    //region Lifecycle

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());

        Analytics.trackEvent(presenter.getOnCreateAnalyticsEvent(), null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_select_wifi_network, container, false);

        this.subheading = (TextView) view.findViewById(R.id.fragment_select_wifi_subheading);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        this.heading = (TextView) view.findViewById(R.id.fragment_select_wifi_heading);
        this.macAddressContainer = view.findViewById(R.id.fragment_select_wifi_mac_address_container);
        this.macAddress = (TextView) view.findViewById(R.id.fragment_select_wifi_mac_address);
        this.copyMacAddress = (TextView) view.findViewById(R.id.fragment_select_wifi_mac_address_copy);

        this.otherNetworkView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        final WifiNetworkAdapter.ViewHolder holder = new WifiNetworkAdapter.ViewHolder(otherNetworkView);
        holder.locked.setVisibility(View.GONE);
        holder.strength.setVisibility(View.INVISIBLE);
        holder.name.setText(R.string.wifi_other_network);
        listView.addFooterView(otherNetworkView, null, true);

        listView.setAdapter(networkAdapter);

        this.scanningIndicatorLabel = (TextView) view.findViewById(R.id.fragment_select_wifi_progress_label);
        this.scanningIndicator = (ProgressBar) view.findViewById(R.id.fragment_select_wifi_progress);

        this.rescanButton = (Button) view.findViewById(R.id.fragment_select_wifi_rescan);
        rescanButton.setEnabled(false);
        Views.setSafeOnClickListener(rescanButton, presenter::onRescanButtonClicked);

        this.toolbar = OnboardingToolbar.of(this, view);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (networkAdapter.isEmpty()) {
            presenter.initialScan();
        } else {
            showRescanOption();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toolbar != null) {
            toolbar.onDestroyView();
            toolbar = null;
        }
        heading = null;
        subheading = null;
        scanningIndicator = null;
        scanningIndicatorLabel = null;
        if (listView != null) {
            listView.setOnItemClickListener(null);
            listView.removeFooterView(otherNetworkView);
            listView.setAdapter(null);
            listView = null;
        }
        this.copyMacAddress.setOnClickListener(null);
        otherNetworkView = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.help, menu);
        final MenuItem infoItem = menu.findItem(R.id.action_help);
        if(infoItem != null){
            Styles.tintMenuIcon(getActivity(), infoItem, R.color.action_bar_menu_icon);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help: {
                UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.WIFI_SCAN);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    @Override
    public void onItemClick(final AdapterView<?> adapterView,
                            final View view,
                            final int position,
                            final long id) {
        //todo cleanup manual routing here
        final wifi_endpoint network = (wifi_endpoint) adapterView.getItemAtPosition(position);
        final ConnectToWiFiFragment nextFragment = new ConnectToWiFiFragment();
        final Bundle arguments = new Bundle();
        arguments.putSerializable(ConnectToWiFiFragment.ARG_SCAN_RESULT, network);
        nextFragment.setArguments(arguments);
        getFragmentNavigation().pushFragment(nextFragment, getString(R.string.title_edit_wifi), true);
    }

    @Override
    public void showMacAddress(@Nullable final String macAddress) {
        if (macAddress == null) {
            this.macAddressContainer.setVisibility(View.GONE);
        } else {
            this.macAddressContainer.setVisibility(View.VISIBLE);
            Views.setSafeOnClickListener(this.copyMacAddress, v -> {
                final String copied = getString(R.string.copied);
                final ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                final ClipData clip = ClipData.newPlainText(copied, macAddress);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), copied, Toast.LENGTH_SHORT).show();
            });
        }

        this.macAddress.setText(macAddress);
    }

    @Override
    public void showScanning() {
        scanningIndicatorLabel.setVisibility(View.VISIBLE);
        scanningIndicator.setVisibility(View.VISIBLE);
        if (subheading.getVisibility() != View.GONE) {
            subheading.setVisibility(View.INVISIBLE);
        }
        listView.setVisibility(View.INVISIBLE);
        rescanButton.setVisibility(View.INVISIBLE);
        rescanButton.setEnabled(false);
        networkAdapter.clear();
    }

    @Override
    public void showRescanOption() {
        scanningIndicatorLabel.setVisibility(View.GONE);
        scanningIndicator.setVisibility(View.GONE);
        if (subheading.getVisibility() != View.GONE) {
            subheading.setVisibility(View.VISIBLE);
        }
        listView.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(true);
    }

    @Override
    public void bindScanResults(@NonNull final Collection<wifi_endpoint> scanResults) {
        networkAdapter.clear();
        networkAdapter.addAll(scanResults);
    }

    @Override
    public void useToolbar(final boolean use) {
        if (use) {
            toolbar.setVisible(true);
            toolbar.setWantsBackButton(false)
                   .setOnHelpClickListener(ignored -> UserSupport.showForHelpStep(getActivity(),
                                                                                  UserSupport.HelpStep.WIFI_SCAN));
            heading.setVisibility(View.VISIBLE);
            subheading.setVisibility(View.VISIBLE);

            setHasOptionsMenu(false);
        } else {
            heading.setVisibility(View.GONE);
            subheading.setVisibility(View.GONE);

            setHasOptionsMenu(true);
            toolbar.hide();
        }
    }

    @Override
    public boolean isScanning() {
        return scanningIndicator != null && scanningIndicator.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (presenter != null) {
            return presenter.onBackPressed(defaultBehavior);
        }
        return false;
    }
}
