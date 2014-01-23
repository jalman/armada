package microbot2.soldiers;

import static microbot2.utils.Utils.*;
import microbot2.nav.*;
import battlecode.common.*;

public class Micro {

  private final SoldierBehavior soldierBehavior;
  private final Mover mover;

  public Micro(SoldierBehavior soldierBehavior) {
    this.soldierBehavior = soldierBehavior;
    this.mover = soldierBehavior.mover;
  }

  public void micro() throws GameActionException {
    if (!RC.isActive()) return;

    RobotInfo closestEnemyRobot = null;
    int minDist2 = Integer.MAX_VALUE;

    for (RobotInfo info : getEnemyRobotInfo()) {
      if (info.type == RobotType.HQ) continue;
      int dist2 = currentLocation.distanceSquaredTo(info.location);
      if (dist2 < minDist2) {
        minDist2 = dist2;
        closestEnemyRobot = info;
      }
    }

    // attack a robot in range
    if (minDist2 <= RobotType.SOLDIER.attackRadiusMaxSquared) {
      MapLocation target = acquireTarget().location;
      messagingSystem.writeAttackMessage(target);
      RC.attackSquare(target);
      RC.setIndicatorString(1, "FIRE " + target);
      return;
    }

    // decide whether to attack or retreat
    Robot[] allyRobots = RC.senseNearbyGameObjects(Robot.class, SENSOR_RADIUS2, ALLY_TEAM);

    if (allyRobots.length >= enemyRobots.length) {
      // gang up on an enemy
      MapLocation closestAttackLocation = null;
      minDist2 = 36;

      for (int i = soldierBehavior.attackLocations.size; --i >= 0;) {
        MapLocation loc = soldierBehavior.attackLocations.get(i);
        int dist2 = currentLocation.distanceSquaredTo(loc);
        if (dist2 < minDist2) {
          minDist2 = dist2;
          closestAttackLocation = loc;
        }
      }

      if (closestAttackLocation != null) {
        mover.setTarget(closestAttackLocation);
        mover.move();
        RC.setIndicatorString(1, "GANG UP " + closestAttackLocation);
        return;
      }

      messagingSystem.writeAttackMessage(closestEnemyRobot.location);
      mover.setTarget(closestEnemyRobot.location);
      mover.move();
      RC.setIndicatorString(1, "ATTACK " + closestEnemyRobot.location);
    } else if (allyRobots.length < enemyRobots.length) {
      // do something better here
      mover.setTarget(ALLY_HQ);
      mover.move();
      RC.setIndicatorString(1, "RETREAT");
    } else {
      RC.setIndicatorString(1, "STAND");
    }
  }

  private RobotInfo acquireTarget() throws GameActionException {
    RobotInfo best = null;
    double minHealth = Double.MAX_VALUE;
    int minDist2 = Integer.MAX_VALUE;

    for (RobotInfo info : getEnemyRobotInfo()) {
      int dist2 = currentLocation.distanceSquaredTo(info.location);
      if (dist2 > RobotType.SOLDIER.attackRadiusMaxSquared) continue;

      double health = info.health;
      if (health < minHealth || (health == minHealth && dist2 < minDist2)) {
        minHealth = health;
        minDist2 = dist2;
        best = info;
      }
    }

    return best;
  }
}
