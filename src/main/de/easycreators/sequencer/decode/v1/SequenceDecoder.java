package de.easycreators.sequencer.decode.v1;

import de.easycreators.core.event.IListenerEvent;
import de.easycreators.core.event.ListenerEvent;
import de.easycreators.sequencer.decode.model.Handler;
import de.easycreators.sequencer.decode.model.IMoveChoiceModificator;
import de.easycreators.sequencer.decode.model.Resolution;
import de.easycreators.sequencer.decode.model.SolutionHandler;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings({"WeakerAccess"})
public class SequenceDecoder<TKey> implements ISequenceDecoder {
	
	private final ExecutorService              pool;
	private final TKey[]                       staticOptions;
	private       List<Integer>                sequence;
	private final IMoveChoiceModificator<TKey> offsetHandler;
	private Resolution mode = Resolution.EARLY_RESULT;
	private final ListenerEvent<Handler<List<SequenceNodeSolver<TKey>>>> solutionsEvent;
	private final Map<Integer, List<SequenceNodeSolver<TKey>>>           results;
	
	public SequenceDecoder(TKey[] staticOptionsOrNull, IMoveChoiceModificator<TKey> offsetHandler) {
		this.staticOptions = staticOptionsOrNull == null
		                     ? null
		                     : Arrays.copyOf(staticOptionsOrNull, staticOptionsOrNull.length);
		this.offsetHandler = offsetHandler;
		this.pool = Executors.newCachedThreadPool();
		this.solutionsEvent = new ListenerEvent<>();
		this.results = new HashMap<>();
	}
	
	@Override
	public void setSequence(Integer... sequenceMoves) {
		this.sequence = new ArrayList<>();
		this.sequence.addAll(asList(sequenceMoves));
		this.sequence = Collections.unmodifiableList(this.sequence);
	}
	
	@Override
	public Awaiter decode() {
		int                            id                = Arrays.hashCode(sequence.toArray());
		List<SequenceNodeSolver<TKey>> resultsCollection = results.computeIfAbsent(id, k -> new ArrayList<>());
		resultsCollection.clear();
		Awaiter awaiter = new Awaiter(id, this);
		
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
						if(!solutions.isEmpty()) {
							switch (mode) {
								// stop at very first solution "not fail"
								case EARLY_RESULT: {
									if(node.route != null) {
										resultsCollection.add(node);
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
										resultsCollection.clear();
										resultsCollection.addAll(solutions);
										onSolutions(solutions);
									}
									break;
								}
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
		return awaiter;
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
		notifySolutions(solutions);
	}
	
	protected final void notifySolutions(List<SequenceNodeSolver<TKey>> solutions) {
		solutionsEvent.invokeAll(h -> h.invoke(solutions));
	}
	
	protected TKey[] getOptionsBySequenceInput(SequenceInput<TKey> field) {
		return staticOptions;
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
	
	@Override
	public void setMode(Resolution mode) {
		this.mode = mode == null
		            ? Resolution.EARLY_RESULT
		            : mode;
	}
	
	public IListenerEvent<Handler<List<SequenceNodeSolver<TKey>>>> getSolutionsEvent() {
		return solutionsEvent;
	}
	
	public final List<SequenceNodeSolver<TKey>> awaitSolutions() {
		AtomicReference<List<SequenceNodeSolver<TKey>>>          results = new AtomicReference<>();
		AtomicReference<Handler<List<SequenceNodeSolver<TKey>>>> onEnd   = new AtomicReference<>();
		onEnd.set(r -> {
			results.set(r);
			getSolutionsEvent().removeListener(onEnd.get());
		});
		getSolutionsEvent().addListener(onEnd.get());
		while (results.get() == null) {
			Thread.yield();
		}
		return results.get();
	}
	
	public List<SequenceNodeSolver<TKey>> getResults(int id) {
		return results.get(id);
	}
	
}