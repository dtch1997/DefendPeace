package AI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import CommandingOfficers.Commander;
import Engine.GameAction;
import Engine.GameActionSet;
import Engine.Path;
import Engine.Utils;
import Engine.XYCoord;
import Terrain.GameMap;
import Terrain.Location;
import Terrain.TerrainType;
import Units.Unit;
import Units.UnitModel;

/**
 *  Just build tons of Infantry and try to rush the opponent.
 */
public class InfantrySpamAI implements AIController
{
  Queue<GameAction> actions = new ArrayDeque<GameAction>();

  private Commander myCo = null;

  private ArrayList<XYCoord> unownedProperties;
  private ArrayList<XYCoord> capturingProperties;

  private StringBuffer logger = new StringBuffer();
  private int turnNum = 0;

  public InfantrySpamAI(Commander co)
  {
    myCo = co;
  }

  @Override
  public void initTurn(GameMap gameMap)
  {
    turnNum++;
    logger.append(String.format("[======== ISAI initializing turn %s for %s =========]\n", turnNum, myCo));

    // Make sure we don't have any hang-ons from last time.
    actions.clear();
    
    // Create a list of every property we don't own, but want to.
    unownedProperties = new ArrayList<XYCoord>();
    for( int x = 0; x < gameMap.mapWidth; ++x )
    {
      for( int y = 0; y < gameMap.mapHeight; ++y )
      {
        XYCoord loc = new XYCoord(x, y);
        if( gameMap.getLocation(loc).isCaptureable() && gameMap.getLocation(loc).getOwner() != myCo )
        {
          unownedProperties.add(loc);
        }
      }
    }
    capturingProperties = new ArrayList<XYCoord>();
    for( Unit unit : myCo.units )
    {
      if( unit.getCaptureProgress() > 0 )
      {
        capturingProperties.add(unit.getCaptureTargetCoords());
      }
    }
  }

  @Override
  public void endTurn()
  {
    logger.append(String.format("[======== ISAI ending turn %s for %s =========]\n", turnNum, myCo));
    System.out.println(logger.toString());
    logger = new StringBuffer();
  }

  @Override
  public GameAction getNextAction(GameMap gameMap)
  {
    // Handle actions for each unit the CO owns.
    for( Unit unit : myCo.units )
    {
      if( unit.isTurnOver ) continue; // No actions for stale units.
      boolean foundAction = false;

      // Find the possible destinations.
      ArrayList<XYCoord> destinations = Utils.findPossibleDestinations(unit, gameMap);

      for( XYCoord coord : destinations )
      {
        // Figure out how to get here.
        Path movePath = Utils.findShortestPath(unit, coord, gameMap);

        // Figure out what I can do here.
        ArrayList<GameActionSet> actionSets = unit.getPossibleActions(gameMap, movePath);
        for( GameActionSet actionSet : actionSets )
        {
          // See if we have the option to attack.
          if( actionSet.getSelected().getType() == GameAction.ActionType.ATTACK )
          {
            actions.offer(actionSet.getSelected() );
            foundAction = true;
            break;
          }
          
          // Otherwise, see if we have the option to capture.
          if( actionSet.getSelected().getType() == GameAction.ActionType.CAPTURE )
          {
            actions.offer(actionSet.getSelected() );
            capturingProperties.add(coord);
            foundAction = true;
            break;
          }
        }
        if(foundAction)break; // Only allow one action per unit.
      }
      if(foundAction)break; // Only one action per getNextAction() call, to avoid overlap.

      // If no attack/capture actions are available now, just move towards a non-allied building.
      Utils.sortLocationsByDistance( new XYCoord(unit.x, unit.y), unownedProperties);
      if( !unownedProperties.isEmpty() ) // Sanity check - it shouldn't be, unless this function is called after we win.
      {
        XYCoord goal = unownedProperties.get(0);
        // Check to make sure we haven't captured this since we last checked.
        while(gameMap.getLocation(goal).getOwner() == myCo || capturingProperties.contains(goal))
        {
          unownedProperties.remove(goal);
          goal = unownedProperties.get(0);
        }

        // Sort my possible move locations by distance from the goal, choose
        // the closest one, and build a GameAction to move there.
        Utils.sortLocationsByDistance(goal, destinations);
        XYCoord destination = destinations.get(0);
        Path movePath = Utils.findShortestPath(unit, destination, gameMap);
        GameAction move = new GameAction.WaitAction(gameMap, unit, movePath);
        actions.offer(move);
        break;
      }
    }

    // Finally, build more infantry. We will add all build commands at once, since they can't conflict.
    if( actions.isEmpty() )
    {
      // Create a list of actions to build infantry on every open factory, then return these actions until done.
      for( int i = 0; i < gameMap.mapWidth; i++ )
      {
        for( int j = 0; j < gameMap.mapHeight; j++)
        {
          Location loc = gameMap.getLocation(i, j);
          // If this terrain belongs to me, and I can build something on it, and I have the money, do so.
          if( loc.getEnvironment().terrainType == TerrainType.FACTORY && loc.getOwner() == myCo && loc.getResident() == null )
          {
            ArrayList<UnitModel> units = myCo.getShoppingList(loc.getEnvironment().terrainType);
            if( !units.isEmpty() && units.get(0).moneyCost <= myCo.money )
            {
              GameAction action = new GameAction.UnitProductionAction(gameMap, myCo, units.get(0), loc.getCoordinates());
              actions.offer( action );
            }
          }
        }
      }
    }

    // Return the next action, or null if actions is empty.
    GameAction nextAction = actions.poll();
    logger.append(String.format("  Action: %s\n", nextAction));
    return nextAction;
  }
}
