package is.hello.sense.ui.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class DevicesFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject DevicesPresenter devicesPresenter;

    private ProgressBar loadingIndicator;
    private DevicesAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicesPresenter.update();
        addPresenter(devicesPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_devices, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.adapter = new DevicesAdapter(getActivity());
        listView.setAdapter(adapter);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_settings_devices_progress);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(devicesPresenter.devices, this::bindDevices, this::devicesUnavailable);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

    }


    public void bindDevices(@NonNull List<Device> devices) {
        animate(loadingIndicator)
                .fadeOut(View.GONE)
                .start();

        adapter.clear();
        adapter.addAll(devices);
    }

    public void devicesUnavailable(Throwable e) {
        animate(loadingIndicator)
                .fadeOut(View.GONE)
                .start();

        adapter.clear();

        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    private static class DevicesAdapter extends ArrayAdapter<Device> {
        private final LayoutInflater inflater;

        private DevicesAdapter(Context context) {
            super(context, R.layout.item_device);

            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_device, parent, false);
                view.setTag(new ViewHolder(view));
            }

            ViewHolder holder = (ViewHolder) view.getTag();

            Device device = getItem(position);
            holder.icon.setImageResource(device.getType().iconRes);
            holder.title.setText(device.getType().nameRes);
            holder.status.setText(device.getState().nameRes);

            return view;
        }


        private class ViewHolder {
            private final ImageView icon;
            private final TextView title;
            private final TextView status;

            private ViewHolder(@NonNull View view) {
                this.icon = (ImageView) view.findViewById(R.id.item_device_icon);
                this.title = (TextView) view.findViewById(R.id.item_device_name);
                this.status = (TextView) view.findViewById(R.id.item_device_status);
            }
        }
    }
}
