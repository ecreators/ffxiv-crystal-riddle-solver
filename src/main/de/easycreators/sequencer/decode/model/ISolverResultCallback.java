package de.easycreators.sequencer.decode.model;

import de.easycreators.sequencer.decode.SequenceInput;
import de.easycreators.sequencer.decode.SequenceNodeSolver;

import java.util.List;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface ISolverResultCallback<T> {
	
	void onResult(SequenceNodeSolver<T> tKeySequenceNode, SequenceInput<T> foundStart, List<T> foundRoute);
}
