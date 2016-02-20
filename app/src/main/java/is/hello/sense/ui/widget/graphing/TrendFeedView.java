package is.hello.sense.ui.widget.graphing;

import android.animation.LayoutTransition;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.List;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.TrendCardView;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.BubbleGraphDrawable;

/**
 * Temporary class for quickly displaying trends.
 */
public class TrendFeedView extends LinearLayout {
    private Trends trends;
    private boolean isError;
    private AnimatorContext animatorContext;
    private static final int DAYS_IN_WEEK = 7;

    public TrendFeedView(@NonNull Context context) {
        this(context, null);
    }

    public TrendFeedView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrendFeedView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutTransition(new LayoutTransition());
    }


    public void setAnimatorContext(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void update(Trends trends) {
        if (isError) {
            isError = false;
            removeAllViews();
        }
        this.trends = trends;
        inflateTrends();
    }

    public void presentError(TrendCardView.OnRetry onRetry) {
        isError = true;
        removeAllViews();
        addView(TrendCardView.createErrorCard(getContext(), onRetry));

    }

    private void inflateTrends() {
        List<Graph> graphList = trends.getGraphs();
        if (graphList.size() == 0) {
            if (findViewWithTag(TrendCardView.StaticCardLayout.class) == null) {
                addView(TrendCardView.createWelcomeCard(getContext()));
            }
        } else if (graphList.size() == 1) {
            Graph graph = graphList.get(0);
            if (graph.getGraphType() == Graph.GraphType.GRID) {
                if (findViewWithTag(TrendCardView.StaticCardLayout.class) == null) {
                    int numberOfDaysWithValues = 0;
                    for (int i = 0; i < graph.getSections().size(); i++) {
                        List<Float> values = graph.getSections().get(i).getValues();
                        for (int j = 0; j < values.size(); j++) {
                            if (values.get(j) != null) {
                                numberOfDaysWithValues++;
                                if (numberOfDaysWithValues == DAYS_IN_WEEK) {
                                    break;
                                }
                            }
                        }
                    }
                    int days = DAYS_IN_WEEK - numberOfDaysWithValues;
                    if (days > 0) {
                        addView(TrendCardView.createComingSoonCard(getContext(), days));
                    }
                }
            }
        }
        for (Graph graph : graphList) {
            Graph.GraphType graphType = graph.getGraphType();
            TrendCardView item = (TrendCardView) findViewWithTag(graphType);
            if (item != null) {
                item.bindGraph(graph);
                continue;
            }
            switch (graph.getGraphType()) {
                case BAR:
                    final BarGraphDrawable barGraphDrawable = new BarGraphDrawable(getContext(), graph, animatorContext);
                    final TrendGraphView barGraphView = new TrendGraphView(getContext(), barGraphDrawable);
                    addView(TrendCardView.createGraphCard(getContext(), graph, barGraphView));
                    break;
                case BUBBLES:
                    final BubbleGraphDrawable bubbleGraphDrawable = new BubbleGraphDrawable(getContext(), graph, animatorContext);
                    final TrendGraphView bubbleGraphView = new TrendGraphView(getContext(), bubbleGraphDrawable);
                    addView(TrendCardView.createGraphCard(getContext(), graph, bubbleGraphView));
                    break;
                case GRID:
                    final GridGraphView gridGraphView = new GridGraphView(getContext());
                    gridGraphView.bindRootLayoutTransition(getLayoutTransition());
                    addView(TrendCardView.createGridCard(getContext(), graph, gridGraphView));
                    break;
                case OVERVIEW:
                    break;
            }
        }
        if (getChildCount() > 0) {
            ((LayoutParams) getChildAt(getChildCount() - 1).getLayoutParams()).bottomMargin = 0;
        }
    }


}
