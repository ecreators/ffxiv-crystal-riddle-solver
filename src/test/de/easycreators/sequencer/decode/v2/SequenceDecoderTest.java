package de.easycreators.sequencer.decode.v2;

import de.easycreators.sequencer.decode.model.Handler;
import de.easycreators.sequencer.decode.model.Resolution;
import de.easycreators.sequencer.decode.v2.SequenceDecoder.Input;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings("ALL")
public class SequenceDecoderTest {
	
	@Test
	public void test() {
		SequenceDecoder          decoder  = new SequenceDecoder();
		Input[]                  fields   = defineFields(2, 2, 2, 2, 2);
		SequenceDecoder.Sequence sequence = decoder.setSequence(fields);
		System.out.println(Arrays.toString(Arrays.stream(sequence.getPins()).mapToInt(p -> ((MoveInput) p.input).getMove()).toArray()));
		System.out.println("");

//		// A) do this
//		//noinspection MismatchedQueryAndUpdateOfCollection
//		List<List<Integer>> results = new ArrayList<>();
//
//		// handle results
//		decoder.getDecodingCompletedEvent().addListener(new SequenceHandler(results));
//		decoder.decode(Resolution.EARLY_RESULT).begin();
		
		// B) or this
		List<List<Integer>> routes = decoder.decode(Resolution.EARLY_RESULT).execute()
		                                    .stream()
		                                    .map(in -> in.stream()
		                                                 .map(i -> new Integer((int) i.getId() - 1))
		                                                 .collect(Collectors.toList()))
		                                    .collect(Collectors.toList());
	}
	
	@Test
	public void test_all() {
		SequenceDecoder          decoder  = new SequenceDecoder();
		Input[]                  fields   = defineFields(2, 2, 2, 2, 2);
		SequenceDecoder.Sequence sequence = decoder.setSequence(fields);
		
		System.out.println(Arrays.toString(Arrays.stream(sequence.getPins()).mapToInt(p -> ((MoveInput) p.input).getMove()).toArray()));
		System.out.println("");
		
		//noinspection MismatchedQueryAndUpdateOfCollection
		List<List<Integer>> results = new ArrayList<>();
		
		// handle results
		decoder.getDecodingCompletedEvent().addListener(new SequenceHandler(results));
		decoder.decode(Resolution.ALL_RESULTS);
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
			return (int) getId() - 1;
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
	
	private static class SequenceHandler implements Handler<SequenceDecoder.Sequence> {
		private final List<List<Integer>> results;
		
		public SequenceHandler(List<List<Integer>> results) {
			this.results = results;
		}
		
		@Override
		public void invoke(SequenceDecoder.Sequence sq) {
			// Auswertung
			for (SequenceDecoder.Pin pin : sq.getDonePins()) {
				List<Input> raw_route = pin.getRouteOrNull();
				if(raw_route != null && !raw_route.isEmpty()) {
					// cast
					MoveInput[] route = raw_route.stream().map(i -> (MoveInput) i).collect(toList()).toArray(new MoveInput[0]);
					
					// recognize result
					List<Integer> way = Arrays.stream(route).map(moveInput -> moveInput.getIndex()).collect(toList());
					if(!results.contains(way)) {
						results.add(way);
						// debug
						System.out.println(Arrays.toString(way.toArray()));
					}
				} else {
					// debug
					System.out.println("No result!");
				}
			}
		}
	}
}