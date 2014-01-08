package examplejurgzplayer.soldiers;

import static examplejurgzplayer.soldiers.SoldierUtils.updateSoldierUtils;
import static examplejurgzplayer.utils.Utils.ALLY_HQ;
import static examplejurgzplayer.utils.Utils.HQ_DX;
import static examplejurgzplayer.utils.Utils.HQ_DY;
import static examplejurgzplayer.utils.Utils.RC;
import static examplejurgzplayer.utils.Utils.updateUnitUtils;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import examplejurgzplayer.RobotBehavior;
import examplejurgzplayer.nav.Mover;



public class SoldierBehavior extends RobotBehavior {

  enum Role {
    LEFT, RIGHT
  };

  enum Mode {
    GOING_TO_MIDDLE, SWEEP_OUT, RETURN_HOME, BUILD_PASTR, ENGAGE
  };

  static int bornRound;
  Mover mover;
  Mode mode;
  Role role;

  MapLocation dest;

  // public static int soldierID = -1;

	public SoldierBehavior() {
		bornRound = Clock.getRoundNum();
    mover = new Mover();
    mode = Mode.GOING_TO_MIDDLE;
    role = (bornRound + RC.getRobot().getID()) % 4 < 2 ? Role.LEFT : Role.RIGHT;
	}

	@Override
	public void beginRound() throws GameActionException {
    updateUnitUtils();
		updateSoldierUtils();
	}

	@Override
	public void run() throws GameActionException {
	  switch(mode) {
      case GOING_TO_MIDDLE:
        if (mover.getTarget() == null) {
          dest = new MapLocation(ALLY_HQ.x + HQ_DX / 2, ALLY_HQ.y + HQ_DY / 2);
          mover.setTarget(dest);
        }
        if (!mover.arrived()) {
          mover.execute();
        } else {
          dest =
              (role == Role.LEFT) ?
                  new MapLocation(ALLY_HQ.x + HQ_DX / 2 + HQ_DY / 4, ALLY_HQ.y + HQ_DY / 2 - HQ_DX
                      / 4) :
                  new MapLocation(ALLY_HQ.x + HQ_DX / 2 - HQ_DY / 4, ALLY_HQ.y + HQ_DY / 2 + HQ_DX
                      / 4);
              mover.setTarget(dest);

          mode = Mode.SWEEP_OUT;
        }
        break;
      case SWEEP_OUT:
        if (!mover.arrived()) {
          mover.execute();
        } else {
          dest = ALLY_HQ;
          mover.setTarget(dest);
          mode = Mode.RETURN_HOME;
        }
        break;
      case RETURN_HOME:
        if (!mover.arrived()) {
          mover.execute();
        }
        break;
      case BUILD_PASTR:
        // buildPastr();
        break;
      case ENGAGE:
        break;
      default:
        break;
	  }
	}

  @Override
  public void endRound() {

  }
}
