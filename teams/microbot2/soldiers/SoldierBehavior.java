package microbot2.soldiers;

import static microbot2.utils.Utils.*;
import microbot2.*;
import microbot2.messaging.*;
import microbot2.messaging.MessagingSystem.MessageType;
import microbot2.nav.*;
import microbot2.utils.*;
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

  private final Micro micro = new Micro(this);

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
      // messagingSystem.writeAttackMessage(target);
      setMode(Mode.RUN);
      //RC.setIndicatorString(0, "RUN " + target);
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

    if (mover.arrived() || mode == Mode.COMBAT) {
      target = findExploreLocation();
      setMode(Mode.EXPLORE);
    }
  }

  private void setMode(Mode m) {
    //RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private MapLocation closestTarget() {
    MapLocation closest = null;
    int min = Integer.MAX_VALUE;

    for (int i = attackLocations.size; --i >= 0;) {
      MapLocation loc = attackLocations.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    // RC.sensePastrLocations(ENEMY_TEAM);

    for (int i = messagedEnemyRobots.size; --i >= 0;) {
      MapLocation loc = messagedEnemyRobots.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    return closest;
  }

  /**
   * TODO: Make this smarter.
   * @return Place to explore to.
   */
  private MapLocation findExploreLocation() {
    return ALLY_HQ.add(HQ_DX / 2, HQ_DY / 2);
  }

  private void act() throws GameActionException {
    if (!RC.isActive()) return;
    switch (mode) {
      case COMBAT:
    	  NathanMicro.luge();
    	  //micro.micro();
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
