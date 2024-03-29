package cubicbot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import cubicbot.hq.HQBehavior;
import cubicbot.noise.NewNoiseTowerBehavior;
import cubicbot.noise.OctantNoiseTowerBehavior;
import cubicbot.noise.SmartNoiseTowerBehavior;
import cubicbot.noise.SpiralNoiseTowerBehavior;
import cubicbot.soldiers.SoldierBehavior;
import cubicbot.utils.Utils;

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
        robot = new SmartNoiseTowerBehavior();
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