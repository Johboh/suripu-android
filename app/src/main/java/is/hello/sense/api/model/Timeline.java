package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.api.ApiService;

public class Timeline extends ApiResponse {
    @JsonProperty("score")
    private int score;

    @JsonProperty("message")
    private String message;

    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private DateTime date;

    @JsonProperty("segments")
    @JsonDeserialize(contentAs = TimelineSegment.class)
    private List<TimelineSegment> segments;

    @JsonProperty("insights")
    @JsonDeserialize(contentAs = PreSleepInsight.class)
    private List<PreSleepInsight> preSleepInsights;


    public int getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    public DateTime getDate() {
        return date;
    }

    public List<TimelineSegment> getSegments() {
        return segments;
    }

    public List<PreSleepInsight> getPreSleepInsights() {
        return preSleepInsights;
    }

    @Override
    public String toString() {
        return "Timeline{" +
                "score=" + score +
                ", message='" + message + '\'' +
                ", date='" + date + '\'' +
                ", segments=" + segments +
                ", insights=" + preSleepInsights +
                '}';
    }
}
