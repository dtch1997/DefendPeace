package Engine.GameEvents;

import Engine.Path;
import Terrain.GameMap;
import Terrain.Location;
import UI.MapView;
import UI.Art.Animation.GameAnimation;
import Units.Unit;

/**
 * Moves a unit to the end of the provided path. Only the final path location
 * is validated, so any path-validation checks (e.g. for collisions) should
 * be done before the creation of the MoveEvent. Having the whole path is
 * still useful for the animator.
 */
public class MoveEvent implements GameEvent
{
  private Unit unit = null;
  private Path unitPath = null;

  public MoveEvent(Unit u, Path path)
  {
    unit = u;
    unitPath = path;
  }

  @Override
  public GameAnimation getEventAnimation(MapView mapView)
  {
    return mapView.buildMoveAnimation(unit, unitPath);
  }

  @Override
  public void performEvent(GameMap gameMap)
  {
    if( unitPath.getPathLength() > 0 ) // Make sure we have a destination.
    {
      Path.PathNode endpoint = unitPath.getEnd();
      Location loc = gameMap.getLocation(endpoint.x, endpoint.y);

      // Make sure it is valid to move this unit to its destination.
      if( loc.getResident() == null && unit.model.propulsion.getMoveCost(loc.getEnvironment()) < 99)
      {
        gameMap.moveUnit(unit, endpoint.x, endpoint.y);

        // Every unit action begins with a (potentially 0-distance) move,
        // so we'll just set the "has moved" flag here.
        unit.isTurnOver = true;
      }
      else
      {
        System.out.println("WARNING! Unable to move " + unit.model.type + " to (" + endpoint.x + ", " + endpoint.y + ")");
      }
    }
  }
}