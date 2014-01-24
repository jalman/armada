package mergebot.soldiers;

import static mergebot.soldiers.SoldierUtils.*;
import static mergebot.utils.Utils.*;
import mergebot.messaging.MessagingSystem.ReservedMessageType;
import mergebot.nav.*;
import battlecode.common.*;

public class NathanMicro {

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  public static final int HELP_CHANNEL = ReservedMessageType.HELP_CHANNEL.channel();
  public static final int GREAT_LUGE_ASSIST = 60;

  public static boolean GREAT_LUGE = false;

  public static boolean luge(Mover mover) throws GameActionException {
    RobotInfo[] nearbyEnemies = getEnemyRobotInfo();
    Robot[] enemiesInRange = RC.senseNearbyGameObjects(Robot.class, 10, ENEMY_TEAM);

    int nearbyEnemySoldiers = 0;
    for (RobotInfo info : nearbyEnemies) {
      if (info.type == RobotType.SOLDIER) {
        nearbyEnemySoldiers++;
      }
    }

    /*if (nearbyEnemySoldiers == 0) { // no enemies: don't micro
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
      return false;*/
    if (RC.isActive()) {
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
              allyWeight += (ri.health/2); // change me later
            }
            else {
              //allyWeight += Math.max(0, ri.health - 40);
            }
            break;
          default:
            break;
        }
      }
      // find enemy weight
      for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
        ri = nearbyEnemies[i];
        switch (ri.type) {
          case HQ:
            if (ri.location.distanceSquaredTo(currentLocation) <= 25) {
              enemyWeight += 1000;
            }
            break;
          case SOLDIER:
            if (currentLocation.distanceSquaredTo(ri.location) <= 17) enemyWeight += ri.health / 2;
          default:
            break;
        }
      }
      for (int i = enemiesInRange.length - 1; i >= 0; --i) {
        ri = RC.senseRobotInfo(enemiesInRange[i]);
        switch (ri.type) {
          case SOLDIER:
            enemyWeight += ri.health / 2; // 10 / (currentLocation.distanceSquaredTo(ri.location));
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
            //enemyWeight += 1000;
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
          isHelpingOut = false;
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
          MapLocation target = getHighestPriority(nearbyEnemies);

          RC.setIndicatorString(1, "..." + isHelpingOut + "," + Clock.getRoundNum());
          if (!isHelpingOut && (nearbyEnemies.length == 0 || (enemiesInRange.length == 0 && allyWeight >= enemyWeight + 75))) {
            // willing to move forward and attack!
            RC.setIndicatorString(2, "not microing");
            if (target != null) {
              mover.setTarget(target); // jurgz should take a look at this ...
              mover.move();
              return true;
            } else {
              return false;
            }
          }
          else if (isHelpingOut && enemiesInRange.length == 0) { //change me eventually
            RC.setIndicatorString(2, "helping" + "," + RC.canSenseSquare(m) + "," + m.x + "," + m.y);
            Direction newDir = currentLocation.directionTo(m);
            if (RC.isActive() && newDir != Direction.NONE && newDir != Direction.OMNI) {
              if (RC.canMove(newDir)) {
                mover.setTarget(currentLocation.add(newDir, 3));
                mover.move();
                return true;
              }
              else if (RC.canMove(newDir.rotateLeft())) {
                mover.setTarget(currentLocation.add(newDir.rotateLeft(), 3));
                mover.move();
                return true;
              }
              else if (RC.canMove(newDir.rotateRight())) {
                mover.setTarget(currentLocation.add(newDir.rotateRight(), 3));
                mover.move();
                return true;
              }
            }
          }

          if (target != null)
            RC.setIndicatorString(2, "" + target.x + "," + target.y + "," + Clock.getRoundNum());
          else
            RC.setIndicatorString(2, "null target" + "," + Clock.getRoundNum());

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
            /*else if (currentLocation.distanceSquaredTo(ENEMY_HQ) <= 100) {
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
            }*/
          } else if (RC.canAttackSquare(target)) {
            int d = 1;
            double dd = currentLocation.distanceSquaredTo(target) - 0.5;
            if (dd > 1 && dd <= 4) d = 2;
            if (dd > 4 && dd <= 9) d = 3;
            if (dd > 9) d = 4;

            int maxDmg = (int) (enemiesInRange.length * (d - 1) * RobotType.SOLDIER.attackPower);
            RC.setIndicatorString(2, maxDmg + "," + RC.getHealth() + "," + allyWeight + "," + enemyWeight + " | " + GREAT_LUGE + "," + target.x + "," + target.y + " - round " + Clock.getRoundNum());
            if (RC.getHealth() > maxDmg + 5 && allyWeight < enemyWeight - GREAT_LUGE_ASSIST) {
              // TEMPORARY CHANGE ME LATER
              GREAT_LUGE = true;
            }
            if (GREAT_LUGE && RC.isActive()) {
              if (d <= 1.)
                RC.selfDestruct();
              else if (RC.canMove(currentLocation.directionTo(target))) {
                RC.move(currentLocation.directionTo(target));
              } else {
                System.out.println("great luge attack");
                RC.attackSquare(target);
              }
              return true;
            } else {
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
            mover.setTarget(currentLocation.add(newDir, 3));
            mover.move();
          }
          else if (RC.canMove(newDir.rotateLeft())) {
            mover.setTarget(currentLocation.add(newDir.rotateLeft(), 3));
            mover.move();
          }
          else if (RC.canMove(newDir.rotateRight())) {
            mover.setTarget(currentLocation.add(newDir.rotateRight(), 3));
            mover.move();
          }
        }
      }
    }

    return true;
  }
}
