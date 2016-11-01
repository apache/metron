package org.apache.metron.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSParser implements MessageParser<JSONObject>,Serializable{
	
	protected static final Logger LOG = LoggerFactory.getLogger(JSParser.class);
	protected String jsPath;
	protected ScriptEngine engine;
	protected String parseFunction;
	//protected String language;
	protected String commonJS="/scripts/js/common";

	@Override
	public void configure(Map<String, Object> config) {
		// TODO Auto-generated method stub
		this.jsPath=(String) config.get("path");
		this.parseFunction=(String)config.get("function");
		if(this.parseFunction==null)
			this.parseFunction="parse";
	}
	
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
		engine = new ScriptEngineManager().getEngineByName("nashorn");
		try{
			InputStream commonStream = openInputStream(this.commonJS);
			if (commonStream == null) {
		        throw new RuntimeException(
		                "Unable to initialize JS Parser: Unable to load " + this.commonJS + " from either classpath or HDFS");
		      }

		      engine.eval(new InputStreamReader(commonStream));
		      if (LOG.isDebugEnabled()) {
		        LOG.debug("Loading parser-specific patterns from: " + this.jsPath);
		      }

		      InputStream patterInputStream = openInputStream(this.jsPath);
		      if (patterInputStream == null) {
		        throw new RuntimeException("Grok parser unable to initialize grok parser: Unable to load " + this.jsPath
		                + " from either classpath or HDFS");
		      }
		      engine.eval(new InputStreamReader(patterInputStream));
		}catch(Throwable e){
			LOG.error(e.getMessage(), e);
		    throw new RuntimeException("JS parser Error: " + e.getMessage(), e);
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
			JSONObject jsonObject = new JSONObject((Map)result);
			messages.add(jsonObject);
			return messages;
		}catch (Exception e) {
		    LOG.error(e.getMessage(), e);
		    throw new IllegalStateException("Grok parser Error: " + e.getMessage() + " on " + originalMessage , e);
		}
	}

	@Override
	public boolean validate(JSONObject message) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static void main(String[] args){
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		String json ="var test = {'id': 10,'Hello': 'World','test':"
				+ " {'Lorem' : 'Ipsum','java'  : true },}";
		try{
			engine.eval("var fun1 = function() {\n"+json+";return test;\n};");
			Invocable invocable = (Invocable) engine;

			Object result = invocable.invokeFunction("fun1", "Peter Parker");
			System.out.println(result);
			Map obj = (Map)result;
			System.out.println(obj);
			JSONObject jsonObject = new JSONObject(obj);
			System.out.println(jsonObject);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}

}
