package java.ffxIIIsolver.decode.model;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface IMoveChoiceModificator<TKey> {
	
	int handleMoveByChoice(TKey dir, int move);
}
