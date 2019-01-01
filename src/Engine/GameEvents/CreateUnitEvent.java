package Engine.GameEvents;

import CommandingOfficers.Commander;
import Engine.XYCoord;
import Terrain.MapMaster;
import UI.MapView;
import UI.Art.Animation.GameAnimation;
import Units.Unit;
import Units.UnitModel;

public class CreateUnitEvent implements GameEvent
{
  private final Commander myCommander;
  private final Unit myNewUnit;
  private final XYCoord myBuildCoords;
  public CreateUnitEvent(Commander commander, UnitModel model, XYCoord coords)
  {
    myCommander = commander;
    myBuildCoords = coords;

    // TODO: Consider breaking the fiscal part into its own event.
    if( model.moneyCost <= commander.money )
    {
      myNewUnit = new Unit(myCommander, model);
    }
    else
    {
      System.out.println("WARNING! Attempting to build unit with insufficient funds.");
      myNewUnit = null;
    }
  }

  @Override
  public GameAnimation getEventAnimation(MapView mapView)
  {
    return null;
  }

  @Override
  public void sendToListener(GameEventListener listener)
  {
    if( null != myNewUnit )
    {
      listener.receiveCreateUnitEvent(myNewUnit);
    }
  }

  @Override
  public void performEvent(MapMaster gameMap)
  {
    if( null != myNewUnit )
    {
      myCommander.money -= myNewUnit.model.moneyCost;
      myCommander.units.add(myNewUnit);
      gameMap.addNewUnit(myNewUnit, myBuildCoords.xCoord, myBuildCoords.yCoord);
    }
    else
    {
      System.out.println("Warning! Attempting to build unit with insufficient funds.");
    }
  }
  
  @Override // there's no known way for this to fail after the GameAction is constructed
  public boolean shouldPreempt(MapMaster gameMap )
  {
    return false;
  }
}
