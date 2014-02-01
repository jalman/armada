package orpheusbot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import orpheusbot.hq.HQBehavior;
import orpheusbot.noise.NewNoiseTowerBehavior;
import orpheusbot.noise.OctantNoiseTowerBehavior;
import orpheusbot.noise.SmartNoiseTowerBehavior;
import orpheusbot.noise.SpiralNoiseTowerBehavior;
import orpheusbot.soldiers.SoldierBehavior;
import orpheusbot.utils.Utils;

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