package de.easycreators.sequencer.decode.demo.model;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public enum Choice {
	FORWARD(1),
	BACKWARD(-1);
	
	public final int f;
	public static final Random RANDOM = new Random(System.currentTimeMillis());
	
	Choice(int f) {
		this.f = f;
	}
	
	public Choice opposite() {
		return get(-f);
	}
	
	private static Choice get(int f) {
		return Arrays.stream(Choice.values()).filter(m -> m.f == f).findFirst().orElse(null);
	}
	
	public static Choice random() {
		return get((int) Math.round(RANDOM.nextGaussian()) == 1
		           ? 1
		           : -1);
	}
}
