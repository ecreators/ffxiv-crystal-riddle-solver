package de.easycreators.sequencer.decode.v1;

import de.easycreators.sequencer.decode.model.SolutionHandler;
import de.easycreators.sequencer.encode.SequenceEncoder;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings("WeakerAccess")
public class SequenceNodeSolver<TKey> implements Runnable {
	
	private static final Random RANDOM = new Random(System.currentTimeMillis());
	private final SolutionHandler<SequenceNodeSolver<TKey>> solutionCallback;
	private final SequenceInput<TKey>                       input;
	private final Map<Integer, SequenceUsedInfo<TKey>>      takenInfo;
	private final List<Integer>                             fields;
	public        List<TKey>                                route;
	private       boolean                                   done;
	
	public SequenceNodeSolver(SolutionHandler<SequenceNodeSolver<TKey>> solutionOut, SequenceInput<TKey> input, List<SequenceInput<TKey>> fields) {
		this.solutionCallback = solutionOut;
		this.input = input;
		this.fields = fields.stream().map(f -> f.move).collect(toList());
		takenInfo = new HashMap<>();
		for (SequenceInput<TKey> field : fields) {
			takenInfo.put(field.index, new SequenceUsedInfo<>(field));
		}
	}
	
	public final boolean isDone() {
		return done;
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
				done = true;
				onFail();
				return;
			}
			taken.add(info.field);
			
			// bestimme die nächste Entscheidung
			Map<TKey, SequenceUsedInfo<TKey>> choices    = new HashMap<>();
			SequenceUsedInfo<TKey>            current    = info;
			TKey[]                            av_choices = info.field.getChoices();
			if(av_choices == null) {
				throw new IllegalArgumentException("At least you need to define a non null amount of choices. Even empty is ok!");
			}
			Arrays.stream(av_choices).forEach(option -> choices.put(option, takenInfo.get(current.field.next.get(option).index)));
			
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
					List<TKey> route = new ArrayList<>();
					for (SequenceInput<TKey> field : taken) {
						SequenceUsedInfo<TKey> inf = takenInfo.get(field.index);
						route.add(inf.choice);
					}
					done = true;
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
			}
		} while (!solutionCallback.hasSolution());
		done = true;
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
	
	protected void success(List<TKey> route) {
		if(avoidNotUnique(route)) {
			return;
		}
		onCleanedSuccess(route);
	}
	
	protected void onCleanedSuccess(List<TKey> route) {
		this.route = route;
		notifySolution();
	}
	
	public final void notifySolution() {
		if(solutionCallback != null) {
			if(done && (route == null || isValidRoute(route))) {
				solutionCallback.invoke(this);
			}
		}
	}
	
	private boolean avoidNotUnique(List<TKey> route) {
		if(!isValidRoute(route)) {
			onFail();
			return true;
		}
		return false;
	}
	
	private boolean isValidRoute(List<TKey> route) {
		List<Integer>   indices = listRoute(route);
		SequenceEncoder encoder = new SequenceEncoder();
		encoder.setFields(indices);
		return encoder.encodeMoves().equals(fields);
	}
	
	public List<Integer> listRoute(List<TKey> route) {
		this.route = route;
		SequenceInput<TKey> info    = input;
		List<Integer>       indeces = new ArrayList<>();
		for (TKey choice : route) {
			indeces.add(info.index);
			info = info.moveTo(choice);
		}
		return indeces;
	}
	
	private void onFail() {
		route = null;
		notifySolution();
	}
	
	@SuppressWarnings("unused")
	public SequenceInput<TKey> getInput() {
		return input;
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
			return Arrays.stream(field.getChoices()).filter(this::canMoveTo).collect(toList());
		}
	}
}