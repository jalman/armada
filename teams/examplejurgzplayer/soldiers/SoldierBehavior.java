package examplejurgzplayer.soldiers;

import static examplejurgzplayer.soldiers.SoldierUtils.getHighestPriority;
import static examplejurgzplayer.soldiers.SoldierUtils.inRange;
import static examplejurgzplayer.soldiers.SoldierUtils.updateSoldierUtils;
import static examplejurgzplayer.utils.Utils.ALLY_HQ;
import static examplejurgzplayer.utils.Utils.HQ_DX;
import static examplejurgzplayer.utils.Utils.HQ_DY;
import static examplejurgzplayer.utils.Utils.RC;
import static examplejurgzplayer.utils.Utils.enemyRobots;
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

  static final MapLocation middle = new MapLocation(ALLY_HQ.x + HQ_DX / 2, ALLY_HQ.y + HQ_DY / 2);

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
    if (enemyRobots.length > 0) {
      attack();
    }

	  switch(mode) {
      case GOING_TO_MIDDLE:
        RC.setIndicatorString(0, "Going to middle");
        if (mover.getTarget() == null) {
          dest = middle;
          mover.setTarget(dest);
        }
        if (!mover.arrived()) {
          mover.sneak();
        } else {
          dest =
              (role == Role.LEFT) ? middle.add(HQ_DY / 4, -HQ_DX / 4) :
                  middle.add(-HQ_DY / 4, HQ_DX / 4);
              mover.setTarget(dest);
          mode = Mode.SWEEP_OUT;
        }
        break;
      case SWEEP_OUT:
        RC.setIndicatorString(0, "Sweep out");
        if (!mover.arrived()) {
          mover.move();
        } else {
          dest = ALLY_HQ;
          mover.setTarget(dest);
          mode = Mode.RETURN_HOME;
        }
        break;
      case RETURN_HOME:
        RC.setIndicatorString(0, "Return home");
        if (!mover.arrived()) {
          mover.move();
        } else {
          dest = middle;
          mover.setTarget(dest);
          mode = Mode.GOING_TO_MIDDLE;
          role = (Clock.getRoundNum() + RC.getRobot().getID()) % 4 < 2 ? Role.LEFT : Role.RIGHT;
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

  private void attack() {
    try {
      if (RC.isActive()) {
        MapLocation loc = getHighestPriority(enemyRobots);
        if (inRange(loc)) {
          RC.attackSquare(loc);
        }
      }
    } catch (GameActionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void endRound() {

  }
}
