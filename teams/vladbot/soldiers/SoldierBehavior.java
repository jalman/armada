package vladbot.soldiers;

import static vladbot.soldiers.SoldierUtils.getHighestPriority;
import static vladbot.soldiers.SoldierUtils.inRange;
import static vladbot.utils.Utils.*;
import vladbot.RobotBehavior;
import vladbot.messaging.MessageHandler;
import vladbot.messaging.MessagingSystem.MessageType;
import vladbot.nav.Mover;
import vladbot.utils.ArraySet;
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

  public SoldierBehavior() {}

  @Override
  protected void initMessageHandlers() {
    super.initMessageHandlers();

    handlers[MessageType.ATTACK_LOCATION.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        // MapLocation loc = new MapLocation(message[0], message[1]);
        // mover.setTarget(loc);
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
    messagedEnemyRobots.clear();
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
  }

  @Override
  public void run() throws GameActionException {
    think();
    System.out.println(mode + " " + target);
    act();
  }

  private void think() {
    if (enemyRobots.length > 0) {
      setMode(Mode.COMBAT);
      return;
    }

    MapLocation closest = closestMessagedEnemyRobot();
    if (closest != null) {
      target = closest;
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

    if (mover.arrived()) {
      target = findExploreLocation();
      setMode(Mode.EXPLORE);
    }
  }

  private void setMode(Mode m) {
    RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private MapLocation closestMessagedEnemyRobot() {
    MapLocation closest = null;
    int min = Integer.MAX_VALUE;
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

  private MapLocation findRichSquare() {
    double maxCows = currentCowsHere + 50 * COW_GROWTH[curX][curY] + 300;// favor staying here
    double curCows;

    MapLocation best = currentLocation;
    for (MapLocation current : MapLocation.getAllMapLocationsWithinRadiusSq(currentLocation,
        RobotType.SOLDIER.sensorRadiusSquared)) {
      try {
        if (RC.senseTerrainTile(current) == TerrainTile.OFF_MAP) continue;

        curCows = RC.senseCowsAtLocation(current) + 50 * COW_GROWTH[current.x][current.y];
        if (curCows > maxCows && RC.senseObjectAtLocation(current) == null) {
          best = current;
          maxCows = curCows;
        }
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }

    RC.setIndicatorString(1, "max nearby cows: " + maxCows + " at " + best);
    if (maxCows > 1000) {
      return best;
    }

    return null;
  }

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
        attack();
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

  private void attack() throws GameActionException {
    if (RC.isActive()) {
      MapLocation loc = getHighestPriority(enemyRobots);
      // messagingSystem.writeAttackMessage(loc, 100);
      if (inRange(loc)) {
        RC.attackSquare(loc);
      }
    }
  }

}
