package de.easycreataors.ffxIIIsolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings("WeakerAccess")
public abstract class SequenceSolver<TKey> {
	
	private final ExecutorService pool;
	private final String          name;
	private       List<Integer>   sequence;
	
	protected SequenceSolver(String name) {
		this.name = name;
		this.pool = Executors.newCachedThreadPool();
	}
	
	public void setSequence(List<Integer> clock) {
		this.sequence = new ArrayList<>(clock);
	}
	
	public void solve() {
		AtomicReference<SequenceInputSolver<TKey>> stop = new AtomicReference<>();
		int                                        size = sequence.size();
		
		// define
		List<SequenceInput<TKey>> seq_inputs = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			seq_inputs.add(new SequenceInput<>(sequence.get(i), i, size, this::getOptionsBySequenceInput));
		}
		
		// align
		for (SequenceInput<TKey> input : seq_inputs) {
			for (TKey choice : input.choices()) {
				input.next.put(choice, seq_inputs.get(getNext(input, choice)));
			}
		}
		
		for (SequenceInput<TKey> input : seq_inputs) {
			if(stop.get() == null) {
				pool.execute(new SequenceInputSolver<TKey>(stop, name, input, seq_inputs, this::onInputSolved) {
					
					@Override
					protected void printUsed(Collection<SequenceInput<TKey>> taken, String progress) {
//						super.printUsed(taken, progress);
					}
					
					@Override
					protected String format(TKey finalChoice, SequenceInput<TKey> t) {
						return SequenceSolver.this.takenFormat(finalChoice, t);
					}
					
					@Override
					protected List<Integer> listRoute(List<TKey> route) {
						List<Integer> indices = super.listRoute(route);
//						System.out.println("OK: " + super.getName() + " " + Arrays.toString(indices.toArray()));
						return indices;
					}
				});
			}
		}
	}
	
	@SuppressWarnings("unused")
	protected String takenFormat(TKey finalChoice, SequenceInput<TKey> t) {
		return t.toString();
	}
	
	protected abstract TKey[] getOptionsBySequenceInput(SequenceInput<TKey> field);
	
	@SuppressWarnings("unused")
	protected void onInputSolved(SequenceInputSolver<TKey> sender, SequenceInput<TKey> input, List<TKey> input_solution) {
		List<Integer> indices = sender.listRoute(input_solution);
		System.out.println("Done: " + name + " " + Arrays.toString(sequence.stream().mapToInt(i -> i).toArray()) + " = " + indices.toString());
	}
	
	protected int getNext(SequenceInput<TKey> field, TKey dir) {
		int modulo = field.input_count;
		return (field.index + getIndexOffset(field, dir) + modulo) % modulo;
	}
	
	protected abstract int getIndexOffset(SequenceInput<TKey> field, TKey dir);
}
