package microbot3.soldiers;

import static microbot3.soldiers.SoldierUtils.*;
import static microbot3.utils.Utils.*;
import microbot3.messaging.MessagingSystem.ReservedMessageType;
import battlecode.common.*;

public class NathanMicro {

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  public static final int HELP_CHANNEL = ReservedMessageType.HELP_CHANNEL.channel();
  public static final int GREAT_LUGE_ASSIST = 60;

  public static boolean GREAT_LUGE = false;

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
      float allyWeight = 0, enemyWeight = 0;

      // get robot infos of enemy
      RobotInfo[] nearbyEnemyInfo = new RobotInfo[enemiesInRange.length];
      RobotInfo ri;

      MapLocation nearestPastrLoc = null;
      int nearestPastrDistance = 1000000;

      // find ally weight
      allyWeight += RC.getHealth();
      for (int i = 0; i < nearbyTeam.length; ++i) {
        ri = RC.senseRobotInfo(nearbyTeam[i]);

        switch (ri.type){
          case SOLDIER:
            Robot[] stuff = RC.senseNearbyGameObjects(Robot.class, ri.location, 17, ENEMY_TEAM);
            boolean inCombat = false;
            for (int j=0; j<stuff.length; ++j) {
              if (ri.location.distanceSquaredTo(RC.senseRobotInfo(stuff[j]).location) <= 10) {
                inCombat = true;
              }
            }
            if (inCombat) {
              allyWeight += ri.health;
            }
            else if (stuff.length > 0) {
              allyWeight += ri.health / 2; // change me later
            }
            break;
          default:
            break;
        }
      }
      // find enemy weight
      for (int i = enemiesInRange.length - 1; i >= 0; --i) {
        ri = RC.senseRobotInfo(enemiesInRange[i]);
        switch (ri.type) {
          case SOLDIER:
            enemyWeight += ri.health; // 10 / (currentLocation.distanceSquaredTo(ri.location));
            // weight by how close enemy soldier is?
            break;
          case PASTR:
            //enemyWeight -= 1;
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

      int help_message = RC.readBroadcast(HELP_CHANNEL);
      int callX = help_message / 256, callY = help_message % 256;
      boolean isHelpingOut = false;

      if (help_message > 0) {
        MapLocation callLoc = new MapLocation(callX, callY);
        if (RC.canSenseSquare(callLoc) && RC.senseObjectAtLocation(callLoc) == null) {
          RC.broadcast(HELP_CHANNEL, -1);
        }

        if ((callX - currentLocation.x) * (callX - currentLocation.x) + (callY - currentLocation.y)
            * (callY - currentLocation.y) < 8 * 8
            && (callX != currentLocation.x || callY != currentLocation.y)) {
          isHelpingOut = true;
        }
      }
      RC.setIndicatorString(0, "ally " + allyWeight + " / enemy " + enemyWeight);
      if (allyWeight >= enemyWeight - 25 || GREAT_LUGE) {
        if (isHelpingOut) {
          RC.setIndicatorString(2, "helping out to kill guy at " + callX + "," + callY);
        }
        else {
          RC.setIndicatorString(2, "");
        }
        if (RC.isActive()) { // willing to attack!
          if (nearbyEnemies.length == 0) {
            // willing to move forward and attack!
            return false; // jurgz should take a look at this ...
          }
          else if (isHelpingOut && enemiesInRange.length == 0) { //change me eventually
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

            if (RC.getHealth() > maxDmg + 5 && RC.getHealth() < maxDmg + 40 && allyWeight < enemyWeight + GREAT_LUGE_ASSIST) {
              // TEMPORARY CHANGE ME LATER
              GREAT_LUGE = true;
            }
            if (GREAT_LUGE) {
              if (RC.isActive()) {
                if (d <= 1.)
                  RC.selfDestruct();
                else if (RC.canMove(currentLocation.directionTo(target))) {
                  RC.move(currentLocation.directionTo(target));
                }
                else {
                  RC.attackSquare(target);
                }
              }
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
        dx /= nearbyEnemyInfo.length;
        dy /= nearbyEnemyInfo.length;

        Direction newDir =
            currentLocation.directionTo(new MapLocation(2 * curX - dx, 2 * curY - dy));

        RC.setIndicatorString(2, "" + curX + "," + dx + "," + curY + "," + dy + " / " + "running " + newDir.dx + "," + newDir.dy);
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
