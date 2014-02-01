package mergebot.nav;

import static mergebot.utils.Utils.*;
import battlecode.common.*;

/**
 * Moves down a gradient.
 * @author vlad
 *
 */
public abstract class GradientMover {
  public abstract int getWeight(MapLocation loc);

  /**
   * Move down the gradient (towards the source).
   * @return Whether ended up moving.
   */
  public boolean move(MovementType type) throws GameActionException {
    if (!RC.isActive()) return false;

    Direction dir = Direction.SOUTH, best = null;
    int min = Integer.MAX_VALUE;
    // StringBuilder str = new StringBuilder();
    for (int i = 8; --i >= 0;) {
      MapLocation next = currentLocation.add(dir);
      int d = RC.canMove(dir) && isSafe(next) ? getWeight(next) : Integer.MAX_VALUE;
      // str.append(dir + "(" + d + "), ");

      if (d < min) {
        min = d;
        best = dir;
      }
      dir = dir.rotateRight();
    }
    // RC.setIndicatorString(1, str.toString());

    // System.out.println(str);

    if (best != null) {
      // RC.setIndicatorString(1, "gradient " + min);
      switch (type) {
        case RUN:
          RC.move(best);
          break;
        case SNEAK:
          RC.sneak(best);
          break;
      }
      return true;
    }
    return false;
  }

  /**
   * Move up the gradient (away from the sources).
   * @return Whether ended up moving.
   */
  public boolean ascend(MovementType type) throws GameActionException {
    Direction dir = Direction.NORTH, best = null;
    int max = Integer.MIN_VALUE;
    // StringBuilder str = new StringBuilder();
    for (int i = 8; --i >= 0;) {
      MapLocation next = currentLocation.add(dir);
      int d = RC.canMove(dir) && isSafe(next) ? getWeight(next) : Integer.MIN_VALUE;
      // str.append(dir + "(" + d + "), ");

      if (d > max) {
        max = d;
        best = dir;
      }
      dir = dir.rotateRight();
    }

    if (best != null && !RC.isActive()) {
      switch (type) {
        case RUN:
          RC.move(best);
          break;
        case SNEAK:
          RC.sneak(best);
          break;
      }
      return true;
    }
    return false;
  }

}
