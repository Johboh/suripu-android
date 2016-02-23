package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.BubbleGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;

public class Graph extends ApiResponse {
    @SerializedName("time_scale")
    private Trends.TimeScale timeScale;

    @SerializedName("title")
    private String title;

    @SerializedName("data_type")
    private DataType dataType;

    @SerializedName("graph_type")
    private GraphType graphType;

    @SerializedName("min_value")
    private float minValue;

    @SerializedName("max_value")
    private float maxValue;

    @SerializedName("sections")
    private List<GraphSection> sections;

    @SerializedName("condition_ranges")
    @VisibleForTesting
    List<ConditionRange> conditionRanges;

    @SerializedName("annotations")
    private List<Annotation> annotations;

    @VisibleForTesting
    public Graph(@NonNull String title,
                 @NonNull DataType dataType,
                 @NonNull GraphType graphType) {
        this.title = title;
        this.dataType = dataType;
        this.graphType = graphType;
    }

    public Trends.TimeScale getTimeScale() {
        return timeScale;
    }

    public String getTitle() {
        return title;
    }

    public DataType getDataType() {
        return dataType;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public GraphType getSpecConformingGraphType() {
        if (timeScale == Trends.TimeScale.LAST_3_MONTHS && graphType == GraphType.GRID) {
            return GraphType.OVERVIEW;
        } else {
            return graphType;
        }
    }

    public boolean isGrid() {
        final GraphType graphType = getSpecConformingGraphType();
        return (graphType == GraphType.GRID ||
                graphType == GraphType.OVERVIEW);
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public List<GraphSection> getSections() {
        return sections;
    }

    public List<ConditionRange> getConditionRanges() {
        return conditionRanges;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public Condition getConditionForValue(float value) {
        for (final ConditionRange conditionRange : conditionRanges) {
            if (value >= conditionRange.getMinValue() && value <= conditionRange.getMaxValue()) {
                return conditionRange.getCondition();
            }
        }

        return Condition.UNKNOWN;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "timeScale=" + timeScale.toString() +
                ", title='" + title + '\'' +
                ", dataType='" + dataType + '\'' +
                ", graphType='" + graphType.toString() + '\'' +
                ", minValue='" + minValue + '\'' +
                ", maxValue='" + maxValue + '\'' +
                ", sections='" + sections.toString() + '\'' +
                ", conditionRanges='" + conditionRanges.toString() + '\'' +
                ", annotations='" + annotations.toString() + '\'' +
                '}';
    }

    public enum GraphType implements Enums.FromString {
        NO_DATA,
        EMPTY,
        GRID,
        OVERVIEW,
        BAR,
        BUBBLES;

        public static GraphType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), EMPTY);
         }
    }

    public enum DataType implements Enums.FromString {
        NONE,
        SCORES,
        HOURS {
            @Override
            public CharSequence renderAnnotation(@NonNull Annotation annotation) {
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue(), 2),
                                                     BarGraphDrawable.HOUR_SYMBOL,
                                                     Styles.UNIT_STYLE_SUBSCRIPT);
            }
        },
        PERCENTS {
            @Override
            public CharSequence renderAnnotation(@NonNull Annotation annotation) {
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue() * 100, 0),
                                                     BubbleGraphDrawable.PERCENT_SYMBOL,
                                                     Styles.UNIT_STYLE_SUBSCRIPT);
            }
        };

        public boolean wantsConditionTinting() {
            return (this == SCORES);
        }

        public CharSequence renderAnnotation(@NonNull Annotation annotation) {
            return Styles.createTextValue(annotation.getValue(), 0);
        }

        public static DataType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }

}