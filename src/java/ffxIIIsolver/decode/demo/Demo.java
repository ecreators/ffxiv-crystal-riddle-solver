package java.ffxIIIsolver.decode.demo;

import java.ffxIIIsolver.decode.SequenceDecoder;
import java.ffxIIIsolver.decode.demo.model.Choice;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class Demo {
	
	public static void main(String[] args) {
		Demo demo = new Demo();
		demo.start();
	}
	
	private void start() {
		SequenceDecoder<Choice> sequenceDecoder = new SequenceDecoder<>(Choice.values(), (dir, move) -> move * dir.f);
		sequenceDecoder.setMode(SequenceDecoder.Resolution.EARLY_RESULT);
		sequenceDecoder.setSequence(2, 2, 2, 2, 2);
		sequenceDecoder.solve();
	}
	
	/*
	output example
	          0  1  2  3  4
	Done: #1 [2, 2, 2, 2, 2] = [4, 2, 0, 3, 1]
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
