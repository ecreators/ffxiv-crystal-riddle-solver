package de.easycreataors.ffxIIIsolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class CrystalClockRiddle {
	
	private final ExecutorService pool;
	private       List<Integer>   clock;
	
	public CrystalClockRiddle() {
		pool = Executors.newCachedThreadPool();
	}
	
	public void setClock(List<Integer> clock) {
		this.clock = new ArrayList<>(clock);
	}
	
	public void solve() {
		AtomicBoolean stop = new AtomicBoolean(false);
		int           size = clock.size();
		
		// define
		List<ClockField> solutions = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			ClockField field = new ClockField(clock.get(i), i, size);
			solutions.add(field);
		}
		
		// allign
		for (ClockField solution : solutions) {
			ClockField left  = solutions.get(getNext(solution, Move.BACKWARD));
			ClockField right = solutions.get(getNext(solution, Move.FORWARD));
			solution.next.put(Move.BACKWARD, left);
			solution.next.put(Move.FORWARD, right);
			left.next.put(Move.FORWARD, solution);
			right.next.put(Move.BACKWARD, solution);
		}
		
		for (ClockField solution : solutions) {
			if(!stop.get()) {
				Solver solver = new Solver(solution, solutions, (f, route) -> {
					stop.set(true);
				});
				if(!stop.get()) {
					solver.run(); // debug
//					pool.execute(solver);
				}
			}
		}
	}
	
	private int getNext(ClockField solution, Move dir) {
		switch (dir) {
			case FORWARD: {
				return (solution.index + solution.move) % clock.size();
			}
			case BACKWARD: {
				int i = solution.index - solution.move;
				while (i < 0) {
					i += clock.size();
				}
				return i;
			}
		}
		throw new RuntimeException("switch unsolved: " + dir);
	}
}
