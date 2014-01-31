package cubicbot.nav;

import static cubicbot.utils.Utils.*;
import battlecode.common.*;

public class Mover {
  private MapLocation dest;
  private MapLocation here;
  private NavAlg navAlg;
  private NavType navType;

  public static final int SNEAK = 0, RUN = 1, PUSH_HOME = 2;

  public Mover() {
    this.dest = null;
    setNavType(NavType.BUG_2);
  }

  public Mover(NavType navType) {
    this.dest = null;
    setNavType(navType);
  }

  // public Mover(RobotBehavior rb) {
  // this.dest = null;
  // this.navAlg = NavType.BUG_HIGH_DIG.navAlg;
  // this.defuseMoving = true;
  // }

  public void setNavType(NavType navType) {
    this.navType = navType;
    this.navAlg = navType.navAlg;
  }

  public void setTarget(MapLocation dest) {
    // RC.setIndicatorString(2, "Mover target set to: " + dest);
    if (!dest.equals(this.dest)) {
      this.dest = dest;
      navAlg.recompute(dest);
    }
  }

  public MapLocation getTarget() {
    return dest;
  }

  public boolean arrived() {
    if (dest == null || dest.equals(currentLocation)) {
      return true;
    }
    return false;
  }

  public void move() {
    execute(RUN);
  }

  public void sneak() {
    execute(SNEAK);
  }

  public void movePushHome() {
    execute(PUSH_HOME);
  }

  /**
   * Try to move.
   * @param sneak:
   * @return
   */
  public void execute(int sneak) {
    // int bc = Clock.getBytecodesLeft();
    // RC.setIndicatorString(1, "my x = " + Integer.toString(RC.getLocation().x) + ", my y = " +
    // Integer.toString(RC.getLocation().y)
    // + "x = " + Integer.toString(dest.x) + ", y = " + Integer.toString(dest.y));
    // RC.setIndicatorString(2, Clock.getRoundNum() + " | dest = " + dest + ", navtype = " +
    // navType);

    if (arrived()) return;

    if (RC.isActive()) {
      Direction d;
      d = navAlg.getNextDir();
      if (d != null && d != Direction.NONE && d != Direction.OMNI) {
        if (RC.canMove(d)) {
          try {
            switch (sneak) {
              case SNEAK:
                // RC.setIndicatorString(2, dest.x + ", " + dest.y + ": sneak");
                RC.sneak(d);
                break;
              case RUN:
                // RC.setIndicatorString(2, dest.x + ", " + dest.y + ": run");
                RC.move(d);
                break;
              case PUSH_HOME:
                // RC.setIndicatorString(2, dest.x + ", " + dest.y + ": push_home");
                Direction awayFromHome = currentLocation.directionTo(ALLY_HQ).opposite();
                if (d == awayFromHome || d == awayFromHome.rotateLeft()
                    || d == awayFromHome.rotateRight()) {
                  RC.sneak(d);
                } else {
                  RC.move(d);
                }
                break;
              default:
                break;
            }
          } catch (GameActionException e) {
            e.printStackTrace();
          }
        } else if (currentLocation.distanceSquaredTo(dest) <= 2) {
          setTarget(currentLocation);
        }
      }
    }
    // System.out.println("Bytecodes used by Mover.execute() = " +
    // Integer.toString(bc-Clock.getBytecodesLeft()));
  }
}