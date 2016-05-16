package org.apache.metron.parsers.mcafeeepo;


        import com.opencsv.CSVReader;

        import oi.thekraken.grok.api.exception.GrokException;
        import org.apache.metron.parsers.BasicParser;
        import org.apache.metron.parsers.GrokParser;
        import org.apache.metron.parsers.bluecoat.BasicBluecoatParser;
        import org.json.simple.JSONObject;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.io.IOException;
        import java.io.StringReader;
        import java.io.UnsupportedEncodingException;
        import java.text.ParseException;
        import java.text.SimpleDateFormat;
        import java.util.*;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

/**
 * Created by rzf350 and vbz083 on 4/26/2016.
 */
public class McAfeeEpoParser extends BasicParser {
    private static final Logger _LOG = LoggerFactory.getLogger(BasicBluecoatParser.class);
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void init() {

    }

    @SuppressWarnings({ "unchecked", "unused" })
    public List<JSONObject> parse(byte[] msg) {

        String message = "";
        List<JSONObject> messages = new ArrayList<>();
        JSONObject payload = new JSONObject();

        try {
            message = new String(msg, "UTF-8");


            String[] parts = message.split("<|>|\", |\" ");
            payload.put("original_string", message);
            payload.put("priority", parts[1]);






            messages.add(payload);
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            _LOG.error("Failed to parse: " + message);
            return null;
        }
    }
}
