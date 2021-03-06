package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import is.hello.sense.R;

/**
 * Extends the standard <code>LinearLayout</code> to support
 * rounded corners to ease implementation of card-style designs.
 * <p />
 * <b>Important:</b> combining this view with scale and alpha
 * transformations as part of an interaction will result in less
 * than acceptable performance.
 */
public class RoundedLinearLayout extends LinearLayout {
    //region Fields

    private final Path clippingPath = new Path();
    private final RectF clippingRect = new RectF();

    private final float[] cornerRadii = new float[8];
    private boolean clipToPadding;

    //endregion


    //region Lifecycle

    public RoundedLinearLayout(@NonNull Context context) {
        this(context, null);
    }

    public RoundedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.RoundedLayout, defStyleAttr, 0);

            float radius = values.getDimension(R.styleable.RoundedLayout_senseCornerRadius, 0f);
            setCornerRadii(radius);

            values.recycle();
        }

        //noinspection SimplifiableIfStatement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.clipToPadding = getClipToPadding();
        } else {
            this.clipToPadding = true;
        }
    }

    //endregion


    //region Drawing

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.clipPath(clippingPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        clippingPath.reset();
        if (clipToPadding) {
            clippingRect.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        } else {
            clippingRect.set(0, 0, w, h);
        }
        clippingPath.addRoundRect(clippingRect, cornerRadii, Path.Direction.CW);
    }

    //endregion


    //region Properties

    public void setCornerRadii(float topLeft, float topRight, float bottomLeft, float bottomRight) {
        cornerRadii[0] = topLeft;
        cornerRadii[1] = topLeft;

        cornerRadii[2] = topRight;
        cornerRadii[3] = topRight;

        cornerRadii[4] = bottomLeft;
        cornerRadii[5] = bottomLeft;

        cornerRadii[6] = bottomRight;
        cornerRadii[7] = bottomRight;

        invalidate();
    }

    public void setCornerRadii(float value) {
        setCornerRadii(value, value, value, value);
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        this.clipToPadding = clipToPadding;
        super.setClipToPadding(clipToPadding);
    }

    //endregion
}
