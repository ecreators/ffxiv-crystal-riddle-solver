package de.easycreataors.ffxIIIsolver;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings("WeakerAccess")
public abstract class SequenceInputSolver<TKey> implements Runnable {
	
	private static final Random RANDOM = new Random(System.currentTimeMillis());
	private final AtomicReference<SequenceInputSolver<TKey>> solution;
	private final String                                     name;
	private final SequenceInput<TKey>                        input;
	private final ISolverResultCallback<TKey>                solverResultCallback;
	private final Map<Integer, SequenceUsedInfo<TKey>>       takenInfo;
	
	protected SequenceInputSolver(AtomicReference<SequenceInputSolver<TKey>> solutionOut, String name, SequenceInput<TKey> input, List<SequenceInput<TKey>> fields, ISolverResultCallback<TKey> solverResultCallback) {
		this.solution = solutionOut;
		this.name = name;
		this.input = input;
		this.solverResultCallback = solverResultCallback;
		takenInfo = new HashMap<>();
		for (SequenceInput<TKey> field : fields) {
			takenInfo.put(field.index, new SequenceUsedInfo<>(field));
		}
	}
	
	@Override
	public void run() {
		
		// Die Regeln:
		/*
		1. Eine Reihe von Schritten wird aufgezeigt
		> 2. Mit der ersten Enscheidung auf welchem Feld mit Schrittangabe begonnen wird, wird das Feld deaktiviert
		i----- Mit der Deaktivierung steht das Feld für Schritte nicht länger zur Verfüguung für eine Entscheidung
		3. Die Folgeentscheidung kann nun ausgehend vom aktuellen Feld in verschiedene Richtungen gehen, jedoch muss die Schrittanzahl des Feldes durchgeführt werden
		4. Bei der Durchführung der Schrittanzahl können deaktivierte Feld nicht für eine Entscheidung berücksichtigt werden (siehe i)
		ii---- Im Falle, dass ein Feld bereits deaktiviert ist, kann nur nur die andere Richtung eingeschlagen werden
		iii--- Sind alle Entscheidungsfelder deaktiviert, ist es ein Fehlschlag
		6. Ist mit der Schrittausführung das einzig verbliebene Feld erreicht, ist das Rätsel gelöst
		 */
		
		SequenceUsedInfo<TKey>    info  = takenInfo.get(input.index);
		List<SequenceInput<TKey>> taken = new ArrayList<>();
		TKey                      choice;
		do {
			// wieder am Anfang
			if(info == null) {
//				System.out.println(String.takenFormat("FAILED at index %d=%d", input.index, input.move));
				failAll();
				return;
			}
			taken.add(info.field);
			
			// bestimme die nächste Entscheidung
			Map<TKey, SequenceUsedInfo<TKey>> choices = new HashMap<>();
			SequenceUsedInfo<TKey>            current = info;
			Arrays.stream(info.field.choices()).forEach(option -> choices.put(option, takenInfo.get(current.field.next.get(option).index)));
			
			// Entscheidung treffen oder NULL
			// Durch Zufall schneller sein
			choice = randomChoice(taken, info);
			
			if(choice != null) {
				info.decideFor(choice);
			}
			
			if(choice == null) {
				// FIX #1
				if(taken.size() == input.input_count) {
					//noinspection ConstantConditions
					info.choice = getBeforeOrNull(taken).choice;
					printUsed(taken, "!SOLVED");
					List<TKey> route = new ArrayList<>();
					for (SequenceInput<TKey> field : taken) {
						SequenceUsedInfo<TKey> inf = takenInfo.get(field.index);
						route.add(inf.choice);
					}
					success(route);
					return;
				}
				
				// erneut eine Entscheidung treffen
				choice = randomChoice(taken, info);
				if(choice == null) {
					SequenceUsedInfo<TKey> before = getBeforeOrNull(taken);
					// Entscheidungen dieses Feldes entfernen
					info.choices.clear();
					info = before;
					// diese Entcheidung und anschließend die Entscheidung davor entfernen
					taken.remove(taken.size() - 1);
				}
				// else: Eine andere Entscheidung ist möglich -> weitermachen
				// erneut versuchen
				if(!taken.isEmpty()) {
					taken.remove(taken.size() - 1);
				}
			} else {
				SequenceUsedInfo<TKey> next = choices.get(choice);
				info.choice = choice;
				info = next;
				printUsed(taken, "SOLVING");
			}
		} while (solution.get() == null);
	}
	
	private SequenceUsedInfo<TKey> getBeforeOrNull(List<SequenceInput<TKey>> taken) {
		if(taken.size() > 1) {
			return takenInfo.get(taken.get(taken.size() - 2).index);
		}
		return null;
	}
	
	private TKey randomChoice(Collection<SequenceInput<TKey>> taken, SequenceUsedInfo<TKey> current) {
		List<TKey> untakenPossible = new ArrayList<>();
		List<TKey> untakenChoices  = current.getUntakenChoices();
		for (TKey untakenChoice : untakenChoices) {
			SequenceInput field = current.field.moveTo(untakenChoice);
			if(!taken.contains(field)) {
				untakenPossible.add(untakenChoice);
			}
		}
		boolean random = untakenPossible.size() > 1;
		if(random) {
			int i = (int) Math.round(RANDOM.nextDouble() * (untakenChoices.size() - 1));
			return untakenPossible.get(i);
		} else if(!untakenPossible.isEmpty()) {
			return untakenPossible.get(0);
		}
		return null;
	}
	
	protected void printUsed(Collection<SequenceInput<TKey>> taken, String progress) {
		System.out.println(progress + ": " + taken.stream().map(t -> format(takenInfo.get(t.index).choice, t)).collect(toList()));
	}
	
	protected abstract String format(TKey finalChoice, SequenceInput<TKey> t);
	
	protected void success(List<TKey> route) {
		solution.set(this);
		listRoute(route);
		if(solverResultCallback != null) {
			solverResultCallback.onResult(this, input, route);
		}
	}
	
	protected List<Integer> listRoute(List<TKey> route) {
		SequenceInput<TKey> info    = input;
		List<Integer>       indeces = new ArrayList<>();
		for (TKey choice : route) {
			indeces.add(info.index);
			info = info.moveTo(choice);
		}
		return indeces;
	}
	
	private void failAll() {
	
	}
	
	@SuppressWarnings("unused")
	public SequenceInput<TKey> getInput() {
		return input;
	}
	
	public String getName() {
		return name;
	}
	
	private static class SequenceUsedInfo<TKey> {
		
		private final SequenceInput<TKey> field;
		public        TKey                choice;
		public final  Set<TKey>           choices;
		
		public SequenceUsedInfo(SequenceInput<TKey> field) {
			this.field = field;
			this.choices = new LinkedHashSet<>();
		}
		
		public void decideFor(TKey choice) {
			choices.add(choice);
		}
		
		public boolean canMoveTo(TKey choice) {
			return !choices.contains(choice);
		}
		
		public List<TKey> getUntakenChoices() {
			return Arrays.stream(field.choices()).filter(this::canMoveTo).collect(toList());
		}
	}
}