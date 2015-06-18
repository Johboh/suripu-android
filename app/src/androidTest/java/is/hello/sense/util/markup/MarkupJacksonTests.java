package is.hello.sense.util.markup;

import android.graphics.Typeface;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;

public class MarkupJacksonTests extends TestCase {
    private final ObjectMapper mapper;

    public MarkupJacksonTests() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new MarkupJacksonModule());
    }


    public void testDeserialize() throws Exception {
        String json = "{\"message\": \"This **really** works\"}";
        TestObject testObject = mapper.readValue(json, TestObject.class);
        assertNotNull(testObject);

        MarkupString message = testObject.message;
        assertNotNull(message);
        assertEquals("This really works", message.toString());

        MarkupStyleSpan[] spans = message.getSpans(0, message.length(), MarkupStyleSpan.class);
        assertEquals(1, spans.length);
        assertEquals(Typeface.BOLD, spans[0].getStyle());
        assertEquals(5, message.getSpanStart(spans[0]));
        assertEquals(11, message.getSpanEnd(spans[0]));
    }


    public static class TestObject {
        public final MarkupString message;

        public TestObject(@JsonProperty("message") MarkupString message) {
            this.message = message;
        }
    }
}