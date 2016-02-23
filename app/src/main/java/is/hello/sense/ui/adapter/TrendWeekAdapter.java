package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.graphing.GridGraphCellView;
import is.hello.sense.ui.widget.graphing.GridGraphView;

public class TrendWeekAdapter extends GridGraphView.Adapter {
    private final Context context;
    private Graph graph;

    //region Lifecycle

    public TrendWeekAdapter(@NonNull Context context) {
        this.context = context;
    }

    public int getEstimatedRowCount(@NonNull Trends.TimeScale timeScale) {
        switch (timeScale) {
            case LAST_MONTH:
                return 5;
            case LAST_WEEK:
                return 2;
            default:
                return 1;
        }
    }

    public void bind(@NonNull Graph graph) {
        this.graph = graph;
        notifyDataSetChanged();
    }

    public void clear() {
        this.graph = null;
        notifyDataSetChanged();
    }

    //endregion


    //region Providing Data

    @Override
    public int getRowCount() {
        if (graph != null) {
            return graph.getSections().size();
        } else {
            return 0;
        }
    }

    @Override
    public int getRowCellCount(int row) {
        return graph.getSections().get(row).getValues().size();
    }

    @Override
    public int getMaximumRowCellCount() {
        return 7;
    }

    @Nullable
    @Override
    public String getCellReading(int row, int cell) {
        final GraphSection section = graph.getSections().get(row);
        final Float value = section.getValues().get(cell);
        if (value != null) {
            if (value < 0f) {
                return context.getString(R.string.missing_data_placeholder);
            } else {
                return Integer.toString(value.intValue());
            }
        } else {
            return null;
        }
    }

    @Override
    @ColorInt
    public int getCellColor(int row, int cell) {
        final GraphSection section = graph.getSections().get(row);
        final Float value = section.getValues().get(cell);
        if (value != null) {
            if (value < 0f) {
                return ContextCompat.getColor(context, R.color.graph_grid_empty_missing);
            } else {
                final Condition condition = graph.getConditionForValue(value);
                return ContextCompat.getColor(context, condition.colorRes);
            }
        } else {
            return ContextCompat.getColor(context, R.color.graph_grid_empty_cell);
        }
    }

    @NonNull
    @Override
    public GridGraphCellView.Border getCellBorder(int row, int cell) {
        final GraphSection section = graph.getSections().get(row);
        final Float value = section.getValues().get(cell);
        if (value == null || value < 0f) {
            return GridGraphCellView.Border.OUTSIDE;
        } else if (section.getHighlightedValues().contains(cell)) {
            return GridGraphCellView.Border.INSIDE;
        } else {
            return GridGraphCellView.Border.NONE;
        }
    }

    //endregion
}