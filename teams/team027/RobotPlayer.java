package team027;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import team027.hq.HQBehavior;
import team027.noise.NewNoiseTowerBehavior;
import team027.noise.OctantNoiseTowerBehavior;
import team027.noise.SmartNoiseTowerBehavior;
import team027.noise.SpiralNoiseTowerBehavior;
import team027.soldiers.SoldierBehavior;
import team027.utils.Utils;

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