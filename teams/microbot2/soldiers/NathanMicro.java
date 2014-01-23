package microbot2.soldiers;

import static microbot2.soldiers.SoldierUtils.*;
import static microbot2.utils.Utils.*;
import microbot2.messaging.MessagingSystem.ReservedMessage;
import battlecode.common.*;

public class NathanMicro {

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  public static final int HELP_CHANNEL = ReservedMessage.HELP_CHANNEL.channel;

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
            * (callY - currentLocation.y) < 8 * 8
            && (callX != currentLocation.x || callY != currentLocation.y)) {
          isHelpingOut = true;
        }
      }

      if (nearbyTeam.length + 1 >= enemyWeight
          || (nearbyTeam.length - 1 >= enemyWeight && isHelpingOut)) {
        if (isHelpingOut) {
          RC.setIndicatorString(2, "helping out to kill guy at " + callX + "," + callY);
        }
        if (RC.isActive()) { // willing to attack!
          if (nearbyEnemies.length == 0) {
            // willing to move forward and attack!
            return false; // jurgz should take a look at this ...
          }
          else if (isHelpingOut && nearbyTeam.length - 1 >= enemyWeight
              && enemiesInRange.length == 0) {
            Direction newDir = currentLocation.directionTo(new MapLocation(callX, callY));
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
            int d = 1;
            double dd = currentLocation.distanceSquaredTo(target) - 0.5;
            if (dd > 1 && dd <= 4) d = 2;
            if (dd > 4 && dd <= 9) d = 3;

            int maxDmg = (int) (enemiesInRange.length * (d - 1) * RobotType.SOLDIER.attackPower);
            RC.setIndicatorString(1, "" + enemiesInRange.length + "," + d + ","
                + RobotType.SOLDIER.attackPower + " - " + target.x + "/" + target.y);

            if (RC.getHealth() > maxDmg + 5 && RC.getHealth() < maxDmg + 40) {
              // TEMPORARY CHANGE ME LATER
              if (dd <= 1.)
                RC.selfDestruct();
              else
                RC.move(currentLocation.directionTo(target));
            }
            else {
              RC.attackSquare(target);
              if (callX != target.x || callY != target.y) {
                RC.broadcast(HELP_CHANNEL, 256 * target.x + target.y);
              }
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
}
