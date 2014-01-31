package anagrambot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import anagrambot.hq.HQBehavior;
import anagrambot.noise.NewNoiseTowerBehavior;
import anagrambot.noise.OctantNoiseTowerBehavior;
import anagrambot.noise.SmartNoiseTowerBehavior;
import anagrambot.noise.SpiralNoiseTowerBehavior;
import anagrambot.soldiers.SoldierBehavior;
import anagrambot.utils.Utils;

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