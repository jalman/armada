package mergebot.nav;

import static mergebot.utils.Utils.*;
import mergebot.utils.ArraySet;
import mergebot.utils.BucketQueue;
import battlecode.common.*;

public class DStar extends GradientMover {

  /**
   * Starting locations of DStar.
   */
  // public ArraySet<MapLocation> sources = new ArraySet<MapLocation>(MAP_SIZE);

  public MapLocation dest;

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

  private final boolean expanded[][] = new boolean[MAP_WIDTH][MAP_HEIGHT];

  private final BucketQueue<MapLocation> queue = new BucketQueue<MapLocation>(5 * MAP_SIZE, 4);

  public DStar() {
    // hack to go around the hqs
    distance[ALLY_HQ.x][ALLY_HQ.y] = Integer.MAX_VALUE;
    expanded[ALLY_HQ.x][ALLY_HQ.y] = true;

    ArraySet<MapLocation> unsafeLocs = getUnsafeLocs();
    for (int i = unsafeLocs.size; --i >= 0;) {
      MapLocation loc = unsafeLocs.get(i);
      distance[loc.x][loc.y] = Integer.MAX_VALUE;
      expanded[loc.x][loc.y] = true;
    }
  }

  public DStar(ArraySet<MapLocation> sources, int[] distances, MapLocation dest) {
    this();
    // this.sources = sources;

    for (int i = sources.size; --i >= 0;) {
      insert(sources.get(i), distances[i]);
    }
  }

  public void insert(MapLocation source, int distance) {
    int e = distance + naiveDistance(source, dest);
    queue.insert(e, source);
    this.distance[source.x][source.y] = distance;
    // leave as null to cause exceptions if we accidentally try to use it?
    from[source.x][source.y] = Direction.NONE;
  }

  public void insert(MapLocation source, int distance, Direction dir) {
    int x = source.x, y = source.y;
    expanded[x][y] = false;
    this.distance[x][y] = distance;
    int e = distance + naiveDistance(source, dest);
    queue.insert(e, source);
    from[x][y] = dir;
  }

  /**
   * @return Whether we have any more computation to do.
   */
  public boolean done() {
    return queue.size == 0;
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
    final MapLocation dest = this.dest;

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
      if (!expanded[x][y]) {
        expanded[x][y] = true;
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
            e = w + naiveDistance(nbr, dest);

            x = nbr.x;
            y = nbr.y;

            if (from[x][y] == null) {
              queue.insert(e, nbr);
              distance[x][y] = w;
              // estimate[x][y] = e;
              from[x][y] = dir;
            } else {
              if (w < distance[x][y]) {
                queue.insert(e, nbr);
                distance[x][y] = w;
                // estimate[x][y] = e;
                from[x][y] = dir;
                expanded[x][y] = false;
              }
            }
          }
        }
      }
    }

    // bc = Clock.getBytecodeNum() - bc;
    // RC.setIndicatorString(2, "average DStar bytecodes: " + (iters > 0 ? bc / iters : bc));

    return visited(dest);
  }

  public int getDistance(int x, int y) {
    return from[x][y] != null ? distance[x][y] : Integer.MAX_VALUE;
  }

  public int getDistance(MapLocation loc) {
    return getDistance(loc.x, loc.y);
  }

  @Override
  public int getWeight(MapLocation loc) {
    return getDistance(loc.x, loc.y);
  }

  public boolean visited(MapLocation loc) {
    return from[loc.x][loc.y] != null;
  }
}
