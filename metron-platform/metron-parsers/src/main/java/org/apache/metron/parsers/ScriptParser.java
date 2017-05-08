package org.apache.metron.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class ScriptParser implements MessageParser<JSONObject>,Serializable{
	
	protected static final Logger LOG = LoggerFactory.getLogger(ScriptParser.class);
	protected String scriptPath;
	protected ScriptEngine engine;
	protected String parseFunction;
	protected String language;
	protected String commonScript="/scripts/";
	protected List<String> timeFields = new ArrayList<>();
	protected String timestampField;
	protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");

	@Override
	public void configure(Map<String, Object> config) {
		// TODO Auto-generated method stub
		this.scriptPath=(String) config.get("path");
		this.parseFunction=(String)config.get("function");
		this.language=(String)config.get("language");
		this.commonScript=this.commonScript+language+"/common";
		if(this.parseFunction==null)
			this.parseFunction="parse";
	}
	//Should this be sent to the interface as a default method?
	public InputStream openInputStream(String streamName) throws IOException {
	    FileSystem fs = FileSystem.get(new Configuration());
	    Path path = new Path(streamName);
	    if(fs.exists(path)) {
	      return fs.open(path);
	    } else {
	      return getClass().getResourceAsStream(streamName);
	    }
	  }

	@Override
	public void init() {
		// TODO Auto-generated method stub
		engine = new ScriptEngineManager().getEngineByName(this.language);
		try{
			InputStream commonStream = openInputStream(this.commonScript);
			if (commonStream == null) {
		        throw new RuntimeException(
		                "Unable to initialize "+this.language+" Parser: Unable to load " + this.commonScript + " from either classpath or HDFS");
		      }

		      engine.eval(new InputStreamReader(commonStream));
		      if (LOG.isDebugEnabled()) {
		        LOG.debug("Loading parser-specific functions from: " + this.scriptPath);
		      }

		      InputStream patterInputStream = openInputStream(this.scriptPath);
		      if (patterInputStream == null) {
		        throw new RuntimeException("Script parser unable to initialize "+this.language+" parser: Unable to load " + this.scriptPath
		                + " from either classpath or HDFS");
		      }
		      engine.eval(new InputStreamReader(patterInputStream));
		}catch(Throwable e){
			LOG.error(e.getMessage(), e);
		    throw new RuntimeException(this.language+" Script parser Error: " + e.getMessage(), e);
		}
		
		Invocable invocable = (Invocable) engine;
		
	}

	@Override
	public List<JSONObject> parse(byte[] rawMessage) {
		// TODO Auto-generated method stub
		List<JSONObject> messages = new ArrayList<JSONObject>();
		if(engine==null)
			init();
		String originalMessage = null;
		try{
			originalMessage = new String(rawMessage, "UTF-8");
			if (LOG.isDebugEnabled()) {
	          LOG.debug("JS parser parsing message: " + originalMessage);
	        }
			Invocable invocable = (Invocable) engine;
	
			Object result = invocable.invokeFunction(this.parseFunction,originalMessage);
			JSONObject message = new JSONObject((Map)result);
			
			if (message.size() == 0)
		        throw new RuntimeException("Script produced a null message. Original message was: "
		                + originalMessage + " and the parsed message was: " + message + " . Check the function at: "
		                + this.scriptPath);

		      message.put("original_string", originalMessage);
		      for (String timeField : timeFields) {
		        String fieldValue = (String) message.get(timeField);
		        if (fieldValue != null) {
		          message.put(timeField, toEpoch(fieldValue));
		        }
		      }
		      if (timestampField != null) {
		        message.put(Constants.Fields.TIMESTAMP.getName(), formatTimestamp(message.get(timestampField)));
		      }
		      messages.add(message);
		      if (LOG.isDebugEnabled()) {
		        LOG.debug("Grok parser parsed message: " + message);
		      }
		}catch (Exception e) {
		    LOG.error(e.getMessage(), e);
		    throw new IllegalStateException("Grok parser Error: " + e.getMessage() + " on " + originalMessage , e);
		}
		return messages;
	}

	@Override
	public boolean validate(JSONObject message) {
		// TODO Auto-generated method stub
		return true;
	}
	
	protected long toEpoch(String datetime) throws ParseException {

	    LOG.debug("Grok parser converting timestamp to epoch: {}", datetime);
	    LOG.debug("Grok parser's DateFormat has TimeZone: {}", dateFormat.getTimeZone());

	    Date date = dateFormat.parse(datetime);
	    if (LOG.isDebugEnabled()) {
	      LOG.debug("Grok parser converted timestamp to epoch: " + date);
	    }

	    return date.getTime();
	  }

	  protected long formatTimestamp(Object value) {

	    if (LOG.isDebugEnabled()) {
	      LOG.debug("Grok parser formatting timestamp" + value);
	    }


	    if (value == null) {
	      throw new RuntimeException(this.parseFunction + " parser does not include field " + timestampField);
	    }
	    if (value instanceof Number) {
	      return ((Number) value).longValue();
	    } else {
	      return Long.parseLong(Joiner.on("").join(Splitter.on('.').split(value + "")));
	    }
	  }
	
	
}
