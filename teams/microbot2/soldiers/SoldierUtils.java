package microbot2.soldiers;

import static mergebot.utils.Utils.*;
import battlecode.common.*;

public class SoldierUtils {

  public final static double MAX_SOLDIER_HEALTH = RobotType.SOLDIER.maxHealth;
  public final static double MAX_NOISE_TOWER_HEALTH = RobotType.NOISETOWER.maxHealth;
  public final static double MAX_PASTR_HEALTH = RobotType.PASTR.maxHealth;

  public static int closeEnoughToGoToBattleSquared = 64;
  public static Robot[] enemiesFarAway; // enemies within closeEnoughToGoToBattle of a soldier. Only used to find farawayEnemyTarget
  public static final int maxNumberOfEnemiesToCheckToFindATarget = 9;

  static RobotInfo tempRobotInfo;

  public static MapLocation enemyTarget;
  public static RobotInfo enemyTargetRobotInfo;
  public static int enemyWeight;
  public static int allyWeight;

  public static boolean farawayTargetSet = false;
  public static MapLocation farawayEnemyTarget = null; // the maplocation of a high priority bot in enemiesFarAway
  //	public static int farawayEnemyTargetAge = 1000;


  /**
   * Returns the priority that an enemy robot has, based on distance, health of enemy, and type of enemy.
   *
   * TODO: The priority system right now is arbitrary.
   * @param RobotInfo r
   * @return
   * @throws GameActionException
   */
  public static int getPriority(RobotInfo r) throws GameActionException
  {
    if (!inRange(r.location)) {
      return -100;
    } else if (r.type == RobotType.HQ) {
      return -10000;
    }

    int distance = naiveDistance(currentLocation, r.location);
    // int cows = (int) RC.senseCowsAtLocation(r.location);
    double healthPercent = robotHealthPercent(r);
    int priority = robotTypePriority(r);
    int roundsUntilActive = 0;
    if (r.type == RobotType.SOLDIER) {
      roundsUntilActive = (int) r.actionDelay;
    }

    return (5000 - (int) (healthPercent * 50) + 50 / distance + priority - (5 * roundsUntilActive));
  }

  //Helper methods for overallPriority
  private static int robotTypePriority(RobotInfo r) throws GameActionException{
    switch (r.type) {
      case SOLDIER:
        return 20;
      case PASTR:
        return 100;
      case NOISETOWER:
        return 0;
      case HQ:
        return -1000;
      default:
        return -30;
    }
  }

  public static boolean inRange(MapLocation loc) {
    return currentLocation.distanceSquaredTo(loc) <= RobotType.SOLDIER.attackRadiusMaxSquared;
  }

  private static double robotHealthPercent(RobotInfo r) throws GameActionException{
    switch (r.type) {
      case SOLDIER:
        return r.health / MAX_SOLDIER_HEALTH;
      case NOISETOWER:
        return r.health / MAX_NOISE_TOWER_HEALTH;
      case PASTR:
        return r.health / MAX_PASTR_HEALTH;
      default:
        return 1;
    }
  }

  /**
   * Finds an enemy to go to battle with;
   * If sees enemy within radius squared of 81, then go to battle;
   */
  private static void detectenemiesFarAway()
  {
    enemiesFarAway = RC.senseNearbyGameObjects(Robot.class, currentLocation, closeEnoughToGoToBattleSquared, ENEMY_TEAM);
  }

  /**
   * Get location with most cows in array locs.
   * @param locs: array of MapLocations
   * @return location with most cows in locs[].
   * @throws GameActionException
   */
  public static MapLocation getMostCowsLoc(MapLocation[] locs, int threshold)
      throws GameActionException {
    if (locs.length == 0) return null;

    int targetInfoIndex = -1;
    double maxCows = 0, cows;

    for (int i = locs.length - 1; i >= 0; i--) {
      if (RC.canSenseSquare(locs[i])) {
        cows = RC.senseCowsAtLocation(locs[i]);
        if (cows > maxCows) {
          targetInfoIndex = i;
          maxCows = cows;
        }
      }
    }

    if (targetInfoIndex < 0 || maxCows < threshold) {
      return null;
    } else {
      return locs[targetInfoIndex];
    }
  }

  /**
   * @param array of (enemy) robot infos
   * @return robot info with highest priority
   * @throws GameActionException
   */
  public static MapLocation getHighestPriority(RobotInfo[] arr) throws GameActionException
  {
    if (arr.length == 0) return null;

    int targetInfoIndex = -1;
    int maxPriority = -100, priority;

    for (int i = arr.length - 1; i >= 0; i--) {
      priority = getPriority(arr[i]);
      if (priority < 0) {
        continue;
      } else if (priority > maxPriority) {
        targetInfoIndex = i;
        maxPriority = priority;
      }
    }

    if (targetInfoIndex < 0) {
      return null;
    }
    return arr[targetInfoIndex].location;
  }
}
