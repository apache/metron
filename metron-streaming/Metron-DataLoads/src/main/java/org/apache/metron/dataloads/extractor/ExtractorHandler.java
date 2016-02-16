package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.extractor.inputformat.Formats;
import org.apache.metron.dataloads.extractor.inputformat.InputFormatHandler;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by cstella on 2/2/16.
 */
public class ExtractorHandler {
    final static ObjectMapper _mapper = new ObjectMapper();
    private Map<String, Object> config;
    private Extractor extractor;
    private InputFormatHandler inputFormatHandler = Formats.BY_LINE;

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public InputFormatHandler getInputFormatHandler() {
        return inputFormatHandler;
    }

    public void setInputFormatHandler(String handler) {
        try {
            this.inputFormatHandler= Formats.create(handler);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to create an inputformathandler", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to create an inputformathandler", e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Unable to create an inputformathandler", e);
        }
    }

    public Extractor getExtractor() {
        return extractor;
    }
    public void setExtractor(String extractor) {
        try {
            this.extractor = Extractors.create(extractor);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to create an extractor", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to create an extractor", e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Unable to create an extractor", e);
        }
    }

    public static synchronized ExtractorHandler load(InputStream is) throws IOException {
        ExtractorHandler ret = _mapper.readValue(is, ExtractorHandler.class);
        ret.getExtractor().initialize(ret.getConfig());
        return ret;
    }
    public static synchronized ExtractorHandler load(String s, Charset c) throws IOException {
        return load( new ByteArrayInputStream(s.getBytes(c)));
    }
    public static synchronized ExtractorHandler load(String s) throws IOException {
        return load( s, Charset.defaultCharset());
    }
}
