package CommandingOfficers;

import CommandingOfficers.Modifiers.CODamageModifier;
import CommandingOfficers.Modifiers.CODefenseModifier;
import Engine.Combat.BattleInstance.BattleParams;
import Terrain.GameMap;
import Units.Unit;
import Units.UnitModel;

public class YCKanbei extends Commander
{
  private static final CommanderInfo coInfo = new CommanderInfo("Kanbei", new instantiator());

  private static class instantiator implements COMaker
  {
    @Override
    public Commander create()
    {
      return new YCKanbei();
    }
  }

  private double counterBoost = 1;

  public YCKanbei()
  {
    super(coInfo);

    new CODamageModifier(30).apply(this);
    new CODefenseModifier(30).apply(this);
    for( UnitModel um : unitModels )
    {
      um.COcost = 1.2;
    }

    addCommanderAbility(new MoraleBoost(this));
    addCommanderAbility(new SamuraiSpirit(this));
  }

  public static CommanderInfo getInfo()
  {
    return coInfo;
  }

  @Override
  public void initTurn(GameMap map)
  {
    counterBoost = 1;
    super.initTurn(map);
  }

  public void applyCombatModifiers(BattleParams params, GameMap map)
  {
    Unit minion = null;
    if( params.attacker.CO == this )
    {
      minion = params.attacker;
    }

    if( null != minion && params.isCounter )
    {
      // it's a multiplier according to the damage calc
      params.attackFactor *= counterBoost;
    }
  }

  private static class MoraleBoost extends CommanderAbility
  {
    private static final String NAME = "Morale Boost";
    private static final int COST = 3;
    private static final int VALUE = 10;

    MoraleBoost(Commander commander)
    {
      super(commander, NAME, COST);
    }

    @Override
    protected void perform(GameMap gameMap)
    {
      myCommander.addCOModifier(new CODamageModifier(VALUE));
    }
  }

  private static class SamuraiSpirit extends CommanderAbility
  {
    private static final String NAME = "Samurai Spirit";
    private static final int COST = 6;
    private YCKanbei COcast;

    SamuraiSpirit(Commander commander)
    {
      super(commander, NAME, COST);
      COcast = (YCKanbei) commander;
    }

    @Override
    protected void perform(GameMap gameMap)
    {
      myCommander.addCOModifier(new CODamageModifier(10));
      myCommander.addCOModifier(new CODefenseModifier(20));
      COcast.counterBoost = 1.5;
    }
  }
}
