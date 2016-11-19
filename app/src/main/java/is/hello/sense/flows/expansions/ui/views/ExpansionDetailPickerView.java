package is.hello.sense.flows.expansions.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.flows.expansions.ui.widget.ExpansionRangePickerView;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class ExpansionDetailPickerView extends PresenterView {

    final TextView enabledTextView;
    final CompoundButton enabledSwitch;

    final TextView configurationTypeTextView;
    final TextView configurationSelectedTextView;
    final ImageView configurationErrorImageView;

    final ViewGroup connectedContainer;
    final ViewGroup enabledContainer;

    final ProgressBar configurationLoading;

    final ExpansionRangePickerView expansionRangePicker;

    public ExpansionDetailPickerView(@NonNull final Activity activity) {
        super(activity);

        // connected
        this.connectedContainer = (ViewGroup) findViewById(R.id.view_expansion_detail_picker_bottom);
        this.showConnectedContainer(false); // can't set included layouts to gone
        this.enabledContainer = (ViewGroup) connectedContainer.findViewById(R.id.view_expansion_detail_enabled_container);
        this.enabledTextView = (TextView) enabledContainer.findViewById(R.id.view_expansion_detail_enabled_tv);
        this.enabledSwitch = (CompoundButton) enabledContainer.findViewById(R.id.view_expansion_detail_configuration_selection_switch);
        // connected and configurations found
        this.configurationErrorImageView = (ImageView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_error);
        this.configurationTypeTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_type_tv);
        this.configurationSelectedTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_selection_tv);
        this.configurationLoading = (ProgressBar) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_loading);
        this.expansionRangePicker = (ExpansionRangePickerView) findViewById(R.id.view_expansion_detail_picker_value_widget);
    }


    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail_picker;
    }

    @Override
    public void releaseViews() {
        this.configurationSelectedTextView.setOnClickListener(null);
        this.enabledSwitch.setOnClickListener(null);
        this.enabledTextView.setOnClickListener(null);
    }

    public void showConfigurationSuccess(@Nullable final String configurationName,
                                         @NonNull final OnClickListener configurationSelectedTextViewClickListener) {
        Views.setSafeOnClickListener(this.configurationSelectedTextView, configurationSelectedTextViewClickListener);
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setText(configurationName);
        this.configurationSelectedTextView.setVisibility(VISIBLE);
    }

    public void showConfigurationsError(@NonNull final OnClickListener configurationErrorImageViewClickListener) {
        Views.setSafeOnClickListener(this.configurationErrorImageView, configurationErrorImageViewClickListener);
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setVisibility(GONE);
        this.configurationErrorImageView.setVisibility(VISIBLE);
    }

    public void showConfigurationSpinner() {
        this.configurationSelectedTextView.setVisibility(GONE);
        this.configurationErrorImageView.setVisibility(GONE);
        this.configurationLoading.setVisibility(VISIBLE);
    }

    public void setExpansionEnabledTextViewClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.enabledTextView, listener);
    }

    public void setConfigurationTypeText(@NonNull final String configType){
        this.configurationTypeTextView.setText(configType);
    }

    /**
     * @param min          min value of all pickers
     * @param max          max value of all pickers
     * @param suffix       will be attached to each value. If no suffix should be used pass
     *                     {@link is.hello.sense.util.Constants#EMPTY_STRING}.
     */
    public void initExpansionRangePicker(final int min,
                                         final int max,
                                         final int defaultValue,
                                         @NonNull final String suffix){
        this.expansionRangePicker.init(min,
                                       max,
                                       suffix);
    }

    /**
     * @param initialValues should be the actual values, not index position.
     *                     {@link is.hello.sense.util.Constants#EMPTY_STRING}.
     */
    public void showExpansionRangePicker(final int[] initialValues) {
        post(() -> {
            this.expansionRangePicker.setVisibility(VISIBLE);
            this.expansionRangePicker.initPickers(initialValues);
        });
    }

    //region switch

    /**
     * Call once for expansions that need to display an on/off switch.
     *
     * @param isOn                       starting value of switch.
     * @param enabledSwitchClickListener callback when switch is pressed.
     */
    public void showEnableSwitch(final boolean isOn,
                                 @NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        this.enabledContainer.setVisibility(VISIBLE);
        this.setEnableSwitch(isOn, enabledSwitchClickListener);
    }

    private void setEnableSwitch(final boolean isOn,
                                 @NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        this.enabledSwitch.setOnCheckedChangeListener(null);
        this.enabledSwitch.setChecked(isOn);
        this.enabledSwitch.setEnabled(false);
        Views.setSafeOnSwitchClickListener(this.enabledSwitch, enabledSwitchClickListener);
        this.enabledSwitch.setEnabled(true);
    }

    public int getSelectedValue() {
        return this.expansionRangePicker.getSelectedMinValue();
    }

    public void showConnectedContainer(final boolean isOn) {
        this.connectedContainer.setVisibility(isOn ? VISIBLE : GONE);
    }

    //endregion


}
