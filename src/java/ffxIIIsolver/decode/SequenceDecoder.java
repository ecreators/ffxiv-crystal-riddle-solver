package java.ffxIIIsolver.decode;

import java.ffxIIIsolver.decode.model.IMoveChoiceModificator;
import java.ffxIIIsolver.decode.model.SolutionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings("WeakerAccess")
public class SequenceDecoder<TKey> {
	
	private final ExecutorService              pool;
	private final TKey[]                       staticOptions;
	private       List<Integer>                sequence;
	private final IMoveChoiceModificator<TKey> offsetHandler;
	private Resolution mode = Resolution.EARLY_RESULT;
	
	public SequenceDecoder() {
		this(null, null);
	}
	
	public SequenceDecoder(IMoveChoiceModificator<TKey> offsetHandler) {
		this(null, offsetHandler);
	}
	
	public SequenceDecoder(TKey[] staticOptionsOrNull, IMoveChoiceModificator<TKey> offsetHandler) {
		this.staticOptions = staticOptionsOrNull == null
		                     ? null
		                     : Arrays.copyOf(staticOptionsOrNull, staticOptionsOrNull.length);
		this.offsetHandler = offsetHandler;
		this.pool = Executors.newCachedThreadPool();
	}
	
	public void setSequence(Integer... sequenceMoves) {
		this.sequence = new ArrayList<>();
		this.sequence.addAll(asList(sequenceMoves));
		this.sequence = Collections.unmodifiableList(this.sequence);
	}
	
	public void solve() {
		AtomicReference<SequenceNodeSolver<TKey>> firstSolution = new AtomicReference<>();
		int                                       size          = sequence.size();
		
		// define
		List<SequenceInput<TKey>> seq_inputs = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			seq_inputs.add(new SequenceInput<>(sequence.get(i), i, size, this::getOptionsBySequenceInput));
		}
		
		// align
		for (SequenceInput<TKey> input : seq_inputs) {
			TKey[] choices = input.getChoices();
			if(choices == null) {
				throw new IllegalArgumentException("You need to define static or variable choices or empty!");
			}
			for (TKey choice : choices) {
				input.next.put(choice, seq_inputs.get(getNext(input, choice)));
			}
		}
		//noinspection unchecked
		SequenceNodeSolver<TKey>[] nodes = new SequenceNodeSolver[seq_inputs.size()];
		for (SequenceInput<TKey> input : seq_inputs) {
			if(firstSolution.get() == null) {
				// handle after solvation
				SolutionHandler<SequenceNodeSolver<TKey>> handler = new SolutionHandler<SequenceNodeSolver<TKey>>() {
					
					@Override
					public void invoke(SequenceNodeSolver<TKey> node) {
						// notify here ...
						
						boolean                        allDone   = Arrays.stream(nodes).allMatch(SequenceNodeSolver::isDone);
						List<SequenceNodeSolver<TKey>> solutions = collectSolutions(nodes);
						switch (mode) {
							// stop at very first solution "not fail"
							case EARLY_RESULT: {
								if(node.route != null) {
									firstSolution.set(node);
									onSolutions(solutions);
								} else if(allDone) {
									onSolutions(solutions);
								}
								break;
							}
							case ALL_RESULTS: {
								if(allDone) {
									firstSolution.set(solutions.stream().findFirst().orElse(null));
									onSolutions(solutions);
								}
								break;
							}
						}
					}
					
					@Override
					public boolean hasSolution() {
						return firstSolution.get() != null;
					}
				};
				
				SequenceNodeSolver<TKey> nodeSolver = new SequenceNodeSolver<>(handler, input, seq_inputs);
				nodes[input.index] = nodeSolver;
				pool.execute(nodeSolver);
			}
		}
	}
	
	@SafeVarargs
	protected static <T> List<SequenceNodeSolver<T>> collectSolutions(SequenceNodeSolver<T>... nodes) {
		return Arrays.stream(nodes).filter(n -> n.route != null).collect(Collectors.toList());
	}
	
	/**
	 * Invoken after solvations controlled by {@link #setMode(Resolution)}.
	 *
	 * @param solutions Solved Routes.
	 */
	@SuppressWarnings("unused")
	protected void onSolutions(List<SequenceNodeSolver<TKey>> solutions) {
	
	}
	
	@SuppressWarnings("unused")
	protected String takenFormat(TKey finalChoice, SequenceInput<TKey> t) {
		return t.toString();
	}
	
	@SuppressWarnings("unused")
	protected TKey[] getOptionsBySequenceInput(SequenceInput<TKey> field) {
		return staticOptions;
	}
	
	@SuppressWarnings("unused")
	protected final void logResult(SequenceNodeSolver<TKey> sender, List<TKey> route) {
		System.out.println("Done: " + Arrays.toString(sequence.stream().mapToInt(i -> i).toArray()) + " = " + sender.listRoute(route).toString());
	}
	
	protected int getNext(SequenceInput<TKey> field, TKey dir) {
		int modulo = field.input_count;
		return (field.index + handleMove(dir, field.move) + modulo) % modulo;
	}
	
	protected int handleMove(TKey dir, int move) {
		return offsetHandler == null
		       ? move
		       : offsetHandler.handleMoveByChoice(dir, move);
	}
	
	public void setMode(Resolution mode) {
		this.mode = mode;
	}
	
	public enum Resolution {
		ALL_RESULTS,
		EARLY_RESULT
	}
}
