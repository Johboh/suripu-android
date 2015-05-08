package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;

public class SenseAlertDialog extends Dialog {
    private TextView titleText;
    private TextView messageText;

    private View buttonDivider;
    private Button negativeButton;
    private Button positiveButton;

    public SenseAlertDialog(Context context) {
        super(context, R.style.AppTheme_Dialog_Simple);
        initialize();
    }

    protected void initialize() {
        setContentView(R.layout.dialog_sense_alert);

        this.titleText = (TextView) findViewById(R.id.dialog_sense_alert_title);
        this.messageText = (TextView) findViewById(R.id.dialog_sense_alert_message);
        messageText.setMovementMethod(LinkMovementMethod.getInstance());

        this.buttonDivider = findViewById(R.id.dialog_sense_alert_button_divider);
        this.negativeButton = (Button) findViewById(R.id.dialog_sense_alert_cancel);
        this.positiveButton = (Button) findViewById(R.id.dialog_sense_alert_ok);
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        super.setTitle(title);

        if (TextUtils.isEmpty(title)) {
            titleText.setVisibility(View.GONE);
        } else {
            titleText.setVisibility(View.VISIBLE);
        }

        titleText.setText(title);
    }

    @Override
    public void setTitle(@StringRes int titleId) {
        super.setTitle(titleId);

        if (titleId == 0) {
            titleText.setVisibility(View.GONE);
        } else {
            titleText.setVisibility(View.VISIBLE);
        }

        titleText.setText(titleId);
    }

    public void setTitleColor(int color) {
        titleText.setTextColor(color);
    }

    public void setMessage(@Nullable CharSequence message) {
        if (TextUtils.isEmpty(message)) {
            messageText.setVisibility(View.GONE);
        } else {
            messageText.setVisibility(View.VISIBLE);
        }

        messageText.setText(message);
    }

    public void setMessage(@StringRes int messageId) {
        if (messageId == 0) {
            messageText.setVisibility(View.GONE);
            messageText.setText(null);
        } else {
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(messageId);
        }
    }

    public CharSequence getMessage() {
        return messageText.getText();
    }

    private View.OnClickListener createClickListener(@Nullable DialogInterface.OnClickListener onClickListener, int which) {
        if (onClickListener != null) {
            return view -> {
                onClickListener.onClick(this, which);
                dismiss();
            };
        } else {
            return view -> dismiss();
        }
    }

    private void updateButtonDivider() {
        if (positiveButton.getVisibility() == View.VISIBLE && negativeButton.getVisibility() == View.VISIBLE) {
            buttonDivider.setVisibility(View.VISIBLE);
        } else {
            buttonDivider.setVisibility(View.GONE);
        }
    }

    public void setPositiveButton(@Nullable CharSequence title, @Nullable OnClickListener onClickListener) {
        if (title != null) {
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setText(title);
            Views.setSafeOnClickListener(positiveButton, createClickListener(onClickListener, DialogInterface.BUTTON_POSITIVE));
        } else {
            positiveButton.setVisibility(View.GONE);
        }

        updateButtonDivider();
    }

    public void setPositiveButton(@StringRes int titleId, @Nullable OnClickListener onClickListener) {
        setPositiveButton(getContext().getString(titleId), onClickListener);
    }

    public void setNegativeButton(@Nullable CharSequence title, @Nullable OnClickListener onClickListener) {
        if (title != null) {
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setText(title);
            Views.setSafeOnClickListener(negativeButton, createClickListener(onClickListener, DialogInterface.BUTTON_NEGATIVE));
        } else {
            negativeButton.setVisibility(View.GONE);
        }

        updateButtonDivider();
    }

    public void setNegativeButton(@StringRes int titleId, @Nullable OnClickListener onClickListener) {
        setNegativeButton(getContext().getString(titleId), onClickListener);
    }

    /**
     * @see android.content.DialogInterface#BUTTON_POSITIVE
     * @see android.content.DialogInterface#BUTTON_NEGATIVE
     */
    protected @Nullable Button getButton(int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                return positiveButton;

            case DialogInterface.BUTTON_NEGATIVE:
                return negativeButton;

            default:
                return null;
        }
    }

    public void setButtonDestructive(int which, boolean flag) {
        Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        if (flag) {
            button.setTextColor(getContext().getResources().getColor(R.color.destructive_accent));
        } else {
            button.setTextColor(getContext().getResources().getColor(R.color.light_accent));
        }
    }

    public void setButtonDeemphasized(int which, boolean flag) {
        Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        if (flag) {
            button.setTextColor(getContext().getResources().getColor(R.color.text_dark));
        } else {
            button.setTextColor(getContext().getResources().getColor(R.color.light_accent));
        }
    }
}
