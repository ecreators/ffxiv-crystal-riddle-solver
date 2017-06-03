package de.easycreataors.ffxIIIsolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class Solver implements Runnable {
	
	private final ClockField                   start;
	private final ISolverResultCallback        solverResultCallback;
	private final Map<Integer, ClockFieldInfo> takenInfo;
	
	public Solver(ClockField start, List<ClockField> solutions, ISolverResultCallback solverResultCallback) {
		this.start = start;
		this.solverResultCallback = solverResultCallback;
		takenInfo = new HashMap<Integer, ClockFieldInfo>();
		for (ClockField solution : solutions) {
			takenInfo.put(solution.index, new ClockFieldInfo(solution));
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
		
		ClockFieldInfo info = takenInfo.get(start.index);
		do {
			// wieder am Anfang
			if(info == null) {
				failAll();
				return;
			} else if(singleUntakenField(info)) {
				success();
				return;
			}
			
			info.setTaken(true);
			ClockFieldInfo forward  = takenInfo.get(info.field.moveTo(Move.FORWARD).index);
			ClockFieldInfo backward = takenInfo.get(info.field.moveTo(Move.BACKWARD).index);
			Move           choice   = null;
			if(!forward.taken && !backward.taken) {
				choice = Move.random();
			} else if(!forward.taken) {
				choice = Move.FORWARD;
			} else if(!backward.taken) {
				choice = Move.BACKWARD;
			}
			
			if(choice == null) {
				// FIX #1
				if(countTaken(true) == start.fullClockCount) {
					success();
					return;
				}
				ClockFieldInfo old = info;
				info = info.comefrom;
				// revidieren
				old.comefrom = null;
				old.setTaken(false);
			} else {
				switch (choice) {
					case FORWARD:
						forward.comefrom = info;
						info = forward;
						break;
					case BACKWARD:
						backward.comefrom = info;
						info = backward;
						break;
				}
			}
		} while (true);
	}
	
	private boolean singleUntakenField(ClockFieldInfo info) {
		List<ClockFieldInfo> untakenInfos = takenInfo.values().stream().filter(i -> !i.taken).collect(Collectors.toList());
		return untakenInfos.size() == 1 && untakenInfos.get(0).field.index == info.field.index;
	}
	
	private int countTaken(boolean takenState) {
		return (int) takenInfo.values().stream().filter(i -> i.taken == takenState).count();
	}
	
	private void success() {
	
	}
	
	private void failAll() {
	
	}
	
	private class ClockFieldInfo {
		
		private final ClockField     field;
		private       boolean        taken;
		public        ClockFieldInfo comefrom;
		
		public ClockFieldInfo(ClockField field) {
			this.field = field;
		}
		
		public void setTaken(boolean taken) {
			this.taken = taken;
		}
	}
}
