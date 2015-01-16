package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.joda.time.LocalDate;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterBirthdayFragment extends AccountEditingFragment {
    private static final int[] FIELD_LIMITS = { 2, 2, 4 };

    private Account account;

    private TextView[] fields;
    private int activeField = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = getContainer().getAccount();

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.EVENT_ONBOARDING_BIRTHDAY, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_birthday, container, false);

        TextView monthText = (TextView) view.findViewById(R.id.fragment_onboarding_register_birthday_month);
        TextView dayText = (TextView) view.findViewById(R.id.fragment_onboarding_register_birthday_day);
        TextView yearText = (TextView) view.findViewById(R.id.fragment_onboarding_register_birthday_year);
        this.fields = new TextView[] { monthText, dayText, yearText };

        int hintColor = getResources().getColor(R.color.text_dim_placeholder);
        monthText.setHintTextColor(hintColor);
        dayText.setHintTextColor(hintColor);
        yearText.setHintTextColor(hintColor);

        if (account.getBirthDate() != null) {
            LocalDate birthDate = account.getBirthDate();
            monthText.setHint(String.format("%02d", birthDate.getMonthOfYear()));
            dayText.setHint(String.format("%02d", birthDate.getDayOfMonth()));
            yearText.setHint(String.format("%04d", birthDate.getYear()));
        }


        TableLayout keys = (TableLayout) view.findViewById(R.id.fragment_onboarding_register_birthday_keys);

        Button skip = (Button) keys.findViewById(R.id.fragment_onboarding_register_birthday_skip);
        Views.setSafeOnClickListener(skip, this::skip);

        Button backspace = (Button) keys.findViewById(R.id.fragment_onboarding_register_birthday_delete);
        backspace.setOnClickListener(this::backspace);

        View.OnClickListener appendNumber = this::appendNumber;
        for (int row = 0, rowCount = keys.getChildCount(); row < rowCount; row++) {
            TableRow rowLayout = (TableRow) keys.getChildAt(row);
            for (int column = 0, columnCount = rowLayout.getChildCount(); column < columnCount; column++) {
                Button columnButton = (Button) rowLayout.getChildAt(column);
                if ("-1".equals(columnButton.getTag())) {
                    continue;
                }

                columnButton.setOnClickListener(appendNumber);
            }
        }

        setActiveField(0);

        return view;
    }


    private void setActiveField(int activeField) {
        int activeColor = getResources().getColor(R.color.light_accent),
            inactiveColor = getResources().getColor(R.color.text_dark);
        for (int i = 0; i < fields.length; i++) {
            TextView field = fields[i];
            int padding = field.getPaddingTop();
            if (activeField == i) {
                field.setBackgroundResource(R.drawable.edit_text_background_focused);
                field.setTextColor(activeColor);
            } else {
                if (TextUtils.isEmpty(field.getText())) {
                    field.setBackgroundResource(R.drawable.edit_text_background_disabled);
                } else {
                    field.setBackgroundResource(R.drawable.edit_text_background_normal);
                }
                field.setTextColor(inactiveColor);
            }
            field.setPadding(padding, padding, padding, padding);
        }

        this.activeField = activeField;
    }

    private void decrementActiveField() {
        setActiveField(activeField - 1);
    }

    private void incrementActiveField() {
        setActiveField(activeField + 1);
    }


    public void backspace(@NonNull View sender) {
        TextView field = fields[activeField];
        if (TextUtils.isEmpty(field.getText())) {
            if (activeField == 0) {
                return;
            } else {
                decrementActiveField();

                field = fields[activeField];
            }
        }

        CharSequence text = field.getText();
        CharSequence textDelta = text.subSequence(0, text.length() - 1);
        field.setText(textDelta);
    }

    public void appendNumber(@NonNull View sender) {
        String numberValue = sender.getTag().toString();
        TextView field = fields[activeField];

        CharSequence text = field.getText();
        if (text.length() == FIELD_LIMITS[activeField]) {
            return;
        }

        String newText = text + numberValue;
        field.setText(newText);

        if (newText.length() == FIELD_LIMITS[activeField]) {
            if (activeField == fields.length - 1) {
                next();
            } else {
                incrementActiveField();
            }
        }
    }

    public void skip(@NonNull View sender) {
        getContainer().onAccountUpdated(this);
    }

    public void next() {
        getContainer().onAccountUpdated(this);
    }
}
