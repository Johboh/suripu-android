package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;

/**
 * A simple staggered fade-in animation.
 * <p />
 * Each item faded-in has the delay <code>{@link #DELAY} * index</code>.
 */
public class TimelineFadeItemAnimator extends AbstractTimelineItemAnimator {
    public static final long DELAY = 20;

    private final AnimatorTemplate config = AnimatorTemplate.DEFAULT;

    private final List<RecyclerView.ViewHolder> pending = new ArrayList<>();
    private final List<RecyclerView.ViewHolder> running = new ArrayList<>();

    public TimelineFadeItemAnimator(@NonNull AnimatorContext animatorContext, int headerCount) {
        super(animatorContext, headerCount);
    }

    @Override
    public void runPendingAnimations() {
        sortByPosition(pending);
        getAnimatorContext().transaction(config, AnimatorContext.OPTIONS_DEFAULT, t -> {
            dispatchAnimationWillStart(t);

            long delay = DELAY;
            for (RecyclerView.ViewHolder item : pending) {
                dispatchAddStarting(item);

                t.animatorFor(item.itemView)
                 .withStartDelay(delay)
                 .fadeIn();

                delay += DELAY;
            }

            running.addAll(pending);
            pending.clear();
        }, finished -> {
            for (RecyclerView.ViewHolder item : running) {
                if (!finished) {
                    item.itemView.setAlpha(1f);
                }

                dispatchAddFinished(item);
            }

            running.clear();

            dispatchAnimationDidEnd(finished);
        });
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (!isViewHolderAnimated(holder)) {
            dispatchAddFinished(holder);
            return false;
        }

        holder.itemView.setAlpha(0f);
        pending.add(holder);
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        Anime.cancelAll(item.itemView);
    }

    @Override
    public void endAnimations() {
        for (RecyclerView.ViewHolder item : running) {
            Anime.cancelAll(item.itemView);
        }
    }

    @Override
    public boolean isRunning() {
        return !running.isEmpty();
    }
}
