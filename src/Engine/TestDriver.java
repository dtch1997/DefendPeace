package Engine;

import java.awt.Color;

import CommandingOfficers.CmdrStrong;
import CommandingOfficers.Commander;
import Terrain.GameMap;
import UI.MapView;
import Units.Unit;
import Units.UnitModel.UnitEnum;

/**
 * Instantiate this to run a bunch of tests, 
 * @author Christopher White
 *
 */
public class TestDriver
{
  private static Commander testCo1;
  private static Commander testCo2;
  private static GameMap testMap;
  private static GameInstance testGame;
  
  public static boolean performTests()
  {
    testCo1 = new CmdrStrong();
    testCo2 = new Commander();
    Commander[] cos = {testCo1, testCo2};
    
    // TODO: This will have to change once GameMap doesn't build a default map.
    testMap = new GameMap(cos);
    testGame = new GameInstance(testMap, cos);
    
    // Remove the default units. TODO: Remove this once there isn't a default map.
    testMap.getLocation(6, 5).setResident(null);
    testMap.getLocation(8, 4).setResident(null);
    
    // We start out optimistic.
    boolean passed = true;
    
    passed &= validate(testMovement(), "Movement test failed");
    passed &= validate(testTransport(), "Transport test failed");
    
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
