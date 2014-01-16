package vladbot.soldiers;

import static vladbot.utils.Utils.*;
import vladbot.nav.*;
import battlecode.common.*;

public class Micro {

  public static void micro(Mover mover) throws GameActionException {
    if (!RC.isActive()) return;

    RobotInfo closest = null;
    int minDist2 = Integer.MAX_VALUE;

    for (RobotInfo info : getEnemyRobotInfo()) {
      if (info.type == RobotType.HQ) continue;
      int dist2 = currentLocation.distanceSquaredTo(info.location);
      if (dist2 < minDist2) {
        minDist2 = dist2;
        closest = info;
      }
    }

    // attack a robot in range
    if (minDist2 <= RobotType.SOLDIER.attackRadiusMaxSquared) {
      MapLocation target = acquireTarget().location;
      messagingSystem.writeAttackMessage(target);
      RC.attackSquare(target);
      RC.setIndicatorString(1, "ATTACK " + target);
      return;
    }

    // decide whether to attack or retreat
    Robot[] allyRobots = RC.senseNearbyGameObjects(Robot.class, SENSOR_RADIUS2, ALLY_TEAM);

    if (allyRobots.length >= enemyRobots.length) {
      countBytecodes();
      messagingSystem.writeAttackMessage(closest.location);
      countBytecodes();
      mover.setTarget(closest.location);
      mover.move();
      RC.setIndicatorString(1, "ATTACK " + closest.location);
    } else if (allyRobots.length < enemyRobots.length) {
      // do something better here
      mover.setTarget(ALLY_HQ);
      mover.move();
      RC.setIndicatorString(1, "RETREAT");
    } else {
      RC.setIndicatorString(1, "STAND");
    }
  }

  private static RobotInfo acquireTarget() throws GameActionException {
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
