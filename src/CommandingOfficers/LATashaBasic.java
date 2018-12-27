package CommandingOfficers;

import CommandingOfficers.Modifiers.COMovementModifier;
import Terrain.GameMap;
import Units.UnitModel;
import Units.UnitModel.ChassisEnum;

public class LATashaBasic extends Commander
{
  private static final CommanderInfo coInfo = new CommanderInfo("Tasha\nBasic", new instantiator());
  private static class instantiator implements COMaker
  {
    @Override
    public Commander create()
    {
      return new LATashaBasic();
    }
  }

  public LATashaBasic()
  {
    super(coInfo);

    for( UnitModel um : unitModels )
    {
      if( um.chassis == ChassisEnum.AIR_HIGH || um.chassis == ChassisEnum.AIR_LOW )
      {
        um.modifyDamageRatio(30);
        um.modifyDefenseRatio(15);
      }
    }

    addCommanderAbility(new AirMoveBonus(this, "Sonic Boom", 3, 2));
  }

  public static CommanderInfo getInfo()
  {
    return coInfo;
  }

  private static class AirMoveBonus extends CommanderAbility
  {
    private int power = 1;

    AirMoveBonus(Commander commander, String name, int cost, int buff)
    {
      super(commander, name, cost);
      power = buff;
    }

    @Override
    protected void perform(GameMap gameMap)
    {
      COMovementModifier airMoveMod = new COMovementModifier(power);
      for( UnitModel um : myCommander.unitModels )
      {
        if( um.chassis == ChassisEnum.AIR_HIGH ||  um.chassis == ChassisEnum.AIR_LOW)
        {
          airMoveMod.addApplicableUnitModel(um);
        }
      }
      myCommander.addCOModifier(airMoveMod);
    }
  }
}