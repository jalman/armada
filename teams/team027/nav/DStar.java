package team027.nav;

import static team027.utils.Utils.*;
import team027.utils.*;
import battlecode.common.*;

public class DStar {

  /**
   * Starting locations of DStar.
   */
  public final LocSet sources;

  private final MapLocation dest;

  /**
   * Direction in which we traveled to get to this location.
   */
  public Direction[][] from = new Direction[MAP_WIDTH][MAP_HEIGHT];
  /**
   * Distance (times 5) to this location.
   */
  public int distance[][] = new int[MAP_WIDTH][MAP_HEIGHT];

  /**
   * Estimated distance from source + distance to dest.
   */
  // private final int estimate[][] = new int[MAP_WIDTH][MAP_HEIGHT];

  private final boolean visited[][] = new boolean[MAP_WIDTH][MAP_HEIGHT];

  private final BucketQueue<MapLocation> queue = new BucketQueue<MapLocation>(5 * MAP_SIZE, 4);

  public DStar(LocSet sources, int[] distances, MapLocation dest) {
    this.sources = sources;
    this.dest = dest;

    for (int i = sources.size; --i >= 0;) {
      MapLocation source = sources.get(i);
      int e = distances[i] + heuristic(source, dest);
      queue.insert(e, source);
      distance[source.x][source.y] = distances[i];
      // estimate[source.x][source.y] = e;
      // leave as null to cause exceptions if we accidentally try to use it?
      from[source.x][source.y] = Direction.NONE;
    }

    // hack to go around the hqs
    distance[ALLY_HQ.x][ALLY_HQ.y] = Integer.MAX_VALUE;
    visited[ALLY_HQ.x][ALLY_HQ.y] = true;

    ArraySet<MapLocation> unsafeLocs = getUnsafeLocs();
    for (int i = unsafeLocs.size; --i >= 0;) {
      MapLocation loc = unsafeLocs.get(i);
      distance[loc.x][loc.y] = Integer.MAX_VALUE;
      visited[loc.x][loc.y] = true;
    }
  }

  /**
   * @return Whether we have any more computation to do.
   */
  public boolean done() {
    return queue.size == 0;
  }

  /**
   * Approximate action delay between two locations.
   */
  public static int heuristic(MapLocation loc1, MapLocation loc2) {
    int dx = Math.abs(loc1.x - loc2.x);
    int dy = Math.abs(loc1.y - loc2.y);
    int min, diff;
    if (dx > dy) {
      min = dy;
      diff = dx - dy;
    } else {
      min = dx;
      diff = dy - dx;
    }
    return (min * NORMAL_DIAGONAL + diff * NORMAL_ORTHOGONAL);
  }

  public boolean compute(int bytecodes) {
    // cache variables
    int d, w, e, x, y;
    int[] weight;
    MapLocation next, nbr;
    Direction dir;
    final BucketQueue<MapLocation> queue = this.queue;
    final int[][] distance = this.distance;
    final Direction[][] from = this.from;

    // int iters = 0;
    // int bc = Clock.getBytecodeNum();

    while (queue.size > 0) {
      // iters++;
      if (Clock.getBytecodeNum() >= bytecodes - 600) {
        break;
      }

      // RC.setIndicatorString(0, Integer.toString(min));
      // ALERT: queue.min is valid only after a call to deleteMin()!
      next = queue.deleteMin();

      x = next.x;
      y = next.y;
      d = distance[x][y];

      // check if we have already visited this node
      if (!visited[x][y]) {
        visited[x][y] = true;
        /*
         * if (broadcast) {
         * try {
         * messagingSystem.writePathingDirection(next, from[x][y]);
         * } catch (GameActionException ex) {
         * ex.printStackTrace();
         * }
         * }
         */

        weight = WEIGHT[RC.senseTerrainTile(next).ordinal()];

        dir = from[x][y];
        int i;
        if (dir == Direction.NONE) {
          dir = Direction.NORTH;
          i = 8;
        } else if (dir.isDiagonal()) {
          dir = dir.rotateLeft().rotateLeft();
          i = 5;
        } else {
          dir = dir.rotateLeft();
          i = 3;
        }

        for (; --i >= 0; dir = dir.rotateRight()) {
          nbr = next.add(dir);
          if (RC.senseTerrainTile(nbr).isTraversableAtHeight(RobotLevel.ON_GROUND)) {
            w = d + weight[dir.ordinal()];
            e = w + heuristic(next, dest);

            x = nbr.x;
            y = nbr.y;

            if (from[x][y] == null) {
              queue.insert(e, nbr);
              // if (RC.getRobot().getID() == 118)
              // System.out.println("inserted " + nbr + ": " + w + " " + e);
              distance[x][y] = w;
              // estimate[x][y] = e;
              from[x][y] = dir;
            } else {
              if (w < distance[x][y]) {
                queue.insert(e, nbr);
                distance[x][y] = w;
                // estimate[x][y] = e;
                from[x][y] = dir;
                visited[x][y] = false;
              }
            }
          }
        }
      }
    }

    // bc = Clock.getBytecodeNum() - bc;
    // RC.setIndicatorString(2, "average DStar bytecodes: " + (iters > 0 ? bc / iters : bc));

    return arrived(dest);
  }

  /**
   * Computes a path from a location to the nearest source.
   * Contains both start and end points.
   * @param loc The location.
   * @return The path as a LocSet.
   */
  public LocSet getPath(MapLocation loc) {
    LocSet path = new LocSet();
    path.insert(loc);

    while (!sources.contains(loc)) {
      loc = loc.subtract(from[loc.x][loc.y]);
      path.insert(loc);
    }

    return path;
  }

  public int getDistance(int x, int y) {
    return from[x][y] != null ? distance[x][y] : Integer.MAX_VALUE;
  }

  public int getDistance(MapLocation loc) {
    return getDistance(loc.x, loc.y);
  }

  public boolean arrived(MapLocation loc) {
    return visited[loc.x][loc.y];
  }
}
