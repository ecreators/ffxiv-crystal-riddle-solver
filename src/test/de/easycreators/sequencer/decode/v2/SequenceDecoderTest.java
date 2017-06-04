package de.easycreators.sequencer.decode.v2;

import de.easycreators.sequencer.decode.model.Resolution;
import de.easycreators.sequencer.decode.v2.SequenceDecoder.Input;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class SequenceDecoderTest {
	
	@Test
	public void test() {
		SequenceDecoder decoder = new SequenceDecoder();
		Input[]         fields  = defineFields(2, 2, 2, 2, 2);
		decoder.setSequence(fields);
		
		//noinspection MismatchedQueryAndUpdateOfCollection
		List<int[]> results = new ArrayList<>();
		
		// handle results
		decoder.getDecodingCompletedEvent().addListener(sq -> {
			// Auswertung
			for (SequenceDecoder.Pin pin : sq.getDonePins()) {
				List<Input> raw_route = pin.getRoute();
				if(raw_route != null && !raw_route.isEmpty()) {
					// cast
					MoveInput[] route = raw_route.stream().map(i -> (MoveInput) i).collect(toList()).toArray(new MoveInput[0]);
					
					// recognize result
					results.add(Arrays.stream(route).mapToInt(MoveInput::getIndex).toArray());
					
					// debug
					System.out.println(Arrays.toString(route));
				} else {
					// debug
					System.out.println("No result!");
				}
			}
		});
		decoder.decode(Resolution.EARLY_RESULT);
	}
	
	private static Input[] defineFields(int... moves) {
		
		AtomicInteger id     = new AtomicInteger(1);
		MoveInput[]   inputs = Arrays.stream(moves).mapToObj(move -> new MoveInput(id.getAndIncrement(), move)).toArray(value -> new MoveInput[moves.length]);
		
		for (int i = 0; i < inputs.length; i++) {
			moveOption(inputs, i);
		}
		
		return inputs;
	}
	
	private static void moveOption(MoveInput[] inputs, int i) {
		MoveInput input  = inputs[i];
		int       length = inputs.length;
		
		int       a    = offsetIndex(i, input.move, length);
		MoveInput left = inputs[a];
		
		int       b     = offsetIndex(i, -input.move, length);
		MoveInput right = inputs[b];
		input.addOption(left);
		input.addOption(right);
	}
	
	private static int offsetIndex(int index, int offset, int modulo) {
		return (index + offset + modulo) % modulo;
	}
	
	private static class MoveInput extends Input {
		
		private final int move;
		
		public MoveInput(int id, int move) {
			super(id);
			this.move = move;
		}
		
		public int getIndex() {
			return (int) getId();
		}
		
		public int getMove() {
			return move;
		}
		
		@Override
		public String toString() {
			return String.format("%d", getIndex());
		}
		
		@Override
		public int hashCode() {
			return (int) getId();
		}
	}
}