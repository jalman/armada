package examplejurgzplayer;

import battlecode.common.GameActionException;

public abstract class RobotBehavior {
	public RobotBehavior() {
	}

	/**
	 * Called at the beginning of each round.
	 */
  public abstract void beginRound() throws GameActionException;

	/**
	 * Called every round.
	 */
  public abstract void run() throws GameActionException;

	/**
	 * Called at the end of each round.
	 */
	public abstract void endRound();
}
