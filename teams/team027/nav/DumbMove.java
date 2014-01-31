package team027.nav;

import static team027.utils.Utils.*;
import battlecode.common.*;

public class DumbMove extends NavAlg {
  MapLocation target;

  public DumbMove() {
  }

  @Override
  public void recompute(MapLocation loc) {
    this.target = loc;
  }

  @Override
  public void recompute() {
  }

  @Override
  public Direction getNextDir() {
    Direction dir = currentLocation.directionTo(target);
    if (RC.canMove(dir)) {
      return dir;
    }

    Direction dirl = dir.rotateLeft(), dirr = dir.rotateRight();
    if (currentLocation.add(dirl).distanceSquaredTo(target) < currentLocation.add(dirr)
        .distanceSquaredTo(target)) {
      if (RC.canMove(dirl)) {
        return dirl;
      } else if (RC.canMove(dirr)) {
        return dirr;
      }
    } else {
      if (RC.canMove(dirr)) {
        return dirr;
      } else if (RC.canMove(dirl)) {
        return dirl;
      }
    }

    return Direction.NONE;
  }
}