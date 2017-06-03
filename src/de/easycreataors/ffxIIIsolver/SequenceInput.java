package de.easycreataors.ffxIIIsolver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class SequenceInput<TKey> {
	
	public final  int                                   move;
	public final  int                                   index;
	public final  Map<TKey, SequenceInput<TKey>>        next;
	public final  int                                   input_count;
	private final Function<SequenceInput<TKey>, TKey[]> choices;
	
	public SequenceInput(int move, int index, int input_count, Function<SequenceInput<TKey>, TKey[]> choices) {
		this.move = move;
		this.index = index;
		this.input_count = input_count;
		this.choices = choices;
		this.next = new HashMap<>();
	}
	
	public SequenceInput<TKey> moveTo(TKey move) {
		return next.get(move);
	}
	
	@Override
	public int hashCode() {
		return index;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof SequenceInput && obj.hashCode() == hashCode();
	}
	
	public TKey[] choices() {
		return choices.apply(this);
	}
}
