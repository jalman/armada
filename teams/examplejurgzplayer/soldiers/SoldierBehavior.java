package examplejurgzplayer.soldiers;

import static examplejurgzplayer.soldiers.SoldierUtils.updateSoldierUtils;
import static examplejurgzplayer.utils.Utils.updateUnitUtils;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import examplejurgzplayer.RobotBehavior;



public class SoldierBehavior extends RobotBehavior {
	public static int bornRound;
	public static int soldierID = -1;

	public SoldierBehavior() {
		bornRound = Clock.getRoundNum();
	}

	@Override
	public void beginRound() throws GameActionException {
    updateUnitUtils();
		updateSoldierUtils();
	}

	@Override
	public void run() throws GameActionException {
	}

  @Override
  public void endRound() {

  }
}
