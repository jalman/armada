package team027.nav;

import static team027.utils.Utils.*;
import team027.utils.LocSet;
import team027.utils.BucketQueue;
import battlecode.common.*;

public class Dijkstra {

  /**
   * Starting locations of Dijkstra.
   */
  public final LocSet sources;
  /**
   * First location reached among the destinations.
   */
  public MapLocation reached = null;
  /**
   * Direction in which we traveled to get to this location.
   */
  public Direction[][] from = new Direction[MAP_WIDTH][MAP_HEIGHT];
  /**
   * Distance (times 5) to this location.
   */
  public int distance[][] = new int[MAP_WIDTH][MAP_HEIGHT];

  private final BucketQueue<MapLocation> queue = new BucketQueue<MapLocation>(2*MAP_SIZE, 50);

  public Dijkstra(MapLocation... sources) {
    this.sources = new LocSet(sources);
    for (MapLocation source : sources) {
      queue.insert(0, source);
      distance[source.x][source.y] = 0;
      // leave as null to cause exceptions if we accidentally try to use it
      // from[source.x][source.y] = Direction.NONE;
    }
  }

  public boolean done() {
    return queue.size == 0;
  }

  /**
   * Compute until either bytecodes have run out or we find a destination.
   * @param bytecodes The bytecode limit.
   * @param broadcast Whether to broadcast the results (used by the HQ).
   * @param dests Destinations to stop at.
   * @return Whether we found a destination.
   */
  public boolean compute(int bytecodes, boolean broadcast, MapLocation... dests) {
    boolean[][] end = new boolean[MAP_WIDTH][MAP_HEIGHT];
    MapLocation dest;
    for (int i = dests.length - 1; i >= 0; --i) {
      dest = dests[i];
      end[dest.x][dest.y] = true;
    }
    return compute(end, bytecodes, broadcast);
  }

  /**
   * Compute until either bytecodes have run out or we find a destination.
   * @param end Hash-map of destination.
   * @param bytecodes The bytecode limit.
   * @param broadcast Whether to broadcast the results (used by the HQ).
   * @return Whether we found a destination.
   */
  public boolean compute(boolean[][] end, int bytecodes, boolean broadcast) {
    // cache variables
    int min, w, x, y;
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
      if (Clock.getBytecodeNum() >= bytecodes - 500) {
        break;
      }

      // RC.setIndicatorString(0, Integer.toString(min));
      // ALERT: queue.min is valid only after a call to deleteMin()!
      next = queue.deleteMin();
      min = queue.min;

      x = next.x;
      y = next.y;

      // check if we have already visited this node
      if (min == distance[x][y]) {

        if (broadcast) {
          try {
            messagingSystem.writePathingInfo(next, from[x][y], distance[x][y]);
          } catch (GameActionException e) {
            e.printStackTrace();
          }
        }

        if (end[x][y]) {
          reached = next;
          break;
        }

        weight = WEIGHT[RC.senseTerrainTile(next).ordinal()];

        dir = from[x][y];
        int i;
        if (dir == null) {
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
            w = min + weight[dir.ordinal()];

            x = nbr.x;
            y = nbr.y;

            if (from[x][y] == null) {
              queue.insert_fast(w, nbr);
              // System.out.println("inserted " + nbr + " with distance " + w + " from " + dir);
              distance[x][y] = w;
              from[x][y] = dir;
            } else {
              if (w < distance[x][y]) {
                queue.insert_fast(w, nbr);
                distance[x][y] = w;
                from[x][y] = dir;
              }
            }
          }
        }
      }
    }

    // bc = Clock.getBytecodeNum() - bc;
    // RC.setIndicatorString(2, "average Dijkstra bytecodes: " + bc / iters);

    return reached != null;
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

}
