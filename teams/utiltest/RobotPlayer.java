package utiltest;

import battlecode.common.*;

public class RobotPlayer {

  static TerrainTile[][] terrainCache;
  static int[][] terrainCacheNew;
  static RobotController RC;
  static int MAP_WIDTH, MAP_HEIGHT;
  public static void run(RobotController rc) {
    RC = rc;
    int n, startBytecodes, spent, sqrt1, sqrt2;
    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();
    terrainCache = new TerrainTile[MAP_WIDTH][MAP_HEIGHT];
    terrainCacheNew = new int[MAP_WIDTH][MAP_HEIGHT];

    int tc1d[] = new int[256 * 256];

    int TERRAIN_CHANNEL = 5000;

    int x = 11, y = 11;
    countBytecodes(false);
    terrainCacheNew[x][y] = 1;
    terrainCacheNew[x + 1][y + 1] = 1;
    countBytecodes(true);

    countBytecodes(false);
    terrainCacheNew[x][y] = 1;
    terrainCacheNew[x + 1][y + 1] = 1;
    terrainCacheNew[x + 2][y + 2] = 1;
    countBytecodes(true);
    rc.yield();

    countBytecodes(false);
    n = terrainCacheNew[x][y];
    n = terrainCacheNew[x + 1][y + 1];
    countBytecodes(true);

    countBytecodes(false);
    n = terrainCacheNew[x][y];
    n = terrainCacheNew[x + 1][y + 1];
    n = terrainCacheNew[x + 2][y + 2];
    countBytecodes(true);
    rc.yield();

    countBytecodes(false);
    tc1d[(y << 8) + x] = 1;
    tc1d[((y + 1) << 8) + (x + 1)] = 1;
    countBytecodes(true);

    countBytecodes(false);
    n = ((y + 2) << 8) + (x + 2);
    countBytecodes(true);

    countBytecodes(false);
    tc1d[(y << 8) + x] = 1;
    tc1d[((y + 1) << 8) + (x + 1)] = 1;
    tc1d[((y + 2) << 8) + (x + 2)] = 1;
    countBytecodes(true);
    rc.yield();

    countBytecodes(false);
    n = tc1d[(y << 8) + x];
    n = tc1d[((y + 1) << 8) + (x + 1)];
    countBytecodes(true);

    countBytecodes(false);
    n = tc1d[(y << 8) + x];
    n = tc1d[((y + 1) << 8) + (x + 1)];
    n = tc1d[((y + 2) << 8) + (x + 2)];
    countBytecodes(true);
    rc.yield();

    for (TerrainTile t : TerrainTile.values()) {
      System.out.println(t + " is " + t.ordinal());
    }

    System.out.println("---");

    rc.yield();
    System.out.println("sense and cache 100 tiles:");
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        senseCachedTerrainTile(x, y);
      }
    }
    countBytecodes(true);
    rc.yield();

    System.out.println("sense and broadcast 100 tiles:");
    countBytecodes(false);
    try {
      for (y = 9; y >= 0; --y) {
        int terrainMessage = 1; // begin with 01
        for (x = 9; x >= 0; --x) {
          terrainMessage <<= 3; // 3-bit values
          // switch (rc.senseTerrainTile(new MapLocation(x, y))) {
          // case VOID:
          // terrainMessage += 1;
          // break;
          // case NORMAL:
          // terrainMessage += 2;
          // break;
          // case ROAD:
          // terrainMessage += 3;
          // break;
          // default:
          // break;
          // }
          terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(x, y)).ordinal());
        }
        rc.broadcast(TERRAIN_CHANNEL + y, terrainMessage);
      }
    } catch (GameActionException e) {
      e.printStackTrace();
    }
    countBytecodes(true);
    rc.yield();

    System.out.println("sense and broadcast 100 tiles, hardcoded:");
    countBytecodes(false);
    try {
      int terrainMessage;
      for (y = 9; y >= 0; --y) {
        terrainMessage = 1 << 30; // begin with 01

        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(9, y)).ordinal()) << 27;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(8, y)).ordinal()) << 24;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(7, y)).ordinal()) << 21;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(6, y)).ordinal()) << 18;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(5, y)).ordinal()) << 15;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(4, y)).ordinal()) << 12;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(3, y)).ordinal()) << 9;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(2, y)).ordinal()) << 6;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(1, y)).ordinal()) << 3;
        terrainMessage += (3 - rc.senseTerrainTile(new MapLocation(0, y)).ordinal());
        rc.broadcast(TERRAIN_CHANNEL + y, terrainMessage);
      }
    } catch (GameActionException e) {
      e.printStackTrace();
    }
    countBytecodes(true);
    rc.yield();

    System.out.println("read broadcast and cache 100 tiles:");
    countBytecodes(false);
    try {
      for (y = 9; y >= 0; --y) {
        int terrainMessage = rc.readBroadcast(TERRAIN_CHANNEL + y);
        if (terrainMessage >> 30 == 1) {
          for (x = 0; x < 10; ++x) {
            terrainCacheNew[x][y] = (terrainMessage >> (3 * x)) & 7; // last 3 bits
          }
        }
      }
    } catch (GameActionException e) {
      e.printStackTrace();
    }
    countBytecodes(true);
    rc.yield();


    System.out.println("read broadcast and cache 100 tiles, hardcoded:");
    countBytecodes(false);
    try {
      for (y = 9; y >= 0; --y) {
        int terrainMessage = rc.readBroadcast(TERRAIN_CHANNEL + y);
        if (terrainMessage >> 30 == 1) {
          terrainCacheNew[0][y] = terrainMessage & 7;
          terrainCacheNew[1][y] = terrainMessage >> 3 & 7;
          terrainCacheNew[2][y] = terrainMessage >> 6 & 7;
          terrainCacheNew[3][y] = terrainMessage >> 9 & 7;
          terrainCacheNew[4][y] = terrainMessage >> 12 & 7;
          terrainCacheNew[5][y] = terrainMessage >> 15 & 7;
          terrainCacheNew[6][y] = terrainMessage >> 18 & 7;
          terrainCacheNew[7][y] = terrainMessage >> 21 & 7;
          terrainCacheNew[8][y] = terrainMessage >> 24 & 7;
          terrainCacheNew[9][y] = terrainMessage >> 27 & 7;
        }
      }
    } catch (GameActionException e) {
      e.printStackTrace();
    }
    countBytecodes(true);
    rc.yield();

    for (y = 0; y < 10; y++) {
      for (x = 0; x < 10; x++) {
        switch (terrainCache[x][y]) {
          case VOID:
            if (terrainCacheNew[x][y] != 1) {
              System.out.println("error at (" + x + ", " + y + "): " + terrainCacheNew[x][y]
                  + " isn't 1 = VOID");
            }
            break;
          case NORMAL:
            if (terrainCacheNew[x][y] != 3) {
              System.out.println("error at (" + x + ", " + y + "): " + terrainCacheNew[x][y]
                  + " isn't 3 = NORMAL");
            }
            break;

          case ROAD:
            if (terrainCacheNew[x][y] != 2) {
              System.out.println("error at (" + x + ", " + y + "): " + terrainCacheNew[x][y]
                  + " isn't 2 = ROAD");
            }
            break;
          default:
            if (terrainCacheNew[x][y] != 0) {
              System.out.println("error at (" + x + ", " + y + "): " + terrainCacheNew[x][y]
                  + " isn't 0 = OFF_MAP");
            }
            break;
        }
      }
    }
    rc.yield();

    System.out.println("just sense 100 tiles:");
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        rc.senseTerrainTile(new MapLocation(x, y));
      }
    }
    countBytecodes(true);
    rc.yield();

    System.out.println("-------");

    int t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        if (terrainCache[x][y] == TerrainTile.ROAD || terrainCache[x][y] == TerrainTile.NORMAL) {
          t++;
        }
      }
    }
    System.out
    .println("tiles traversable 1 [if statement] precompute tile, no out-of-bounds, two array accesses: "
        + t);
    countBytecodes(true);
    rc.yield();

    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        TerrainTile tile = terrainCache[x][y];
        if (tile == TerrainTile.ROAD || tile == TerrainTile.NORMAL) {
          t++;
        }
      }
    }
    System.out
    .println("tiles traversable 1 [if statement] precompute tile, no out-of-bounds,  one store one array access: "
        + t);
    countBytecodes(true);
    rc.yield();


    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        TerrainTile tile = rc.senseTerrainTile(new MapLocation(x, y));
        if (tile == TerrainTile.ROAD || tile == TerrainTile.NORMAL) {
          t++;
        }
      }
    }
    System.out.println("tiles traversable 2 [if statement] senseTerrainTile: " + t);
    countBytecodes(true);
    rc.yield();

    terrainCache = new TerrainTile[MAP_WIDTH][MAP_HEIGHT];
    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        TerrainTile tile = senseCachedTerrainTile(x, y);
        if (tile == TerrainTile.ROAD || tile == TerrainTile.NORMAL) {
          t++;
        }
      }
    }
    System.out.println("tiles traversable 2 [if statement] senseCachedTerrainTile empty cache: "
        + t);
    countBytecodes(true);
    rc.yield();

    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        TerrainTile tile = senseCachedTerrainTile(x, y);
        if (tile == TerrainTile.ROAD || tile == TerrainTile.NORMAL) {
          t++;
        }
      }
    }
    System.out
    .println("tiles traversable 2 [if statement] senseCachedTerrainTile full cache: " + t);
    countBytecodes(true);
    rc.yield();

    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        if (terrainCache[x][y].isTraversableAtHeight(RobotLevel.ON_GROUND)) {
          t++;
        }
      }
    }
    System.out.println("tiles traversable 3 [isTraversable] use cached tile: " + t);
    countBytecodes(true);
    rc.yield();

    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        if (rc.senseTerrainTile(new MapLocation(x, y)).isTraversableAtHeight(RobotLevel.ON_GROUND)) {
          t++;
        }
      }
    }
    System.out.println("tiles traversable 3 [isTraversable] use senseTerrainTile: " + t);
    countBytecodes(true);
    rc.yield();

    t = 0;
    countBytecodes(false);
    for (x = 9; x >= 0; --x) {
      for (y = 9; y >= 0; --y) {
        if (senseCachedTerrainTile(x, y).isTraversableAtHeight(RobotLevel.ON_GROUND)) {
          t++;
        }
      }
    }
    System.out
    .println("tiles traversable 3 [isTraversable] use senseCachedTerrainTile full cache: " + t);
    countBytecodes(true);
    rc.yield();
  }

  static int bytecodes = 0, round = 0;

  public static void countBytecodes(boolean print) {
    int tempBytecodes = Clock.getBytecodeNum();
    int tempRound = Clock.getRoundNum();
    if (print) {
      System.out.println("Bytecodes used since last call: " + ((tempRound - round) * 10000
          + (tempBytecodes - bytecodes - 13))); // 13 = bytecode cost of this method
      System.out.println("---");
    }
    bytecodes = tempBytecodes;
    round = tempRound;
  }

  public static TerrainTile senseCachedTerrainTile(int x, int y) {
    if (x < 0 || x >= MAP_WIDTH || y < 0 || y >= MAP_HEIGHT) {
      return TerrainTile.OFF_MAP;
    } else if (terrainCache[x][y] == null) {
      TerrainTile tile = RC.senseTerrainTile(new MapLocation(x, y));
      terrainCache[x][y] = tile;
      return tile;
    } else {
      return terrainCache[x][y];
    }
  }
  /**
   * integer sqrt, by binary search
   * @param n (assume positive)
   * @return
   */
  public static int sqrt1(int n) {
    if (n <= 3) return 1;

    int lo = 1;
    int hi = n / 2;
    int g = 1;

    while (lo < hi) {
      g = (lo + hi + 1) / 2;
      if (g * g > n) {
        hi = g - 1;
      } else if (g * g < n) {
        lo = g;
      } else {
        return g;
      }
    }
    return lo;
  }

  /**
   * integer sqrt, by newton's method
   * @param n (assume positive)
   * @return
   */
  public static int sqrt2(int n) {
    if (n <= 3) return 1;

    double g1 = 1.0, g2 = 0.0;

    while (true) {
      g2 = (g1 + n / g1) / 2.0;
      if (g2 - g1 < 1 && g2 - g1 > 0) {
        return (int) g1;
      } else if (g1 - g2 < 1 && g1 - g2 > 0) {
        return (int) g2;
      } else {
        g1 = g2;
      }
    }
  }

  public static final int[] SQRTS = {0, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4,
      4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9,
      9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
      10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
      11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
      12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
      13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 14,
      14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
      15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
      15, 15, 15, 15, 15, 15, 15, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
      16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 17,
      17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
      17, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
      18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 19, 19,
      19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19,
      19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20, 20, 20, 20, 20, 20, 20,
      20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
      20, 20, 20, 20, 20, 20, 20, 20, 20, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
      21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
      21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
      22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
      22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
      23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
      23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
      24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
      24, 24, 24, 24, 24, 24, 24, 24, 24, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
      25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
      25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26, 26,
      26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
      26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27,
      27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
      27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
      27, 27, 27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
      28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
      28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29,
      29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
      29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
      29, 29, 29, 29, 29, 29, 29, 29, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
      30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
      30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 32, 32, 32, 32, 32, 32,
      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
      33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
      33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
      33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34,
      34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34,
      34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34,
      34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35,
      35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35,
      35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35,
      35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36,
      36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36,
      36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36,
      36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 37, 37, 37, 37, 37, 37,
      37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
      37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
      37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
      38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
      38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
      38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
      38, 38, 38, 38, 38, 38, 38, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
      39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
      39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
      39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 40, 40, 40, 40, 40,
      40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
      40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
      40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
      40, 40, 40, 40, 40, 40, 40, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
      41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
      41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
      41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 42, 42,
      42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
      42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
      42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
      42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 43, 43, 43, 43, 43, 43, 43, 43, 43,
      43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43,
      43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43,
      43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43, 43,
      43, 43, 43, 43, 43, 43, 43, 43, 43, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
      44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
      44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
      44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
      44, 44, 44, 44, 44, 44, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45,
      45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45,
      45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45,
      45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45,
      45, 45, 45, 45, 45, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46,
      46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46,
      46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46,
      46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46,
      46, 46, 46, 46, 46, 46, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47,
      47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47,
      47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47,
      47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47,
      47, 47, 47, 47, 47, 47, 47, 47, 47, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
      48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
      48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
      48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
      48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 49, 49, 49, 49, 49, 49, 49, 49, 49,
      49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49,
      49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49,
      49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49,
      49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 50, 50,
      50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
      50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
      50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
      50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
      50, 50, 50, 50, 50, 50, 50, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
      51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
      51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
      51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
      51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 52, 52, 52, 52, 52,
      52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
      52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
      52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
      52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
      52, 52, 52, 52, 52, 52, 52, 52, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
      53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
      53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
      53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
      53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
      54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
      54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
      54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
      54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
      54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 55, 55, 55, 55, 55, 55,
      55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
      55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
      55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
      55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
      55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
      56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
      56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
      56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
      56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
      56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57,
      57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57,
      57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57,
      57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57,
      57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57,
      57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
      58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
      58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
      58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
      58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
      58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59,
      59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59,
      59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59,
      59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59,
      59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59,
      59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 60, 60, 60, 60, 60, 60,
      60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60,
      60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60,
      60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60,
      60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60,
      60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60};

  public static final int[] SQUARES = {0, 1, 4, 9, 16, 25, 36, 49, 64, 81, 100, 121, 144, 169, 196,
      225, 256, 289, 324, 361, 400, 441, 484, 529, 576, 625, 676, 729, 784, 841, 900, 961, 1024,
      1089, 1156, 1225, 1296, 1369, 1444, 1521, 1600, 1681, 1764, 1849, 1936, 2025, 2116, 2209,
      2304, 2401, 2500, 2601, 2704, 2809, 2916, 3025, 3136, 3249, 3364, 3481, 3600, 3721, 3844,
      3969, 4096, 4225, 4356, 4489, 4624, 4761, 4900, 5041, 5184, 5329, 5476, 5625, 5776, 5929,
      6084, 6241, 6400, 6561, 6724, 6889, 7056, 7225, 7396, 7569, 7744, 7921, 8100, 8281, 8464,
      8649, 8836, 9025, 9216, 9409, 9604, 9801, 10000, 10201, 10404, 10609, 10816, 11025, 11236,
      11449, 11664, 11881, 12100, 12321, 12544, 12769, 12996, 13225, 13456, 13689, 13924, 14161,
      14400, 14641, 14884, 15129, 15376, 15625, 15876, 16129, 16384, 16641, 16900, 17161, 17424,
      17689, 17956, 18225, 18496, 18769, 19044, 19321, 19600, 19881, 20164, 20449, 20736, 21025,
      21316, 21609, 21904, 22201};

  /**
   * integer sqrt by binary search and lookup table
   * @param n (assume positive)
   * @return
   */
  public static int sqrt3(int n) {
    return SQRTS[n];
  }


  public static int sqrt4(int n) {
    int i = 0;
    while (true) {
      i++;
      if (SQUARES[i] > n) return i - 1;
    }
  }
}
