package Engine;

import Terrain.GameMap;
import Units.Unit;

/**
 * Allows the building of an in-game action as a Unit, a location to move to, an ActionType, and an (optional) location to act on.
 * Once fully constructed (as affirmed by isReadyToExecute()), a call to execute() will cause GameAction to perform the action.
 */
public class GameAction
{
  public enum ActionType
  {
    INVALID, ATTACK, CAPTURE, LOAD, UNLOAD, WAIT
  };
  
  private Unit unitActor = null;
  private ActionType actionType;

  private int moveX;
  private int moveY;
  private int actX;
  private int actY;
  
  public GameAction(Unit unit)
  {
    unitActor = unit;
    actionType = ActionType.INVALID;
    moveX = -1;
    moveY = -1;
    actX = -1;
    actY = -1;
  }

  public void setActionType(ActionType type)
  {
    actionType = type;
  }
  
  public Unit getActor()
  {
    return unitActor;
  }

  public ActionType getActionType()
  {
    return actionType;
  }
  
  public void setMoveLocation(int x, int y)
  {
    moveX = x;
    moveY = y;
  }
  
  public int getMoveX()
  {
    return moveX;
  }
  public int getMoveY()
  {
    return moveY;
  }
  
  public void setActionLocation(int x, int y)
  {
    actX = x;
    actY = y;
  }
  
  /** Returns true if this GameAction has all the information it needs to execute, false else. */
  public boolean isReadyToExecute()
  {
    boolean ready = false;
    if(moveX >= 0 && moveY >= 0 && actionType != ActionType.INVALID)
    {
      if(actionType == ActionType.WAIT || actionType == ActionType.LOAD || actionType == ActionType.CAPTURE)
      {
        ready = true;
      }
      else // ActionType is Attack or Unload - needs a target
      {
        if(actX >= 0 && actY >= 0)
        {
          ready = true;
        }
      }
    }
    return ready;
  }
  
  /**
   * Performs the built-up action, using the passed-in GameMap.
   * @return true if the action successfully executes, false if a problem occurs.
   */
  public boolean execute(GameMap gameMap)
  {
    if(!isReadyToExecute())
    {
      System.out.println("ERROR! Attempting to execute an incomplete GameAction");
      return false;
    }
    
    switch(actionType)
    {
      case ATTACK:
        Unit unitTarget = gameMap.getLocation(actX, actY).getResident();
        
        if( unitTarget != null && unitActor.getDamage(unitTarget, moveX, moveY) != 0 )
        {
          unitActor.isTurnOver = true;
          gameMap.moveUnit(unitActor, moveX, moveY);

          boolean canCounter = unitTarget.getDamage(unitActor) != 0;
          CombatEngine.resolveCombat(unitActor, unitTarget, gameMap, canCounter);
          if( unitActor.HP <= 0 )
          {
            gameMap.removeUnit(unitActor);
            unitActor.CO.units.remove(unitActor);
          }
          if( unitTarget.HP <= 0 )
          {
            gameMap.removeUnit(unitTarget);
            unitTarget.CO.units.remove(unitTarget);
          }
          System.out.println("unitActor hp: " + unitActor.HP);
          System.out.println("unitTarget hp: " + unitTarget.HP);
        }
        break;
      case CAPTURE:
        unitActor.isTurnOver = true;
        gameMap.moveUnit(unitActor, moveX, moveY);
        unitActor.capture(gameMap.getLocation(unitActor.x, unitActor.y));
        break;
      case LOAD:
        Unit transport = gameMap.getLocation(moveX, moveY).getResident();
        
        if(null != transport && transport.hasCargoSpace(unitActor.model.type))
        {
          unitActor.isTurnOver = true;
          gameMap.removeUnit(unitActor);
          unitActor.x = -1;
          unitActor.y = -1;
          transport.heldUnits.add(unitActor);
        }
        break;
      case UNLOAD:
        // If we have cargo and the landing zone is empty, we drop the cargo.
        if( !unitActor.heldUnits.isEmpty() && null == gameMap.getLocation(actX, actY).getResident() )
        {
          unitActor.isTurnOver = true;
          gameMap.moveUnit(unitActor, moveX, moveY);
          Unit droppable = unitActor.heldUnits.remove(0); // TODO: Account for multi-Unit transports 
          gameMap.moveUnit(droppable, actX, actY);
          droppable.isTurnOver = true;
        }
        break;
      case WAIT:
        unitActor.isTurnOver = true;
        gameMap.moveUnit(unitActor, moveX, moveY);
        break;
      case INVALID:
        default:
          System.out.println("Attempting to execute an invalid GameAction!");
    }
    
    return unitActor.isTurnOver;
  }
}