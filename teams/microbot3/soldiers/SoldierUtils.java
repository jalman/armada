package microbot3.soldiers;

import static microbot3.utils.Utils.*;
import battlecode.common.*;

public class SoldierUtils {

  public final static double MAX_SOLDIER_HEALTH = RobotType.SOLDIER.maxHealth;
  public final static double MAX_NOISE_TOWER_HEALTH = RobotType.NOISETOWER.maxHealth;
  public final static double MAX_PASTR_HEALTH = RobotType.PASTR.maxHealth;

  public static int sensorRadius = SENSOR_RADIUS2;
  public static int closeEnoughToGoToBattleSquared = 64;
  public static Robot[] enemiesFarAway; // enemies within closeEnoughToGoToBattle of a soldier. Only
  // used to find farawayEnemyTarget
  public static final int maxNumberOfEnemiesToCheckToFindATarget = 9;

  static RobotInfo tempRobotInfo;

  public static MapLocation enemyTarget;
  public static RobotInfo enemyTargetRobotInfo;
  public static int enemyWeight;
  public static int allyWeight;

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  public static final int HELP_CHANNEL = 65500;

  // micro
  public static boolean luge() throws GameActionException {
    Robot[] nearbyEnemies = RC.senseNearbyGameObjects(Robot.class, 35, ENEMY_TEAM);
    Robot[] enemiesInRange = RC.senseNearbyGameObjects(Robot.class, 10, ENEMY_TEAM);

    // sense every round -- maybe send messages on non-active rounds?

    if (nearbyEnemies.length == 0) { // no enemies: don't micro
      if (RC.isActive() && currentLocation.distanceSquaredTo(ENEMY_HQ) <= 100) {
        MapLocation cowTarget =
            getMostCowsLoc(
                MapLocation.getAllMapLocationsWithinRadiusSq(
                    currentLocation.add(currentLocation.directionTo(ENEMY_HQ)), 5),
                //
                500);
        if (cowTarget != null && RC.canAttackSquare(cowTarget)
            && RC.senseObjectAtLocation(cowTarget) == null) {
          RC.attackSquare(cowTarget);
          return true;
        }
      }
      return false;
    } else if (RC.isActive()) {
      RC.setIndicatorString(2, "luging!");
      Robot[] nearbyTeam = RC.senseNearbyGameObjects(Robot.class, 35, ALLY_TEAM);
      int enemyWeight = 0;

      // get robot infos of enemy
      RobotInfo[] nearbyEnemyInfo = new RobotInfo[nearbyEnemies.length];
      RobotInfo ri;

      MapLocation nearestPastrLoc = null;
      int nearestPastrDistance = 1000000;

      // find enemy weight
      for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
        ri = RC.senseRobotInfo(nearbyEnemies[i]);
        switch (ri.type) {
          case SOLDIER:
            enemyWeight += 1; // 10 / (currentLocation.distanceSquaredTo(ri.location));
            // weight by how close enemy soldier is?
            break;
          case PASTR:
            enemyWeight -= 1;
            int d = currentLocation.distanceSquaredTo(ri.location);
            if (d < nearestPastrDistance) {
              nearestPastrDistance = d;
              nearestPastrLoc = ri.location;
            }
            break;
          case NOISETOWER:
            break;
          case HQ:
            enemyWeight += 1000;
            break;
          default:
            break;
        }
        nearbyEnemyInfo[i] = ri;
      }
      RC.setIndicatorString(1, "" + Clock.getRoundNum() + "," + enemyWeight);

      int help_message = RC.readBroadcast(HELP_CHANNEL);
      int callX = help_message / 256, callY = help_message % 256;
      boolean isHelpingOut = false;

      if (help_message > 0) {
        MapLocation callLoc = new MapLocation(callX, callY);
        if (RC.canSenseSquare(callLoc) && RC.senseObjectAtLocation(callLoc) == null) {
          RC.broadcast(65513, -1);
        }

        if ((callX - currentLocation.x) * (callX - currentLocation.x) + (callY - currentLocation.y)
            * (callY - currentLocation.y) < 8 * 8) {
          isHelpingOut = true;
        }
      }

      if (nearbyTeam.length + 1 >= enemyWeight || isHelpingOut) {
        if (isHelpingOut) {
          RC.setIndicatorString(2, "helping out to kill guy at " + callX + "," + callY);
        }
        if (RC.isActive()) { // willing to attack!
          if ((nearbyEnemies.length == 0 || nearbyTeam.length - 1 >= enemyWeight || isHelpingOut)
              && enemiesInRange.length == 0) {
            // willing to move forward and attack!
            return false; // jurgz should take a look at this ...
          }

          MapLocation target = getHighestPriority(nearbyEnemyInfo);
          if (target == null) {
            if (nearestPastrLoc != null) {
              // PASTR_RANGE = 5
              MapLocation cowTarget =
                  getMostCowsLoc(MapLocation.getAllMapLocationsWithinRadiusSq(nearestPastrLoc, 5),
                      500);
              if (cowTarget != null && RC.canAttackSquare(cowTarget)) {
                RC.attackSquare(cowTarget);
              }
            }
            else if (currentLocation.distanceSquaredTo(ENEMY_HQ) <= 100) {
              // copy-pasted from above.
              // if we're near the HQ but have nothing to do just randomly kill shit
              MapLocation cowTarget =
                  getMostCowsLoc(
                      MapLocation.getAllMapLocationsWithinRadiusSq(
                          currentLocation.add(currentLocation.directionTo(ENEMY_HQ)), 5),
                      //
                      500);
              if (cowTarget != null && RC.canAttackSquare(cowTarget)
                  && RC.senseObjectAtLocation(cowTarget) == null) {
                RC.attackSquare(cowTarget);
                return true;
              }
            }
          } else if (RC.canAttackSquare(target)) {
            RC.attackSquare(target);
            if (callX != target.x || callY != target.y) {
              RC.broadcast(HELP_CHANNEL, 256 * target.x + target.y);
            }
          }
        }
      } else {
        int dx = 0, dy = 0;
        for (int i = nearbyEnemyInfo.length - 1; i >= 0; --i) {
          dx += nearbyEnemyInfo[i].location.x;
          dy += nearbyEnemyInfo[i].location.y;
        }
        dx /= nearbyEnemies.length;
        dy /= nearbyEnemies.length;

        Direction newDir =
            currentLocation.directionTo(new MapLocation(2 * curX - dx, 2 * curY - dy));

        if (RC.isActive() && newDir != Direction.NONE && newDir != Direction.OMNI) {
          if (RC.canMove(newDir)) {
            RC.move(newDir);
          }
          else if (RC.canMove(newDir.rotateLeft())) {
            RC.move(newDir.rotateLeft());
          }
          else if (RC.canMove(newDir.rotateRight())) {
            RC.move(newDir.rotateRight());
          }
        }
      }
    }

    return true;
  }

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

  // Helper method to get "weight" of enemy robots (determining whether to move toward/away)
  // private static int enemyRobotWeight(RobotType r) {
  // switch (r) {
  // case SOLDIER:
  // return 1;
  // case PASTR:
  // return -1;
  // case NOISETOWER:
  // return 0;
  // case HQ:
  // return 1000;
  // default:
  // return 0;
  // }
  // }

  // Helper methods for overallPriority
  private static int robotTypePriority(RobotInfo r) {
    switch (r.type) {
      case SOLDIER:
        return 20;
      case PASTR:
        return 50;
      case NOISETOWER:
        return 0;
      default:
        return -1000000;
    }
  }

  private static double robotHealthPercent(RobotInfo r) {
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

  public static boolean inRange(MapLocation loc) {
    return currentLocation.distanceSquaredTo(loc) <= RobotType.SOLDIER.attackRadiusMaxSquared;
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
