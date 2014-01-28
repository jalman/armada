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

  private LocSet path = null;

  private MovementType movementType;

  public void setTarget(MapLocation dest) throws GameActionException {
    if (!dest.equals(this.dest)) {
      this.dest = dest;
      computeOutPath();
      path = null;
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
      if (outPath.contains(currentLocation)) {
        outIndex = outPath.getIndex(currentLocation);
        Direction dir = currentLocation.directionTo(outPath.get(outIndex - 1));
        if (!move(dir)) {
          // FIXME: try to move around ally robots?
          RC.setIndicatorString(1, "Blocked on outPath");
        } else {
          RC.setIndicatorString(1, "Moving on outPath");
        }
      } else {
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
      }
    } else {
      simpleMove(dest);
      RC.setIndicatorString(1, "No outPath, simpleMove to dest");
    }
  }

  private boolean moveToPath() throws GameActionException {
    if (path == null || !path.contains(currentLocation)) {
      Dijkstra dijkstra = new Dijkstra(currentLocation);
      if (dijkstra.compute(outPath.has, 6000, false)) {
        path = dijkstra.getPath(dijkstra.reached);
      } else {
        return false;
      }
    }

    // System.out.println(outPath.contains(currentLocation));

    int index = path.getIndex(currentLocation);
    // System.out.println(path.contains(currentLocation));
    // System.out.println(path.get(index).equals(currentLocation));
    Direction dir = currentLocation.directionTo(path.get(index - 1));
    if (!move(dir)) {
      // FIXME: try to move around ally robots?
      RC.setIndicatorString(1, "Blocked on move to outPath");
    } else {
      RC.setIndicatorString(1, "Moving to outPath");
    }

    return true;
  }

  public boolean arrived() {
    return currentLocation.equals(dest);
  }
}
