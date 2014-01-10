package team027.nav;

import static team027.utils.Utils.RC;
import static team027.utils.Utils.currentLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Mover {
  private MapLocation dest;
  private MapLocation here;
  private NavAlg navAlg;
  private NavType navType;

  public Mover() {
    this.dest = null;
    setNavType(NavType.BUG_FUN);
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
    RC.setIndicatorString(2, dest.x + ", " + dest.y);
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
    execute(false);
  }

  public void sneak() {
    execute(true);
  }

  /**
   * Try to move.
   * @param sneak: sneak if true, run if false
   * @return
   */
  public void execute(boolean sneak) {
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
              if (sneak) {
                RC.sneak(d);
              } else {
                RC.move(d);
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