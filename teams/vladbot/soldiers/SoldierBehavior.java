package vladbot.soldiers;

import static vladbot.utils.Utils.*;
import vladbot.*;
import vladbot.messaging.*;
import vladbot.messaging.MessagingSystem.MessageType;
import vladbot.nav.*;
import vladbot.utils.*;
import battlecode.common.*;

public class SoldierBehavior extends RobotBehavior {

  enum Mode {
    COMBAT, RUN, FARM, EXPLORE
  };

  // state machine stuff
  Mode mode;
  MapLocation target;

  // basic data
  int bornRound = Clock.getRoundNum();
  Mover mover = new Mover();

  ArraySet<MapLocation> messagedEnemyRobots = new ArraySet<MapLocation>(100);
  int[][] enemyLastSeen = new int[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];

  ArraySet<MapLocation> attackLocations = new ArraySet<MapLocation>(100);

  public SoldierBehavior() {}

  @Override
  protected void initMessageHandlers() {
    super.initMessageHandlers();

    handlers[MessageType.ATTACK_LOCATION.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        attackLocations.insert(loc);
      }
    };

    handlers[MessageType.ENEMY_BOT.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        enemyLastSeen[loc.x][loc.y] = currentRound;
        messagedEnemyRobots.insert(loc);
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    updateUnitUtils();
    attackLocations.clear();
    messagedEnemyRobots.clear();
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void endRound() throws GameActionException {
    // sendEnemyMessages();
    messagingSystem.endRound();
  }

  @Override
  public void run() throws GameActionException {
    think();
    act();
  }

  private void think() throws GameActionException {
    if (enemyRobots.length > (RC.canSenseSquare(ENEMY_HQ) ? 1 : 0)) {
      setMode(Mode.COMBAT);
      return;
    }

    MapLocation closest = closestTarget();
    if (closest != null) {
      target = closest;
      // messagingSystem.writeAttackMessage(target, 0);
      setMode(Mode.RUN);
      return;
    }

    /*
     * MapLocation rich = findRichSquare();
     * if (rich != null) {
     * target = rich;
     * setMode(Mode.FARM);
     * return;
     * }
     */

    if (mover.arrived() || mode != Mode.EXPLORE) {
      target = findExploreLocation();
      setMode(Mode.EXPLORE);
    }
  }

  private void setMode(Mode m) {
    RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private MapLocation closestTarget() {
    MapLocation closest = null;
    int min = Integer.MAX_VALUE;

    for (int i = 0; i < attackLocations.size; i++) {
      MapLocation loc = attackLocations.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    // RC.sensePastrLocations(ENEMY_TEAM);

    for (int i = 0; i < messagedEnemyRobots.size; i++) {
      MapLocation loc = messagedEnemyRobots.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    return closest;
  }

  private void sendEnemyMessages() throws GameActionException {
    for (RobotInfo info : getEnemyRobotInfo()) {
      if (info.type == RobotType.HQ) continue;
      MapLocation loc = info.location;
      if (enemyLastSeen[loc.x][loc.y] < currentRound) {
        // enemyLastSeen[loc.x][loc.y] = currentRound;
        messagingSystem.writeEnemyBotMessage(loc);
      }
    }
  }

  /*
   * private MapLocation findRichSquare() {
   * double maxCows = currentCowsHere + 50 * COW_GROWTH[curX][curY] + 300;// favor staying here
   * double curCows;
   * MapLocation best = currentLocation;
   * for (MapLocation current : MapLocation.getAllMapLocationsWithinRadiusSq(currentLocation,
   * RobotType.SOLDIER.sensorRadiusSquared)) {
   * try {
   * if (RC.senseTerrainTile(current) == TerrainTile.OFF_MAP) continue;
   * curCows = RC.senseCowsAtLocation(current) + 50 * COW_GROWTH[current.x][current.y];
   * if (curCows > maxCows && RC.senseObjectAtLocation(current) == null) {
   * best = current;
   * maxCows = curCows;
   * }
   * } catch (GameActionException e) {
   * e.printStackTrace();
   * }
   * }
   * RC.setIndicatorString(1, "max nearby cows: " + maxCows + " at " + best);
   * if (maxCows > 1000) {
   * return best;
   * }
   * return null;
   * }
   */

  /**
   * TODO: Make this smarter.
   * @return Place to explore to.
   */
  private MapLocation findExploreLocation() {
    return ALLY_HQ.add(HQ_DX / 2, HQ_DY / 2);
  }

  private void act() throws GameActionException {
    switch (mode) {
      case COMBAT:
        Micro.micro(mover);
        break;
      case RUN:
        mover.setTarget(target);
        mover.move();
        break;
      case FARM:
        mover.setTarget(target);
        mover.execute(Mover.SNEAK);
        break;
      case EXPLORE:
        mover.setTarget(target);
        mover.move();
        break;
    }
  }
}
