package Engine;

import CommandingOfficers.CmdrStrong;
import CommandingOfficers.Commander;
import Terrain.GameMap;
import Units.Unit;
import Units.UnitModel.UnitEnum;

/**
 * Provides a framework for automated regression-testing of game functionality.
 */
public class TestDriver
{
  private static Commander testCo1;
  private static Commander testCo2;
  private static GameMap testMap;
  
  /** Initializes and runs all of the tests
   * @return true if all tests pass, false else.
   */
  public static boolean performTests()
  {
    testCo1 = new CmdrStrong();
    testCo2 = new Commander();
    Commander[] cos = {testCo1, testCo2};
    
    // TODO: This will have to change once GameMap doesn't build a default map.
    testMap = new GameMap(cos);
    
    // Remove the default units. TODO: Remove this once there isn't a default map.
    testMap.getLocation(6, 5).setResident(null);
    testMap.getLocation(8, 4).setResident(null);
    
    // We start out optimistic.
    boolean passed = true;
    
    passed &= validate(testMovement(), "Movement test failed");
    passed &= validate(testTransport(), "Transport test failed");
    passed &= validate(testCounter(), "Counter test failed");
    passed &= validate(testHealing(), "Healing test failed");
    
    return passed;
  }
  
  private static boolean testMovement()
  {
    Unit mover = addUnit(testMap, testCo1, UnitEnum.INFANTRY, 4, 4);
    GameAction ga = new GameAction(mover, 6, 5, GameAction.ActionType.WAIT);
    ga.execute(testMap);
    
    boolean result = validate(testMap.getLocation(4, 4).getResident() == null, "  Infantry still at start point!");
    result &= validate(testMap.getLocation(6, 5).getResident() == mover, "  Infantry not at the destination!");
    result &= validate((6 == mover.x) && (5 == mover.y), "  Infantry doesn't think he's at the destination!");
    
    // Clean up for the next test.
    testMap.removeUnit(mover);
    
    return result;
  }
  
  private static boolean testTransport()
  {
    Unit cargo = addUnit(testMap, testCo1, UnitEnum.INFANTRY, 4, 1);
    Unit apc = addUnit(testMap, testCo1, UnitEnum.APC, 4, 2);
    
    // Try a basic load/move/unload order.
    new GameAction(cargo, 4, 2, GameAction.ActionType.LOAD).execute(testMap);
    new GameAction(apc, 7, 3, GameAction.ActionType.UNLOAD, 7, 4).execute(testMap);
    
    // Make sure we can unload a unit on the apc's current location.
    new GameAction(cargo, 7, 3, GameAction.ActionType.LOAD).execute(testMap);
    new GameAction(apc, 7, 4, GameAction.ActionType.UNLOAD, 7, 3).execute(testMap);
    
    boolean result = validate(testMap.getLocation(7, 4).getResident() == apc, "  APC not where it belongs.");
    result &= validate(testMap.getLocation(7, 3).getResident() == cargo, "  Cargo not at dropoff location");
    
    // Clean up
    testMap.removeUnit(cargo);
    testMap.removeUnit(apc);
    
    return result;
  }
  
  private static boolean testCounter()
  {
    Unit mechA = addUnit(testMap, testCo1, UnitEnum.MECH, 1, 1);
    Unit infB = addUnit(testMap, testCo2, UnitEnum.INFANTRY, 1, 2);
    
    // make him git rekt first attack
    infB.damageHP(9.5);
    
    // make the attack
    new GameAction(mechA, 1, 1, GameAction.ActionType.ATTACK, 1, 2).execute(testMap);
    
    boolean result = validate(mechA.getPreciseHP() == 10, "  attacker lost or gained health.");
    result &= validate(testMap.getLocation(1, 2).getResident() == null, "  Defender didn't die.");
    
    // Clean up
    testMap.removeUnit(mechA);
    
    return result;
  }
  
  private static boolean testHealing()
  {
    Unit victim = addUnit(testMap, testCo1, UnitEnum.INFANTRY, 1, 4);
    testCo1.money = 999; // 999\200 = 4, plus one healing after that = 5, plus another iteration to check poverty conditions = 6 iterations
    int lastMoney, lastHP, iterations = 0; // tracking vars
    boolean result = true; // start optimistic

    while(true)
    {
    	iterations++;
	    victim.damageHP(2.5); // hurt the victim
	    lastMoney = testCo1.money; // track money
	    lastHP = victim.getHP(); // track HP
	    victim.initTurn(testMap.getLocation(1, 4)); // Make the unit try to heal itself
	    // if the unit was healed or we were too poor, we're good
	    result &= validate(victim.getHP() > lastHP || lastMoney < 100, "  Unit was not healed when he should have been.");
	    // if the unit was healed AND we were too poor, that's bad
	    result &= validate(!(victim.getHP() > lastHP && lastMoney < 100), "  Unit was healed when he should not have been.");
	    if (victim.getHP() < 9) // Test continues until unit fails to heal itself due to monetary drain.
	    	break;
    }

    // if the loop went the wrong number of times, that's bad
    result &= validate(iterations == 6, "  Heal test iterated an improper number of times.");
    
    // Clean up
    testMap.removeUnit(victim);
    
    return result;
  }
  
  private static boolean validate(boolean condition, String faultMsg)
  {
    if(!condition)
    {
      System.out.println(faultMsg);
    }
    return condition;
  }
  
  private static Unit addUnit(GameMap map, Commander co, UnitEnum type, int x, int y)
  {
    Unit u = new Unit(co, co.getUnitModel(type));
    u.x = x;
    u.y = y;
    map.getLocation(x, y).setResident(u);
    return u;
  }
}