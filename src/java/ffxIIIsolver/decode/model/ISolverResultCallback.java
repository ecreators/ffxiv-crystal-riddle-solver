package java.ffxIIIsolver.decode.model;

import java.ffxIIIsolver.decode.SequenceInput;
import java.ffxIIIsolver.decode.SequenceNodeSolver;
import java.util.List;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface ISolverResultCallback<T> {
	
	void onResult(SequenceNodeSolver<T> tKeySequenceNode, SequenceInput<T> foundStart, List<T> foundRoute);
}
