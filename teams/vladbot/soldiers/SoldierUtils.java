package vladbot.soldiers;

import static vladbot.utils.Utils.*;
import battlecode.common.*;

public class SoldierUtils {

  public final static double MAX_SOLDIER_HEALTH = RobotType.SOLDIER.maxHealth;
  public final static double MAX_NOISE_TOWER_HEALTH = RobotType.NOISETOWER.maxHealth;
  public final static double MAX_PASTR_HEALTH = RobotType.PASTR.maxHealth;

	public static int sensorRadius = ENEMY_RADIUS2;
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
	 * TODO: The priorities right now is random.
	 * @param RobotInfo r
	 * @return
	 * @throws GameActionException
	 */
	public static int priority(RobotInfo r) throws GameActionException
	{
    if (!inRange(r.location)) {
      return -100;
    }

    int distanceSquared = currentLocation.distanceSquaredTo(r.location);
    int cows = (int) RC.senseCowsAtLocation(r.location);
		double healthPercent = robotHealthPercent(r);
		int priority = robotTypePriority(r);
		int roundsUntilActive = 0;
    if (r.type == RobotType.SOLDIER) {
      roundsUntilActive = (int) r.actionDelay;
		}

    return (200 - (int) (healthPercent * 20) + 100 / distanceSquared + priority + cows / 20 + (5 * roundsUntilActive));
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
	 * @param array of (enemy) robots
	 * @return robot with highest priority
	 * @throws GameActionException
	 */
  public static MapLocation getHighestPriority(Robot[] arr) throws GameActionException
	{
    if (arr.length == 0) return null;

    RobotInfo targetInfo = RC.senseRobotInfo(arr[0]);
    int maxPriority = priority(targetInfo);

    for (int i = 1; i < maxNumberOfEnemiesToCheckToFindATarget && i < arr.length; i++)
		{
			tempRobotInfo = RC.senseRobotInfo(arr[i]);
      int priority = priority(tempRobotInfo);
      if (priority > maxPriority) {
				targetInfo = tempRobotInfo;
        maxPriority = priority;
			}
		}
    if (targetInfo == null) {
			return null;
    }

    // RC.setIndicatorString(1, "target: " + targetInfo.location + ", priority: " + maxPriority);
		return targetInfo.location;
	}
}
