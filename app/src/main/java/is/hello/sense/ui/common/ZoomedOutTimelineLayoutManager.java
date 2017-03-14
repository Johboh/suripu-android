package is.hello.sense.ui.common;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class ZoomedOutTimelineLayoutManager extends LinearLayoutManager {
    @VisibleForTesting
    static final int NUMBER_ITEMS_ON_SCREEN = 3;

    private @Nullable Runnable onPostLayout;

    public ZoomedOutTimelineLayoutManager(Context context) {
        super(context, HORIZONTAL, true);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);

        if (onPostLayout != null) {
            onPostLayout.run();
            this.onPostLayout = null;
        }
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        this.onPostLayout = null;
    }

    public int getItemWidth() {
        return getWidth() / NUMBER_ITEMS_ON_SCREEN;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        RecyclerView.LayoutParams params = super.generateLayoutParams(c, attrs);
        params.width = getItemWidth();
        return params;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        RecyclerView.LayoutParams params = super.generateLayoutParams(lp);
        params.width = getItemWidth();
        return params;
    }

    public void postLayout(@Nullable final Runnable onPostLayout) {
        this.onPostLayout = onPostLayout;
    }
}
