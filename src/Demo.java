import de.easycreataors.ffxIIIsolver.SequenceInput;
import de.easycreataors.ffxIIIsolver.SequenceSolver;

import static java.util.Arrays.asList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class Demo {
	
	public static void main(String[] args) {
		Demo demo = new Demo();
		demo.start();
	}
	
	private void start() {
		SequenceSolver<Boolean> sequenceSolver = new SequenceSolver<Boolean>("#1") {
			@Override
			protected Boolean[] getOptionsBySequenceInput(SequenceInput<Boolean> field) {
				return new Boolean[]{true, false};
			}
			
			@Override
			protected int getIndexOffset(SequenceInput<Boolean> field, Boolean dir) {
				return field.move * (dir
				                     ? 1
				                     : -1);
			}
		};
		sequenceSolver.setSequence(asList(2, 2, 2, 2, 2));
		sequenceSolver.solve();
	}
	
	/*
	output example
	          0  1  2  3  4
	Done: #1 [2, 2, 2, 2, 2] = [1, 4, 2, 0, 3]
	Done: #1 [2, 2, 2, 2, 2] = [4, 2, 0, 3, 1]
	Done: #1 [2, 2, 2, 2, 2] = [0, 2, 4, 1, 3]
	Done: #1 [2, 2, 2, 2, 2] = [2, 4, 1, 3, 0]
	Done: #1 [2, 2, 2, 2, 2] = [3, 1, 4, 2, 0]
	
	#1 = name
	{0} = [input sequence]
	{1} = [indices in "input sequence"]
	{1}[0] = 2
	
	meaning:
	                   input   =  solved = translated solution
	= in a sequence of [1,2,3] = [1,0,2] = [2,1,3]
	 */
}
