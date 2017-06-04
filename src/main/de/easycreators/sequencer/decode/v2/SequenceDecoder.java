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
public class SequenceDecoder {
	
	private       Sequence                         sequence;
	private final ListenerEvent<Handler<Sequence>> decodingCompletedEvent;
	
	public SequenceDecoder() {
		decodingCompletedEvent = new ListenerEvent<>();
	}
	
	public final IListenerEvent<Handler<Sequence>> getDecodingCompletedEvent() {
		return decodingCompletedEvent;
	}
	
	public void setSequence(Input... pins) {
		Pin[] sequence = new Pin[pins.length];
		for (int i = 0; i < pins.length; i++) {
			Input pin = pins[i];
			sequence[i] = new Pin(i, pin);
		}
		this.sequence = new Sequence(System.currentTimeMillis(), sequence);
	}
	
	public void decode(Resolution resolution) {
		if(sequence == null || sequence.done) {
			return;
		}
		
		for (Pin pin : sequence.getPins()) {
			if(resolution != Resolution.EARLY_RESULT || !hasRouteFound()) {
				solveFromPin(sequence, pin, resolution);
			}
			// early stop
			else {
				break;
			}
		}
	}
	
	private void solveFromPin(Sequence sender, Pin start, Resolution resolution) {
		if(shouldStop(resolution)) {
			return;
		}
		
		start.done = false;
		start.route = null;
		List<Input> current_route = new ArrayList<>();
		Pin         current       = start;
		do {
			current_route.add(current.getInput());
			// sequence complete
			if(current_route.size() == sender.getPins().length) {
				if(shouldStop(resolution)) {
					return;
				}
				start.route = current_route;
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
				revertPin(current, current_route);
				current = current.getPrevious();
				// continue "backwards"
			}
			
			if(shouldStop(resolution)) {
				return;
			}
		} while (current != null);
		// win or lose ... and done ;-)
		start.done = true;
		onPinValidated(start, resolution);
	}
	
	private boolean shouldStop(Resolution resolution) {
		return resolution == Resolution.EARLY_RESULT && hasRouteFound();
	}
	
	public boolean hasRouteFound() {
		return sequence.done && sequence.getDonePins().stream().anyMatch(Pin::isSuccess);
	}
	
	private void onPinValidated(Pin pin, Resolution resolution) {
		sequence.donePins.add(pin);
		switch (resolution) {
			case ALL_RESULTS:
				if(isCompleted()) {
					sequence.done = true;
					decodingCompletedEvent.invokeAll(h -> h.invoke(sequence));
				}
				break;
			case EARLY_RESULT:
				if(isCompleted() || pin.isSuccess()) {
					sequence.done = true;
					decodingCompletedEvent.invokeAll(h -> h.invoke(sequence));
				}
				break;
		}
	}
	
	private boolean isCompleted() {
		return sequence.donePins.size() == sequence.pins.length;
	}
	
	private static void revertPin(Pin current, List<Input> current_route) {
		current.getTakenInputs().clear();
		current_route.remove(current.getInput());
	}
	
	private static Input determineOption(List<Input> current_route, Pin current) {
		Input option = null;
		foundOption:
		for (Input input : current.getInput().getOptions()) {
			// ungone und untaken
			if(!current_route.contains(input) && !current.getTakenInputs().contains(input)) {
				// can go
				option = input;
				break foundOption;
			}
		}
		return option;
	}
	
	public static class Pin {
		
		private final int         index;
		private final Input       input;
		private final List<Input> takenInputs;
		private       Pin         previous;
		boolean     done;
		List<Input> route;
		
		public Pin(int index, Input input) {
			this.index = index;
			this.input = input;
			this.takenInputs = new ArrayList<>();
		}
		
		public List<Input> getRoute() {
			return route;
		}
		
		public Pin getPrevious() {
			return previous;
		}
		
		public int getIndex() {
			return index;
		}
		
		public Input getInput() {
			return input;
		}
		
		public List<Input> getTakenInputs() {
			return takenInputs;
		}
		
		private boolean isSuccess() {
			List<Input> route = this.route;
			return route != null && !route.isEmpty();
		}
	}
	
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
	
	protected static class Sequence {
		
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
