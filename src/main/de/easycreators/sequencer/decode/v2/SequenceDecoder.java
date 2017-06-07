package de.easycreators.sequencer.decode.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.easycreators.core.event.IListenerEvent;
import de.easycreators.core.event.ListenerEvent;
import de.easycreators.sequencer.decode.model.Handler;
import de.easycreators.sequencer.decode.model.Resolution;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
	public Sequence setSequence(Input... pins) {
		Pin[] sequence  = new Pin[pins.length];
		int   predicted = 0;
		for (int i = 0; i < pins.length; i++) {
			Input pin = pins[i];
			// der Pin selbst
			for (Input option : pin.options) {
				predicted++;
			}
			if(!pin.options.isEmpty()) {
				predicted--;
			} else {
				predicted++;
			}
			sequence[i] = new Pin(pin);
		}
		this.sequence = new Sequence(System.currentTimeMillis(), sequence);
		this.sequence.predictedPins = predicted;
		return this.sequence;
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
	public DecodeStrategy decode(Resolution resolution) {
		if(sequence == null) {
			return null;
		} else if(sequence.done) {
			notifySequenceDone();
			return new DecodeStrategy(sequence, resolution);
		}
		return new DecodeStrategy(sequence, resolution);
	}
	
	/**
	 * @param start      Pin to hold completion state of tested start and previous routed pin and taken option
	 *                   each step in way finding
	 * @param resolution Kind of stop type to reduce redundant iterations
	 */
	protected void solveFromPin(Pin start, Resolution resolution, int optionOffset) {
		// if started asnchron or parallel: stop this determineValidRoutes, if another determineValidRoutes found a route
		if(shouldStop(resolution)) {
			return;
		}
		
		// reset this pin
		start.route = null;
		sequence.reset();
		
		Pin         current       = start;
		List<Input> current_route = new ArrayList<>();
		
		outer:
		do {
			// mark this pin as taken and anymore available for further options
			if(!current_route.contains(current.getInput())) {
				current_route.add(current.getInput());
			}
			
			// determineValidRoutes: sequence complete
			if(current_route.size() == sequence.getPins().length) {
				// no further route when already a route was set
				if(shouldStop(resolution)) {
					current_route.clear();
					break; // do
				}
				// solved solution from this
				start.route = Collections.unmodifiableList(current_route);
				if(!start.routes.contains(start.route)) {
					start.routes.add(start.route);
				}
				// start.done = true; <- required after "break do"
				// win and exit
				break; // do
			}
			
			// where to go from this pin
			List<Input> unchosenOptions = determineOptions(current_route, current, optionOffset);
			// more than one option produce a new possible destiny
			if(!unchosenOptions.isEmpty()) {
				for (Input option : unchosenOptions) {
					// has option
					current.getTakenOptions().add(option);
					// go there
					Pin option_pin = sequence.asPinOrDie(option);
					Pin old        = current;
					current = option_pin;
					current.previous = old;
					// continue "further"
					
					// determineValidRoutes: another pin was already solved
					if(shouldStop(resolution)) {
						current_route.clear();
						break outer; // do
					}
					
					// stop first option
					break;
				}
			}
			// no option anymore
			else {
				// redecide in a previous pin
				revertPin(current, current_route);
				current = current.getPrevious();
				// continue "backwards"
				
				// determineValidRoutes: another pin was already solved
				if(shouldStop(resolution)) {
					current_route.clear();
					break; // do
				}
			}
			
		} while (current != null);
		
		// win or lose ... and done ;-)
		start.done = true;
		
		// notify solution
		onPinValidated(start, resolution, optionOffset);
	}
	
	/**
	 * Returns true, if EARLY and any match succeeded in a pin.
	 */
	boolean shouldStop(Resolution resolution) {
		return resolution == Resolution.EARLY_RESULT && hasRouteFound();
	}
	
	/**
	 * Returns true, if any pin of done determineValidRoutes has a valid route.
	 */
	public boolean hasRouteFound() {
		return sequence.done && sequence.getDonePins().stream().anyMatch(Pin::isSuccess);
	}
	
	/**
	 * Notifies a working start pin, at end using ALL or asap using EARLY if a working pin is given
	 *
	 * @param pin        a done pin determineValidRoutes
	 * @param resolution given resolution of fullfillment
	 */
	protected void onPinValidated(Pin pin, Resolution resolution, int optionOffset) {
		if(!sequence.donePins.contains(pin)) {
			sequence.donePins.add(pin);
		}
		switch (resolution) {
			case ALL_RESULTS:
				if(isCompletedSequence() && sequence.donePins.size() >= sequence.predictedPins) {
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
		return sequence.donePins.size() >= sequence.pins.length;
	}
	
	/**
	 * Unmark a pin. Means, this pin options are all available and it is removed from current route.
	 *
	 * @param current       Pin which options should be available
	 * @param current_route Route whe the pin should be removed from. Note to call this method when the pin is at end of the route
	 *                      or your route will be inconsequent and unform.
	 */
	static void revertPin(Pin current, List<Input> current_route) {
		current.getTakenOptions().clear();
		current_route.remove(current.getInput());
	}
	
	/**
	 * Determine a valid, means untaken option from this pin, if the option isn't already in your route,
	 * because each option is only allowed to be hit once a time in your route.
	 *
	 * @param current      The pin to take a decission of.
	 * @param optionOffset
	 * @return null means, no choice is available or already take in your current route.
	 */
	static List<Input> determineOptions(List<Input> current_route, Pin current, int optionOffset) {
		List<Input> options = new ArrayList<>();
		for (Input input : current.getInput().getOptions()) {
			// ungone und untaken
			if(!current_route.contains(input) && !current.getTakenOptions().contains(input)) {
				// can go
				options.add(input);
			}
		}
		
		// remove until option
		for (int i = 0, j = 0; i < options.size(); i++) {
			if(j < optionOffset) {
				if(options.size() == 1) {
					break;
				}
				j++;
				options.remove(i--);
			}
		}
		return options;
	}
	
	/**
	 * Defines a holder for an input varable. It determines taken options and the pin that continued to this pin and finally the decoded route, if it is the starting pin.
	 */
	public static class Pin {
		
		Input             input;
		List<Input>       takenOptions;
		Pin               previous;
		boolean           done;
		List<Input>       route;
		List<List<Input>> routes;
		
		public Pin(Input input) {
			this.input = input;
			this.takenOptions = new ArrayList<>();
			this.routes = new ArrayList<>();
		}
		
		public final List<Input> getRouteOrNull() {
			return route == null || route.isEmpty()
			       ? null
			       : route;
		}
		
		final Pin getPrevious() {
			return previous;
		}
		
		public final Input getInput() {
			return input;
		}
		
		List<Input> getTakenOptions() {
			return takenOptions;
		}
		
		public final boolean isSuccess() {
			List<Input> route = this.route;
			return route != null && !route.isEmpty();
		}
		
		public final void reset() {
			done = false;
			previous = null;
			takenOptions.clear();
		}
		
		public Pin cloneThis() {
			try {
				ObjectMapper mapper = new ObjectMapper();
				String       json   = mapper.writeValueAsString(this);
				return mapper.readValue(json, getClass());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	
	/**
	 * Defines your possible input you want to determineValidRoutes. All options need to be set before decode. If no
	 * option can be determine, then it inflicts to use the caller input to determine a different
	 * way to reach the decoding, if possible.
	 */
	@SuppressWarnings("unused")
	public static class Input {
		
		@JsonProperty
		private List<Input> options;
		@JsonProperty
		private long        id;
		
		public Input() {
		}
		
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
		
		final long id;
		Pin[]     pins;
		boolean   done;
		List<Pin> donePins;
		public long predictedPins;
		
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
		
		public final List<List<Input>> collectRoutes() {
			return donePins.stream().filter(Pin::isSuccess).map(Pin::getRouteOrNull).collect(toList());
		}
		
		public <T> List<T[]> collectRouteType(Function<Input, T> mapper) {
			return collectRoutes().stream().map((List<Input> list) -> {
				List<T> mapped = list.stream().map(mapper).collect(Collectors.toList());
				T       dummy  = mapped.stream().findFirst().orElse(null);
				if(dummy != null) {
					return mapped.toArray((T[]) Array.newInstance(dummy.getClass(), 0));
				}
				return Sequence.<T>emptyArray();
			}).collect(toList());
		}
		
		public static <T> T[] emptyArray() {
			return (T[]) Array.newInstance(Object.class, 0);
		}
		
		public void reset() {
			Arrays.stream(pins).forEach(Pin::reset);
		}
	}
	
	public final class DecodeStrategy {
		
		private final Sequence sequence;
		private final Resolution resolution;
		
		public DecodeStrategy() {
			this(null, Resolution.EARLY_RESULT);
		}
		
		public DecodeStrategy(Sequence sequence, Resolution resolution) {
			this.sequence = sequence;
			this.resolution = resolution;
		}
		
		public void begin() {
			for (Pin pin : sequence.getPins()) {
				// stop asap with resolution EARLY, if a route was found
				if(resolution != Resolution.EARLY_RESULT || !hasRouteFound()) {
					// one solution a pin
					int i = 0;
					do {
						if(i > 0) {
							pin = new Pin(pin.input);
						}
						solveFromPin(pin, resolution, i);
						Pin finalPin = pin;
						if(pin.route != null && pin.routes.stream().noneMatch(r -> r.equals(finalPin.route))) {
							pin.routes.add(pin.route);
						}
					} while (i++ < pin.input.options.size());
				}
				// early stop
				else {
					break;
				}
			}
		}
		
		public List<List<Input>> execute() {
			List<List<Input>> results = new ArrayList<>();
			getDecodingCompletedEvent().addListener(new SequenceHandler(results));
			begin();
			return results;
		}
	}
	
	private static class SequenceHandler implements Handler<SequenceDecoder.Sequence> {
		private final List<List<Input>> results;
		
		public SequenceHandler(List<List<Input>> results) {
			this.results = results;
		}
		
		@Override
		public void invoke(SequenceDecoder.Sequence sq) {
			// Auswertung
			for (SequenceDecoder.Pin pin : sq.getDonePins()) {
				List<Input> raw_route = pin.getRouteOrNull();
				if(raw_route != null && !raw_route.isEmpty()) {
					// cast
					
					List<Input> route = new ArrayList<>(raw_route);
					
					// recognize result
					if(!results.contains(route)) {
						results.add(route);
						// debug
						System.out.println(Arrays.toString(route.toArray()));
					}
				} else {
					// debug
					System.out.println("No result!");
				}
			}
		}
	}
}
