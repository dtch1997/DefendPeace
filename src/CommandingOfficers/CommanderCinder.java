package CommandingOfficers;

import java.util.ArrayList;
import java.util.HashMap;

import CommandingOfficers.Modifiers.COModifier;
import Engine.XYCoord;
import Engine.Combat.BattleSummary;
import Engine.GameEvents.GameEventListener;
import Terrain.GameMap;
import Terrain.Location;
import Units.Unit;
import Units.UnitModel;

/*
 * Cinder is based on getting an edge in the action economy, at the cost of unit health.
 */
public class CommanderCinder extends Commander
{
  private static final CommanderInfo coInfo = new CommanderInfo("Cinder", CommanderLibrary.CommanderEnum.CINDER);

  private static final double COST_MOD_PER_BUILD = 1.2;

  private HashMap<XYCoord, Integer> buildCounts = new HashMap<>();

  public CommanderCinder()
  {
    super(coInfo);

    addCommanderAbility(new SearAbility(this));
    addCommanderAbility(new WitchFireAbility(this));
  }

  public static CommanderInfo getInfo()
  {
    return coInfo;
  }

  @Override
  /** Get the list of units this commander can build from the given property type. */
  public ArrayList<UnitModel> getShoppingList(Location buyLocation)
  {
    setPrices(buildCounts.get(buyLocation.getCoordinates()));
    return super.getShoppingList(buyLocation);
  }

  /*
   * Cinder builds units at 8HP ready to act.
   * To compensate for the ability to continue producing units in a single turn,
   * the cost of units increases exponentially for repeated purchases from a single property.
   */
  public void receiveCreateUnitEvent(Unit unit)
  {
    if( this == unit.CO )
    {
      XYCoord buildCoords = new XYCoord(unit.x, unit.y);
      buildCounts.put(buildCoords, buildCounts.get(buildCoords) + 1);
      setPrices(0);

      unit.alterHP(-2);
      unit.isTurnOver = false;
    }
  }

  /*
   * Purchase price problems reset at the start of turn.
   */
  @Override
  public void initTurn(GameMap map)
  {
    for( Location loc : ownedProperties )
    {
      buildCounts.put(loc.getCoordinates(), 0);
    }
    setPrices(0);
    super.initTurn(map);
  }

  public void setPrices(int repetitons)
  {
    for( UnitModel um : unitModels )
    {
      um.COcost = Math.pow(COST_MOD_PER_BUILD, repetitons);
    }
  }

  /*
   * Sear causes 1 mass damage to Cinder's own troops, in exchange for refreshing them.
   */
  private static class SearAbility extends CommanderAbility
  {
    private static final String SEAR_NAME = "Sear";
    private static final int SEAR_COST = 5;
    private static final int SEAR_WOUND = -1;

    SearAbility(Commander commander)
    {
      super(commander, SEAR_NAME, SEAR_COST);
    }

    @Override
    protected void perform(GameMap gameMap)
    {
      for( Unit unit : myCommander.units )
      {
        unit.alterHP(SEAR_WOUND);
        unit.isTurnOver = false;
      }
    }
  }

  /*
   * Witchfire causes Cinder's troops to automatically refresh after attacking, at the cost of 1 HP
   */
  private static class WitchFireAbility extends CommanderAbility implements COModifier
  {
    private static final String WITCHFIRE_NAME = "Witchfire";
    private static final int WITCHFIRE_COST = 9;
    private static final int WITCHFIRE_WOUND = -1;
    private WitchFireListener listener;

    WitchFireAbility(Commander commander)
    {
      // as we start in Bear form, UpTurn is the correct starting name
      super(commander, WITCHFIRE_NAME, WITCHFIRE_COST);
      listener = new WitchFireListener(commander, WITCHFIRE_WOUND);
    }

    @Override
    protected void perform(GameMap gameMap)
    {
      myCommander.addCOModifier(this);
    }

    @Override // COModifier interface.
    public void apply(Commander commander)
    {
      GameEventListener.registerEventListener(listener);
    }

    @Override
    public void revert(Commander commander)
    {
      GameEventListener.unregisterEventListener(listener);
    }
  }

  private static class WitchFireListener extends GameEventListener
  {
    private Commander myCommander = null;
    private final int refreshCost;

    public WitchFireListener(Commander myCo, int refreshWound)
    {
      myCommander = myCo;
      refreshCost = refreshWound;
    }

    @Override
    public void receiveBattleEvent(BattleSummary battleInfo)
    {
      // Determine if we were part of this fight. If so, refresh at our own expense
      if( battleInfo.attacker.CO == myCommander )
      {
        if( battleInfo.attacker.getPreciseHP() > 0 )
        {
          battleInfo.attacker.alterHP(refreshCost);
          battleInfo.attacker.isTurnOver = false;
        }
      }
      else if( battleInfo.defender.CO == myCommander )
      {
        if( battleInfo.defender.getPreciseHP() > 0 )
        {
          battleInfo.defender.alterHP(refreshCost);
          battleInfo.defender.isTurnOver = false;
        }
      }
    }
  }
}
