package Test;

import CommandingOfficers.CommanderPatch;
import CommandingOfficers.CommanderStrong;
import CommandingOfficers.Commander;
import Engine.GameAction;
import Engine.GameEvents.CommanderDefeatEvent;
import Engine.GameEvents.GameEvent;
import Engine.GameEvents.GameEventQueue;
import Terrain.Environment;
import Terrain.GameMap;
import Terrain.Location;
import Terrain.MapLibrary;
import Units.Unit;
import Units.UnitModel.UnitEnum;

public class TestCapture extends TestCase
{
  private static Commander testCo1;
  private static Commander testCo2;
  private static GameMap testMap;

  /** Make two COs and a GameMap to use with this test case. */
  private void setupTest()
  {
    testCo1 = new CommanderStrong();
    testCo2 = new CommanderPatch();
    Commander[] cos = { testCo1, testCo2 };

    testMap = new GameMap(cos, MapLibrary.getByName("Firing Range"));
  }

  @Override
  public boolean runTest()
  {
    setupTest();

    boolean testPassed = true;
    testPassed &= validate(testCapture(), "  Capture test failed.");
    testPassed &= validate(testCaptureHQ(), "  HQ-Capture test failed.");
    return testPassed;
  }

  private boolean testCapture()
  {
    // No problems yet.
    boolean testPassed = true;

    // Get a reference to a capturable property.
    Location prop = testMap.getLocation(2, 2);

    // Make sure this location is capturable.
    testPassed &= validate( prop.isCaptureable(), "    Unexpected terrain found! Test will be invalid." );

    // Add a unit to help run the tests.
    Unit infA = addUnit(testMap, testCo1, UnitEnum.INFANTRY, 2, 2); // On the city.

    // Start capturing the city.
    GameAction captureAction = new GameAction(infA, 2, 2, GameAction.ActionType.CAPTURE);
    performGameAction( captureAction, testMap );

    // Verify that we can start and stop property capture.
    testPassed &= validate( infA.getCaptureProgress() == 10, "    Infantry does not register capture progress.");
    infA.stopCapturing();
    testPassed &= validate( infA.getCaptureProgress() == 0, "    Infantry should not register capture progress.");
    performGameAction( captureAction, testMap );
    testPassed &= validate( infA.getCaptureProgress() == 10, "    Infantry is not capturing, but should be.");

    // Ensure that moving resets the capture counter.
    performGameAction( new GameAction(infA, 2, 3, GameAction.ActionType.WAIT), testMap );
    testPassed &= validate( infA.getCaptureProgress() == 0, "    Infantry is still capturing after moving.");

    // Make sure we can WAIT, and resume capturing.
    performGameAction( new GameAction(infA, 2, 2, GameAction.ActionType.WAIT), testMap ); // Move back onto the city.
    performGameAction( captureAction, testMap );
    performGameAction( new GameAction(infA, 2, 2, GameAction.ActionType.WAIT), testMap ); // Wait on the city.
    testPassed &= validate( infA.getCaptureProgress() == 10, "    Infantry should not lose capture progress due to WAIT.");

    // Make sure that attacking someone else resets capture progress.
    Unit infB = addUnit(testMap, testCo2, UnitEnum.INFANTRY, 1, 2); // Make an enemy adjacent to the city.
    infB.alterHP( -8 ); // Make sure he will die without retaliating.
    performGameAction( new GameAction(infA, 2, 2, GameAction.ActionType.ATTACK, 1, 2), testMap ); // Bop him on the head.
    testPassed &= validate( infA.getCaptureProgress() == 10, "    Infantry should not stop capturing after stationary ATTACK.");

    // See if we can actually capture this thing.
    infA.alterHP( -6 ); // Make it take three attempts to capture the property.
    performGameAction( captureAction, testMap );
    testPassed &= validate( infA.getCaptureProgress() == 14, "    Infantry has wrong capture progress (" +
        infA.getCaptureProgress() + " instead of 14)." );
    performGameAction( captureAction, testMap );
    testPassed &= validate( infA.getCaptureProgress() == 18, "    Infantry has wrong capture progress (" +
        infA.getCaptureProgress() + " instead of 18)." );

    performGameAction( captureAction, testMap );
    // Verify that we now own the property, and that capture progress is reset.
    testPassed &= validate( prop.getOwner() == infA.CO, "    Infantry failed to capture the property.");
    testPassed &= validate( infA.getCaptureProgress() == 0, "    Infantry capture progress did not reset after capture." );

    // Clean up
    testMap.removeUnit(infA);
    testMap.removeUnit(infB);

    return testPassed;
  }

  private boolean testCaptureHQ()
  {
    boolean testPassed = true;

    setupTest(); // Reset test parameters to ensure it's all set up hunky-dory.

    // We loaded Firing Range, so we expect an HQ for testCo2 at location (13, 1)
    Terrain.Location hq = testMap.getLocation(13, 1);
    testPassed &= validate( hq.getOwner() == testCo2, "    HQ at (13, 1) is not owned by testCo2, but should be.");
    testPassed &= validate( hq.getEnvironment().terrainType == Environment.Terrains.HQ, "    HQ for testCo2 is not where expected.");

    // Add a unit to help run the tests.
    Unit mech = addUnit(testMap, testCo1, UnitEnum.MECH, 13, 1); // On the HQ, just to make this easy.

    // Start capturing the HQ.
    GameAction captureAction = new GameAction(mech, 13, 1, GameAction.ActionType.CAPTURE);
    performGameAction(captureAction, testMap);

    // Re-create the event so we can predict the HQ capture, but don't execute; we just want to see the resulting GameEventQueue.
    captureAction = new GameAction(mech, 13, 1, GameAction.ActionType.CAPTURE);
    GameEventQueue events = captureAction.getGameEvents(testMap);

    // Make sure a CommanderDefeatEvent was generated as a result (the actual event test is in TestGameEvent.java).
    boolean hasDefeatEvent = false;
    for( GameEvent event : events )
    {
      if( event instanceof CommanderDefeatEvent )
      {
        hasDefeatEvent = true;
        break;
      }
    }
    testPassed &= validate( hasDefeatEvent, "    No CommanderDefeatEvent generated on HQ capture!");

    return testPassed;
  }
}