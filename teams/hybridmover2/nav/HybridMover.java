package hybridmover2.nav;

import static hybridmover2.utils.Utils.*;
import hybridmover2.utils.LocSet;
import hybridmover2.utils.Pair;
import battlecode.common.*;


public class HybridMover {
  public static MapLocation DIJKSTRA_CENTER = ALLY_HQ;

  NavAlg simple = new BugMoveFun2();

  MapLocation dest = null;

  private MapLocation simpleTarget;

  private LocSet outPath;
  private int[] distances;

  /**
   * Used to move to path.
   */
  private DStar dstar;

  private MovementType movementType;

  public void setTarget(MapLocation dest) throws GameActionException {
    if (!dest.equals(this.dest)) {
      this.dest = dest;
      computeOutPath();
      dstar = null;
    }
  }

  private void simpleMove(MapLocation loc) throws GameActionException {
    if (!loc.equals(simpleTarget)) {
      simpleTarget = loc;
      simple.recompute(loc);
    }

    Direction dir = simple.getNextDir();
    if (RC.canMove(dir)) {
      move(dir);
    }
  }

  public void sneak() throws GameActionException {
    move(MovementType.SNEAK);
  }

  public void move() throws GameActionException {
    move(MovementType.RUN);
  }

  private boolean move(Direction dir) throws GameActionException {
    if (!RC.isActive() || !RC.canMove(dir)) return false;
    switch (movementType) {
      case SNEAK:
        RC.sneak(dir);
        break;
      case RUN:
        RC.move(dir);
        break;
    }
    return true;
  }

  public void move(MovementType movementType) throws GameActionException {
    if (currentLocation.equals(dest)) return;

    this.movementType = movementType;

    // try computing the outpath if it isn't set
    if (outPath == null) {
      computeOutPath();
    }

    if (outPath != null) {
      if (!moveToPath()) {
        simpleMove(dest);
        RC.setIndicatorString(1, "dstar failed, simpleMove to dest");
      }
    } else {
      simpleMove(dest);
      RC.setIndicatorString(1, "no outPath, simpleMove to dest");
    }
  }

  private void computeOutPath() throws GameActionException {
    Pair<Direction, Integer> pathingInfo = messagingSystem.readPathingInfo(dest);
    if (pathingInfo.first == null) {
      outPath = null;
      return;
    }
    RC.setIndicatorString(1, "Computing outPath");

    outPath = new LocSet();
    distances = new int[MAP_SIZE];
    MapLocation loc = dest;
    int d = pathingInfo.second;
    while (!loc.equals(DIJKSTRA_CENTER)) {
      pathingInfo = messagingSystem.readPathingInfo(loc);
      distances[outPath.size] = d - pathingInfo.second;
      outPath.insert(loc);
      loc = loc.subtract(pathingInfo.first);
    }

    int[] diffs = new int[outPath.size - 1];
    for (int i = diffs.length; --i > 0;) {
      diffs[i] = distances[i + 1] - distances[i];
    }

    // heuristic to prefer further away points on the path (which may be closer to us)
    for (int i = 1; i < outPath.size; i++) {
      distances[i] = distances[i - 1] + Math.max(1, diffs[i - 1] * 100 / (100 + 10 * i));
    }
  }

  private boolean moveToPath() throws GameActionException {
    if (dstar == null) {
      dstar = new DStar(outPath, distances, currentLocation);
    }

    if (!dstar.arrived(currentLocation)) {
      dstar.compute(7000);
    }

    if (!RC.isActive()) return true;

    Direction dir = Direction.NORTH, best = null;
    int min = Integer.MAX_VALUE;
    for (int i = 0; i < 8; i++) {

      int d = RC.canMove(dir) ? dstar.getDistance(currentLocation.add(dir)) : Integer.MAX_VALUE;
      if (d < min) {
        min = d;
        best = dir;
      }
      dir = dir.rotateRight();
    }

    if (best != null && move(best)) {
      RC.setIndicatorString(1, "Moving to outPath");
      return true;
    } else {
      return false;
    }
  }

  public boolean arrived() {
    return currentLocation.equals(dest);
  }
}
