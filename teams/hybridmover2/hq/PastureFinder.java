package hybridmover2.hq;

import static hybridmover2.utils.Utils.*;

import java.util.*;

import hybridmover2.utils.*;
import battlecode.common.*;

public class PastureFinder {
  public static MapLocation[] cowMiningLocations() {
    // System.out.println(" > " + Clock.getBytecodeNum());
    int xparts = MAP_WIDTH < 50 ? 5 : 7;
    int yparts = MAP_HEIGHT < 50 ? 5 : 7;
    @SuppressWarnings("unchecked")
    Pair<MapLocation, Double>[] ret = new Pair[(1 + xparts) * (1 + yparts) / 2];
    int i = 0;
    for (int x = xparts - 1; x >= 0; x--) {
      for (int y = yparts - 1; y >= 0; y--) {
        MapLocation inittry =
            new MapLocation((2 * x * MAP_WIDTH + 1) / (2 * xparts),
                (2 * y * MAP_HEIGHT + 1) / (2 * yparts));
        if (inittry.distanceSquaredTo(ALLY_HQ) < inittry.distanceSquaredTo(ENEMY_HQ)) {
          ret[i] = gradientAscent(inittry);
          // ret[i] = gradientDescentOnNegativeCowScalarField(inittry.x, inittry.y, 6);
          i++;
        }
      }
    }
    System.out.println(" > " + Clock.getBytecodeNum());

    Arrays.sort(ret, new Comparator<Pair<MapLocation, Double>>() {

      @Override
      public int compare(Pair<MapLocation, Double> a, Pair<MapLocation, Double> b) {
        return a == null ? 1 : b == null ? -1 : Double.compare(b.second, a.second);
      }

    });

    System.out.println(Clock.getBytecodeNum());

    MapLocation[] locs = new MapLocation[i];
    while (i-- > 0) {
      locs[i] = ret[i].first;
    }

    return locs;
  }

  private static double effectiveCowGrowth(MapLocation loc) {
    return (RC.senseTerrainTile(loc).isTraversableAtHeight(RobotLevel.ON_GROUND))
        ? COW_GROWTH[loc.x][loc.y] : 0.0;
  }

  private static MapLocation randomNearbyLocation(MapLocation loc, int d2) {
    MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(loc, d2);
    return locs[random.nextInt(locs.length)];
  }

  private static MapLocation[] randomNearbyLocations(MapLocation loc, int d2, int num) {
    MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(loc, d2);
    MapLocation[] chosen = new MapLocation[num];
    for (int i = num - 1; i >= 0; i--) {
      chosen[i] = locs[random.nextInt(locs.length)];
    }
    return chosen;
  }

  private static MapLocation[] symmetrize(MapLocation center, int dx, int dy) {
    MapLocation[] locs = {center.add(dx, dy), center.add(dy, dx), center.add(dx, -dy),
        center.add(dy, -dx), center.add(-dx, dy), center.add(-dy, dx), center.add(-dx, -dy),
        center.add(-dy, -dx)};
    return locs;
  }

  private static MapLocation[] randomUniformNearbyLocations(MapLocation loc, int d2, int num) {
    MapLocation[] dxdylist = MapLocation.getAllMapLocationsWithinRadiusSq(new MapLocation(0, 0), d2);
    MapLocation[] chosen = new MapLocation[num * 8];
    for (int i = num - 1; i >= 0; --i) {
      MapLocation rand = dxdylist[random.nextInt(dxdylist.length)];
      int dx = rand.x, dy = rand.y;
      // inlining for performance
      /*
       * MapLocation[] locs = symmetrize(loc, rand.x, rand.y);
       * for (int j = 7; j >= 0; j--) {
       * chosen[i * 8 + j] = locs[j];
       * }
       */
      chosen[i * 8 + 0] = loc.add(dx, dy);
      chosen[i * 8 + 1] = loc.add(dy, dx);
      chosen[i * 8 + 2] = loc.add(dx, -dy);
      chosen[i * 8 + 3] = loc.add(dy, -dx);
      chosen[i * 8 + 4] = loc.add(-dx, dy);
      chosen[i * 8 + 5] = loc.add(-dy, dx);
      chosen[i * 8 + 6] = loc.add(-dx, -dy);
      chosen[i * 8 + 7] = loc.add(-dy, -dx);
    }

    return chosen;
  }

  static final int COW_GROWTH_RAND_ITERS = 2;
  static final int COW_GROWTH_RAND_DIST = 80;

  static final int COW_CHECK_RADIUS_SQUARED = 9;
  private static double estimateCowGrowth(MapLocation loc) {
    double estimate = 0;

    // 5 = PASTR radius
    MapLocation[] locs =
        MapLocation.getAllMapLocationsWithinRadiusSq(loc, COW_CHECK_RADIUS_SQUARED);
    for (int i = locs.length - 1; i >= 0; --i) {
      estimate += effectiveCowGrowth(locs[i]);
    }

    // locs = randomUniformNearbyLocations(loc, COW_GROWTH_RAND_DIST, COW_GROWTH_RAND_ITERS);
    // for (int i = locs.length - 1; i >= 0; --i) {
    // estimate += effectiveCowGrowth(locs[i]);
    // }

    return estimate;
  }


  private static Pair<MapLocation, Double> gradientAscent(MapLocation current) {
    boolean[][] tried = new boolean[MAP_WIDTH][MAP_HEIGHT];

    // int tries = 10;
    int dist = 9;
    double best = estimateCowGrowth(current);
    // System.out.println("search started from " + current + ", which had estimate " + best);

    loop: {
      MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(current, dist);
      for (int i = locs.length - 1; i >= 0; --i) {
        MapLocation loc = locs[i];
        // remove utils function call for bytecode savings
        if (!RC.senseTerrainTile(loc).isTraversableAtHeight(RobotLevel.ON_GROUND)
            || tried[loc.x][loc.y]) continue;
        tried[loc.x][loc.y] = true;
        double estimate = estimateCowGrowth(loc);
        if (estimate > best) {
          best = estimate;
          current = loc;
          break loop;
        }
      }
    }
    return new Pair<MapLocation, Double>(current, best);
  }

  private static Pair<MapLocation, Double> gradientDescentOnNegativeCowScalarField(int x, int y,
      int d) {
    int xl = Math.max(x - d, 0);
    int xu = Math.min(x + d, MAP_WIDTH - 1);
    int yl = Math.max(y - d, 0);
    int yu = Math.min(y + d, MAP_HEIGHT - 1);
    boolean changed = false;

    for (int i = xl; i <= xu; i++)
      for (int j = yl; j <= yu; j++) {
        if (COW_GROWTH[i][j] > COW_GROWTH[x][y]) {
          changed = true;
          x = i;
          y = j;
        }
      }

    return !changed || d == 1 ? new Pair<MapLocation, Double>(new MapLocation(x, y),
        COW_GROWTH[x][y]) : gradientDescentOnNegativeCowScalarField(x, y, d - 1);
  }

}
