package storm.kafka;

import java.io.Serializable;
import java.util.List;

public interface Callback extends AutoCloseable, Serializable {
    List<Object> apply(List<Object> tuple, EmitContext context);
    void initialize(EmitContext context);
}
