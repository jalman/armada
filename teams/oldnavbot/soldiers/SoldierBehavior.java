package oldnavbot.soldiers;

import static oldnavbot.utils.Utils.*;
import oldnavbot.RobotBehavior;
import oldnavbot.messaging.MessageHandler;
import oldnavbot.messaging.MessagingSystem.MessageType;
import oldnavbot.nav.HybridMover;
import oldnavbot.nav.Mover;
import oldnavbot.utils.ArraySet;
import battlecode.common.*;

public class SoldierBehavior extends RobotBehavior {

  enum Mode {
    COMBAT, MOVE, FARM, EXPLORE, BUILD_PASTURE, DEFEND_PASTURE
  };

  // state machine stuff
  Mode mode;
  MapLocation target;

  // basic data
  int bornRound = Clock.getRoundNum();
  HybridMover hybrid = new HybridMover();
  Mover mover = new Mover();

  ArraySet<MapLocation> messagedEnemyRobots = new ArraySet<MapLocation>(100);
  int[][] enemyLastSeen = new int[MAP_WIDTH][MAP_HEIGHT];

  ArraySet<MapLocation> attackLocations = new ArraySet<MapLocation>(100);
  static ArraySet<MapLocation> microLocations = new ArraySet<MapLocation>(100);

  private MapLocation buildPastureLoc = null;
  private int buildPastureRound;

  private final Micro micro = new Micro(this);

  public SoldierBehavior() {
  }

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
    handlers[MessageType.MICRO_INFO.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        microLocations.insert(loc);
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

    handlers[MessageType.BUILD_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        buildPastureLoc = new MapLocation(message[0], message[1]);
        buildPastureRound = currentRound;
      }
    };

    handlers[MessageType.BUILDING_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) throws GameActionException {
        MapLocation loc = new MapLocation(message[0], message[1]);
        if (!loc.equals(currentLocation)) {
          buildPastureLoc = null;
          target = loc;
          setMode(Mode.DEFEND_PASTURE, target);
        }
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    updateUnitUtils();
    attackLocations.clear();
    microLocations.clear();
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
    //for answering pleas for help
    boolean hasNearbyPlea = false;
    for (int i=0; i<SoldierBehavior.microLocations.size; ++i) {
      MapLocation m = SoldierBehavior.microLocations.get(i);

      if (currentLocation.distanceSquaredTo(m) <= 8*8) {
        hasNearbyPlea = true;
        break;
      }
    }

    if (enemyRobots.length > (RC.canSenseSquare(ENEMY_HQ) ? 1 : 0) || hasNearbyPlea) {
      setMode(Mode.COMBAT);
      return;
    }

    if (buildPastureLoc != null) {
      // build a pasture!
      if (mode != Mode.BUILD_PASTURE) {
        target = buildPastureLoc;
        setMode(Mode.BUILD_PASTURE, target);
        return;
      } else {
        // already building a pasture
        return;
      }
    }

    // TODO: use priorities for where to be?

    MapLocation closestTarget = closestTarget();
    if (closestTarget != null) {
      target = closestTarget;
      setMode(Mode.MOVE, target);
      return;
    }

    MapLocation[] allyPastures = RC.sensePastrLocations(ALLY_TEAM);
    MapLocation closestPasture = closestLocation(allyPastures, currentLocation);

    if (closestPasture != null) {
      target = closestPasture;
      setMode(Mode.DEFEND_PASTURE, target);
      return;
    }

    // keep defending our pasture, even if it hasn't been built
    if (mode == Mode.DEFEND_PASTURE) {
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

    if (hybrid.arrived() || mode != Mode.MOVE) {
      target = findExploreLocation();
      setMode(Mode.EXPLORE);
    }
  }

  private void setMode(Mode m) {
    RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private void setMode(Mode m, MapLocation target) {
    RC.setIndicatorString(0, m + " " + target);
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

    for (int i = messagedEnemyRobots.size; --i >= 0;) {
      MapLocation loc = messagedEnemyRobots.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    return closest;
  }

  /**
   * TODO: Make this smarter.
   * @return Place to explore to.
   */
  private MapLocation findExploreLocation() {
    return ALLY_HQ.add(HQ_DX / 4, HQ_DY / 4);
  }

  private void act() throws GameActionException {
    if (!RC.isActive()) return;

    switch (mode) {
      case COMBAT:
        //micro.micro();
        if (!NathanMicro.luge(mover)) {
          //micro.micro();
        }
        break;
      case MOVE:
        hybrid.setTarget(target);
        hybrid.move();
        break;
      case FARM:
        hybrid.setTarget(target);
        hybrid.sneak();
        break;
      case EXPLORE:
        hybrid.setTarget(target);
        hybrid.move();
        break;
      case BUILD_PASTURE:
        int d = currentLocation.distanceSquaredTo(target);
        // if we're there build a noise tower
        if (d == 0) {
          RC.construct(RobotType.NOISETOWER);
          RC.setIndicatorString(1, "Building Noise Tower");
          break;
        } else if (d <= 2) {
          // if noise tower has been built build a pasture
          if (!RC.canMove(currentLocation.directionTo(target))) {
            messagingSystem.writeBuildingPastureMessage(target);
            RC.setIndicatorString(1, "Building Pasture");
            RC.construct(RobotType.PASTR);
            break;
          }
        }
        hybrid.setTarget(target);
        hybrid.move();
        break;
      case DEFEND_PASTURE:
        boolean inRange = currentLocation.distanceSquaredTo(target) < 30;
        hybrid.setTarget(target);
        if (inRange) {
          hybrid.sneak();
        } else {
          hybrid.move();
        }
        break;
      default:
        break;
    }
  }
}
