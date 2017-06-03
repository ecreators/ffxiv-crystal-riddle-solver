package de.easycreataors.ffxIIIsolver;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class ClockField {
	
	public final int                   move;
	public final int                   index;
	public final Map<Move, ClockField> next;
	public final int                   fullClockCount;
	
	public ClockField(int move, int index, int fullClockCount) {
		this.move = move;
		this.index = index;
		this.fullClockCount = fullClockCount;
		this.next = new HashMap<>();
	}
	
	public ClockField moveTo(Move move) {
		return next.get(move);
	}
	
	@Override
	public int hashCode() {
		return index;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ClockField && obj.hashCode() == hashCode();
	}
}
