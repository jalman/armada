package mergebot.soldiers;

import static mergebot.soldiers.SoldierUtils.*;
import static mergebot.utils.Utils.*;
import mergebot.nav.*;
import battlecode.common.*;

public class NathanMicro {

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  // public static final int HELP_CHANNEL = ReservedMessageType.HELP_CHANNEL.channel();
  public static final int GREAT_LUGE_ASSIST = 60;
  public static final int FIRE_RANGE_SQUARED = RobotType.SOLDIER.attackRadiusMaxSquared;
  public static final int HELP_DURATION = 8;
  public static final double ALLY_WEIGHT_DECAY = 12;
  public static final double POWER_ADVANTAGE = 1.2f;
  public static final int LIFE_VALUE = 20;
  public static final int DEFEND_PASTR_INITIATIVE = 50;

  /**
   * TODO give actual names and/or definitions for these constants so other people know what's going on
   */
  public static boolean GREAT_LUGE = false; // flag for if the bot is suiciding
  public static boolean JON_SCHNEIDER = false; // flag for if the bot is running away (low health)
  public static boolean AIRBENDER = false; // flag for if the bot is running away (imminent
  // explosion)

  public static boolean isHelpingOut = false;
  public static MapLocation helpingLoc = new MapLocation(0, 0);
  public static int lastHelpRequest = 0;

  public static NavAlg navAlg = new BugMoveFun2();
  public static NavAlg dumbMove = new DumbMove();
  public static MapLocation dest = null;
  public static double currentHealth;

  public static MapLocation microLoc = new MapLocation(0, 0);

  public static boolean luge(Mover mover) throws GameActionException {
    currentHealth = RC.getHealth();

    for (int i = 0; i < SoldierBehavior.microLocations.size; ++i) {
      microLoc = SoldierBehavior.microLocations.get(i);
      if ((!isHelpingOut && currentLocation.distanceSquaredTo(microLoc) <= 100)
          || currentLocation.distanceSquaredTo(microLoc) <= currentLocation
              .distanceSquaredTo(helpingLoc)) {
        isHelpingOut = true;
        helpingLoc = microLoc;
        lastHelpRequest = Clock.getRoundNum();
        break;
      }
    }

    if (getSquareSuicideValue(currentLocation) > 0) {
      // no message for enemies killed via selfdestruct...
      RC.selfDestruct();
    }

    if (RC.isActive()) {
      RobotInfo[] nearbyEnemies = getEnemyRobotInfo();
      Robot[] enemiesInRange = RC.senseNearbyGameObjects(Robot.class, 10, ENEMY_TEAM);

      Robot[] nearbyTeam = RC.senseNearbyGameObjects(Robot.class, 35, ALLY_TEAM);
      double allyWeight = 0, enemyWeight = 0;
      // String bytect = "" + Clock.getBytecodeNum();

      if (SoldierBehavior.pastrDenyRequestLoc != null) {
        MapLocation denyLoc = SoldierBehavior.pastrDenyRequestLoc;
        if (RC.canAttackSquare(denyLoc)) { // can attack means can sense
          Robot denyPastr = (Robot) RC.senseObjectAtLocation(denyLoc);
          // System.out.println("wanting to deny ...");
          if (denyPastr != null) {
            RobotInfo denyPastrInfo = getRobotInfo(denyPastr);
            if (denyPastrInfo.health <= 10) {
              // System.out.println("DENIED");
              RC.attackSquare(denyLoc);
              return true;
            }
          }
        }
      }

      // find ally weight
      allyWeight = currentHealth + allyWeightAboutPoint(currentLocation, nearbyTeam);
      // bytect += " " + Clock.getBytecodeNum();
      // find enemy weight
      enemyWeight = enemyWeightAboutPoint(currentLocation, nearbyEnemies, false);

      /**
       * Reset isHelpingOut
       */
      if (isHelpingOut) {
        if ((RC.canSenseSquare(helpingLoc) && RC.senseObjectAtLocation(helpingLoc) == null)
            || Clock.getRoundNum() > lastHelpRequest + HELP_DURATION) {
          isHelpingOut = false;
        }
      }
      // bytect += " " + Clock.getBytecodeNum();
      /*
       * String zzz = (isHelpingOut ? "HELPING " + helpingLoc : "") + "last help: "
       * + lastHelpRequest + " | round: " + Clock.getRoundNum();
       */

      RobotInfo targetInfo = getHighestPriority(nearbyEnemies);
      MapLocation target = targetInfo == null ? null : targetInfo.location;

      RobotInfo nonAttackInfo = targetInfo;
      MapLocation nonAttackTarget = target;

      /**
       * decide whether to be aggressive or not
       */
      // more conservative: if (!JON_SCHNEIDER && (currentHealth < 30.1 && currentHealth < 10 *
      // (enemiesInRange.length + 1) + 0.1)
      if (!JON_SCHNEIDER
          && currentHealth <= enemiesInRange.length * 10 + 0.01 && targetInfo.health > 10.0
          // && (currentHealth <= 10.01 || (currentHealth <= 20.1 && enemiesInRange.length >= 3))
          && allyWeight >= enemyWeight + 40) {
        JON_SCHNEIDER = true;
      }
      if (JON_SCHNEIDER && currentHealth >= 50) JON_SCHNEIDER = false;

      // bytect += " " + Clock.getBytecodeNum();

      AIRBENDER = false;

      if (target != null) {
        /**
         * If close and worth it, try to suicide
         */
        if (target.distanceSquaredTo(currentLocation) <= 8 && getSquareSuicideValue(currentLocation.add(currentLocation.directionTo(target))) >= 40) {
          GREAT_LUGE = true;
        }
        else {
          Robot[] threaten = RC.senseNearbyGameObjects(Robot.class, target, 2, ALLY_TEAM);

          if (threaten.length > 0) {

            int count = 0;
            for (int i = threaten.length; --i >= 0;) {
              RobotInfo k = getRobotInfo(threaten[i]);
              if (k.actionDelay < 1.) {
                count++;
              }
            }
            if (count * 10 > threaten.length) {
              // blow it up!
            }
            else {
              AIRBENDER = true;
            }
          }
        }
      }
      // bytect += " " + Clock.getBytecodeNum();

      // RC.setIndicatorString(1, "in range " + enemiesInRange.length + " | " + "ally " + allyWeight
      // + " / enemy " + enemyWeight + " (turn " + Clock.getRoundNum() + ") | " + zzz
      // + (JON_SCHNEIDER ? " RUNAWAY" : "") + " | " + bytect);

      /**
       * Choose an aggressive option
       */
      if ((!JON_SCHNEIDER || (currentHealth >= 30 && allyWeight >= enemyWeight + 100) || enemyWeight < 0)
          && (allyWeight >= enemyWeight || GREAT_LUGE)
          && !AIRBENDER) {
        // if (RC.isActive()) { // willing to attack!
        MapLocation nextLoc = null;
        // String zyzzl = "" + Clock.getBytecodeNum();
        double nextAllyWeight = 0, nextEnemyWeight = 0;
        Direction nextDir = Direction.NONE;


        if (target == null) {
          if (isHelpingOut) {
            // Direction helpDir = currentLocation.directionTo(helpingLoc);
            // nextLoc = currentLocation.add(helpDir);
            nonAttackTarget = helpingLoc;
            dumbMove.recompute(nonAttackTarget);
            nextDir = dumbMove.getNextDir();
            if (nextDir == Direction.NONE) {
              setMoveTarget(nonAttackTarget);
              nextDir = navAlg.getNextDir();
            }
            if (nextDir != null && nextDir != Direction.NONE && nextDir != Direction.OMNI) {
              nextLoc = currentLocation.add(nextDir);
              nextAllyWeight = currentHealth + allyWeightAboutPoint(nextLoc, nearbyTeam);
              nextEnemyWeight = enemyWeightAboutPoint(nextLoc, nearbyEnemies, true);
            }

            /*if (nextAllyWeight < POWER_ADVANTAGE * nextEnemyWeight) {
              nextLoc = currentLocation.add(helpDir.rotateLeft());
            }*/
            // target = getHighestPriority(nextLoc, nearbyEnemies);
          } else if (nearbyEnemies.length > 0) {
            // if we don't have to do anything, consider moving towards the closest enemy
            int minDist = 1000, dist;
            for (int i = nearbyEnemies.length; --i >= 0;) {
              dist = currentLocation.distanceSquaredTo(nearbyEnemies[i].location);
              if (dist < minDist) {
                minDist = dist;
                nonAttackInfo = nearbyEnemies[i];
              }
            }
            nonAttackTarget = nonAttackInfo.location;
            dumbMove.recompute(nonAttackTarget);
            nextDir = dumbMove.getNextDir();
            if (nextDir == Direction.NONE) {
              setMoveTarget(nonAttackTarget);
              nextDir = navAlg.getNextDir();
            }
            if (nextDir != null && nextDir != Direction.NONE && nextDir != Direction.OMNI) {
              nextLoc = currentLocation.add(nextDir);
              nextAllyWeight = currentHealth + allyWeightAboutPoint(nextLoc, nearbyTeam);
              nextEnemyWeight = enemyWeightAboutPoint(nextLoc, nearbyEnemies, true);
            }

          }

          for (int i = 0; i < ALLY_PASTR_COUNT; ++i) {
            MapLocation loc = ALLY_PASTR_LOCS[i];
            // Direction towardsPastr = currentLocation.directionTo(loc);

            if (currentLocation.distanceSquaredTo(loc) <= 20
                && (currentLocation.distanceSquaredTo(loc) >= currentLocation
                    .distanceSquaredTo(nextLoc))) {
              nextAllyWeight += DEFEND_PASTR_INITIATIVE;
            }
          }
        }
        // bytect += "-" + Clock.getBytecodeNum();
        // zyzzl +=
        // "/// nextWeight: " + nextEnemyWeight + " at " + nextLoc + ")";

        // if nextLoc is set, we know where we're gonna move
        if (!isHelpingOut
            && nextLoc != null
            && ((enemiesInRange.length == 0 && nextAllyWeight >= POWER_ADVANTAGE
                * nextEnemyWeight + 100))) {
            // nearbyEnemies.length == 0 ||
          // willing to move forward and attack!

          /*
           * String zzss =
           * "moving forward (ally weight: " + nextAllyWeight + ", enemy weight: "
           * + nextEnemyWeight + " at " + nextLoc + ")"
           * + (target == null ? " -- ceding control" : target) + " | " + zyzzl
           * + " | turn " + Clock.getRoundNum();
           */

          if (nonAttackTarget != null) {
            // zzss += " going " + nextDir;
            // RC.setIndicatorString(2, zzss);

            for (int i = nearbyEnemies.length; --i >= 0;) {
              MapLocation nearbyEnemyLoc = nearbyEnemies[i].location;
              if (nearbyEnemyLoc.distanceSquaredTo(nextLoc) <= FIRE_RANGE_SQUARED) {
                messagingSystem.writeMicroMessage(nearbyEnemyLoc, 1);
                // RC.setIndicatorString(2, zzss + "//" + nearbyEnemyLoc);
                break;
              }
            }
            // if (nextDir != null && nextDir != Direction.NONE && nextDir != Direction.OMNI)
            // already checked for ^
            RC.move(nextDir);
          }
          // else {
          // RC.setIndicatorString(2, zzss);
          // }
        }
        /**
         * Logic about advancing when helping out
         */
        else if (isHelpingOut && nextLoc != null
            && nextAllyWeight >= POWER_ADVANTAGE * nextEnemyWeight
            && target == null) {
          // String sz = "";
          /*
           * for (int z = nearbyEnemies.length; --z >= 0;)
           * sz += "/" + nearbyEnemies[z].location;
           * RC.setIndicatorString(2, "/// helping out " + nextLoc + ","
           * + nextAllyWeight + "," + nextEnemyWeight + "|" + sz);
           */

          RC.move(nextDir);
//          Direction newDir = currentLocation.directionTo(helpingLoc);
//          if (newDir != Direction.NONE && newDir != Direction.OMNI) {
//            // go straight towards the target point
//            // the point of the helping out flag is to get manpower ASAP
//            String sz = "";
//            for (int z = nearbyEnemies.length; --z >= 0;)
//              sz += "/" + nearbyEnemies[z].location;
//            RC.setIndicatorString(2, "/// helping out " + nextLoc + ","
//                + nextAllyWeight + "," + nextEnemyWeight + "|" + sz);
//            setTarget(helpingLoc);
//            Direction navDir = navAlg.getNextDir();
//
//            if (navDir == newDir || navDir == newDir.rotateLeft() || navDir == newDir.rotateRight()) {
//              if (RC.canMove(navDir)) {
//                RC.move(navDir);
//                return true;
//              }
//            }
//            else {
//              dumbMove.recompute(currentLocation.add(newDir));
//              newDir = dumbMove.getNextDir(); // variable reuse, sorry
//              if (newDir != Direction.NONE) {
//                RC.move(newDir);
//                return true;
//              }
//            }
          // }
        }
        // RC.setIndicatorString(2, "" + target + "," + nearestPastrLoc + ","
        // + (target != null ? RC.canAttackSquare(target) : ""));
        /**
         * Logic about shooting
         */
        else if (target != null && RC.canAttackSquare(target)) {
          int d = lazySqrt(currentLocation.distanceSquaredTo(target));

          int maxDmg = (int) (enemiesInRange.length * (d - 1) * RobotType.SOLDIER.attackPower);
          /*
           * String sf =
           * maxDmg + "," + currentHealth + "," + allyWeight + "," + enemyWeight + " | "
           * + (GREAT_LUGE ? "suicide" : "") + "," + target + " - round "
           * + currentRound;
           */
          if (currentHealth > maxDmg + 5 && allyWeight < enemyWeight - GREAT_LUGE_ASSIST) {
            // TEMPORARY CHANGE ME LATER
            // GREAT_LUGE = true;
          }
          if (GREAT_LUGE) {
            if (RC.canMove(currentLocation.directionTo(target))) {
              RC.move(currentLocation.directionTo(target));
            } else {
              System.out.println("great luge attack");
              RC.attackSquare(target);
            }
          } else {
            if (targetInfo.health <= 10.01) {
              messagingSystem.writeKill();
              // sf += "| kill count: " + messagingSystem.readKills();
            }
            RC.attackSquare(target);
            if (microLoc.x != target.x || microLoc.y != target.y) {
              messagingSystem.writeMicroMessage(target, 1);
            }
            // sf += " | requesting aid " + target;
          }

          // RC.setIndicatorString(2, sf);
        }
        /**
         * Logic about maybe attacking cows
         */
        else {
          tryAttackingCows(nearbyEnemies);
        }
        // }
      } else if (nearbyEnemies.length > 0) {
        int dx = 0, dy = 0;

        int nearestEnemyDist = 100000, enemyDist;

        for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
          MapLocation nearbyEnemyLoc = nearbyEnemies[i].location;
          dx += nearbyEnemyLoc.x;
          dy += nearbyEnemyLoc.y;
          enemyDist = nearbyEnemyLoc.distanceSquaredTo(currentLocation);
          if (enemyDist < nearestEnemyDist) {
            nearestEnemyDist = enemyDist;
          }
        }
        dx /= nearbyEnemies.length;
        dy /= nearbyEnemies.length;

        Direction newDir =
            currentLocation.directionTo(new MapLocation(2 * curX - dx, 2 * curY - dy));

        if (newDir != Direction.NONE && newDir != Direction.OMNI) {
          Direction wayBack = messagingSystem.readPathingInfo(currentLocation).first.opposite();

          if (RC.canMove(wayBack)
              && (wayBack == newDir || wayBack == newDir.rotateLeft() || wayBack == newDir
                  .rotateRight())) {
            // mover.setTarget(currentLocation.add(wayBack));
            // mover.move();
            // RC.setIndicatorString(2, "flee!");
            RC.move(wayBack);
          } else {
            dumbMove.recompute(currentLocation.add(newDir));
            newDir = dumbMove.getNextDir(); // variable reuse, sorry
            if (newDir != Direction.NONE) {
              RC.move(newDir);
              // RC.setIndicatorString(2, "flee!");
            }
            else {
              // RC.setIndicatorString(2, "want to flee but...");
              if (target != null && RC.canAttackSquare(target)) {
                RC.attackSquare(target);
                if (targetInfo.health <= 10.) {
                  messagingSystem.writeKill();
                  // RC.setIndicatorString(2, "desperation kill " + messagingSystem.readKills());
                }
                if (microLoc.x != target.x || microLoc.y != target.y) {
                  messagingSystem.writeMicroMessage(target, 1);
                }
              }
            }
          }
        }
      }
    }


    return true;
  }

  public static void setMoveTarget(MapLocation loc) {
    if (!loc.equals(dest)) {
      dest = loc;
      navAlg.recompute(loc);
    }
  }

  public static boolean isEnemyHQScary() throws GameActionException {
    return true;
  }

  public static int[] sqrtArray = {0, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3};

  public static int lazySqrt(int k) {
    return (k <= 15) ? sqrtArray[k] : 4;
  }

  public static double getSquareSuicideValue(MapLocation loc) throws GameActionException {
    Robot[] around = RC.senseNearbyGameObjects(Robot.class, 2);
    // double allyDamage = RC.getHealth() + LIFE_VALUE, enemyDamage = 0;
    double damageDelta = -(currentHealth + LIFE_VALUE);
    double predictDamage = 41 + currentHealth / 2;
    // GameConstants.SELF_DESTRUCT_BASE_DAMAGE + GameConstants.SELF_DESTRUCT_DAMAGE_FACTOR
    // * RC.getHealth();
    int predictedKills = 0;

    for (int i = around.length; --i >= 0;) {
      RobotInfo ri = getRobotInfo(around[i]);
      if(predictDamage >= ri.health) {
        damageDelta += ((ri.team == ALLY_TEAM) ? -(ri.health + LIFE_VALUE) : ri.health + LIFE_VALUE);
        predictedKills++;
      } else {
        damageDelta += ((ri.team == ALLY_TEAM) ? -predictDamage : predictDamage);
      }
    }
    // penalize not getting any kills
    return predictedKills > 0 ? damageDelta : damageDelta - LIFE_VALUE;
  }

  public static boolean tryAttackingCows(RobotInfo[] nearbyEnemies) throws GameActionException {
    // find nearest PASTR
    MapLocation nearestPastrLoc = null;
    int nearestPastrDistance = 1000000;
    RobotInfo ri;

    for (int i = nearbyEnemies.length; --i >= 0;) {
      ri = nearbyEnemies[i];
      int d;
      if (ri.type == RobotType.PASTR) {
        d = currentLocation.distanceSquaredTo(ri.location);
        if (d < nearestPastrDistance) {
          nearestPastrDistance = d;
          nearestPastrLoc = ri.location;
        }
      }
    }

    // attack a PASTR i guess
    if (nearestPastrLoc != null) {
      // PASTR_RANGE = 5
      // Direction newDir = currentLocation.directionTo(nearestPastrLoc);
      MapLocation cowTarget =
          getMostCowsLoc(MapLocation.getAllMapLocationsWithinRadiusSq(nearestPastrLoc, 5),
              1000);
      if (cowTarget != null && RC.canAttackSquare(cowTarget)
          && RC.senseObjectAtLocation(cowTarget) == null) {
        RC.attackSquare(cowTarget);
        return true;
      }
      setMoveTarget(nearestPastrLoc);
      Direction newDir = navAlg.getNextDir();
      if (RC.canMove(newDir)
          && currentLocation.add(newDir).distanceSquaredTo(ENEMY_HQ) < RobotType.HQ.attackRadiusMaxSquared) {

        RC.move(newDir);
        return true;
      }
    }
    /*
     * else if (currentLocation.distanceSquaredTo(ENEMY_HQ) <= 100) {
     * // copy-pasted from above.
     * // if we're near the HQ but have nothing to do just randomly kill shit
     * MapLocation cowTarget =
     * getMostCowsLoc(
     * MapLocation.getAllMapLocationsWithinRadiusSq(
     * currentLocation.add(currentLocation.directionTo(ENEMY_HQ)), 5),
     * //
     * 500);
     * if (RC.isActive() && cowTarget != null && RC.canAttackSquare(cowTarget)
     * && RC.senseObjectAtLocation(cowTarget) == null) {
     * RC.attackSquare(cowTarget);
     * return true;
     * }
     * }
     */
    return false;
  }

  public static double allyWeightAboutPoint(MapLocation loc, Robot[] nearbyEnemies)
      throws GameActionException {
    double allyWeight = 0;
    Robot[] nearbyTeam = RC.senseNearbyGameObjects(Robot.class, loc, 17, ALLY_TEAM);

    for (int i = nearbyTeam.length; --i >= 0;) {
      RobotInfo ri = getRobotInfo(nearbyTeam[i]);

      if (ri.type == RobotType.SOLDIER) {
          if (ri.isConstructing) {
            break;
          }

          MapLocation soldierLoc = ri.location;
          int dist = loc.distanceSquaredTo(soldierLoc);
          if (dist > 25) break;

          if (dist <= 3) {
            // allyWeight += (ri.health > 10.0) ? ri.health : 0;
            allyWeight += ri.health;
          } else {
            allyWeight += Math.max(0, ri.health - ALLY_WEIGHT_DECAY * (lazySqrt(dist) - 1));
          }

          /*
           * Robot[] stuff = RC.senseNearbyGameObjects(Robot.class, soldierLoc, 17, ENEMY_TEAM);
           * boolean inCombat = false;
           * int bestDist = 100000;
           * for (int j = stuff.length; --j >= 0;) {
           * int dd = soldierLoc.distanceSquaredTo(getRobotInfo(stuff[j]).location);
           * bestDist = (dd < bestDist ? dd : bestDist);
           * }
           * if (bestDist <= 10) {
           * // allyWeight += (ri.health > 10.0) ? ri.health : 0;
           * allyWeight += ri.health;
           * } else {
           * allyWeight += Math.max(0,
           * ri.health - ALLY_WEIGHT_DECAY * lazySqrt(bestDist - 10));
           * }
           * break;
           */
      }
    }
    return allyWeight;
  }

  /**
   *
   * @param loc: location to center at
   * @param nearbyEnemies: info about nearby enemies
   * @param evaluateNewSquare: whether we're trying to evaluate a new square to move to
   * @return weight of enemies centered at loc
   */
  public static double enemyWeightAboutPoint(MapLocation loc, RobotInfo[] nearbyEnemies,
      boolean evaluateNewSquare) {
    double weight = 0;
    if (evaluateNewSquare
        && (loc.distanceSquaredTo(ENEMY_HQ) <= 25 && Math.max(Math.abs(loc.x - ENEMY_HQ.x),
            Math.abs(loc.y - ENEMY_HQ.y)) < 5)) {
      return 100000;
    }
    for (int i = nearbyEnemies.length; --i >= 0;) {
      RobotInfo ri = nearbyEnemies[i];
      switch(ri.type) {
        case SOLDIER:
          if (ri.isConstructing) {
            break;
          }

          MapLocation soldierLoc = ri.location;

          int d = loc.distanceSquaredTo(soldierLoc);
          if (d > 25) break;


          if (evaluateNewSquare && d <= 8 && ri.actionDelay < 2) {
            weight += (41 + ri.health / 2);
          } else if (d <= FIRE_RANGE_SQUARED) {
            weight += ri.health;
          } else {
            weight += Math.max(0, ri.health - ALLY_WEIGHT_DECAY * lazySqrt(d - FIRE_RANGE_SQUARED));
          }
          break;
        case PASTR:
        case NOISETOWER:
          weight -= 0.1;
          break;
        default:
          break;
      }
    }
    return weight;
  }
}
