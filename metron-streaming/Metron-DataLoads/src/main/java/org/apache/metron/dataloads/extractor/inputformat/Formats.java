package org.apache.metron.dataloads.extractor.inputformat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cstella on 2/8/16.
 */
public enum Formats implements InputFormatHandler{
    BY_LINE(new InputFormatHandler() {
        @Override
        public void set(Job job, Path input, Map<String, Object> config) throws IOException {

            FileInputFormat.addInputPath(job, input);
        }
    })
    ;
    InputFormatHandler _handler = null;
    Formats(InputFormatHandler handler) {
        this._handler = handler;
    }
    @Override
    public void set(Job job, Path path, Map<String, Object> config) throws IOException {
        _handler.set(job, path, config);
    }

    public static InputFormatHandler create(String handlerName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        try {
            InputFormatHandler ec = Formats.valueOf(handlerName);
            return ec;
        }
        catch(IllegalArgumentException iae) {
            InputFormatHandler ex = (InputFormatHandler) Class.forName(handlerName).newInstance();
            return ex;
        }
    }
}
