package de.easycreataors.ffxIIIsolver;

import static java.util.Arrays.asList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class Main {
	
	public static void main(String[] args) {
		solve("#2", 2, 3, 1, 2, 1, 4, 1, 3);
		solve("#1", 2, 2, 2, 2, 2);
	}
	
	private static void solve(String name, Integer... moves) {
		SequenceSolver<Choice> solver = new SequenceSolver<Choice>(name) {
			@Override
			protected String takenFormat(Choice finalChoice, SequenceInput<Choice> t) {
//				int    dest_i;
//				String name;
//				if(finalChoice == null) {
//					return "!";
//				} else {
//					dest_i = t.moveTo(finalChoice).index;
//					name = String.valueOf(finalChoice.name().charAt(0));
//				}
//				return String.format("%d=%d=%s>%d", t.index, t.move, name, dest_i);
				return null;
			}
			
			@Override
			protected Choice[] getOptionsBySequenceInput(SequenceInput<Choice> field) {
				return Choice.values();
			}
			
			@Override
			protected int getIndexOffset(SequenceInput<Choice> field, Choice dir) {
				return dir.f * field.move;
			}
		};
		solver.setSequence(asList(moves));
		solver.solve();
	}
}
