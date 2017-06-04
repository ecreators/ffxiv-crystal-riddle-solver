package de.easycreators.core.event;

import de.easycreators.sequencer.decode.model.Handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class ListenerEvent<T> implements IListenerEvent<T> {
	
	private final Collection<T> listeners;
	private boolean enabled;
	
	public ListenerEvent() {
		listeners = new LinkedHashSet<>();
		enabled = true;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public void addListener(T handler) {
		listeners.add(handler);
	}
	
	@Override
	public void removeListener(T handler) {
		listeners.remove(handler);
	}
	
	public void clear() {
		listeners.clear();
	}
	
	public void invokeAll(Handler<T> handle) {
		if(enabled) {
			new ArrayList<>(listeners).forEach(handle::invoke);
		}
	}
}
