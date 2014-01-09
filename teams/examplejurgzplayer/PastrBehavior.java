package examplejurgzplayer;

import static examplejurgzplayer.utils.Utils.RC;
import battlecode.common.GameActionException;

public class PastrBehavior extends RobotBehavior {
  RobotPlayer player;

  public PastrBehavior(RobotPlayer player) {
    this.player = player;
  }

	public PastrBehavior() {
	}

	/**
	 * Called at the beginning of each round.
	 */
  @Override
  public boolean beginRound() throws GameActionException {
    if (RC.getHealth() < 20) {
      return false;
    }
    return true;
  }

	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {

  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() {

  }
}
