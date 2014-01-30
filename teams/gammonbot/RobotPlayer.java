package gammonbot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import gammonbot.hq.HQBehavior;
import gammonbot.noise.NewNoiseTowerBehavior;
import gammonbot.noise.OctantNoiseTowerBehavior;
import gammonbot.noise.SmartNoiseTowerBehavior;
import gammonbot.noise.SpiralNoiseTowerBehavior;
import gammonbot.soldiers.SoldierBehavior;
import gammonbot.utils.Utils;

public class RobotPlayer {
  public static void run(RobotController rc) {
    Utils.initUtils(rc);
    RobotBehavior robot = null;
    switch (rc.getType()) {
      case HQ:
        robot = new HQBehavior();
        // Strategy strategy = Strategy.decide();
        break;
      case SOLDIER:
        robot = new SoldierBehavior();
        break;
      case PASTR:
        robot = new PastrBehavior();
        break;
      case NOISETOWER:
      try {
        robot = new OctantNoiseTowerBehavior();
      } catch (GameActionException e1) {
        e1.printStackTrace();
      }
        break;
      default: // autokill
        return;
    }

    while (true) {
      try {
        robot.beginRound();
        robot.run();
        robot.endRound();
        rc.yield();
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }
  }
}