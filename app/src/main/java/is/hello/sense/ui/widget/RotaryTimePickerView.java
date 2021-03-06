package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.LocalTime;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class RotaryTimePickerView extends LinearLayout implements RotaryPickerView.OnSelectionListener {
    private static final int PERIOD_AM = Calendar.AM;
    private static final int PERIOD_PM = Calendar.PM;

    //region Pickers

    private final RotaryPickerView hourPicker;
    private final RotaryPickerView minutePicker;
    private final RotaryPickerView periodPicker;

    //endregion


    //region Attributes

    private boolean use24Time = false;
    private @Nullable OnSelectionListener onSelectionListener;

    //endregion


    //region Lifecycle

    public RotaryTimePickerView(@NonNull Context context) {
        this(context, null);
    }

    public RotaryTimePickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotaryTimePickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources resources = getResources();

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        final LayoutParams pickerLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                                 LayoutParams.WRAP_CONTENT);
        final int hourMinutePadding = resources.getDimensionPixelSize(R.dimen.x3);

        this.hourPicker = new RotaryPickerView(context);
        hourPicker.setOnSelectionListener(this);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setRolloverPositions(10, 11); /* e.g. 11am, 12pm */
        hourPicker.setWrapsAround(true);
        hourPicker.setWantsLeadingZeros(false);
        hourPicker.setItemGravity(Gravity.CENTER);
        hourPicker.setItemHorizontalPadding(hourMinutePadding);
        addView(hourPicker, pickerLayoutParams);

        final TextView hourMinuteDivider = new TextView(context);
        Styles.setTextAppearance(hourMinuteDivider, RotaryPickerView.ITEM_TEXT_APPEARANCE_FOCUSED);
        hourMinuteDivider.setText(":");
        hourMinuteDivider.setIncludeFontPadding(false);
        final int hourMinuteDividerYFix = Math.round(resources.getDisplayMetrics().scaledDensity * 4f);
        hourMinuteDivider.setPadding(0, 0, 0, hourMinuteDividerYFix);
        addView(hourMinuteDivider, new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                    LayoutParams.WRAP_CONTENT));

        this.minutePicker = new RotaryPickerView(context);
        minutePicker.setOnSelectionListener(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setWrapsAround(true);
        minutePicker.setItemGravity(Gravity.CENTER);
        minutePicker.setItemHorizontalPadding(hourMinutePadding);
        addView(minutePicker, pickerLayoutParams);

        this.periodPicker = new RotaryPickerView(context);
        periodPicker.setOnSelectionListener(this);
        periodPicker.setMinValue(PERIOD_AM);
        periodPicker.setMaxValue(PERIOD_PM);
        periodPicker.setValueStrings(DateFormatSymbols.getInstance().getAmPmStrings());
        periodPicker.setItemGravity(Gravity.CENTER);
        periodPicker.setMagnifyItemsNearCenter(false);
        addView(periodPicker, pickerLayoutParams);

        if (attrs != null) {
            final TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.RotaryPickerView,
                                                                     defStyleAttr, 0);

            final Drawable itemBackground = styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);
            setItemBackground(itemBackground);

            styles.recycle();
        }

        setTime(LocalTime.now());
    }

    //endregion


    //region Attributes

    public void setCompact(boolean compact) {
        final int unfocusedItemCount;
        if (compact) {
            unfocusedItemCount = 1;
        } else {
            unfocusedItemCount = RotaryPickerView.DEFAULT_UNFOCUSED_ITEM_COUNT;
        }

        hourPicker.setUnfocusedItemCount(unfocusedItemCount);
        minutePicker.setUnfocusedItemCount(unfocusedItemCount);
        periodPicker.setUnfocusedItemCount(unfocusedItemCount);
    }

    public void setUse24Time(boolean use24Time) {
        if (use24Time == this.use24Time) {
            return;
        }

        if (use24Time) {
            periodPicker.setVisibility(GONE);

            final int hour = getHours();
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setValue(hour, false);
        } else {
            periodPicker.setVisibility(VISIBLE);

            final int hour = hourPicker.getValue();
            if (hour > 12) {
                periodPicker.setValue(PERIOD_PM, false);
                hourPicker.setValue(hour - 12, false);
            } else {
                periodPicker.setValue(PERIOD_AM, false);
            }

            hourPicker.setMinValue(1);
            hourPicker.setMaxValue(12);
            hourPicker.setRolloverPositions(10, 11); /* e.g. 11am, 12pm */
        }

        hourPicker.setWantsLeadingZeros(use24Time);

        this.use24Time = use24Time;
    }

    public void setTime(int hour, int minute) {
        if (use24Time) {
            hourPicker.setValue(hour, false);
        } else {
            if (hour == 12) {
                hourPicker.setValue(12, false);
                periodPicker.setValue(PERIOD_PM, false);
            } else if (hour > 12) {
                hourPicker.setValue(hour - 12, false);
                periodPicker.setValue(PERIOD_PM, false);
            } else if (hour == 0) {
                hourPicker.setValue(12, false);
                periodPicker.setValue(PERIOD_AM, false);
            } else {
                hourPicker.setValue(hour, false);
                periodPicker.setValue(PERIOD_AM, false);
            }
        }

        minutePicker.setValue(minute, false);
    }

    public void setTime(@NonNull LocalTime time) {
        final int hour = time.getHourOfDay();
        final int minute = time.getMinuteOfHour();
        setTime(hour, minute);
    }

    public LocalTime getTime() {
        return new LocalTime(getHours(), getMinutes(), 0);
    }

    public int getHours() {
        int hour;
        if (use24Time) {
            hour = hourPicker.getValue();
        } else {
            hour = hourPicker.getValue();
            int period = periodPicker.getValue();
            if (period == PERIOD_PM && hour < 12) {
                hour += 12;
            } else if (period == PERIOD_AM && hour == 12) {
                hour = 0;
            }
        }
        return hour;
    }

    public int getMinutes() {
        return minutePicker.getValue();
    }

    public void setItemBackground(@Nullable Drawable itemBackground) {
        hourPicker.setItemBackground(itemBackground);
        minutePicker.setItemBackground(itemBackground);
        periodPicker.setItemBackground(itemBackground);
    }

    public void setOnSelectionListener(@Nullable OnSelectionListener onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    //endregion


    //region Callbacks

    @Override
    public void onSelectionRolledOver(@NonNull RotaryPickerView picker, @NonNull RotaryPickerView.RolloverDirection direction) {
        if (!use24Time && picker == hourPicker) {
            final int newValue = periodPicker.getValue() == PERIOD_AM
                    ? PERIOD_PM
                    : PERIOD_AM;
            periodPicker.setValue(newValue, true);
        } else if (picker == minutePicker) {
            switch (direction) {
                case FORWARD:
                    hourPicker.increment();
                    break;
                case BACKWARD:
                    hourPicker.decrement();
                    break;
            }
        }
    }

    @Override
    public void onSelectionWillChange() {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionWillChange();
        }
    }

    @Override
    public void onSelectionChanged(int newValue) {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionChanged();
        }
    }

    //endregion

    public interface OnSelectionListener {
        void onSelectionWillChange();
        void onSelectionChanged();
    }
}
