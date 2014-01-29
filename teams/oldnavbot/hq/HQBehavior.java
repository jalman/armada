package oldnavbot.hq;

import static oldnavbot.utils.Utils.*;

import java.util.Arrays;
import java.util.Comparator;

import oldnavbot.RobotBehavior;
import oldnavbot.messaging.MessageHandler;
import oldnavbot.messaging.MessagingSystem.MessageType;
import oldnavbot.nav.Dijkstra;
import oldnavbot.nav.HybridMover;
import oldnavbot.utils.Pair;
import oldnavbot.utils.Utils;
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

    PASTRLocs = cowMiningLocations();

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

  private boolean goodPlaceToMakeSoldier(Direction dir) {
    return RC.canMove(dir);
  }

  private void sendMessagesOnBuild() throws GameActionException {
    // empty for now
  }

  private MapLocation[] cowMiningLocations() {

    int xparts = MAP_WIDTH < 50 ? MAP_WIDTH/10 : MAP_WIDTH/15;
    int yparts = MAP_HEIGHT < 50 ? MAP_HEIGHT/10 : MAP_HEIGHT/15;
    @SuppressWarnings("unchecked")
    Pair<MapLocation, Double>[] ret = new Pair[(1 + xparts) * (1 + yparts) / 2];
    int i = 0;
    for(int x = xparts - 1; x >= 0; x--) {
      for(int y = yparts - 1; y >= 0; y--) {
        MapLocation inittry = new MapLocation(x * MAP_WIDTH / xparts,y * MAP_HEIGHT / yparts);
        if(inittry.distanceSquaredTo(ALLY_HQ) < inittry.distanceSquaredTo(ENEMY_HQ)) {
          ret[i] = gradientDescentOnNegativeCowScalarField(inittry.x, inittry.y, 3);
          i++;
        }
      }
    }
    System.out.println(Clock.getBytecodeNum());

    Arrays.sort(ret, new Comparator<Pair<MapLocation, Double>>() {

      @Override
      public int compare(Pair<MapLocation, Double> a, Pair<MapLocation, Double> b) {
        return a == null ? 1 : b == null ? -1 : Double.compare(a.second, b.second);
      }

    });

    System.out.println(Clock.getBytecodeNum());

    MapLocation[] locs = new MapLocation[i];
    while (i-- > 0) {
      locs[i] = ret[i].first;
    }

    return locs;
  }

  private double effectiveCowGrowth(int x, int y) {
    if (RC.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.VOID) return 0.0;
    return COW_GROWTH[x][y];
  }

  private double effectiveCowGrowth(MapLocation loc) {
    TerrainTile tile = RC.senseTerrainTile(loc);
    if (tile == TerrainTile.VOID || tile == TerrainTile.OFF_MAP) return 0.0;
    return COW_GROWTH[loc.x][loc.y];
  }

  private MapLocation randomNearbyLocation(MapLocation loc, int d2) {
    MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(loc, d2);
    return locs[random.nextInt(locs.length)];
  }

  private double estimateCowGrowth(MapLocation loc) {
    int iters = 10;
    int dist = 35;

    double estimate = effectiveCowGrowth(loc);

    while (--iters > 0) {
      estimate += effectiveCowGrowth(randomNearbyLocation(loc, dist));
    }

    return estimate;
  }

  private Pair<MapLocation, Double> gradientAscent(MapLocation current) {
    int dist = 4;
    double best = estimateCowGrowth(current);

    loop: {
      for (int i = 8; i > 0; --i) {
        MapLocation loc = randomNearbyLocation(current, dist);
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

  private Pair<MapLocation, Double> gradientDescentOnNegativeCowScalarField(int x, int y, int d) {
    int xl = Math.max(x-d, 0);
    int xu = Math.min(x+d, MAP_WIDTH-1);
    int yl = Math.max(y-d, 0);
    int yu = Math.min(y+d, MAP_HEIGHT-1);
    boolean changed = false;

    for(int i = xl; i <= xu; i++) for(int j = yl; j <= yu; j++) {
      if(effectiveCowGrowth(i,j) > effectiveCowGrowth(x,y)) {
        changed = true;
        x = i;
        y = j;
      }
    }

    return !changed || d == 1 ? new Pair<MapLocation, Double>(new MapLocation(x, y),
        effectiveCowGrowth(x, y)) : gradientDescentOnNegativeCowScalarField(x, y, d - 1);
  }
}
