package org.apache.metron.parser.interfaces;

import java.util.List;

public interface MessageParser<T> {

	void init();
	List<T> parse(byte[] rawMessage);
	boolean validate(T message);

}
