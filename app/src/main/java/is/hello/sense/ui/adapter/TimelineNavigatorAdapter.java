package is.hello.sense.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.SafeObserverWrapper;
import is.hello.sense.graph.presenters.TimelineNavigatorPresenter;
import is.hello.sense.ui.widget.MiniTimelineView;
import is.hello.sense.ui.widget.graphing.SimplePieDrawable;
import is.hello.sense.ui.widget.util.Styles;
import rx.Observer;
import rx.Subscription;

public class TimelineNavigatorAdapter extends RecyclerView.Adapter<TimelineNavigatorAdapter.ItemViewHolder> implements View.OnClickListener {
    public static final int TOTAL_DAYS = 366;

    private final Context context;
    private final LayoutInflater inflater;
    private final TimelineNavigatorPresenter presenter;

    private @Nullable OnItemClickedListener onItemClickedListener;

    public TimelineNavigatorAdapter(@NonNull Context context,
                                    @NonNull TimelineNavigatorPresenter presenter) {
        this.context = context;
        this.presenter = presenter;
        this.inflater = LayoutInflater.from(context);
    }


    public void setOnItemClickedListener(@Nullable OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }


    @Override
    public int getItemCount() {
        return TOTAL_DAYS;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View itemView = inflater.inflate(R.layout.item_timeline_navigator, viewGroup, false);
        itemView.setOnClickListener(this);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        DateTime date = presenter.getDateTimeAt(position);

        holder.itemView.setTag(position);

        holder.date = date;
        holder.dayNumber.setText(date.toString("d"));
        holder.dayName.setText(date.toString("EE"));
        holder.pieDrawable.setValue(0);
        holder.pieDrawable.setTrackColor(Styles.getSleepScoreBorderColor(context, 0));
        holder.score.setText(R.string.missing_data_placeholder);
        presenter.post(holder, holder::load);
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
        super.onViewRecycled(holder);

        presenter.cancel(holder);
        holder.reset();
    }


    @Override
    public void onClick(View view) {
        if (onItemClickedListener != null) {
            int position = (int) view.getTag();
            onItemClickedListener.onItemClicked(view, position);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView dayNumber;
        public final TextView dayName;
        public final TextView score;
        public final MiniTimelineView timeline;

        public final SimplePieDrawable pieDrawable;

        public @Nullable DateTime date;

        private @Nullable Subscription loading;

        private ItemViewHolder(View itemView) {
            super(itemView);

            this.dayNumber = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_number);
            this.dayName = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_name);
            this.score = (TextView) itemView.findViewById(R.id.item_timeline_navigator_score);
            this.timeline = (MiniTimelineView) itemView.findViewById(R.id.item_timeline_navigator_timeline);

            this.pieDrawable = new SimplePieDrawable(context.getResources());

            View pieView = itemView.findViewById(R.id.item_timeline_navigator_pie);
            pieView.setBackground(pieDrawable);
        }

        private void reset() {
            if (loading != null) {
                loading.unsubscribe();
                this.loading = null;
            }

            this.date = null;

            pieDrawable.setValue(0);
            pieDrawable.setTrackColor(context.getResources().getColor(R.color.border));
            score.setText(R.string.missing_data_placeholder);

            timeline.setTimelineSegments(null);
        }

        private void load() {
            if (loading == null && date != null) {
                this.loading = presenter.timelineForDate(date).subscribe(new SafeObserverWrapper<>(new Observer<Timeline>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        loading = null;

                        score.setText(R.string.missing_data_placeholder);
                        pieDrawable.setFillColor(context.getResources().getColor(R.color.sensor_warning));
                        pieDrawable.setValue(100);

                        timeline.setTimelineSegments(null);
                    }

                    @Override
                    public void onNext(Timeline timeline) {
                        loading = null;

                        int sleepScore = timeline.getScore();
                        score.setText(Integer.toString(sleepScore));
                        pieDrawable.setTrackColor(Color.TRANSPARENT);
                        pieDrawable.setFillColor(Styles.getSleepScoreColor(context, sleepScore));
                        pieDrawable.setValue(sleepScore);
                    }
                }));
            }
        }
    }

    public interface OnItemClickedListener {
        void onItemClicked(@NonNull View itemView, int position);
    }
}
