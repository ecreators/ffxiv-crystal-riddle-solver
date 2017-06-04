package java.ffxIIIsolver.encode;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class SequenceEncoder {
	
	private List<Integer> sequence;
	private List<Integer> moves;
	
	public void setFields(List<Integer> sequence) throws IllegalArgumentException {
		validate(sequence);
		this.sequence = sequence;
	}
	
	public List<Integer> encodeMoves() {
		int           size  = sequence.size();
		List<Integer> moves = new ArrayList<>();
		int           i;
		for (i = 1; i < size; i++) {
			moves.add(toMove(sequence.get(i - 1), sequence.get(i), size));
		}
		moves.add(toMove(sequence.get(i - 1), sequence.get(0), size));
		this.moves = Collections.unmodifiableList(moves);
		return moves;
	}
	
	private int toMove(int from, int to, int modulo) {
		int res      = from - to;
		int overflow = res + modulo;
		int move     = overflow % modulo;
		return move;
	}
	
	private void validate(List<Integer> sequence) throws IllegalArgumentException {
		if(sequence.size() < 2) {
			throw new IllegalArgumentException("Your sequence is too small. At least 2 Elements!");
		}
		Set<Integer> rules = new LinkedHashSet<>(sequence);
		if(rules.size() < sequence.size()) {
			List<AbstractMap.SimpleEntry<Integer, Integer>> sums = rules.stream().map(i -> new AbstractMap.SimpleEntry<>(i, sequence.stream().filter(j -> Objects.equals(j, i)).mapToInt(j -> j).sum())).filter(e -> e.getValue() > 1).collect(toList());
			throw new IllegalArgumentException("Your sequence has duplicated indices: " + Arrays.toString(sums.toArray()));
		}
	}
	
	public List<Integer> getMoves() {
		return moves;
	}
}
