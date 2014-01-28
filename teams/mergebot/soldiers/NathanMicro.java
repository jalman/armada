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
  public static final int FIRE_RANGE_SQUARED = RobotType.SOLDIER.attackRadiusMaxSquared;
  public static final int HELP_DURATION = 8;
  public static final float ALLY_WEIGHT_DECAY = 12;

  /**
   * TODO give actual names and/or definitions for these constants so other people know what's going on
   */
  public static boolean GREAT_LUGE = false;
  public static boolean JON_SCHNEIDER = false;

  public static boolean isHelpingOut = false;
  public static MapLocation helpingLoc = new MapLocation(0, 0);
  public static int lastHelpRequest = 0;

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
            else {
              allyWeight += Math.max(0, ri.health - ALLY_WEIGHT_DECAY * lazySqrt(ri.location.distanceSquaredTo(currentLocation)));
            }
            break;
          default:
            break;
        }
      }
      // find enemy weight
      enemyWeight = enemyWeightAboutPoint(currentLocation, nearbyEnemies);

      //find nearest PASTR -- should be moved later
      for (int i = enemiesInRange.length - 1; i >= 0; --i) {
        ri = RC.senseRobotInfo(enemiesInRange[i]);
        switch (ri.type) {
          case PASTR:
            int d = currentLocation.distanceSquaredTo(ri.location);
            if (d < nearestPastrDistance) {
              nearestPastrDistance = d;
              nearestPastrLoc = ri.location;
            }
            break;
          default:
            break;
        }
      }
      MapLocation m = new MapLocation(0, 0);
      String zzz = "";
      for (int i=0; i<SoldierBehavior.microLocations.size; ++i) {
        m = SoldierBehavior.microLocations.get(i);
        zzz += "(" + m.x + "," + m.y + "),";
        if (currentLocation.distanceSquaredTo(m) <= 8*8) {
          isHelpingOut = true;
          helpingLoc = m;
          lastHelpRequest = Clock.getRoundNum();
          break;
        }
      }

      if (isHelpingOut) {
        if ((RC.canSenseSquare(m) && RC.senseObjectAtLocation(m) == null) || Clock.getRoundNum() > lastHelpRequest + HELP_DURATION) {
          isHelpingOut = false;
        }
      }
      zzz += (isHelpingOut ? "HELPING " + locToString(helpingLoc) : "") + "last help: " + lastHelpRequest + " | round: " + Clock.getRoundNum();
      RC.setIndicatorString(1, "in range " + enemiesInRange.length + " | " + "ally " + allyWeight + " / enemy " + enemyWeight + " (turn " + Clock.getRoundNum() + ") | " + zzz + (JON_SCHNEIDER ? " RUNAWAY" : ""));

      if (!JON_SCHNEIDER && (RC.getHealth() <= 10.1 || (RC.getHealth() <= 20.1 && enemiesInRange.length >= 3))) {
        JON_SCHNEIDER = true;
      }
      if (JON_SCHNEIDER && RC.getHealth() >= 50) JON_SCHNEIDER = false;

      if (!JON_SCHNEIDER && (allyWeight >= enemyWeight || GREAT_LUGE)) {
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
          MapLocation nextLoc = new MapLocation(-1, -1);
          float nextWeight = 0;
          if (!isHelpingOut && target == null) {
            Direction newDir = Direction.NONE;
            for (int i=0; i<nearbyEnemies.length; ++i) {
              Direction nd = currentLocation.directionTo(nearbyEnemies[i].location);
              if (currentLocation.add(nd).distanceSquaredTo(nearbyEnemies[i].location) <= FIRE_RANGE_SQUARED) {
                newDir = nd;
                nextLoc = currentLocation.add(newDir);
                nextWeight = enemyWeightAboutPoint(nextLoc, nearbyEnemies);

                break;
              }
            }
          }
          else if (isHelpingOut && target == null) {
            nextLoc = currentLocation.add(currentLocation.directionTo(m));
            nextWeight = enemyWeightAboutPoint(nextLoc, nearbyEnemies);
            //target = getHighestPriority(nextLoc, nearbyEnemies);
          }
          RC.setIndicatorString(2, "///" + "nextWeight: " + nextWeight + " at (" + nextLoc.x + "," + nextLoc.y + "))" );

          if (!isHelpingOut && (nearbyEnemies.length == 0 || (enemiesInRange.length == 0 && allyWeight >= 1.10 * nextWeight))) {
            // willing to move forward and attack!
            String ss = "";
            for (int z=0; z<nearbyEnemies.length; ++z) ss += "," + nearbyEnemies[z].location.x + "," + nearbyEnemies[z].location.y;
            RC.setIndicatorString(2, "///" + "moving forward (enemy weight: " + nextWeight + " at (" + nextLoc.x + "," + nextLoc.y + "))" + (target == null ? " -- ceding control" : locToString(target)) );
            if (target != null) {
              mover.setTarget(target); // jurgz should take a look at this ...
              mover.move();
              return true;
            } else {
              return false;
            }
          }
          else if (isHelpingOut && allyWeight >= 1.10 * nextWeight && target == null) {
            Direction newDir = currentLocation.directionTo(m);
            if (RC.isActive() && newDir != Direction.NONE && newDir != Direction.OMNI) {
              // go straight towards the target point
              RC.setIndicatorString(2, "/// helping out " + locToString(currentLocation.add(newDir)));
              if (RC.canMove(newDir)) {
                mover.setTarget(currentLocation.add(newDir));
                mover.move();
                return true;
              }
              else if (RC.canMove(newDir.rotateLeft())) {
                mover.setTarget(currentLocation.add(newDir.rotateLeft()));
                mover.move();
                return true;
              }
              else if (RC.canMove(newDir.rotateRight())) {
                mover.setTarget(currentLocation.add(newDir.rotateRight()));
                mover.move();
                return true;
              }
            }
          }

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
              //GREAT_LUGE = true;
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
      } else if (nearbyEnemies.length > 0) {
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
      else {
        mover.setTarget(RC.senseHQLocation());
        mover.move();
      }
    }


    return true;
  }
  public static int lazySqrt(int k) {
    if (k <= 1) return 1;
    if (k <= 4) return 2;
    if (k <= 9) return 3;
    return 4;
  }
  public static float enemyWeightAboutPoint(MapLocation loc, RobotInfo[] nearbyEnemies) {
    float weight = 0;
    for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
      RobotInfo ri = nearbyEnemies[i];
      switch (ri.type) {
        case HQ:
          if (loc.distanceSquaredTo(ri.location) <= 25) {
            weight += 1000;
          }
          break;
        case SOLDIER:
          int d = loc.distanceSquaredTo(ri.location);
          if (d <= FIRE_RANGE_SQUARED) weight += ri.health;
          else weight += ri.health - ALLY_WEIGHT_DECAY * lazySqrt(d - FIRE_RANGE_SQUARED);
          break;
        default:
          break;
      }
    }
    return weight;
  }
  public static String locToString(MapLocation loc) {
    return "(" + loc.x + "," + loc.y + ")";
  }
}
