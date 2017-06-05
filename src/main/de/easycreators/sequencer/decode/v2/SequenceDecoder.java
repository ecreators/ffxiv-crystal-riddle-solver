package de.easycreators.sequencer.decode.v2;

import de.easycreators.core.event.IListenerEvent;
import de.easycreators.core.event.ListenerEvent;
import de.easycreators.sequencer.decode.model.Handler;
import de.easycreators.sequencer.decode.model.Resolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
@SuppressWarnings("WeakerAccess")
public class SequenceDecoder {
	
	private       Sequence                         sequence;
	private final ListenerEvent<Handler<Sequence>> decodingCompletedEvent;
	
	public SequenceDecoder() {
		decodingCompletedEvent = new ListenerEvent<>();
	}
	
	public final IListenerEvent<Handler<Sequence>> getDecodingCompletedEvent() {
		return decodingCompletedEvent;
	}
	
	
	/**
	 * Set a chosen given sequence of options with different ids and given options to continue the journey.
	 * So, ensure your pins have options to choose a next option pin.
	 */
	public void setSequence(Input... pins) {
		Pin[] sequence = new Pin[pins.length];
		for (int i = 0; i < pins.length; i++) {
			Input pin = pins[i];
			sequence[i] = new Pin(pin);
		}
		this.sequence = new Sequence(System.currentTimeMillis(), sequence);
	}
	
	/**
	 * Begins to decode the sequence you set. Depending on your favourite resolution, the decoding process ends up quicker
	 * or not. This process iterates each input you defined and tests each option to solve a sequence of inputs to
	 * solve a sequence that touches each input only once. This won't start, if your sequence is unset or already
	 * done.
	 *
	 * @param resolution Kind of continue solutions. For example an input of 5x2 with resolution ALL will notify 5 rotes, because you can
	 *                   start up with each of input of 2 to solve a sequence.
	 */
	@SuppressWarnings("UnusedReturnValue")
	public boolean decode(Resolution resolution) {
		if(sequence == null) {
			return false;
		} else if(sequence.done) {
			decodingCompletedEvent.invokeAll(h -> h.invoke(sequence));
			return true;
		}
		
		// test each pin
		for (Pin pin : sequence.getPins()) {
			// stop asap with resolution EARLY, if a route was found
			if(resolution != Resolution.EARLY_RESULT || !hasRouteFound()) {
				solveFromPin(sequence, pin, resolution);
			}
			// early stop
			else {
				break;
			}
		}
		return true;
	}
	
	/**
	 * @param sender     Solution to hold completion state and all pins
	 * @param start      Pin to hold completion state of tested start and previous routed pin and taken option
	 *                   each step in way finding
	 * @param resolution Kind of stop type to reduce redundant iterations
	 */
	protected void solveFromPin(Sequence sender, Pin start, Resolution resolution) {
		// if started asnchron or parallel: stop this test, if another test found a route
		if(shouldStop(resolution)) {
			return;
		}
		
		// reset this pin
		start.done = false;
		start.route = null;
		start.previous = null;
		start.takenInputs.clear();
		
		// solved matching route in a varable state until pin.done=true
		List<Input> current_route = new ArrayList<>();
		Pin         current       = start;
		do {
			// mark this pin as taken and anymore available for further options
			current_route.add(current.getInput());
			
			// test: sequence complete
			if(current_route.size() == sender.getPins().length) {
				// no further route when already a route was set
				if(shouldStop(resolution)) {
					return;
				}
				// solved solution from this
				start.route = Collections.unmodifiableList(current_route);
				// start.done = true; <- required after "break do"
				// win and exit
				break;
			}
			
			// where to go from this pin
			Input option = determineOption(current_route, current);
			// has option
			if(option != null) {
				current.getTakenInputs().add(option);
				// go there
				Pin old = current;
				current = sender.asPinOrDie(option);
				current.previous = old;
				// continue "further"
			}
			// no option anymore
			else {
				// redecide in a previous pin
				revertPin(current, current_route);
				current = current.getPrevious();
				// continue "backwards"
			}
			
			// test: another pin was already solved
			if(shouldStop(resolution)) {
				return;
			}
		} while (current != null);
		// win or lose ... and done ;-)
		start.done = true;
		// notify solution
		onPinValidated(start, resolution);
	}
	
	/**
	 * Returns true, if EARLY and any match succeeded in a pin.
	 */
	boolean shouldStop(Resolution resolution) {
		return resolution == Resolution.EARLY_RESULT && hasRouteFound();
	}
	
	/**
	 * Returns true, if any pin of done test has a valid route.
	 */
	public boolean hasRouteFound() {
		return sequence.done && sequence.getDonePins().stream().anyMatch(Pin::isSuccess);
	}
	
	/**
	 * Notifies a working start pin, at end using ALL or asap using EARLY if a working pin is given
	 *
	 * @param pin        a done pin test
	 * @param resolution given resolution of fullfillment
	 */
	protected void onPinValidated(Pin pin, Resolution resolution) {
		if(!sequence.donePins.contains(pin)) {
			sequence.donePins.add(pin);
		}
		switch (resolution) {
			case ALL_RESULTS:
				if(isCompletedSequence()) {
					notifySequenceDone();
				}
				break;
			case EARLY_RESULT:
				if(isCompletedSequence() || pin.isSuccess()) {
					notifySequenceDone();
				}
				break;
		}
	}
	
	/**
	 * Sets sequence done and promote the sequence to the decodingCompletedEvent
	 */
	protected void notifySequenceDone() {
		sequence.done = true;
		decodingCompletedEvent.invokeAll(h -> h.invoke(sequence));
	}
	
	/**
	 * If all pins are tested, it will return true.
	 */
	private boolean isCompletedSequence() {
		return sequence.donePins.size() == sequence.pins.length;
	}
	
	/**
	 * Unmark a pin. Means, this pin options are all available and it is removed from current route.
	 *
	 * @param current       Pin which options should be available
	 * @param current_route Route whe the pin should be removed from. Note to call this method when the pin is at end of the route
	 *                      or your route will be inconsequent and unform.
	 */
	static void revertPin(Pin current, List<Input> current_route) {
		current.getTakenInputs().clear();
		current_route.remove(current.getInput());
	}
	
	/**
	 * Determine a valid, means untaken option from this pin, if the option isn't already in your route,
	 * because each option is only allowed to be hit once a time in your route.
	 *
	 * @param current The pin to take a decission of.
	 * @return null means, no choice is available or already take in your current route.
	 */
	static Input determineOption(List<Input> current_route, Pin current) {
		Input option = null;
		for (Input input : current.getInput().getOptions()) {
			// ungone und untaken
			if(!current_route.contains(input) && !current.getTakenInputs().contains(input)) {
				// can go
				option = input;
				break;
			}
		}
		return option;
	}
	
	/**
	 * Defines a holder for an input varable. It determines taken options and the pin that continued to this pin and finally the decoded route, if it is the starting pin.
	 */
	public static class Pin {
		
		private final Input       input;
		private final List<Input> takenInputs;
		private       Pin         previous;
		boolean     done;
		List<Input> route;
		
		public Pin(Input input) {
			this.input = input;
			this.takenInputs = new ArrayList<>();
		}
		
		public final List<Input> getRouteOrNull() {
			return route;
		}
		
		final Pin getPrevious() {
			return previous;
		}
		
		public final Input getInput() {
			return input;
		}
		
		List<Input> getTakenInputs() {
			return takenInputs;
		}
		
		public final boolean isSuccess() {
			List<Input> route = this.route;
			return route != null && !route.isEmpty();
		}
	}
	
	
	/**
	 * Defines your possible input you want to test. All options need to be set before decode. If no
	 * option can be determine, then it inflicts to use the caller input to determine a different
	 * way to reach the decoding, if possible.
	 */
	@SuppressWarnings("unused")
	public static class Input {
		
		private final List<Input> options;
		private       long        id;
		
		public Input(long id) {
			this.id = id;
			this.options = new ArrayList<>();
		}
		
		public long getId() {
			return id;
		}
		
		public void addOption(Input input) {
			if(!options.contains(input)) {
				options.add(input);
			}
		}
		
		public void removeSelection(Input input) {
			options.remove(input);
		}
		
		public List<Input> getOptions() {
			return Collections.unmodifiableList(new ArrayList<>(Collections.synchronizedList(options)));
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof Input && ((Input) obj).id == id;
		}
		
		@Override
		public int hashCode() {
			return Long.hashCode(id);
		}
	}
	
	/**
	 * Defines a sequence, holds an info about the state of the solvation of an encoded sequence to decode.
	 */
	static final class Sequence {
		
		final long  id;
		final Pin[] pins;
		boolean   done;
		List<Pin> donePins;
		
		Sequence(long id, Pin[] pins) {
			this.id = id;
			this.pins = pins;
			this.done = false;
			this.donePins = new ArrayList<>();
		}
		
		public List<Pin> getDonePins() {
			return donePins;
		}
		
		@SuppressWarnings("WeakerAccess")
		public Pin[] getPins() {
			return pins;
		}
		
		Pin asPinOrDie(Input option) {
			return Arrays.stream(pins).filter(p -> p.getInput().equals(option)).findFirst().orElseThrow(() -> new RuntimeException("pin not found for " + option.getId()));
		}
	}
}
