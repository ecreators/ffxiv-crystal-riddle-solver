package de.easycreators.sequencer.decode.demo;

import de.easycreators.sequencer.decode.demo.model.Choice;
import de.easycreators.sequencer.decode.model.Resolution;
import de.easycreators.sequencer.decode.v1.SequenceDecoder;

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
		sequenceDecoder.setMode(Resolution.EARLY_RESULT);
		sequenceDecoder.setSequence(2, 2, 2, 2, 2);
		sequenceDecoder.decode();
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
