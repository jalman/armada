package joshbot.soldiers;

import static joshbot.utils.Utils.*;
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
	 * If the soldier does not have a farawayEnemyTarget, he calls getFarAwayEnemyTarget to obtain one. If he receives one, he should
	 * immediately enter micro mode from whichever task he was previously performing.
	 * @throws GameActionException
	 */
	public static void updateSoldierUtils() throws GameActionException{
  }

	public static void getFarAwayEnemyTarget() throws GameActionException{
		detectenemiesFarAway();
		farawayEnemyTarget = getHighestPriority(enemiesFarAway);
		farawayTargetSet = true;
	}
	/**
	 * Obtains target and enemyWeight with one traversal of the enemyRobots array. This is cheaper bytecode-wise.
	 * NOTE: If setEnemyTargetAndWeight, also update enemyWeight
	 * @throws GameActionException
	 */
	public static void setEnemyTargetAndWeight() throws GameActionException{
		int priority = 0;
		enemyWeight = 0;
		enemyTarget = null;
		enemyTargetRobotInfo = null;
		for (Robot enemy : enemyRobots)
		{
			tempRobotInfo = RC.senseRobotInfo(enemy);
			enemyWeight+=findEnemyWeight(tempRobotInfo);

			//updates enemyTarget
			if(overallPriority(tempRobotInfo) > priority)
			{
				enemyTargetRobotInfo = tempRobotInfo;
				priority = overallPriority(tempRobotInfo);
			}
			enemyTarget = enemyTargetRobotInfo.location;
		}
	}
	/**
	 * @param RobotInfo of a robot
	 * @return the weight its given (total weight decides whether to charge in or not)
	 * @throws GameActionException
	 */
	private static int findEnemyWeight(RobotInfo r) throws GameActionException{
		if (r.type == RobotType.SOLDIER)
		{
			//Full health soldier returns 16.
      return Math.max(0, 10 - (int) (1.5 * r.actionDelay) +
					(int)(robotHealthPercent(r)*10)-naiveDistance(currentLocation, r.location));
		}
		return 0;
	}

	private static int findAllyWeight(RobotInfo r) throws GameActionException
	{
		if (r.type == RobotType.SOLDIER)
		{
      return Math.max(0, 10 - (int) (1.5 * r.actionDelay) + (int) (robotHealthPercent(r) * 10)
          - naiveDistance(currentLocation, r.location));
		}
		return 25;
	}

	/**
	 * Finds enemy target within the micro radius with highest priority.
	 * @throws GameActionException
	 */
	public static MapLocation setEnemyTarget() throws GameActionException{
		int priority = -10000;
		enemyTarget = null;
		enemyTargetRobotInfo = null;
		for (Robot enemy : enemyRobots)
		{
			tempRobotInfo = RC.senseRobotInfo(enemy);
			if(overallPriority(tempRobotInfo) > priority)
			{
				enemyTargetRobotInfo = tempRobotInfo;
				priority = overallPriority(tempRobotInfo);
			}
			enemyTarget = enemyTargetRobotInfo.location;
		}
		return enemyTarget;
	}

	/**
	 * Checks a maximum of numberOfTargetsToCheck when searching for an enemy target; saves Bytecode.
	 * @param numberOfTargetsToCheck
	 * @return
	 * @throws GameActionException
	 */
	public static void setEnemyTarget(int numberOfTargetsToCheck) throws GameActionException{
		int priority = -10000;
		enemyTarget = null;
		enemyTargetRobotInfo = null;
		for (int i = 0; i < enemyRobots.length && i < numberOfTargetsToCheck; i++)
		{
			tempRobotInfo = RC.senseRobotInfo(enemyRobots[i]);
			if(overallPriority(tempRobotInfo) > priority)
			{
				enemyTargetRobotInfo = tempRobotInfo;
				priority = overallPriority(tempRobotInfo);
			}
			enemyTarget = enemyTargetRobotInfo.location;
		}
	}

	/**
	 * Returns the priority that an enemy robot has, based on distance, health of enemy, and type of enemy.
	 *
	 * TODO: The priorities right now is random.
	 * @param RobotInfo r
	 * @return
	 * @throws GameActionException
	 */
	public static int overallPriority(RobotInfo r) throws GameActionException
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
	 * Determines whether there are enough allies to engage enemies to engage in an area.
	 * NOTE: If changes are made to setEnemyTargetAndWeight, also update enemyWeight
	 * @param radiusSquared
	 * @return weight; higher weight = more enemies
	 * @throws GameActionException
	 */
	public static int setEnemyWeight(MapLocation m, int radiusSquared) throws GameActionException
	{
		enemyWeight = 0;
		Robot[] enemies = RC.senseNearbyGameObjects(Robot.class, m, radiusSquared, ENEMY_TEAM);
		for(Robot enemy : enemies) {
			tempRobotInfo = RC.senseRobotInfo(enemy);
			enemyWeight += findEnemyWeight(tempRobotInfo);
		}
		return enemyWeight;
	}

	public static int setAllyWeight(MapLocation m, int radiusSquared) throws GameActionException
	{
		//if we scan from currentLocaiton
    allyWeight = 4 + (int) ((RC.senseRobotInfo(RC.getRobot()).health) / MAX_SOLDIER_HEALTH * 12);
		RobotInfo r;
		Robot[] allies = RC.senseNearbyGameObjects(Robot.class, m, radiusSquared, ALLY_TEAM);
		for(Robot ally : allies) {
			tempRobotInfo = RC.senseRobotInfo(ally);
			allyWeight += findAllyWeight(tempRobotInfo);
		}
		return allyWeight;
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
	 * When the soldier sees a faraway Enemy, he updates his target as he gets closer. This method should be called
	 * exclusively in micro.
	 * @param k = the number of turns in between updates to the
	 * @throws GameActionException
	 */
	public static void updateFarawayEnemyTarget(int k) throws GameActionException
	{
		/*if ((Clock.getRoundNum()+RC.getRobot().getID()) % k == 0)
		{*/
			enemiesFarAway = RC.senseNearbyGameObjects(Robot.class, farawayEnemyTarget, 2*k*k, ENEMY_TEAM);
			if (enemiesFarAway.length > 0)
				farawayEnemyTarget = RC.senseRobotInfo(enemiesFarAway[0]).location;
			//getHighestPriority(enemiesFarAway);
		//}
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
    int maxPriority = overallPriority(targetInfo);

    for (int i = 1; i < maxNumberOfEnemiesToCheckToFindATarget && i < arr.length; i++)
		{
			tempRobotInfo = RC.senseRobotInfo(arr[i]);
      int priority = overallPriority(tempRobotInfo);
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
