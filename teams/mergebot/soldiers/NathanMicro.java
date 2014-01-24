package mergebot.soldiers;

import static mergebot.soldiers.SoldierUtils.*;
import static mergebot.utils.Utils.*;
import mergebot.messaging.MessagingSystem.ReservedMessageType;
import battlecode.common.*;

public class NathanMicro {

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  public static final int HELP_CHANNEL = ReservedMessageType.HELP_CHANNEL.channel();
  public static final int GREAT_LUGE_ASSIST = 30;

  public static boolean GREAT_LUGE = false;

  public static boolean luge() throws GameActionException {
    RobotInfo[] nearbyEnemies = getEnemyRobotInfo();
    Robot[] enemiesInRange = RC.senseNearbyGameObjects(Robot.class, 10, ENEMY_TEAM);

    int nearbyEnemySoldiers = 0;
    for (RobotInfo info : nearbyEnemies) {
      if (info.type == RobotType.SOLDIER) {
        nearbyEnemySoldiers++;
      }
    }

    if (nearbyEnemySoldiers == 0) { // no enemies: don't micro
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
      for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
        ri = RC.senseRobotInfo(nearbyEnemies[i]);
        switch (ri.type) {
          case HQ:
            enemyWeight += 1000;
            break;
          default:
            break;
        }
      }
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
      }
      boolean isHelpingOut = false;
      MapLocation m = new MapLocation(0, 0);

      for (int i=0; i<SoldierBehavior.microLocations.size; ++i) {
        m = SoldierBehavior.microLocations.get(i);

        if (currentLocation.distanceSquaredTo(m) <= 8*8) {
          isHelpingOut = true;
          break;
        }
      }

      if (isHelpingOut) {
        if (RC.canSenseSquare(m) && RC.senseObjectAtLocation(m) == null) {
          //RC.broadcast(HELP_CHANNEL, -1);
        }
      }
      RC.setIndicatorString(1, "in range " + enemiesInRange.length + " | " + "ally " + allyWeight + " / enemy " + enemyWeight + " (turn " + Clock.getRoundNum() + ")");
      if (allyWeight >= enemyWeight - 25 || GREAT_LUGE) {
        if (isHelpingOut) {
          RC.setIndicatorString(2, "helping out to kill guy at " + m.x + "," + m.y);
        }
        else {
          String ss = "";
          for (int i=0; i<SoldierBehavior.microLocations.size; ++i) {
            MapLocation k = SoldierBehavior.microLocations.get(i);
            ss += "(" + k.x + "," + k.y + "),";
          }
          RC.setIndicatorString(2, ss);
        }
        if (RC.isActive()) { // willing to attack!
          if (!isHelpingOut && (nearbyEnemies.length == 0 || (enemiesInRange.length == 0 && allyWeight >= enemyWeight + 75))) {
            // willing to move forward and attack!
            return false; // jurgz should take a look at this ...
          }
          else if (isHelpingOut && enemiesInRange.length == 0) { //change me eventually
            Direction newDir = currentLocation.directionTo(m);
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

          MapLocation target = getHighestPriority(nearbyEnemies);
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
              if (RC.isActive() && cowTarget != null && RC.canAttackSquare(cowTarget)
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
            if (dd > 9) d = 4;

            int maxDmg = (int) (enemiesInRange.length * (d - 1) * RobotType.SOLDIER.attackPower);

            if (RC.getHealth() > maxDmg + 5 && RC.getHealth() < maxDmg + 40 && allyWeight < enemyWeight - GREAT_LUGE_ASSIST) {
              // TEMPORARY CHANGE ME LATER
              GREAT_LUGE = true;
            }
            if (GREAT_LUGE && RC.isActive()) {
              if (d <= 1.)
                RC.selfDestruct();
              else if (RC.canMove(currentLocation.directionTo(target))) {
                RC.move(currentLocation.directionTo(target));
              }
              else {
                RC.attackSquare(target);
              }
            }
            else {
              RC.attackSquare(target);
              if (m.x != target.x || m.y != target.y) {
                messagingSystem.writeMicroMessage(target, 1);
              }
            }
          }
        }
      } else {
        int dx = 0, dy = 0;
        for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
          dx += nearbyEnemies[i].location.x;
          dy += nearbyEnemies[i].location.y;
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
