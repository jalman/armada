package mergebot;

import static mergebot.utils.Utils.*;
import mergebot.utils.*;
import battlecode.common.*;

public class PastrBehavior extends RobotBehavior {
  // healthn = health n turns ago
  public static double health0, health1, health2, health3;

  public PastrBehavior() {
    health0 = RC.getHealth();
    health1 = health0;
    health2 = health1;
    health3 = health2;
  }

	/**
	 * Called at the beginning of each round.
	 */
  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    messagingSystem.beginRound(handlers);
    health3 = health2;
    health2 = health1;
    health1 = health0;
    health0 = RC.getHealth();
  }

	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
    double damageRate =
        2 * Math.max(Math.max(health3 - health2, health2 - health1), health1 - health0);
    if (health0 < damageRate || health0 < 20) {
      messagingSystem.writePastureDenyRequest(currentLocation);
      System.out.println("deny me pls i am dying: " + health0);
    }
  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
  }
}
