package mergebot.hq;

import static mergebot.utils.Utils.*;
import mergebot.*;
import mergebot.Strategy.GamePhase;
import mergebot.Strategy.InitialStrategy;
import mergebot.Strategy.MidgameStrategy;
import mergebot.messaging.*;
import mergebot.messaging.MessagingSystem.MessageType;
import mergebot.messaging.MessagingSystem.ReservedMessageType;
import mergebot.nav.*;
import mergebot.utils.*;
import mergebot.utils.Utils.SymmetryType;
import battlecode.common.*;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  static GameObject[] alliedRobots;
  static int numBots; //, numNoiseTowers, numPastrs, numSoldiers;

  static boolean[] knownAlliedIDs = new boolean[10000]; // knownAlliedIDs[n] is true if n is known
  static int numSoldiersSpawned = 0;
  static Direction soldierSpawnDirection = Direction.NONE;
  static int mostRecentlySpawnedSoldierID = -1;

  static int turnsSinceLastSpawn = 0;

  private final AttackSystem attackSystem = new AttackSystem();

  public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };

  //public static final int[]

  /**
   * Good locations for PASTRs that we've determined.
   */
  public static MapLocation[] goodPASTRLocs;

  /**
   * Approximate locations of PASTRs we've actually taken.
   * Approximate because this set contains only locations that the HQ
   * requested, but the noise tower is built there and the PASTR might
   * be placed adjacent to it.
   */
  public static FastIterableLocSet takenPASTRLocs;

  /**
   * Location where HQ requests a PASTR be built. Here we assume that
   * we don't change our mind about this requested location too quickly.
   */
  public static MapLocation requestedPASTRLoc, secondRequestedPASTRLoc;

  /**
   * Whether the building bot has informed us that he has indeed built the PASTR
   */
  public static boolean PASTRMessageSent = false, secondPASTRMessageSent = false;

  /**
   * In two-PASTR strats, we want to send exactly two soldiers to build the second PASTR.
   * This counts the current number of soldiers that have been requested to go build it.
   */
  public static int numberRequestedForSecondPASTR = 0;

  /**
   * Whether the PASTR requested has actually been built
   */
  // public static boolean PASTRBuilt = false, secondPASTRBuilt = false;

  private final Dijkstra dijkstra = new Dijkstra(HybridMover.DIJKSTRA_CENTER);

  public static Strategy.GamePhase gamePhase;
  public static Strategy.InitialStrategy initialStrategy;
  public static Strategy.MidgameStrategy midgameStrategy;


  public HQBehavior() {
    try {
      messagingSystem.writeRallyPoint(ALLY_HQ.add(HQ_DX / 3, HQ_DX / 3));
    } catch (GameActionException e) {
      e.printStackTrace();
    }

    takenPASTRLocs = new FastIterableLocSet();
    knownAlliedIDs[ID] = true;

    initialGuessMapSymmetry();
    macro();
    pickStrategy();

    goodPASTRLocs = PastureFinder.cowMiningLocations();
    // TODO: take into account the strategy
    if (goodPASTRLocs.length > 0) {
      try {
        messagingSystem.writeRallyPoint(goodPASTRLocs[0]);
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }
  }

  private static void pickStrategy() {
    gamePhase = GamePhase.OPENING;
    int hqDist = ALLY_HQ.distanceSquaredTo(ENEMY_HQ);
    if (MAP_SIZE > 2500) {
      if (hqDist > 1200) {
        initialStrategy = InitialStrategy.DOUBLE_PASTR;
        midgameStrategy = MidgameStrategy.DOUBLE_PASTR_AGGRESSIVE;
      } else if (hqDist > 400) {
        initialStrategy = InitialStrategy.EARLY_SINGLE_PASTR;
        midgameStrategy = MidgameStrategy.SINGLE_PASTR_AGGRESSIVE;
      } else {
        initialStrategy = InitialStrategy.SINGLE_PASTR;
        midgameStrategy = MidgameStrategy.SINGLE_PASTR_AGGRESSIVE;
      }
    } else if (MAP_SIZE > 1200) {
      if (hqDist > 1200) {
        initialStrategy = InitialStrategy.SINGLE_PASTR;
        midgameStrategy = MidgameStrategy.SINGLE_PASTR_AGGRESSIVE;
      } else {
        initialStrategy = InitialStrategy.LATE_SINGLE_PASTR;
        midgameStrategy = MidgameStrategy.SINGLE_PASTR_AGGRESSIVE;
      }
    } else {
      initialStrategy = InitialStrategy.LATE_SINGLE_PASTR;
      midgameStrategy = MidgameStrategy.SINGLE_PASTR_AGGRESSIVE;
    }
    RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy + ", phase "
        + gamePhase);
  }


  @Override
  protected void initMessageHandlers() {
    handlers[MessageType.BUILDING_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        PASTRMessageSent = true;
      }
    };

    handlers[MessageType.BUILDING_SECOND_SIMULTANEOUS_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        secondPASTRMessageSent = true;
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    // RC.setIndicatorString(0, generators.size + " generators. " + Double.toString(actualFlux) +
    // " is pow");
    alliedRobots = RC.senseNearbyGameObjects(Robot.class, currentLocation, 10000, ALLY_TEAM);
    numBots = alliedRobots.length;

    turnsSinceLastSpawn++;

    switch (gamePhase) {
      case OPENING:
        if (ALLY_PASTR_COUNT > initialStrategy.desiredPASTRNum) {
          gamePhase = GamePhase.MIDGAME;
        }
        RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy
            + ", phase "
            + gamePhase);
        break;
      case MIDGAME:
        RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy
            + ", phase "
            + gamePhase);
        break;
      case ENDGAME:
        RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy
            + ", phase "
            + gamePhase);
        break;
      default:
        break;
    }

    messagingSystem.beginRound(handlers);
  }

  @Override
  public void run() throws GameActionException {
    attackSystem.tryAttack();
    macro();
    PASTRMessages();
    considerTeamAttacking();
  }

  private boolean dijkstraFinished = false;

  @Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
    if (!dijkstraFinished && currentRound <= 2000) {
      dijkstra.compute(9000, true);
      if (dijkstra.done()) {
        System.out.println("Dijkstra finished on round " + currentRound);
        dijkstraFinished = true;
      }
    }
  }

  /**
   * To be called only on turn 1
   */
  public static void initialGuessMapSymmetry() {
    int ax = ALLY_HQ.x, ay = ALLY_HQ.y;
    int ex = ENEMY_HQ.x, ey = ENEMY_HQ.y;

    if (ax == ex) { // equal x-values of HQs
      if (ax * 2 == MAP_WIDTH - 1) {
        MAP_SYMMETRY = SymmetryType.ROTATION_OR_VERTICAL;
      } else {
        MAP_SYMMETRY = SymmetryType.VERTICAL_REFLECTION;
      }
    } else if (ay == ey) { // equal y-values of HQs
      if (ay * 2 == MAP_HEIGHT - 1) {
        MAP_SYMMETRY = SymmetryType.ROTATION_OR_HORIZONTAL;
      } else {
        MAP_SYMMETRY = SymmetryType.HORIZONTAL_REFLECTION;
      }
    } else if (MAP_WIDTH == MAP_HEIGHT) { // square map; maybe diag reflection
      if (ax == ey && ay == ex) {
        if (ax == ay) {
          MAP_SYMMETRY = SymmetryType.ROTATION_OR_DIAGONAL_SW_NE;
        } else {
          MAP_SYMMETRY = SymmetryType.DIAGONAL_REFLECTION_SW_NE;
        }
      } else if (ax + ey == MAP_WIDTH - 1 && ay + ex == MAP_HEIGHT - 1) {
        if (ax == ay) {
          MAP_SYMMETRY = SymmetryType.ROTATION_OR_DIAGONAL_SE_NW;
        } else {
          MAP_SYMMETRY = SymmetryType.DIAGONAL_REFLECTION_SE_NW;
        }
      } else {
        MAP_SYMMETRY = SymmetryType.ROTATION;
      }
    } else { // that's all, folks (only remaining case)
      MAP_SYMMETRY = SymmetryType.ROTATION;
    }
    try {
      RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
    } catch (GameActionException e) {
      e.printStackTrace();
    }
  }

  private static void considerTeamAttacking() throws GameActionException {
    if(ALLY_PASTR_COUNT <= ENEMY_PASTR_COUNT && ENEMY_PASTR_COUNT > 0) {
      MapLocation closestEnemyPASTR = ENEMY_PASTR_LOCS[0];
      int dist = closestEnemyPASTR.distanceSquaredTo(ALLY_HQ);
      for (int i = ENEMY_PASTR_COUNT - 1; i > 0; i--) {
        int newdist = ENEMY_PASTR_LOCS[i].distanceSquaredTo(ALLY_HQ);
        if(newdist < dist) {
          dist = newdist;
          closestEnemyPASTR = ENEMY_PASTR_LOCS[i];
        }
      }
      messagingSystem.writeAttackMessage(closestEnemyPASTR);
    }
  }

  public static float defendabilityScore(MapLocation loc, Direction toEnemy) {
    //if (Clock.getRoundNum() != 400) return 0.4f;
    //this is pretty inefficient at the moment
    int ourSquaresFree = 0, ourSquaresTotal = 0, theirSquaresFree = 0, theirSquaresTotal = 0;
    System.out.println(Clock.getBytecodeNum() + "," + Clock.getRoundNum());
    MapLocation l;
    System.out.println(toEnemy.dx + "/" + toEnemy.dy);
    //String s1 = "", s2 = "";
    for (int i=-4; i<=4; ++i) {
      for (int j=-4; j<=4; ++j) {
        l = loc.add(i,j);
        if (l.x >= 0 && l.y >= 0 && l.x < MAP_WIDTH && l.y < MAP_HEIGHT) {
          Direction toL = loc.directionTo(l);
          if (toL == toEnemy || toL.rotateLeft() == toEnemy || toL.rotateRight() == toEnemy) {
            /*if (!s1.equals("")) { s1 += ","; s2 += ","; }
            s1 += "" + (l.x - loc.x);
            s2 += "" + (l.y - loc.y);*/
            if ((i >= -2 && i <= 2) && (j >= -2 && j <= 2)) {
              ourSquaresFree += (RC.senseTerrainTile(l) != TerrainTile.VOID ? 1 : 0);
              ourSquaresTotal ++;
            }
            else {
              theirSquaresFree += (RC.senseTerrainTile(l) != TerrainTile.VOID ? 1 : 0);
              theirSquaresTotal ++;
            }
          }
        }
      }
    }
    //System.out.println(s1); System.out.println(s2);
    System.out.println(Clock.getBytecodeNum() + "," + Clock.getRoundNum() + "!");
    //System.out.println("defend (" + loc.x + "," + loc.y + ") -> (" + toEnemy.dx + "," + toEnemy.dy + "): " + ourSquaresFree + "/" + ourSquaresTotal + " vs " + theirSquaresFree + "/" + theirSquaresTotal + " | " +
    //((float)ourSquaresFree / ourSquaresTotal - (float)theirSquaresFree / theirSquaresTotal) + " | turn " + Clock.getRoundNum());
    return (float)ourSquaresFree / ourSquaresTotal - (float)theirSquaresFree / theirSquaresTotal;
  }

  private static void PASTRMessages() throws GameActionException {
    if (takenPASTRLocs.getSize() > ALLY_PASTR_COUNT) { // if PASTR dies
      PASTRMessageSent = false;
    }

    if(PASTRMessageSent && ALLY_PASTR_COUNT > 0) {
      for (int i = ALLY_PASTR_COUNT - 1; i >= 0; --i) {
        if (ALLY_PASTR_LOCS[i].distanceSquaredTo(requestedPASTRLoc) < 5) {
          // 5 is some arbitrary small number... 2 might even suffice
          takenPASTRLocs.add(requestedPASTRLoc);
          break;
        }
      }
    }

    if (!PASTRMessageSent && numBots > initialStrategy.PASTRThreshold
        && ALLY_PASTR_COUNT < midgameStrategy.desiredPASTRNum) {
      // TODO don't always use midgame strategy
      requestedPASTRLoc = goodPASTRLocs[0];
      messagingSystem.writeBuildPastureMessage(requestedPASTRLoc);
    }

    /**
     * Special-case code for the second PASTR in an early-game 2-PASTR strat
     */
    if (gamePhase == GamePhase.OPENING && initialStrategy == InitialStrategy.DOUBLE_PASTR
        && numSoldiersSpawned >= 3) {

      if (secondPASTRMessageSent && numberRequestedForSecondPASTR == 2 && ALLY_PASTR_COUNT > 0) {
        // here we should actually check that the correct PASTR has been built
        for (int i = ALLY_PASTR_COUNT - 1; i >= 0; --i) {
          if (ALLY_PASTR_LOCS[i].distanceSquaredTo(secondRequestedPASTRLoc) < 5) {
            // 5 is some arbitrary small number... 2 might even suffice
            takenPASTRLocs.add(secondRequestedPASTRLoc);
            break;
          }
        }
      }

      if (turnsSinceLastSpawn == 1 && numberRequestedForSecondPASTR < 2
          && numBots > initialStrategy.secondPASTRThreshold) {
        secondRequestedPASTRLoc = goodPASTRLocs[1]; // TODO improve this choice
        // This depends on macro() happening before PASTRMessages()!!!!!!!
        messagingSystem.writeMessage(MessageType.BUILD_SECOND_SIMULTANEOUS_PASTURE,
            mostRecentlySpawnedSoldierID, secondRequestedPASTRLoc.x, secondRequestedPASTRLoc.y);
        numberRequestedForSecondPASTR++;
      }
    }
  }

  /**
   * Handle upgrades and robots.
   */
  private static void macro() {
    // if we just spawned someone, check what his id is
    if (soldierSpawnDirection != Direction.NONE) {
      GameObject[] nearbyBots = RC.senseNearbyGameObjects(Robot.class, currentLocation.add(soldierSpawnDirection), 2, ALLY_TEAM);
      for(int i=nearbyBots.length-1; i>=0; --i) {
        int id = nearbyBots[i].getID();
        if (!knownAlliedIDs[id]) {
          mostRecentlySpawnedSoldierID = id;
          knownAlliedIDs[id] = true;
          break;
        }
      }
      soldierSpawnDirection = Direction.NONE;
    }

    if (!RC.isActive()) return;

    try {
      buildSoldier();
    } catch (GameActionException e) {
      e.printStackTrace();
    }
  }

  /**
   * Tries to build a Soldier.
   * @return Whether successful.
   * @throws GameActionException
   */
  private static boolean buildSoldier() throws GameActionException {
    return buildSoldier(ALLY_HQ.directionTo(ENEMY_HQ));
  }

  /**
   * Tries to build a Soldier.
   * @param dir The direction in which to build.
   * @return Whether successful.
   * @throws GameActionException
   */
  private static boolean buildSoldier(Direction dir) throws GameActionException {
    if (RC.isActive() && RC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
      // Spawn soldier
      for (int i = 0; i < 8; i++) {
        // if square is movable, spawn soldier there and send initial messages
        if (RC.canMove(dir)) {
          // sendMessagesOnBuild();
          RC.spawn(dir);
          soldierSpawnDirection = dir;
          numSoldiersSpawned++;
          turnsSinceLastSpawn = 0;
          return true;
        }
        // otherwise keep rotating until this is possible
        dir = dir.rotateRight();
      }
      // message guys to get out of the way??
    }
    return false;
  }

  // private void sendMessagesOnBuild() throws GameActionException {
  // // empty for now
  // }

  public Direction wayToEnemy(MapLocation m) {
    MapLocation m2 = Utils.getSymmetricSquare(m);
    Direction fromD = dijkstra.from[m2.x][m2.y];
    fromD = getSymmetricDirection(fromD);
    return fromD != null ? fromD : m.directionTo(ENEMY_HQ);
  }

}
