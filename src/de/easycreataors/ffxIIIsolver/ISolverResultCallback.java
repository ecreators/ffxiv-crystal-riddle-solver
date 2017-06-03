package de.easycreataors.ffxIIIsolver;

import java.util.List;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface ISolverResultCallback<T> {
	
	void onResult(SequenceInputSolver<T> tKeySequenceInputSolver, SequenceInput<T> foundStart, List<T> foundRoute);
}
