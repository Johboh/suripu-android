package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.view.View;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.QueryScope;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
import is.hello.sense.api.model.v2.sensors.X;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SensorResponseInteractor;
import is.hello.sense.mvp.view.SensorDetailView;
import is.hello.sense.ui.activities.SensorDetailActivity;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.DateFormatter;

public final class SensorDetailFragment extends PresenterFragment<SensorDetailView>
        implements SelectorView.OnSelectionChangedListener,
        SensorGraphDrawable.ScrubberCallback {
    private static final String ARG_SENSOR = SensorDetailFragment.class.getName() + ".ARG_SENSOR";

    public static SensorDetailFragment createFragment(@NonNull final Sensor sensor) {
        final SensorDetailFragment sensorDetailFragment = new SensorDetailFragment();
        final Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_SENSOR, sensor);
        sensorDetailFragment.setArguments(bundle);
        return sensorDetailFragment;
    }

    @Inject
    ApiService apiService;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    SensorResponseInteractor sensorResponseInteractor;
    @Inject
    UnitFormatter unitFormatter;

    private Sensor sensor;
    private UpdateTimer updateTimer;
    private DateFormatter dateFormatter;
    private TimestampQuery timestampQuery = new TimestampQuery(QueryScope.DAY_5_MINUTE);


    @Override
    public final void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new SensorDetailView(getActivity(),
                                                      unitFormatter,
                                                      this,
                                                      this);
            this.presenterView.updateSensor(sensor);
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(this.preferences);
        addInteractor(this.sensorResponseInteractor);
        dateFormatter = new DateFormatter(getActivity());
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_SENSOR)) {
            this.sensor = (Sensor) savedInstanceState.getSerializable(ARG_SENSOR);
        } else {
            final Bundle args = getArguments();
            if (args == null) {
                finishWithResult(Activity.RESULT_CANCELED, null);
                return;
            }
            if (!args.containsKey(ARG_SENSOR)) {
                finishWithResult(Activity.RESULT_CANCELED, null);
                return;
            }
            this.sensor = (Sensor) args.getSerializable(ARG_SENSOR);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.updateTimer = new UpdateTimer(2, TimeUnit.MINUTES);
        bindAndSubscribe(this.sensorResponseInteractor.sensors,
                         sensorResponse -> {
                             for (final Sensor sensor : sensorResponse.getSensors()) {
                                 if (sensor.getType() == this.sensor.getType()) {
                                     this.sensor = sensor;
                                     updateSensors(timestampQuery.queryScope);
                                 }
                             }
                         },
                         this::handleError);
        this.updateTimer.setOnUpdate(this.sensorResponseInteractor::update);
    }

    @Override
    public final void onResume() {
        super.onResume();
        this.updateTimer.schedule();
    }

    @Override
    public final void onPause() {
        super.onPause();
        if (this.updateTimer != null) {
            this.updateTimer.unschedule();
        }
    }

    @Override
    protected final void onRelease() {
        super.onRelease();
        this.updateTimer = null;
    }

    @Override
    public synchronized void onSelectionChanged(final int newSelectionIndex) {
        switch (newSelectionIndex) {
            case 0:
                updateSensors(QueryScope.DAY_5_MINUTE);
                break;
            case 1:
                updateSensors(QueryScope.WEEK_1_HOUR);
                break;
            default:
                throw new IllegalArgumentException(newSelectionIndex + " is not an option");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        if (sensor != null) {
            outState.putSerializable(ARG_SENSOR, sensor);
        }
        super.onSaveInstanceState(outState);
    }

    // consider creating a hashmap/cache to hold these in. Limit requests to time.
    private synchronized void updateSensors(@NonNull final QueryScope queryScope) {
        timestampQuery = new TimestampQuery(queryScope);
        this.stateSafeExecutor.execute(() -> {
            final ArrayList<Sensor> sensors = new ArrayList<>();
            sensors.add(sensor);
            this.apiService.postSensors(new SensorDataRequest(queryScope, sensors))
                           .subscribe(sensorsDataResponse -> {
                                          changeActionBarColor(sensor.getColor());
                                          timestampQuery.setTimestamps(sensorsDataResponse.getTimestamps());
                                          sensor.setSensorValues(sensorsDataResponse);
                                          presenterView.updateSensor(sensor);
                                          presenterView.setGraph(sensor, SensorGraphView.StartDelay.SHORT, queryScope == QueryScope.DAY_5_MINUTE ? getDayLabels() : getWeekLabels());
                                      },
                                      this::handleError);
        });
    }

    private String[] getWeekLabels() {
        final String[] labels = new String[7];
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        for (int i = 0; i < labels.length; i++) {
            calendar.add(Calendar.DATE, 1);
            final String day = dateFormat.format(calendar.getTime());
            labels[i] = day;
        }
        return labels;
    }

    private String[] getDayLabels() {
        final String[] labels = new String[7];
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat;
        final int minuteDiff;
        if (this.preferences.getUse24Time()) {
            dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            final int unRoundedMins = calendar.get(Calendar.MINUTE) % 15;
            calendar.add(Calendar.MINUTE, unRoundedMins < 8 ? -unRoundedMins : (15 - unRoundedMins));
            calendar.add(Calendar.MINUTE, 15);
            minuteDiff = -25;
        } else {
            dateFormat = new SimpleDateFormat("ha", Locale.getDefault());
            final int unRoundedMins = calendar.get(Calendar.MINUTE) % 30;
            calendar.add(Calendar.MINUTE, 30 - unRoundedMins);
            minuteDiff = -30;
        }
        calendar.add(Calendar.HOUR, -2);

        for (int i = 6; i >= 0; i--) {
            final String day = dateFormat.format(calendar.getTime());
            labels[i] = day;
            calendar.add(Calendar.HOUR, -3);
            calendar.add(Calendar.MINUTE, minuteDiff);
        }
        return labels;
    }

    private void handleError(@NonNull final Throwable throwable) {
        changeActionBarColor(R.color.dim);
        this.presenterView.bindError();
    }

    private void changeActionBarColor(@ColorRes final int colorRes) {
        final Activity activity = getActivity();
        if (activity instanceof SensorDetailActivity) {
            // Some bug occurs when you try to change the actionbar from the main thread causing everything to freeze but never crash.
            presenterView.post(() -> ((SensorDetailActivity) activity).setActionbarColor(colorRes));
        }

    }

    @Override
    public void onPositionScrubbed(final int position) {
        final String value;
        final String message;
        if (timestampQuery.timestamps.size() < position) {
            message = null;
        } else {
            final long timestamp = timestampQuery.timestamps.get(position).getTimestamp();
            if (timestamp != -1) {
                if (timestampQuery.queryScope == QueryScope.DAY_5_MINUTE) {
                    message = dateFormatter.formatAsTime(new DateTime(timestamp),
                                                         preferences.getUse24Time());
                } else {
                    message = dateFormatter.formatAsDayAndTime(new DateTime(timestamp),
                                                               preferences.getUse24Time());
                }
            } else {
                message = null;
            }
        }
        if (sensor.getSensorValues().length > position && sensor.getSensorValues()[position] != Sensor.NO_VALUE) {
            value = unitFormatter.getFormattedSensorValue(sensor.getType(), sensor.getSensorValues()[position]).toString();
        } else {
            value = getString(R.string.missing_data_placeholder);
        }
        presenterView.setValueAndMessage(value,
                                         message);

    }

    @Override
    public void onScrubberReleased() {
        presenterView.setValueAndMessage(unitFormatter.getUnitPrinterForSensorAverageValue(sensor.getType()).print(sensor.getValue()), sensor.getMessage());
    }

    private class TimestampQuery {
        private final QueryScope queryScope;
        private List<X> timestamps = new ArrayList<>();

        public TimestampQuery(@NonNull final QueryScope queryScope) {
            this.queryScope = queryScope;
        }

        public void setTimestamps(@NonNull final List<X> timestamps) {
            this.timestamps = timestamps;
        }
    }
}
