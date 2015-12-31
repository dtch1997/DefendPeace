package Units;

import java.util.Vector;

import Units.MoveTypes.MoveType;
import Units.MoveTypes.Tread;
import Engine.MapController.GameAction;

public class APCModel extends UnitModel {

	private static final MoveType moveType = new Tread();
	private static final GameAction[] actions = {GameAction.UNLOAD, GameAction.WAIT};

	public APCModel() {
		super("APC", UnitEnum.APC, 550, 42, 0, 5, moveType, actions);
		possibleActions = actions;
		holdingCapacity = 1;
		UnitEnum[] carryable = {Units.UnitModel.UnitEnum.INFANTRY, Units.UnitModel.UnitEnum.MECH};
		holdables = new Vector<UnitEnum>(carryable.length);
		for (int i = 0; i < holdables.capacity(); i++) {
			holdables.add(carryable[i]);
		}
	}
}
