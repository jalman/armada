package examplejurgzplayer;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import examplejurgzplayer.hq.HQBehavior;
import examplejurgzplayer.soldiers.SoldierBehavior;
import examplejurgzplayer.utils.Utils;

public class RobotPlayer {
  public static void run(RobotController rc) {
    Utils.initUtils(rc);
    RobotBehavior robot = null;
    switch (rc.getType()) {
      case HQ:
        // Strategy strategy = Strategy.decide();
        robot = new HQBehavior();
        break;
      case SOLDIER:
        robot = new SoldierBehavior();
        break;
      default: // change
        robot = new SoldierBehavior();
        break;
    }

    while (true) {
      try {
        robot.beginRound();
        robot.run();
        robot.endRound();
        rc.yield();
      } catch (GameActionException e) {
        System.out.println("Round number = " + Clock.getRoundNum());
        e.printStackTrace();
      }
    }
  }
}