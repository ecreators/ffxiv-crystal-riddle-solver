package de.easycreators.sequencer.encode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class SequenceEncoderTest {
	
	private SequenceEncoder encoder;
	
	@Before
	public void before() {
		encoder = new SequenceEncoder();
	}
	
	@After
	public void after() {
		System.out.println(Arrays.toString(encoder.getMoves().toArray()));
	}
	
	@Test
	public void sequence42031_sequence22222() {
		encoder.setFields(asList(4, 2, 0, 3, 1));
		encoder.encodeMoves();
		assertThat(encoder.getMoves(), is(equalTo(asList(2, 2, 2, 2, 2))));
	}
	
	@Test
	public void sequence31420_sequence22222() {
		encoder.setFields(asList(3, 1, 4, 2, 0));
		encoder.encodeMoves();
		assertThat(encoder.getMoves(), is(equalTo(asList(2, 2, 2, 2, 2))));
	}
	
	@Test
	public void sequence20314_sequence22222() {
		encoder.setFields(asList(2, 0, 3, 1, 4));
		encoder.encodeMoves();
		assertThat(encoder.getMoves(), is(equalTo(asList(2, 2, 2, 2, 2))));
	}
}