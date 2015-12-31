package Units;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Vector;

import Terrain.GameMap;
import CommandingOfficers.Commander;
import Engine.CombatParameters;
import Engine.DamageChart;
import Engine.GameInstance;
import Engine.MapController;
import Engine.DamageChart.UnitEnum;
import Engine.Utils;

public class Unit {
	public Vector<Unit> heldUnits;
	public UnitModel model;
	public int x, y, movesLeft, fuel;
	public Commander CO;
	public boolean isTurnOver;
	public double HP;

	public Unit(Commander co, UnitModel um)
	{
		System.out.println("Creating a " + um.type);
		CO = co;
		model = um;
		fuel = model.maxFuel;
		isTurnOver = true;
		HP = model.maxHP;
		if (model.holdingCapacity > 0)
			heldUnits = new Vector<Unit>(model.holdingCapacity);
	}
	
	public void initTurn()
	{
		isTurnOver = false;
		fuel -= model.idleFuelBurn;
		movesLeft = model.movePower;
	}
	
	// allows the unit to choose its weapon
	public UnitEnum getWeapon(UnitEnum target) {
		return model.type;
	}
	// for the purpose of letting the unit know it has attacked.
	public void fire(final CombatParameters params) {}

	// Removed for the forseeable future; may be back
/*	public static double getAttackPower(final CombatParameters params) {
//		double output = model
//		return [B*ACO/100+R]*(AHP/10)*[(200-(DCO+DTR*DHP))/100] ;
		return 0;
	}
	public static double getDefensePower(Unit unit, boolean isCounter) {
		return 0;
	}*/

	public MapController.GameAction[] getPossibleActions(GameMap map)
	{
		// TODO - Actually look at the map to see what actions this unit can take from this location.
		ArrayList<MapController.GameAction> actions = new ArrayList<MapController.GameAction>();
		if (map.getLocation(x, y).getResident() == null)
		{
			for (int i = 0; i < model.possibleActions.length; i++) {
				switch (model.possibleActions[i])
				{
				case ATTACK:
					// highlight the tiles in range, and check them for targets
					Utils.findActionableLocations(this, MapController.GameAction.ATTACK, map);
					for (int w = 0; w < map.mapWidth; w++)
					{
						for (int h = 0; h < map.mapHeight; h++)
						{
							Unit target = map.getLocation(w, h).getResident();
							if (map.getLocation(w, h).isHighlightSet() &&
									target != null &&
									target.CO != this.CO &&
									DamageChart.chartDamage(this, target) > 0)
							{
								actions.add(MapController.GameAction.ATTACK);
								break; // just need one target
							}
						}
					}
					map.clearAllHighlights();
					break;
				case CAPTURE:
					if(map.getLocation(x, y).getOwner() != CO && map.getLocation(x, y).isCaptureable())
					{
						actions.add(MapController.GameAction.CAPTURE);
					}
					break;
				case WAIT:
					actions.add(MapController.GameAction.WAIT);
					break;
				case LOAD:
					break;
				case UNLOAD:
					if (heldUnits.size() > 0) {
						actions.add(MapController.GameAction.UNLOAD);
					}
					break;
				default:
					System.out.println("getPossibleActions: Invalid action in model's possibleActions["+i+"]: " + model.possibleActions[i]);
				}
			}
		} else
		{
			actions.add(MapController.GameAction.LOAD);
		}
		MapController.GameAction[] returned = new MapController.GameAction[0];
		return actions.toArray(returned);
	}
}
