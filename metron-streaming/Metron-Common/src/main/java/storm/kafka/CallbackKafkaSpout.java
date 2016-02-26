package storm.kafka;

import backtype.storm.Config;
import backtype.storm.metric.api.IMetric;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import storm.kafka.*;

import java.util.*;

public class CallbackKafkaSpout extends KafkaSpout {
    static final long serialVersionUID = 0xDEADBEEFL;
    Class<? extends Callback> callbackClazz;
    Callback _callback;
    EmitContext _context;
    public CallbackKafkaSpout(SpoutConfig spoutConfig, String callbackClass) {
        this(spoutConfig, toCallbackClass(callbackClass));
    }

    public CallbackKafkaSpout(SpoutConfig spoutConf, Class<? extends Callback> callback) {
        super(spoutConf);
        callbackClazz = callback;
    }

    public void initialize() {
        _callback = createCallback(callbackClazz);
        _context = new EmitContext().with(EmitContext.Type.SPOUT_CONFIG, _spoutConfig)
                                    .with(EmitContext.Type.UUID, _uuid);
        _callback.initialize(_context);
    }


    private static Class<? extends Callback> toCallbackClass(String callbackClass)  {
        try{
            return (Class<? extends Callback>) Callback.class.forName(callbackClass);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(callbackClass + " not found", e);
        }
    }

    protected Callback createCallback(Class<? extends Callback> callbackClass)  {
        try {
            return callbackClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate callback", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access", e);
        }
    }

    @Override
    public void open(Map conf, final TopologyContext context, final SpoutOutputCollector collector) {
        if(_callback == null) {
            initialize();
        }
        super.open( conf, context
                  , new CallbackCollector(_callback, collector
                                         ,_context.cloneContext().with(EmitContext.Type.OPEN_CONFIG, conf)
                                                                 .with(EmitContext.Type.TOPOLOGY_CONTEXT, context)
                                         )
                  );
    }

    @Override
    public void close() {
        super.close();
        if(_callback != null) {
            try {
                _callback.close();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to close callback", e);
            }
        }
    }
}
