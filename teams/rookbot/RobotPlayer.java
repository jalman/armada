package rookbot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import rookbot.hq.HQBehavior;
import rookbot.noise.NewNoiseTowerBehavior;
import rookbot.noise.OctantNoiseTowerBehavior;
import rookbot.noise.SpiralNoiseTowerBehavior;
import rookbot.soldiers.SoldierBehavior;
import rookbot.utils.Utils;

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