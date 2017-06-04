package de.easycreators.sequencer.decode.model;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface SolutionHandler<T> {
	
	void invoke(T args);
	
	boolean hasSolution();
}
