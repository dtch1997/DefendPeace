package Units;

import java.util.ArrayList;
import java.util.Vector;

import CommandingOfficers.Commander;
import Engine.GameAction;
import Engine.GameActionSet;
import Engine.Path;
import Engine.TurnInitAction;
import Engine.Utils;
import Engine.XYCoord;
import Engine.GameEvents.GameEventQueue;
import Terrain.GameMap;
import Terrain.Location;
import Units.UnitModel.UnitEnum;
import Units.Weapons.Weapon;

public class Unit
{
  public Vector<Unit> heldUnits;
  public UnitModel model;
  public int x;
  public int y;
  public int fuel;
  private int captureProgress;
  private Location captureTarget;
  public Commander CO;
  public boolean isTurnOver;
  private double HP;
  public Weapon[] weapons;
  private ArrayList<TurnInitAction> turnInitActions;

  public Unit(Commander co, UnitModel um)
  {
    CO = co;
    model = um;
    fuel = model.maxFuel;
    isTurnOver = true;
    HP = model.maxHP;
    captureProgress = 0;
    captureTarget = null;
    if( model.weaponModels != null )
    {
      weapons = new Weapon[model.weaponModels.length];
      for( int i = 0; i < model.weaponModels.length; i++ )
      {
        weapons[i] = new Weapon(model.weaponModels[i]);
      }
    }
    if( model.holdingCapacity > 0 )
      heldUnits = new Vector<Unit>(model.holdingCapacity);

    turnInitActions = new ArrayList<TurnInitAction>();
    model.getTurnInitActions(turnInitActions);
  }

  public void initTurn(GameMap map, GameEventQueue events)
  {
    Location locus = map.getLocation(x, y);

    // Only perform turn initialization for the unit if it is on the map.
    //   Units that are e.g. in a transport don't burn fuel, etc.
    if( null != locus )
    {
      isTurnOver = false;
      fuel -= model.idleFuelBurn;
      if( captureTarget != null && captureTarget.getResident() != this )
      {
        captureTarget = null;
        captureProgress = 0;
      }

      // If the unit is not at max health, and is on a repair tile, heal it.
      if( HP < model.maxHP )
      {
        if( model.canRepairOn(locus) && locus.getOwner() == CO )
        {
          int neededHP = Math.min(model.maxHP - getHP(), 2); // will be 0, 1, 2
          double proportionalCost = model.moneyCost / model.maxHP;
          if( CO.money >= neededHP * proportionalCost )
          {
            CO.money -= neededHP * proportionalCost;
            alterHP(2);
          }
          else if( CO.money >= proportionalCost )
          {
            // case will only be used if neededHP is 2
            CO.money -= proportionalCost;
            alterHP(1);
          }
        }
      }

      for( TurnInitAction ta : turnInitActions )
      {
        ta.initTurn(this, map, events);
      }
    } // ~If location is valid.
  }

  /**
   * @return whether or not this unit can attack the given unit type at the
   * specified range, accounting for the possibility of moving first.
   */
  public boolean canAttack(UnitModel targetType, int range, boolean afterMoving)
  {
    // if we have no weapons, we can't hurt things
    if( weapons == null )
      return false;

    boolean canHit = false;
    for( int i = 0; i < weapons.length; i++ )
    {
      if( afterMoving && !weapons[i].model.canFireAfterMoving )
      {
        // If we are planning to move first, and the weapon
        // can't shoot after moving, then move along.
        continue;
      }
      if( weapons[i].getDamage(targetType, range) > 0 )
      {
        canHit = true;
        break;
      }
    }
    return canHit;
  }

  /**
   * Select the weapon owned by this unit that can inflict the
   * most damage against the chosen target
   * @param target
   * @param range
   * @param afterMoving
   * @return The best weapon for that target, or null if no usable weapon exists.
   */
  public Weapon chooseWeapon(UnitModel targetType, int range, boolean afterMoving)
  {
    // if we have no weapons, we can't hurt things
    if( weapons == null )
      return null;

    Weapon chosenWeapon = null;
    double maxDamage = 0;
    for( int i = 0; i < weapons.length; i++ )
    {
      Weapon currentWeapon = weapons[i];
      // If the weapon isn't mobile, we cannot fire if we moved.
      if( afterMoving && !currentWeapon.model.canFireAfterMoving )
      {
        continue;
      }
      double currentDamage = currentWeapon.getDamage(targetType, range);
      if( currentWeapon.getDamage(targetType, range) > maxDamage )
      {
        chosenWeapon = currentWeapon;
        maxDamage = currentDamage;
      }
    }
    return chosenWeapon;
  }

  public int getHP()
  {
    return (int) Math.ceil(HP);
  }
  public double getPreciseHP()
  {
    return HP;
  }

  public void damageHP(double damage)
  {
    HP -= damage;
    if( HP < 0 )
    {
      HP = 0;
    }
  }
  public void alterHP(int change)
  {
    // Change the unit's health, but don't grant more
    // than 10 HP, and don't drop HP to zero.
    HP = Math.max(1, Math.min(10, getHP() + change));
  }

  public void capture(Location target)
  {
    if( !target.isCaptureable() )
    {
      System.out.println("ERROR! Attempting to capture an uncapturable Location!");
      return;
    }
    if( target.getOwner() == CO )
    {
      System.out.println("WARNING! Attempting to capture a property we own!");
      return;
    }

    if( target != captureTarget )
    {
      captureTarget = target;
      captureProgress = 0;
    }
    captureProgress += getHP();
    if( captureProgress >= 20 )
    {
      target.setOwner(CO);
      captureProgress = 0;
      target = null;
    }
  }

  public void stopCapturing()
  {
    captureTarget = null;
    captureProgress = 0;
  }

  public int getCaptureProgress()
  {
    return captureProgress;
  }

  // Removed for the forseeable future; may be back
  /*	public static double getAttackPower(final CombatParameters params) {
  //		double output = model
  //		return [B*ACO/100+R]*(AHP/10)*[(200-(DCO+DTR*DHP))/100] ;
  		return 0;
  	}
  	public static double getDefensePower(Unit unit, boolean isCounter) {
  		return 0;
  	}*/

  /** Compiles and returns a list of all actions this unit could perform on map after moving along movePath. */
  public ArrayList<GameActionSet> getPossibleActions(GameMap map, Path movePath)
  {
    XYCoord moveLocation = new XYCoord(movePath.getEnd().x, movePath.getEnd().y);
    ArrayList<GameActionSet> actionSet = new ArrayList<GameActionSet>();
    if( map.isLocationEmpty(this, moveLocation) )
    {
      for( int i = 0; i < model.possibleActions.length; i++ )
      {
        switch (model.possibleActions[i])
        {
          case ATTACK:
          // Evaluate attack options.
          {
            boolean moved = !moveLocation.equals(x, y);
            ArrayList<GameAction> attackOptions = new ArrayList<GameAction>();
            for( Weapon wpn : weapons )
            {
              // Evaluate this weapon for targets if it has ammo, and if either the weapon
              // is mobile or we don't care if it's mobile (because we aren't moving).
              if( wpn.ammo > 0 && (!moved || wpn.model.canFireAfterMoving) )
              {
                ArrayList<XYCoord> locations = Utils.findTargetsInRange(map, CO, moveLocation, wpn);

                for( XYCoord loc : locations )
                {
                  attackOptions.add(new GameAction.AttackAction(this, movePath, loc));
                }
              }
            } // ~Weapon loop

            // Only add this action set if we actually have a target
            if( !attackOptions.isEmpty() )
            {
              // Bundle our attack options into an action set and add it to our return collection.
              actionSet.add(new GameActionSet(attackOptions));
            }
          } // ~attack options
            break;
          case CAPTURE:
            if( map.getLocation(moveLocation).getOwner() != CO && map.getLocation(moveLocation).isCaptureable() )
            {
              actionSet.add(new GameActionSet(new GameAction.CaptureAction(this, movePath), false));
            }
            break;
          case WAIT:
            actionSet.add(new GameActionSet(new GameAction.WaitAction(this, movePath), false));
            break;
          case LOAD:
            // Don't add - there's no unit there to board.
            break;
          case UNLOAD:
            //            if( heldUnits.size() > 0 )
            //            {
            //              actions.add(GameAction.ActionType.UNLOAD);
            //            }
            break;
          case RESUPPLY:
            // Search for a unit in resupply range.
            // Build an array of each adjacent location.
            //            ArrayList<Location> locations = new ArrayList<Location>();
            //            locations.add(map.getLocation(xLoc, yLoc - 1));
            //            locations.add(map.getLocation(xLoc, yLoc + 1));
            //            locations.add(map.getLocation(xLoc - 1, yLoc));
            //            locations.add(map.getLocation(xLoc + 1, yLoc));
            //
            //            // For each location, see if there is a friendly unit to re-supply.
            //            for( Location loc : locations )
            //            {
            //              Unit other = loc.getResident();
            //              if( other != null && other.CO == CO )
            //              {
            //                actions.add(GameAction.ActionType.RESUPPLY);
            //                break;
            //              }
            //            }
            break;
          default:
            System.out
                .println("getPossibleActions: Invalid action in model's possibleActions[" + i + "]: " + model.possibleActions[i]);
        }
      }
    }
    else
    // There is a unit in the space we are evaluating. Only Load actions are supported in this case.
    {
      //  actions.add(GameAction.ActionType.LOAD);
    }

    return actionSet;
  }

  public boolean hasCargoSpace(UnitEnum type)
  {
    return (model.holdingCapacity > 0 && heldUnits.size() < model.holdingCapacity && model.holdables.contains(type));
  }
}
