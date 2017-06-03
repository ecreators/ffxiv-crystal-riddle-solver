package de.easycreataors.ffxIIIsolver;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public enum Move {
	FORWARD(1),
	BACKWARD(-1);
	
	public final int f;
	private static final Random random = new Random(System.currentTimeMillis());
	
	Move(int f) {
		this.f = f;
	}
	
	public Move opposite() {
		return get(-f);
	}
	
	private static Move get(int f) {
		return Arrays.stream(Move.values()).filter(m -> m.f == f).findFirst().orElse(null);
	}
	
	public static Move random() {
		return get((int) Math.round(random.nextGaussian()) == 1
		           ? 1
		           : -1);
	}
}
