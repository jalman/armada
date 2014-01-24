package mergebot.utils;

import java.util.Arrays;
import java.util.Random;

import mergebot.messaging.MessagingSystem;
import mergebot.messaging.MessagingSystem.ReservedMessageType;
import battlecode.common.*;

public class Utils {

  public static final int JOSHBOT_CHANNEL = ReservedMessageType.JOSHBOT.channel();

  //Game constants
  // public final static int MAX_SOLDIER_ENERGON = 40;
  // public final static int MAX_ENCAMPMENT_ENERGON = 100;
  // public final static int MAX_HQ_ENERGON = 500;
  // public static final RobotType[] ROBOT_TYPE = RobotType.values();

  //actual constants

  // public static final int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
  // public static final int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};
  public static final Direction[] DIRECTIONS = Direction.values();

  public static final int[][] d = new int[8][2];
  static {
    for (int i = 0; i < 8; i++) {
      d[i][0] = DIRECTIONS[i].dx;
      d[i][1] = DIRECTIONS[i].dy;
    }
  }

  //these are set from the beginning of the game
  public static RobotController RC;
  public static Robot ROBOT;
  public static int ID;
  public static RobotType TYPE;
  public static int MAP_WIDTH, MAP_HEIGHT;
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

  //these might be set at the beginning of the round
  // public static Strategy strategy = Strategy.NORMAL;
  // public static Parameters parameters = strategy.parameters.clone();

  public static MapLocation currentLocation;
  public static int curX, curY;
  public static double currentCowsHere;
  // public static double forward;
  public static final int ENEMY_RADIUS = 6;
  public static final int SENSOR_RADIUS2 = RobotType.SOLDIER.sensorRadiusSquared;
  public static Robot[] enemyRobots = new Robot[0];

  public static void initUtils(RobotController rc) {
    RC = rc;
    ROBOT = rc.getRobot();
    TYPE = rc.getType();
    ID = ROBOT.getID();

    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();

    ALLY_TEAM = rc.getTeam();
    ENEMY_TEAM = (ALLY_TEAM == Team.A) ? Team.B : Team.A;

    ALLY_HQ = rc.senseHQLocation();
    ENEMY_HQ = rc.senseEnemyHQLocation();

    ENEMY_DIR = ALLY_HQ.directionTo(ENEMY_HQ);

    HQ_DX = ENEMY_HQ.x - ALLY_HQ.x;
    HQ_DY = ENEMY_HQ.y - ALLY_HQ.y;
    HQ_DIST = naiveDistance(ALLY_HQ,ENEMY_HQ);

    currentLocation = RC.getLocation();
    curX = currentLocation.x;
    curY = currentLocation.y;

    COW_GROWTH = RC.senseCowGrowth();

    birthRound = Clock.getRoundNum();

    random = new Random(((long)ID<< 32) ^ Clock.getRoundNum());

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

    updateBuildingUtils();
  }

  private static RobotInfo[] enemyRobotInfo = new RobotInfo[0];
  private static int roundInfoUpdated = -1;

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


  private static int dx, dy;

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

  public static boolean isFirstRound() {
    return Clock.getRoundNum() == birthRound;
  }

  public static boolean isPassable(MapLocation loc) {
    TerrainTile t = RC.senseTerrainTile(loc);
    return (t == TerrainTile.NORMAL || t == TerrainTile.ROAD);
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

  public static int clamp(int i, int min, int max) {
    if(i < min) return min;
    if(i > max) return max;
    return i;
  }

  public static <T> T[] newArray(int length, T... array) {
    return Arrays.copyOf(array, length);
  }

  public static int slowSqrt(int n) {
    int sqrt = 0;
    while(sqrt * sqrt < n) {
      sqrt++;
    }

    return sqrt - 1;
  }

  public static double evaluate(MapLocation loc) {
    int dot1 = (loc.x - ALLY_HQ.x) * HQ_DX + (loc.y - ALLY_HQ.y) * HQ_DY;
    int dot2 = (ENEMY_HQ.x - loc.x) * HQ_DX + (ENEMY_HQ.y - loc.y) * HQ_DY;

    if(dot1 < 0) return -5.0;
    if(dot2 < 0) return 5.0;

    return Math.log((double)dot1 / dot2);
  }

  public static boolean inRangeOfEnemyHQ(MapLocation loc) {
    return loc.distanceSquaredTo(ENEMY_HQ) <= RobotType.HQ.attackRadiusMaxSquared;
  }

  private static int bytecodes = 0;

  public static int countBytecodes() {
    int bc = Clock.getBytecodeNum();
    int d = bc - bytecodes;

    RC.setIndicatorString(2, Integer.toString(d));

    bytecodes = bc;
    return d;
  }

}
