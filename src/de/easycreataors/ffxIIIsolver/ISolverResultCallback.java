package de.easycreataors.ffxIIIsolver;

import java.util.List;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface ISolverResultCallback {
	
	void onResult(ClockField foundStart, List<Move> foundRoute);
}
