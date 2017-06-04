package de.easycreators.sequencer.decode.v1;

import de.easycreators.sequencer.decode.model.Resolution;

import java.util.List;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface ISequenceDecoder {
	
	void setSequence(Integer... sequenceMoves);
	
	SequenceDecoder.Awaiter decode();
	
	void setMode(Resolution mode);
	
	List awaitSolutions();
	
	public final class Awaiter {
		
		private final int id;
		private final ISequenceDecoder decoder;
		
		protected Awaiter(int id, ISequenceDecoder decoder) {
			this.id = id;
			this.decoder = decoder;
		}
		
		public int getId() {
			return id;
		}
		
		public List awaitEarly() {
			return decoder.awaitSolutions();
		}
	}
}
