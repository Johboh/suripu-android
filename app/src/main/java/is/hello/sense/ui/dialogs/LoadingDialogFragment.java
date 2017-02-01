package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public final class LoadingDialogFragment extends SenseDialogFragment {
    public static final String TAG = LoadingDialogFragment.class.getSimpleName();

    public static final long DURATION_DONE_MESSAGE = 2 * 1000;
    public static final long DURATION_DEFAULT = 1000;

    private static final String ARG_TITLE = LoadingDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_FLAGS = LoadingDialogFragment.class.getName() + ".ARG_FLAGS";
    private static final String ARG_DISMISS_MSG = LoadingDialogFragment.class.getName() + ".ARG_DISMISS_MSG";
    private static final String ARG_LOCK_ORIENTATION = LoadingDialogFragment.class.getName() + ".ARG_LOCK_ORIENTATION";
    private static final String ARG_ON_SHOW_RUNNABLE = LoadingDialogFragment.class.getName() + ".ARG_ON_SHOW_RUNNABLE";

    //region Config

    public static final int OPAQUE_BACKGROUND = (1 << 1);
    public static final int DEFAULTS = 0;

    @IntDef(flag = true, value = {DEFAULTS, OPAQUE_BACKGROUND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Config {}

    //endregion


    private TextView titleText;
    private ProgressBar activityIndicator;
    private ImageView checkMark;

    private @Nullable Integer oldOrientation;


    //region Shortcuts

    public static @NonNull LoadingDialogFragment show(@NonNull final FragmentManager fm,
                                                      @Nullable final String title,
                                                      final int flags) {
        final LoadingDialogFragment preexistingDialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (preexistingDialog != null) {
            if (flags == preexistingDialog.getFlags()) {
                preexistingDialog.setTitle(title);
                return preexistingDialog;
            } else {
                preexistingDialog.dismiss();
            }
        }

        final LoadingDialogFragment dialog = LoadingDialogFragment.newInstance(title, flags);
        dialog.show(fm, TAG);
        return dialog;
    }

    public static @NonNull LoadingDialogFragment show(@NonNull final FragmentManager fm) {
        return show(fm, null, DEFAULTS);
    }

    public static void close(@NonNull final FragmentManager fm) {
        final LoadingDialogFragment dialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (dialog != null) {
            dialog.dismissSafely();
        }
    }

    public static void closeWithOnComplete(@NonNull final FragmentManager fm, @Nullable final Runnable onCompletion){
        closeWithMessageTransition(fm, onCompletion, -1);
    }

    public static void closeWithDoneTransition(@NonNull final FragmentManager fm, @Nullable final Runnable onCompletion) {
        closeWithMessageTransition(fm, onCompletion, R.string.action_done);
    }

    public static void closeWithMessageTransition(@NonNull final FragmentManager fm,
                                                  @Nullable final Runnable onCompletion,
                                                  @StringRes final int messageRes){
        final LoadingDialogFragment dialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (dialog != null) {
            if(messageRes != -1) {
                dialog.setDismissMessage(messageRes);
                dialog.dismissWithDoneTransition(onCompletion);
            } else{
                dialog.dismissWithOnComplete(onCompletion,DURATION_DEFAULT);
            }
        } else if (onCompletion != null) {
            onCompletion.run();
        }
    }

    //endregion


    //region Lifecycle

    public static LoadingDialogFragment newInstance(@Nullable final String title, @Config final int flags) {
        final LoadingDialogFragment fragment = new LoadingDialogFragment();

        final Bundle arguments = new Bundle();
        if (!TextUtils.isEmpty(title)) {
            arguments.putString(ARG_TITLE, title);
        }
        arguments.putInt(ARG_FLAGS, flags);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
    }

    @Override
    public @NonNull Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Loading);

        dialog.setContentView(R.layout.fragment_dialog_loading);
        dialog.setCanceledOnTouchOutside(false);

        this.titleText = (TextView) dialog.findViewById(R.id.fragment_dialog_loading_title);
        this.activityIndicator = (ProgressBar) dialog.findViewById(R.id.fragment_dialog_loading_bar);
        this.checkMark = (ImageView) dialog.findViewById(R.id.fragment_dialog_loading_check_mark);

        if (getArguments() != null) {
            final Bundle arguments = getArguments();
            final View root = dialog.findViewById(R.id.fragment_dialog_loading_container);

            @Config final int flags = arguments.getInt(ARG_FLAGS, DEFAULTS);

            if ((flags & OPAQUE_BACKGROUND) == OPAQUE_BACKGROUND) {
                root.setBackgroundColor(ContextCompat.getColor(root.getContext(), R.color.background));
            }

            titleText.setText(arguments.getString(ARG_TITLE));

            this.setLockOrientation(getActivity());

            this.internalSetOnShowListener(arguments, dialog);
        }

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (oldOrientation != null) {
            getActivity().setRequestedOrientation(oldOrientation);
        }
    }

    //endregion


    //region Attributes

    public void setTitle(@Nullable final String title) {
        getArguments().putString(ARG_TITLE, title);
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    public void setDismissMessage(@StringRes final int messageRes) {
        getArguments().putInt(ARG_DISMISS_MSG, messageRes);
    }

    public void setOnShowListener(@NonNull final SenseAlertDialog.SerializedRunnable onShow) {
        getArguments().putSerializable(ARG_ON_SHOW_RUNNABLE, onShow);
    }

    private void internalSetOnShowListener(@NonNull final Bundle arguments,
                                           @NonNull final Dialog dialog) {
        if (arguments.containsKey(ARG_ON_SHOW_RUNNABLE)) {
            final Serializable serializable =  arguments.getSerializable(ARG_ON_SHOW_RUNNABLE);
            if (serializable instanceof SenseAlertDialog.SerializedRunnable) {
                final SenseAlertDialog.SerializedRunnable onShow =
                        (SenseAlertDialog.SerializedRunnable) serializable;
                dialog.setOnShowListener(ignore -> onShow.run());
            }
        }
    }

    /**
     * Causes orientation changes to be blocked for the
     * duration of the loading dialog being visible.
     * <p />
     * Probably not something we want to support long-term.
     */
    public void setLockOrientation() {
        getArguments().putBoolean(ARG_LOCK_ORIENTATION, true);
    }

    private void setLockOrientation(@NonNull final Activity activity) {
        final boolean lockOrientation = getArguments().getBoolean(ARG_LOCK_ORIENTATION, false);
        if (lockOrientation) {
            final int currentOrientation = activity.getRequestedOrientation();
            if (currentOrientation != ActivityInfo.SCREEN_ORIENTATION_LOCKED) {
                oldOrientation = currentOrientation;
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        }
    }

    public int getFlags() {
        final Bundle args = getArguments();
        if(args != null) {
            return args.getInt(ARG_FLAGS, DEFAULTS);
        } else {
            return DEFAULTS;
        }
    }

    //endregion


    //region Dismissing

    public void dismissWithOnComplete(@Nullable final Runnable onCompletion, final long delay){
        if(titleText != null){
            titleText.postDelayed(() -> {
                if (onCompletion != null) {
                    onCompletion.run();
                }
                dismissSafely();
            }, delay);
        }
    }

    public void dismissWithDoneTransition(@Nullable final Runnable onCompletion) {
        if (titleText != null) {
            animatorFor(activityIndicator)
                    .withDuration(Anime.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .start();

            animatorFor(titleText)
                    .withDuration(Anime.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (!finished) {
                            return;
                        }

                        animatorFor(checkMark)
                                .addOnAnimationWillStart(animator -> {
                                    checkMark.setAlpha(0f);
                                    checkMark.setScaleX(0f);
                                    checkMark.setScaleY(0f);
                                    checkMark.setVisibility(View.VISIBLE);
                                })
                                .alpha(1f)
                                .scale(1f)
                                .start();


                        int messageRes = getArguments().getInt(ARG_DISMISS_MSG, R.string.action_done);
                        if (messageRes != 0) {
                            titleText.setText(messageRes);
                        } else {
                            titleText.setText(null);
                        }

                        animatorFor(titleText)
                                .fadeIn()
                                .addOnAnimationCompleted(finished1 -> {
                                    if (!finished1) {
                                        return;
                                    }
                                    dismissWithOnComplete(onCompletion, DURATION_DONE_MESSAGE);
                                })
                                .start();
                    })
                    .start();
        } else if (isAdded()) {
            dismiss();
        }
    }

    //endregion
}
