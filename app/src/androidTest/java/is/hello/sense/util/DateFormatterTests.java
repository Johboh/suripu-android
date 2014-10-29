package is.hello.sense.util;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public class DateFormatterTests extends InstrumentationTestCase {
    private static final LocalDateTime TEST_LOCAL_DATETIME = new LocalDateTime(2014, 10, 1, 10, 30, 0);
    private static final DateTime TEST_DATETIME = new DateTime(2014, 10, 1, 10, 30, 0);
    private static final LocalTime TEST_LOCAL_TIME = new LocalTime(10, 30, 0);
    private DateFormatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.formatter = new DateFormatter(getInstrumentation().getTargetContext());
    }

    public void testTimelineDate() {
        assertNotNull(formatter.formatAsTimelineDate(null));
    }

    public void testFormatAsBirthDate() {
        assertNotNull(formatter.formatAsBirthDate(null));
        assertEquals("10/01/14", formatter.formatAsBirthDate(TEST_DATETIME));
    }

    public void testFormatAsDate() {
        assertNotNull(formatter.formatAsDate(null));
        assertEquals("October 1", formatter.formatAsDate(TEST_DATETIME));
    }

    public void testFormatAsTime() {
        assertNotNull(formatter.formatAsTime((LocalTime) null, false));
        assertNotNull(formatter.formatAsTime((LocalDateTime) null, false));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_LOCAL_DATETIME, false));
        assertEquals("10:30", formatter.formatAsTime(TEST_LOCAL_DATETIME, true));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_LOCAL_TIME, false));
        assertEquals("10:30", formatter.formatAsTime(TEST_LOCAL_TIME, true));
    }
}
