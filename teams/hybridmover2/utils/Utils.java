package hybridmover2.utils;

import static battlecode.common.Direction.*;

import java.util.Arrays;
import java.util.Random;

import hybridmover2.messaging.MessagingSystem;
import hybridmover2.messaging.MessagingSystem.ReservedMessageType;
import battlecode.common.*;

public class Utils {
  //actual constants

  // public static final int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
  // public static final int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};
  public static final TerrainTile[] TERRAIN_TILES = TerrainTile.values();
  public static final Direction[] DIRECTIONS = Direction.values();

  public static final int ROAD_DIAGONAL = 7;
  public static final int ROAD_ORTHOGONAL = 5;
  public static final int NORMAL_DIAGONAL = 14;
  public static final int NORMAL_ORTHOGONAL = 10;

  public static final int WEIGHT[][] = new int[TERRAIN_TILES.length][8];

  static {
    for (int i = 0; i < 8; i += 2) {
      WEIGHT[TerrainTile.NORMAL.ordinal()][i] = NORMAL_ORTHOGONAL;
      WEIGHT[TerrainTile.ROAD.ordinal()][i] = ROAD_ORTHOGONAL;
    }
    for (int i = 1; i < 8; i += 2) {
      WEIGHT[TerrainTile.NORMAL.ordinal()][i] = NORMAL_DIAGONAL;
      WEIGHT[TerrainTile.ROAD.ordinal()][i] = ROAD_DIAGONAL;
    }
  }

  public static final Direction[] REGULAR_DIRECTIONS = new Direction[] {
    EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST, SOUTH, SOUTH_EAST
  };

  public static final int[][] directions = new int[8][2];
  static {
    for (int i = 0; i < 8; i++) {
      directions[i][0] = DIRECTIONS[i].dx;
      directions[i][1] = DIRECTIONS[i].dy;
    }
  }

  //these are set from the beginning of the game
  public static RobotController RC;
  public static Robot ROBOT;
  public static int ID;
  public static RobotType TYPE;
  public static int MAP_WIDTH, MAP_HEIGHT, MAP_SIZE;
  public static Team ALLY_TEAM, ENEMY_TEAM;
  public static MapLocation ALLY_HQ, ENEMY_HQ;
  public static Direction ENEMY_DIR;
  public static int HQ_DX, HQ_DY;
  public static int HQ_DIST;
  public static Random random;
  public static int birthRound, currentRound;

  public static MapLocation[] ALLY_PASTR_LOCS, ENEMY_PASTR_LOCS;
  public static int ALLY_PASTR_COUNT, ENEMY_PASTR_COUNT;

  public static double ALLY_MILK, ENEMY_MILK;

  public static double[][] COW_GROWTH;

  //this is for messaging
  public static MessagingSystem messagingSystem;

  public static MapLocation currentLocation;
  public static int curX, curY;
  public static double currentCowsHere;
  // public static double forward;
  public static int SENSOR_RADIUS2;
  public static Robot[] enemyRobots = new Robot[0];


  /**
   * Type of map symmetry
   */
  public enum SymmetryType {
    ROTATION(true),
    HORIZONTAL_REFLECTION(true), // reflect across a vertical line
    VERTICAL_REFLECTION(true), // reflect across a horizontal line
    ROTATION_OR_HORIZONTAL(false), // unsure about rotation or horizontal
    ROTATION_OR_VERTICAL(false), // unsure about rotation or vertical
    ;

    public boolean certainty;
    private SymmetryType(boolean certainty) {
      this.certainty = certainty;
    }
  }

  public static SymmetryType MAP_SYMMETRY;

  public static void initUtils(RobotController rc) {
    RC = rc;
    ROBOT = rc.getRobot();
    TYPE = rc.getType();
    ID = ROBOT.getID();
    SENSOR_RADIUS2 = TYPE.sensorRadiusSquared;

    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();
    MAP_SIZE = MAP_WIDTH * MAP_HEIGHT;

    ALLY_TEAM = rc.getTeam();
    ENEMY_TEAM = (ALLY_TEAM == Team.A) ? Team.B : Team.A;

    ALLY_HQ = rc.senseHQLocation();
    ENEMY_HQ = rc.senseEnemyHQLocation();

    ENEMY_DIR = ALLY_HQ.directionTo(ENEMY_HQ);

    HQ_DX = ENEMY_HQ.x - ALLY_HQ.x;
    HQ_DY = ENEMY_HQ.y - ALLY_HQ.y;
    // HQ_DIST = naiveDistance(ALLY_HQ,ENEMY_HQ);

    currentLocation = RC.getLocation();
    curX = currentLocation.x;
    curY = currentLocation.y;

    COW_GROWTH = RC.senseCowGrowth();

    birthRound = Clock.getRoundNum();

    random = new Random(((long) ID << 32) ^ birthRound);

    messagingSystem = new MessagingSystem();

    if (TYPE == RobotType.SOLDIER) {
      updateUnitUtils();
    } else {
      updateBuildingUtils();
    }

  }

  /**
   * Called at the beginning of each round by buildings.
   */
  public static void updateBuildingUtils() {
    enemyRobots =
        RC.senseNearbyGameObjects(Robot.class, currentLocation, SENSOR_RADIUS2, ENEMY_TEAM);
    currentRound = Clock.getRoundNum();
    bytecodes = Clock.getBytecodeNum();

    ALLY_PASTR_LOCS = RC.sensePastrLocations(ALLY_TEAM);
    ENEMY_PASTR_LOCS = RC.sensePastrLocations(ENEMY_TEAM);
    ALLY_PASTR_COUNT = ALLY_PASTR_LOCS.length;
    ENEMY_PASTR_COUNT = ENEMY_PASTR_LOCS.length;

    ALLY_MILK = RC.senseTeamMilkQuantity(ALLY_TEAM);
    ENEMY_MILK = RC.senseTeamMilkQuantity(ENEMY_TEAM);
  }

  /**
   * Called at the beginning of each round by moving units.
   */
  public static void updateUnitUtils() {
    currentLocation = RC.getLocation();
    curX = currentLocation.x;
    curY = currentLocation.y;

    try {
      currentCowsHere = RC.senseCowsAtLocation(currentLocation);
    } catch (GameActionException e) {
      e.printStackTrace();
    }

    enemyRobots =
        RC.senseNearbyGameObjects(Robot.class, currentLocation, SENSOR_RADIUS2, ENEMY_TEAM);
    currentRound = Clock.getRoundNum();
    bytecodes = Clock.getBytecodeNum();
  }

  private static RobotInfo[] enemyRobotInfo = new RobotInfo[0];
  private static int roundInfoUpdated = -1;

  /**
   * Get RobotInfos of each enemy in @enemyRobots.
   * @return RobotInfos of each enemy in @enemyRobots
   * @throws GameActionException
   */
  public static RobotInfo[] getEnemyRobotInfo() throws GameActionException {
    if (roundInfoUpdated < currentRound) {
      enemyRobotInfo = new RobotInfo[enemyRobots.length];
      for (int i = enemyRobots.length - 1; i >= 0; i--) {
        enemyRobotInfo[i] = RC.senseRobotInfo(enemyRobots[i]);
      }
      roundInfoUpdated = currentRound;
    }
    return enemyRobotInfo;
  }

  /**
   * Get the MapLocation believed to be symmetric with loc.
   * If we are unsure about the map's symmetry (ROTATION_OR_HORIZONTAL or ROTATION_OR_VERTICAL),
   * further tries to determine the map's symmetry and does a bunch of checks.
   *
   * If only one check works, return the appropriate square and broadcast globally the correct symmetry.
   * If both work, choose the rotationally symmetric MapLocation.
   * @param loc
   * @return
   */
  public static MapLocation getSymmetricSquare(MapLocation loc) {
    try {
      MAP_SYMMETRY =
          SymmetryType.values()[RC.readBroadcast(ReservedMessageType.MAP_SYMMETRY.channel())];
      int x = loc.x, y = loc.y;
      MapLocation rotLoc, refLoc;
      TerrainTile here;
      double cowGrowthHere;
      switch (MAP_SYMMETRY) {
        case ROTATION:
          return new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
        case HORIZONTAL_REFLECTION:
          return new MapLocation(MAP_WIDTH - 1 - x, y);
        case VERTICAL_REFLECTION:
          return new MapLocation(x, MAP_HEIGHT - 1 - y);
        case ROTATION_OR_HORIZONTAL:
          rotLoc = new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
          refLoc = new MapLocation(MAP_WIDTH - 1 - x, y);
          here = RC.senseTerrainTile(loc);
          cowGrowthHere = COW_GROWTH[x][y];
          if (here != RC.senseTerrainTile(rotLoc)
              || cowGrowthHere != COW_GROWTH[rotLoc.x][rotLoc.y]) {
            // check rotation first
            MAP_SYMMETRY = SymmetryType.HORIZONTAL_REFLECTION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return refLoc;
          } else if (here != RC.senseTerrainTile(refLoc)
              || cowGrowthHere != COW_GROWTH[refLoc.x][refLoc.y]) {
            // then check reflection
            MAP_SYMMETRY = SymmetryType.ROTATION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return rotLoc;
          } else {
            // if both work, return rotationally symmetric guy
            return rotLoc;
          }
        case ROTATION_OR_VERTICAL:
          rotLoc = new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
          refLoc = new MapLocation(x, MAP_HEIGHT - 1 - y);
          here = RC.senseTerrainTile(loc);
          cowGrowthHere = COW_GROWTH[x][y];
          if (here != RC.senseTerrainTile(rotLoc)
              || cowGrowthHere != COW_GROWTH[rotLoc.x][rotLoc.y]) {
            // check rotation first
            MAP_SYMMETRY = SymmetryType.VERTICAL_REFLECTION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return refLoc;
          } else if (here != RC.senseTerrainTile(refLoc)
              || cowGrowthHere != COW_GROWTH[refLoc.x][refLoc.y]) {
            // then check reflection
            MAP_SYMMETRY = SymmetryType.ROTATION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return rotLoc;
          } else {
            // if both work, return rotationally symmetric guy
            return rotLoc;
          }
        default:
          return null;
      }
    } catch (GameActionException e) {
      e.printStackTrace();
      return null;
    }
  }


  private static int dx, dy;

  /**
   * Returns naiveDistance between locations loc0 and loc1,
   *  defined as max(|loc0.x - loc1.x|, |loc0.y - loc1.y|)
   * @param loc0
   * @param loc1
   * @return naiveDistance between loc0 and loc1
   */
  public static int naiveDistance(MapLocation loc0, MapLocation loc1) {
    // call takes 33 bytecodes
    // dx = loc0.x > loc1.x ? loc0.x - loc1.x : loc1.x - loc0.x;
    // dy = loc0.y > loc1.y ? loc0.y - loc1.y : loc1.y - loc0.y;
    // int c = dx > dy ? dx : dy;
    //    int bc = Clock.getBytecodeNum();
    dx = loc0.x - loc1.x; // call takes 31 bytecodes
    dy = loc0.y - loc1.y;
    dx = dx * dx > dy * dy ? dx : dy;
    return dx > 0 ? dx : -dx;
    // int c = dx > 0 ? dx : -dx;
    // int c = Math.max(Math.max(dx, dy), Math.max(-dx, -dy));
    //return naiveDistance(loc0.x, loc0.y, loc1.x, loc1.y);
    //    System.out.println("bc used by naiveDistance: " + (Clock.getBytecodeNum()-bc));
    //    return c;
  }

  /**
   * Returns naiveDistance between locations (x1, y1) and (x2, y2),
   *  defined as max(|x1-x2|, |y1-y2|)
   * @param x1
   * @param y1
   * @param x2
   * @param y2
   * @return naiveDistance between (x1, y1) and (x2, y2)
   */
  public static int naiveDistance(int x1, int y1, int x2, int y2) {
    dx = x1 > x2 ? x1 - x2 : x2 - x1;
    dy = y1 > y2 ? y1 - y2 : y2 - y1;
    return dx > dy ? dx : dy;
    // dx = x1 - x2;
    // dy = y1 - y2;
    // dx = dx*dx > dy*dy ? dx : dy;
    // return dx > 0 ? dx : -dx;
    //return Math.max(Math.abs(x1-x2), Math.abs(y1-y2));
  }

  /**
   * @return whether it's my first round executing or not
   */
  public static boolean isFirstRound() {
    return Clock.getRoundNum() == birthRound;
  }

  /**
   * Finds the closest (by naive distance) map location to the target among a set of map locations.
   * @param locs The set of map locations.
   * @param target The target location.
   * @return The closest map location.
   */
  public static MapLocation closestLocation(MapLocation[] locs, MapLocation target) {
    MapLocation close = null;
    int distance = Integer.MAX_VALUE;

    for (int i = locs.length - 1; i >= 0; i--) {
      int d = locs[i].distanceSquaredTo(target);
      if(d < distance) {
        close = locs[i];
        distance = d;
      }
    }

    return close;
  }

  public static <T> T[] newArray(int length, T... array) {
    return Arrays.copyOf(array, length);
  }

  /**
   * @param loc
   * @return Whether loc is within range (without accounting for splash...) of enemy HQ
   */
  public static boolean inRangeOfEnemyHQ(MapLocation loc) {
    return loc.distanceSquaredTo(ENEMY_HQ) <= RobotType.HQ.attackRadiusMaxSquared;
  }

  /**
   * Bytecode counting
   */
  private static int bytecodes = 0;

  /**
   * Set an indicator string with # bytecodes used since last invocation of countBytecodes
   * @return # bytecodes used since last invocation of countBytecodes
   */
  public static int countBytecodes() {
    int bc = Clock.getBytecodeNum();
    int d = bc - bytecodes;

    RC.setIndicatorString(2, Integer.toString(d));

    bytecodes = bc;
    return d;
  }

  public static boolean isPathable(MapLocation loc) {
    return RC.senseTerrainTile(loc).isTraversableAtHeight(RobotLevel.ON_GROUND);
  }

}
