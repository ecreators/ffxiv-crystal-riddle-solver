package de.easycreataors.ffxIIIsolver;

import static java.util.Arrays.asList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class Main {
	
	public static void main(String[] args) {
		CrystalClockRiddle riddle = new CrystalClockRiddle();
		riddle.setClock(asList(2, 3, 4, 1, 3, 2, 5, 6, 6, 2));
		riddle.solve();
	}
}
