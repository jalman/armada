package hybridmover2.hq;

import static hybridmover2.utils.Utils.*;
import hybridmover2.RobotBehavior;
import hybridmover2.messaging.*;
import hybridmover2.messaging.MessagingSystem.MessageType;
import hybridmover2.messaging.MessagingSystem.ReservedMessageType;
import hybridmover2.nav.Dijkstra;
import hybridmover2.nav.HybridMover;
import hybridmover2.utils.Utils;
import hybridmover2.utils.Utils.SymmetryType;
import battlecode.common.*;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  int numBots, numNoiseTowers, numPastrs, numSoldiers;
  int SoldierID = 0;

  private final AttackSystem attackSystem = new AttackSystem();

  public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };

  public static MapLocation[] PASTRLocs;
  public static boolean PASTRMessageSent = false, PASTRBuilt = false;

  private final Dijkstra dijkstra = new Dijkstra(HybridMover.DIJKSTRA_CENTER);

  public HQBehavior() {
    initialGuessMapSymmetry();
    macro();
    PASTRLocs = PastureFinder.cowMiningLocations();

    //pick a strategy
    //    double totalcows = 0.0;
    //    for(int x = Math.max(-17, -curX); x <= Math.min(17, MAP_WIDTH - 1 - curX); x++) {
    //      int range = yrangefornoise[Math.abs(x)];
    //      for(int y = Math.max(- range, -curY); y <= Math.min(range, MAP_HEIGHT - 1 - curY); y++) {
    //        totalcows += COW_GROWTH[curX+x][curY+y];
    //      }
    //    }
    //
    //    try {
    //      RC.broadcast(JOSHBOT_CHANNEL, totalcows > 150 && 10*totalcows + MAP_HEIGHT*MAP_WIDTH + 10*HQ_DIST*HQ_DIST > 11000 ? 1 : 0);
    //    } catch (GameActionException e) {
    //      e.printStackTrace();
    //    }

  }


  @Override
  protected void initMessageHandlers() {
    handlers[MessageType.BUILDING_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        PASTRMessageSent = true;
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    // RC.setIndicatorString(0, generators.size + " generators. " + Double.toString(actualFlux) +
    // " is pow");
    numBots = RC.senseNearbyGameObjects(Robot.class, currentLocation, 10000, ALLY_TEAM).length;
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
    if (ALLY_HQ.x == ENEMY_HQ.x) {
      if (ALLY_HQ.x * 2 == MAP_WIDTH - 1) {
        MAP_SYMMETRY = SymmetryType.ROTATION_OR_VERTICAL;
      } else {
        MAP_SYMMETRY = SymmetryType.VERTICAL_REFLECTION;
      }
    } else if (ALLY_HQ.y == ENEMY_HQ.y) {
      if (ALLY_HQ.y * 2 == MAP_HEIGHT - 1) {
        MAP_SYMMETRY = SymmetryType.ROTATION_OR_HORIZONTAL;
      } else {
        MAP_SYMMETRY = SymmetryType.HORIZONTAL_REFLECTION;
      }
    } else {
      MAP_SYMMETRY = SymmetryType.ROTATION;
    }
    try {
      RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
    } catch (GameActionException e) {
      e.printStackTrace();
    }
  }

  private void considerTeamAttacking() throws GameActionException {

    if(ALLY_PASTR_COUNT < ENEMY_PASTR_COUNT) {
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

  private void PASTRMessages() throws GameActionException {
    if(PASTRBuilt && ALLY_PASTR_COUNT == 0) {
      PASTRBuilt = false;
      PASTRMessageSent = false;
    }

    if(PASTRMessageSent && ALLY_PASTR_COUNT > 0) {
      PASTRBuilt = true;
    }


    if(!PASTRMessageSent && RC.senseRobotCount() > 5) {
      messagingSystem.writeBuildPastureMessage(PASTRLocs[0]);
    }

  }

  /**
   * Handle upgrades and robots.
   */
  private void macro() {
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
  boolean buildSoldier() throws GameActionException {
    return buildSoldier(ALLY_HQ.directionTo(ENEMY_HQ));
  }

  /**
   * Tries to build a Soldier.
   * @param dir The direction in which to build.
   * @return Whether successful.
   * @throws GameActionException
   */
  boolean buildSoldier(Direction dir) throws GameActionException {
    if (RC.isActive() && RC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
      // Spawn soldier
      for (int i = 0; i < 8; i++) {
        // if square is movable, spawn soldier there and send initial messages
        if (RC.canMove(dir)) {
          sendMessagesOnBuild();
          RC.spawn(dir);
          return true;
        }
        // otherwise keep rotating until this is possible
        dir = dir.rotateRight();
      }
      // message guys to get out of the way??
    }
    return false;
  }

  private void sendMessagesOnBuild() throws GameActionException {
    // empty for now
  }

}
