package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;
import is.hello.sense.util.StateSafeExecutor;

public class ProfileImageView extends FrameLayout implements Target {

    private final ImageView plusButton;
    private final ImageView profileImage;
    private final ProgressBar progressBar;

    public ProfileImageView(@NonNull final Context context) {
        this(context, null);
    }

    public ProfileImageView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProfileImageView(@NonNull final Context context, @Nullable final AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final View view = LayoutInflater.from(context).inflate(R.layout.item_profile_picture,this,false);
        this.profileImage = (ImageView) view.findViewById(R.id.item_profile_picture_image);
        this.plusButton = (ImageView) view.findViewById(R.id.item_profile_picture_button);
        this.progressBar = (ProgressBar) view.findViewById(R.id.item_profile_progress_bar);

        this.addView(view);
    }

    public int getSizeDimen() {
        return R.dimen.profile_image_size;
    }

    public int getDefaultProfileRes() {
        return R.drawable.default_profile_picture;
    }

    public int getDefaultErrorRes() { return R.drawable.profile_image_error; }

    public void setButtonClickListener(@Nullable final OnClickListener listener){
        if(listener == null){
            plusButton.setOnClickListener(null);
            return;
        }
        setButtonClickListener(null, listener);
    }

    public void setButtonClickListener(@Nullable final StateSafeExecutor stateSafeExecutor,
                                       @NonNull final OnClickListener listener){
        plusButton.setHapticFeedbackEnabled(true);
        Views.setSafeOnClickListener(plusButton, stateSafeExecutor, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onClick(v);
        });
    }

    @Override
    public void onBitmapLoaded(@NonNull final Bitmap bitmap,
                               @NonNull final Picasso.LoadedFrom from) {
        progressBar.setVisibility(GONE);
        profileImage.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    @Override
    public void onBitmapFailed(@NonNull final Drawable errorDrawable) {
        progressBar.setVisibility(GONE);
        profileImage.setImageResource(getDefaultErrorRes());
        Logger.error(ProfileImageView.class.getSimpleName(), "failed to load bitmap.");
    }

    @Override
    public void onPrepareLoad(@NonNull final Drawable placeHolderDrawable) {
        profileImage.setImageDrawable(placeHolderDrawable);
        progressBar.setVisibility(VISIBLE);
    }
}
