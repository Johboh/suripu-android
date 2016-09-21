package is.hello.sense.mvp.view.home.roomconditions;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.ui.widget.util.Views;

public class SensorResponseAdapter extends ArrayRecyclerAdapter<Sensor, SensorResponseAdapter.BaseViewHolder> {
    private static final int VIEW_SENSOR = 0;
    private static final int VIEW_ID_MESSAGE = 1;
    private final LayoutInflater inflater;

    private boolean messageWantsSenseIcon;
    private boolean completedAnimation;
    private float scaleFactor = 0;
    @StringRes
    private int messageTitle;
    @Nullable
    private CharSequence messageBody;
    @StringRes
    private int messageActionTitle;
    @Nullable
    private View.OnClickListener messageActionOnClick;

    private final HashMap<SensorType, SensorGraphDrawable> drawableHolder = new HashMap<>();


    public SensorResponseAdapter(@NonNull final LayoutInflater inflater) {
        super(new ArrayList<>());
        this.inflater = inflater;
    }

    //region adapter overrides
    @Override
    public long getItemId(final int position) {
        return VIEW_SENSOR;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case VIEW_ID_MESSAGE:
                final View view = this.inflater.inflate(R.layout.item_message_card, parent, false);

                final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                if (this.messageTitle != 0) {
                    title.setText(this.messageTitle);
                    title.setVisibility(View.VISIBLE);
                } else {
                    title.setVisibility(View.GONE);
                }

                final ImageView image = (ImageView) view.findViewById(R.id.item_message_card_image);
                if (this.messageWantsSenseIcon) {
                    image.setImageResource(R.drawable.illustration_no_sense);
                    image.setVisibility(View.VISIBLE);
                } else {
                    image.setVisibility(View.GONE);
                }

                final TextView messageText = (TextView) view.findViewById(R.id.item_message_card_message);
                messageText.setText(this.messageBody);

                final Button action = (Button) view.findViewById(R.id.item_message_card_action);
                action.setText(this.messageActionTitle);
                if (this.messageActionOnClick != null) {
                    Views.setSafeOnClickListener(action, this.messageActionOnClick);
                }
                return new BaseViewHolder(view);
            case VIEW_SENSOR:
                return new BaseViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_response, parent, false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    @Override
    public boolean replaceAll(@NonNull final Collection<? extends Sensor> collection) {
        this.drawableHolder.clear();
        super.replaceAll(collection);
        notifyDataSetChanged();
        return true;
    }
    //endregion

    //region adapter helpers

    public synchronized void stepAnimation() {
        scaleFactor += .06f;
        if (scaleFactor > 1f) {
            scaleFactor = 1;
            completedAnimation = true;
        }
        notifyDataSetChanged();
    }

    public synchronized boolean completedAnimation() {
        return completedAnimation;
    }


    public void displayMessage(final boolean messageWantsSenseIcon,
                               @StringRes final int title,
                               @NonNull final CharSequence message,
                               @StringRes final int actionTitle,
                               @NonNull final View.OnClickListener actionOnClick) {
        clear();
        this.messageWantsSenseIcon = messageWantsSenseIcon;
        this.messageTitle = title;
        this.messageBody = message;
        this.messageActionTitle = actionTitle;
        this.messageActionOnClick = actionOnClick;

        notifyDataSetChanged();
    }

    public void dismissMessage() {
        this.messageTitle = 0;
        this.messageBody = null;
        this.messageActionTitle = 0;
        this.messageActionOnClick = null;
    }

    public void release() {
        replaceAll(new ArrayList<>());
        notifyDataSetChanged();
        this.drawableHolder.clear();
    }

    //endregion

    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder implements Drawable.Callback {
        private final TextView title;
        private final TextView body;
        private final TextView value;
        private final TextView descriptor;
        private final SensorGraphView graphView;
        private final View view;

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.view = itemView;
            this.title = (TextView) itemView.findViewById(R.id.item_server_response_title);
            this.body = (TextView) itemView.findViewById(R.id.item_server_response_body);
            this.value = (TextView) itemView.findViewById(R.id.item_server_response_value);
            this.descriptor = (TextView) itemView.findViewById(R.id.item_server_response_descriptor);
            this.graphView = (SensorGraphView) itemView.findViewById(R.id.item_server_response_graph);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Sensor sensor = getItem(position);
            this.title.setText(sensor.getName());
            this.body.setText(sensor.getMessage());
            this.value.setText(sensor.getFormattedValue(sensor.getType() == SensorType.TEMPERATURE || sensor.getType() == SensorType.HUMIDITY));
            this.value.setTextColor(sensor.getColor(SensorResponseAdapter.this.inflater.getContext()));
            this.descriptor.setText(sensor.getSensorSuffix());
            final SensorGraphDrawable sensorGraphDrawable;
            if (SensorResponseAdapter.this.drawableHolder.containsKey(sensor.getType())) {
                sensorGraphDrawable = SensorResponseAdapter.this.drawableHolder.get(sensor.getType());
            } else {
                sensorGraphDrawable = new SensorGraphDrawable(SensorResponseAdapter.this.inflater.getContext(), sensor);
                sensorGraphDrawable.setCallback(this);
                SensorResponseAdapter.this.drawableHolder.put(sensor.getType(), sensorGraphDrawable);
            }
            sensorGraphDrawable.setScaleFactor(scaleFactor);
            this.graphView.setBackground(sensorGraphDrawable);
        }

        @Override
        public void invalidateDrawable(final Drawable who) {
            this.view.invalidate();
            this.view.requestLayout();
        }

        @Override
        public void scheduleDrawable(final Drawable who, final Runnable what, final long when) {
            invalidateDrawable(who);
        }

        @Override
        public void unscheduleDrawable(final Drawable who, final Runnable what) {

        }
    }

}