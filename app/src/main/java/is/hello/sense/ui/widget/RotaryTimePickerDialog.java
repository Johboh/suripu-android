package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import org.joda.time.LocalTime;

public class RotaryTimePickerDialog extends SenseAlertDialog
        implements RotaryTimePickerView.OnSelectionListener {
    private final RotaryTimePickerView rotaryTimePickerView;
    private final OnTimeSetListener onTimeSetListener;

    private boolean waitingForSelectionChange = false;
    private @Nullable Runnable afterSelectionChange;

    public RotaryTimePickerDialog(@NonNull Context context,
                                  @NonNull OnTimeSetListener onTimeSetListener,
                                  int hourOfDay,
                                  int minute,
                                  boolean is24HourView) {
        super(context);

        final int orientation = context.getResources().getConfiguration().orientation;
        final boolean isLandscape = (orientation == Configuration.ORIENTATION_LANDSCAPE);

        this.rotaryTimePickerView = new RotaryTimePickerView(context);
        rotaryTimePickerView.setCompact(isLandscape);
        rotaryTimePickerView.setUse24Time(is24HourView);
        rotaryTimePickerView.setTime(hourOfDay, minute);
        rotaryTimePickerView.setOnSelectionListener(this);
        setView(rotaryTimePickerView, false);

        this.onTimeSetListener = onTimeSetListener;

        setNegativeButton(android.R.string.cancel, null);
        setButtonDeemphasized(BUTTON_NEGATIVE, true);

        final Button positiveButton = getButton(BUTTON_POSITIVE);
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setText(android.R.string.ok);
        positiveButton.setOnClickListener(ignored -> {
            if (waitingForSelectionChange) {
                setButtonEnabled(BUTTON_POSITIVE, false);
                this.afterSelectionChange = this::onTimeSet;
            } else {
                onTimeSet();
            }
        });
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        final LocalTime selectedTime = (LocalTime) savedInstanceState.getSerializable("selectedTime");
        if (selectedTime != null) {
            rotaryTimePickerView.setTime(selectedTime);
        }

        final Bundle parentSavedState = savedInstanceState.getParcelable("savedState");
        if (parentSavedState != null) {
            super.onRestoreInstanceState(parentSavedState);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        final Bundle savedState = new Bundle();
        savedState.putSerializable("selectedTime", rotaryTimePickerView.getTime());
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        return savedState;
    }

    public void updateTime(int hourOfDay, int minute) {
        rotaryTimePickerView.setTime(hourOfDay, minute);
    }

    private void onTimeSet() {
        onTimeSetListener.onTimeSet(rotaryTimePickerView.getHours(),
                                    rotaryTimePickerView.getMinutes());
        dismiss();
    }


    @Override
    public void onSelectionWillChange() {
        this.waitingForSelectionChange = true;
    }

    @Override
    public void onSelectionChanged() {
        this.waitingForSelectionChange = false;
        if (afterSelectionChange != null) {
            afterSelectionChange.run();
        }
    }


    public interface OnTimeSetListener {
        void onTimeSet(int hourOfDay, int minute);
    }
}
