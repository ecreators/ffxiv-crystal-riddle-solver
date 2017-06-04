package de.easycreators.core.event;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface IListenerEvent<T> {
	
	void addListener(T handler);
	
	void removeListener(T handler);
}
