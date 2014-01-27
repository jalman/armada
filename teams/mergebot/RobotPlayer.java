package mergebot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import mergebot.hq.HQBehavior;
import mergebot.noise.NewNoiseTowerBehavior;
import mergebot.noise.OctantNoiseTowerBehavior;
import mergebot.noise.SpiralNoiseTowerBehavior;
import mergebot.soldiers.SoldierBehavior;
import mergebot.utils.Utils;

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
      case PASTR:
        robot = new PastrBehavior();
        break;
      case NOISETOWER:
        robot = new OctantNoiseTowerBehavior();
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