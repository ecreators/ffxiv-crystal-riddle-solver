package de.easycreators.sequencer.decode.model;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface IMoveChoiceModificator<TKey> {
	
	int handleMoveByChoice(TKey dir, int move);
}
