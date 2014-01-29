package mergebot.nav;

import static mergebot.utils.Utils.*;
import mergebot.utils.LocSet;
import battlecode.common.*;


public class HybridMover {
  public static MapLocation DIJKSTRA_CENTER = ALLY_HQ;

  NavAlg simple = new BugMoveFun2();

  MapLocation dest = null;

  private MapLocation simpleTarget;

  private LocSet outPath = null;
  private int outIndex;

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

  private void computeOutPath() throws GameActionException {
    Direction dir = messagingSystem.readPathingDirection(dest);
    if (dir == null) {
      outPath = null;
      return;
    }
    outPath = new LocSet();
    MapLocation loc = dest;
    // System.out.println(dest);
    while (!loc.equals(DIJKSTRA_CENTER)) {
      // System.out.print(dir + ", ");
      dir = messagingSystem.readPathingDirection(loc);
      outPath.insert(loc);
      loc = loc.subtract(dir);
    }
    // System.out.println();
  }

  public void sneak() throws GameActionException {
    move(MovementType.SNEAK);
  }

  public void move() throws GameActionException {
    move(MovementType.RUN);
  }

  private boolean move(Direction dir) throws GameActionException {
    if (!RC.canMove(dir)) return false;
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
        Direction inDir = messagingSystem.readPathingDirection(currentLocation);
        if (inDir != null) {
          inDir = inDir.opposite();
          if (!move(inDir)) {
            // FIXME: try to move around ally robots
            RC.setIndicatorString(1, "Blocked on inDir " + inDir);
          } else {
            RC.setIndicatorString(1, "Moving in inDir " + inDir);
          }
        } else {
          // or DIJKSTRA_CENTER?
          simpleMove(dest);
          RC.setIndicatorString(1, "No inDir, simpleMove to dest");
        }
      }
    } else {
      simpleMove(dest);
      RC.setIndicatorString(1, "No outPath, simpleMove to dest");
    }
  }

  private boolean moveToPath() throws GameActionException {
    if (dstar == null) {
      int[] weights = new int[outPath.size];
      for (int i = weights.length - 1; i >= 0; i--) {
        // System.out.print(outPath.get(i));
        weights[i] = i;
      }
      // System.out.println();
      dstar = new DStar(outPath, weights, currentLocation);
    }

    dstar.compute(6000);

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
