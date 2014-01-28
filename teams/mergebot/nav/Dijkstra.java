package mergebot.nav;

import static mergebot.utils.Utils.*;
import mergebot.utils.LocSet;
import mergebot.utils.OnePassQueue;
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

  private final boolean inserted[][] = new boolean[MAP_WIDTH][MAP_HEIGHT];
  private final boolean removed[][] = new boolean[MAP_WIDTH][MAP_HEIGHT];
  private final OnePassQueue<MapLocation> queue = new OnePassQueue<MapLocation>(1000, 50);

  public Dijkstra(MapLocation... sources) {
    this.sources = new LocSet(sources);
    for (MapLocation source : sources) {
      queue.insert(0, source);
      inserted[source.x][source.y] = true;
      distance[source.x][source.y] = 0;
      // leave as null to cause exceptions if we accidentally try to use it
      // from[source.x][source.y] = Direction.NONE;
    }
  }

  private static final int WEIGHT[][] = new int[TERRAIN_TILES.length][8];

  static {
    for (int i = 0; i < 8; i += 2) {
      WEIGHT[TerrainTile.NORMAL.ordinal()][i] = 10;
      WEIGHT[TerrainTile.ROAD.ordinal()][i] = 5;
    }
    for (int i = 1; i < 8; i += 2) {
      WEIGHT[TerrainTile.NORMAL.ordinal()][i] = 14;
      WEIGHT[TerrainTile.ROAD.ordinal()][i] = 7;
    }
  }

  public boolean compute(int bytecodes, boolean broadcast, MapLocation... dests) {
    boolean[][] end = new boolean[MAP_WIDTH][MAP_HEIGHT];
    for (MapLocation dest : dests) {
      end[dest.x][dest.y] = true;
    }
    return compute(end, bytecodes, broadcast);
  }

  public boolean compute(boolean[][] end, int bytecodes, boolean broadcast) {
    // cache variables
    int min, w, x, y;
    int[] weight;
    MapLocation next, nbr;
    Direction dir;
    final OnePassQueue<MapLocation> queue = this.queue;
    final int[][] distance = this.distance;
    final boolean[][] inserted = this.inserted;
    final boolean[][] removed = this.removed;
    final Direction[][] from = this.from;

    // int iters = 0;
    // int bc = Clock.getBytecodeNum();

    while (queue.size > 0) {
      // iters++;
      if (Clock.getBytecodeNum() >= bytecodes - 500) {
        break;
      }

      // RC.setIndicatorString(0, Integer.toString(min));
      next = queue.deleteMin();
      min = queue.min;

      x = next.x;
      y = next.y;

      if (removed[x][y]) {
        // verify that it has smaller distance
        if (min <= distance[x][y]) {
          System.out.println("BUG: removed loc had smaller min dist!");
        }
      } else {
        // System.out.println(next + " at distance " + distance[x][y] + " from " + from[x][y]);

        removed[x][y] = true;

        if (broadcast) {
          try {
            messagingSystem.writePathingDirection(next, from[x][y]);
          } catch (GameActionException e) {
            e.printStackTrace();
          }
        }

        if (end[x][y]) {
          reached = next;
          break;
        }

        weight = WEIGHT[RC.senseTerrainTile(next).ordinal()];

        for (int i = 7; i >= 0; i--) {
          dir = DIRECTIONS[i];
          nbr = next.add(dir);
          if (RC.senseTerrainTile(nbr).isTraversableAtHeight(RobotLevel.ON_GROUND)) {
            w = min + weight[i];

            x = nbr.x;
            y = nbr.y;

            if (!inserted[x][y]) {
              queue.insert(w, nbr);
              // System.out.println("inserted " + nbr + " with distance " + w + " from " + dir);
              distance[x][y] = w;
              inserted[x][y] = true;
              from[x][y] = dir;
            } else {
              if (w < distance[x][y]) {
                queue.insert(w, nbr);
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
