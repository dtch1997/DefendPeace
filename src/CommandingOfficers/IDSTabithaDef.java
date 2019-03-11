package CommandingOfficers;

import CommandingOfficers.Modifiers.CODamageModifier;
import CommandingOfficers.Modifiers.CODefenseModifier;
import Engine.Combat.BattleSummary;
import Engine.Combat.CostValueFinder;
import Engine.Combat.MassStrikeUtils;
import Engine.GameEvents.GameEventQueue;
import Engine.Combat.BattleInstance.BattleParams;
import Terrain.GameMap;
import Terrain.MapMaster;
import Units.Unit;

public class IDSTabithaDef extends Commander
{
  private static final CommanderInfo coInfo = new CommanderInfo("Tabitha def", new instantiator());

  private static class instantiator implements COMaker
  {
    @Override
    public Commander create()
    {
      return new IDSTabithaDef();
    }
  }

  public IDSTabithaDef()
  {
    super(coInfo);

    addCommanderAbility(new Firestorm(this));
    addCommanderAbility(new Apocolypse(this));
  }

  public static CommanderInfo getInfo()
  {
    return coInfo;
  }
  
  private Unit COU;
  private int COUBoost;

  @Override
  public char getUnitMarking(Unit unit)
  {
    if (unit == COU)
      return 'T';
    
    return super.getUnitMarking(unit);
  }

  @Override
  public GameEventQueue initTurn(GameMap map)
  {
    this.COU = null;
    COUBoost = 35;
    return super.initTurn(map);
  }

  @Override
  public void applyCombatModifiers(BattleParams params, boolean amITheAttacker)
  {
    if( params.attacker.CO == this )
    {
      Unit minion = params.attacker;

      if( null == COU || minion == COU )
        params.attackFactor += COUBoost;
    }

    if( params.defender.CO == this )
    {
      Unit minion = params.defender;

      if( null == COU || minion == COU )
        params.defenseFactor += COUBoost;
    }

  }

  @Override
  public void receiveBattleEvent(BattleSummary battleInfo)
  {
    super.receiveBattleEvent(battleInfo);
    // Determine if we were part of this fight.
    if( COU == null && battleInfo.attacker.CO == this )
    {
      COU = battleInfo.attacker;
    }
    if( COU == null && battleInfo.defender.CO == this )
    {
      COU = battleInfo.defender;
    }
  }

  private static class Firestorm extends CommanderAbility
  {
    private static final String NAME = "Firestorm";
    private static final int COST = 6;
    private static final int POWER = 4;
    IDSTabithaDef COcast;

    Firestorm(Commander commander)
    {
      super(commander, NAME, COST);
      COcast = (IDSTabithaDef) commander;
    }

    @Override
    protected void perform(MapMaster gameMap)
    {
      COcast.COUBoost -= 10;
      myCommander.addCOModifier(new CODamageModifier(10));
      myCommander.addCOModifier(new CODefenseModifier(10));
      MassStrikeUtils.damageStrike(gameMap, POWER,
          MassStrikeUtils.findValueConcentration(gameMap, 2, new CostValueFinder(myCommander, true)), 2);
    }
  }

  private static class Apocolypse extends CommanderAbility
  {
    private static final String NAME = "Apocolypse";
    private static final int COST = 10;
    private static final int POWER = 8;
    IDSTabithaDef COcast;

    Apocolypse(Commander commander)
    {
      super(commander, NAME, COST);
      COcast = (IDSTabithaDef) commander;
    }

    @Override
    protected void perform(MapMaster gameMap)
    {
      COcast.COUBoost = 0;
      myCommander.addCOModifier(new CODamageModifier(35));
      myCommander.addCOModifier(new CODefenseModifier(35));
      MassStrikeUtils.damageStrike(gameMap, POWER,
          MassStrikeUtils.findValueConcentration(gameMap, 2, new CostValueFinder(myCommander, true)), 2);
    }
  }
}
