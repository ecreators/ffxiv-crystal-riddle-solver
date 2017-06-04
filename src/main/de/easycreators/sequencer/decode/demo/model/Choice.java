package de.easycreators.sequencer.decode.demo.model;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public enum Choice {
	FORWARD(1),
	BACKWARD(-1);
	
	public final int f;
	
	Choice(int f) {
		this.f = f;
	}
}