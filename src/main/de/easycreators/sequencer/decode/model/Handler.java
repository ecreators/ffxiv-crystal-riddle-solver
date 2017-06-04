package de.easycreators.sequencer.decode.model;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface Handler<T> {
	
	void invoke(T args);
}
