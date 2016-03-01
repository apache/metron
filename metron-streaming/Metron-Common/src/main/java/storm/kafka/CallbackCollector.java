package storm.kafka;

import backtype.storm.spout.ISpoutOutputCollector;
import backtype.storm.spout.SpoutOutputCollector;

import java.io.Serializable;
import java.util.List;

public class CallbackCollector extends SpoutOutputCollector implements Serializable {
    static final long serialVersionUID = 0xDEADBEEFL;
    Callback _callback;
    SpoutOutputCollector _delegate;
    EmitContext _context;
    public CallbackCollector(Callback callback, SpoutOutputCollector collector, EmitContext context) {
        super(collector);
        this._callback = callback;
        this._delegate = collector;
        this._context = context;
    }


    /**
     * Emits a new tuple to the specified output stream with the given message ID.
     * When Storm detects that this tuple has been fully processed, or has failed
     * to be fully processed, the spout will receive an ack or fail callback respectively
     * with the messageId as long as the messageId was not null. If the messageId was null,
     * Storm will not track the tuple and no callback will be received. The emitted values must be
     * immutable.
     *
     * @param streamId
     * @param tuple
     * @param messageId
     * @return the list of task ids that this tuple was sent to
     */
    @Override
    public List<Integer> emit(String streamId, List<Object> tuple, Object messageId) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.MESSAGE_ID, messageId)
                                                                       .with(EmitContext.Type.STREAM_ID, streamId)
                                        );
        return _delegate.emit(streamId, t, messageId);
    }

    /**
     * Emits a new tuple to the default output stream with the given message ID.
     * When Storm detects that this tuple has been fully processed, or has failed
     * to be fully processed, the spout will receive an ack or fail callback respectively
     * with the messageId as long as the messageId was not null. If the messageId was null,
     * Storm will not track the tuple and no callback will be received. The emitted values must be
     * immutable.
     *
     * @param tuple
     * @param messageId
     * @return the list of task ids that this tuple was sent to
     */
    @Override
    public List<Integer> emit(List<Object> tuple, Object messageId) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.MESSAGE_ID, messageId));
        return _delegate.emit(t, messageId);
    }

    /**
     * Emits a tuple to the default output stream with a null message id. Storm will
     * not track this message so ack and fail will never be called for this tuple. The
     * emitted values must be immutable.
     *
     * @param tuple
     */
    @Override
    public List<Integer> emit(List<Object> tuple) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext());
        return _delegate.emit(t);
    }

    /**
     * Emits a tuple to the specified output stream with a null message id. Storm will
     * not track this message so ack and fail will never be called for this tuple. The
     * emitted values must be immutable.
     *
     * @param streamId
     * @param tuple
     */
    @Override
    public List<Integer> emit(String streamId, List<Object> tuple) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.STREAM_ID, streamId));
        return _delegate.emit(streamId, t);
    }

    /**
     * Emits a tuple to the specified task on the specified output stream. This output
     * stream must have been declared as a direct stream, and the specified task must
     * use a direct grouping on this stream to receive the message. The emitted values must be
     * immutable.
     *
     * @param taskId
     * @param streamId
     * @param tuple
     * @param messageId
     */
    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple, Object messageId) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.STREAM_ID, streamId)
                                                                       .with(EmitContext.Type.MESSAGE_ID, messageId)
                                                                       .with(EmitContext.Type.TASK_ID, new Integer(taskId))
                                        );
        _delegate.emitDirect(taskId, streamId, t, messageId);
    }

    /**
     * Emits a tuple to the specified task on the default output stream. This output
     * stream must have been declared as a direct stream, and the specified task must
     * use a direct grouping on this stream to receive the message. The emitted values must be
     * immutable.
     *
     * @param taskId
     * @param tuple
     * @param messageId
     */
    @Override
    public void emitDirect(int taskId, List<Object> tuple, Object messageId) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.MESSAGE_ID, messageId)
                                                                       .with(EmitContext.Type.TASK_ID, new Integer(taskId))
                       );
        _delegate.emitDirect(taskId, t, messageId);
    }

    /**
     * Emits a tuple to the specified task on the specified output stream. This output
     * stream must have been declared as a direct stream, and the specified task must
     * use a direct grouping on this stream to receive the message. The emitted values must be
     * immutable.
     * <p/>
     * <p> Because no message id is specified, Storm will not track this message
     * so ack and fail will never be called for this tuple.</p>
     *
     * @param taskId
     * @param streamId
     * @param tuple
     */
    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple) {
        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.STREAM_ID, streamId)
                                                                       .with(EmitContext.Type.TASK_ID, new Integer(taskId))
                       );
        _delegate.emitDirect(taskId, streamId, t);
    }

    /**
     * Emits a tuple to the specified task on the default output stream. This output
     * stream must have been declared as a direct stream, and the specified task must
     * use a direct grouping on this stream to receive the message. The emitted values must be
     * immutable.
     * <p/>
     * <p> Because no message id is specified, Storm will not track this message
     * so ack and fail will never be called for this tuple.</p>
     *
     * @param taskId
     * @param tuple
     */
    @Override
    public void emitDirect(int taskId, List<Object> tuple) {

        List<Object> t = _callback.apply(tuple, _context.cloneContext().with(EmitContext.Type.TASK_ID, new Integer(taskId)));
        _delegate.emitDirect(taskId, t);
    }
}
