package de.easycreators.sequencer.decode.v1;

import de.easycreators.sequencer.decode.demo.model.Choice;
import de.easycreators.sequencer.decode.model.Resolution;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class SequenceDecoderTest {
	
	@Test
	public void decode22222_allResults() {
		
		SequenceDecoder<Choice> sequenceDecoder = new SequenceDecoder<>(Choice.values(), (dir, move) -> move * dir.f);
		sequenceDecoder.setMode(Resolution.ALL_RESULTS);
		sequenceDecoder.setSequence(2, 2, 2, 2, 2);
		SequenceDecoder.Awaiter awaiter = sequenceDecoder.decode();
		awaiter.awaitEarly();
		sleep(5000);
		
		List<SequenceNodeSolver<Choice>> solutions = sequenceDecoder.getResults(awaiter.getId());
		assertThat(solutions.size(), is(equalOrBiggerThan(3)));
		
		for (SequenceNodeSolver<Choice> solution : solutions) {
			System.out.println(Arrays.toString(solution.listRoute(solution.route).toArray()));
		}
	}
	
	private Matcher<Integer> equalOrBiggerThan(int number) {
		return new BaseMatcher<Integer>() {
			
			@Override
			public boolean matches(Object o) {
				return o instanceof Integer && (int) o >= number;
			}
			
			@Override
			public void describeTo(Description description) {
				description.appendText(String.format(" not or greater than %d", number));
			}
		};
	}
	
	protected void sleep(int millisToSleep) {
		try {
			Thread.sleep(millisToSleep);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}