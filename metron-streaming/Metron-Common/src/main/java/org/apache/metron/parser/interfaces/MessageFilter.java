package org.apache.metron.parser.interfaces;

public interface MessageFilter<T> {

	boolean emitTuple(T message);

}
