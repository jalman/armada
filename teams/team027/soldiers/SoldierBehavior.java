package team027.soldiers;

import static team027.utils.Utils.*;
import team027.*;
import team027.hq.*;
import team027.messaging.*;
import team027.messaging.MessagingSystem.MessageType;
import team027.nav.*;
import team027.utils.*;
import battlecode.common.*;

public class SoldierBehavior extends RobotBehavior {

  enum Mode {
    COMBAT, MOVE, FARM, EXPLORE, BUILD_PASTURE, DEFEND_PASTURE
  };

  // state machine stuff
  Mode mode;
  static MapLocation target;

  // basic data
  static int bornRound = Clock.getRoundNum();
  HybridMover hybrid = new HybridMover();
  Mover mover = new Mover();

  static ArraySet<MapLocation> messagedEnemyRobots = new ArraySet<MapLocation>(100);
  static int[][] enemyLastSeen = new int[MAP_WIDTH][MAP_HEIGHT];

  static ArraySet<MapLocation> attackLocations = new ArraySet<MapLocation>(100);
  static ArraySet<MapLocation> microLocations = new ArraySet<MapLocation>(100);
  public static MapLocation pastrDenyRequestLoc = null;

  static MapLocation buildPastureLoc = null;
  static MapLocation buildPrecisePastureLoc = null;

  static boolean buildingSecondPastr;
  // private int buildPastureRound;

  // private final Micro micro = new Micro(this);

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
        if (!buildingSecondPastr) {
          buildPastureLoc = new MapLocation(message[0], message[1]);
        }
        // buildPastureRound = currentRound;
      }
    };

    handlers[MessageType.BUILD_SECOND_SIMULTANEOUS_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        if (ID == message[0]) {
          buildingSecondPastr = true;
          buildPastureLoc = new MapLocation(message[1], message[2]);
          target = buildPastureLoc;
          setMode(Mode.BUILD_PASTURE, target);
        }
      }
    };

    handlers[MessageType.BUILDING_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) throws GameActionException {
        MapLocation loc = new MapLocation(message[0], message[1]);
        if (!buildingSecondPastr) {
          if (mode == Mode.BUILD_PASTURE && !loc.equals(currentLocation)) {
            // System.out.println("received BUILDING_PASTR at " + loc);
            target = loc;
            setMode(Mode.DEFEND_PASTURE, target);
          }
          buildPrecisePastureLoc = null;
          buildPastureLoc = null;
        }
      }
    };

    handlers[MessageType.BUILDING_SECOND_SIMULTANEOUS_PASTURE.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) throws GameActionException {
        MapLocation loc = new MapLocation(message[0], message[1]);
        if (mode == Mode.BUILD_PASTURE && buildingSecondPastr && !loc.equals(currentLocation)) {
          buildPrecisePastureLoc = null;
          buildPastureLoc = null;
          target = loc;
        }
      }
    };

    handlers[MessageType.PASTURE_DENY_REQUEST.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) throws GameActionException {
        MapLocation loc = new MapLocation(message[0], message[1]);
        pastrDenyRequestLoc = loc;
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
    pastrDenyRequestLoc = null;
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

      if (currentLocation.distanceSquaredTo(m) <= 10*10) {
        hasNearbyPlea = true;
        break;
      }
    }

    if (enemyRobots.length > (RC.canSenseSquare(ENEMY_HQ) ? 1 : 0) || hasNearbyPlea) {
      setMode(Mode.COMBAT);
      return;
    }

    if (buildingSecondPastr) {
      return;
    }

    if (buildPastureLoc != null) {
      // build a pasture! Overwrites previous pasture target.
      target = buildPastureLoc;
      setMode(Mode.BUILD_PASTURE, target);
      return;
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
      setMode(Mode.EXPLORE, target);
    }
  }

  private void setMode(Mode m) {
    // RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private void setMode(Mode m, MapLocation target) {
    // RC.setIndicatorString(0, m + " " + target + " 2nd? " + buildingSecondPastr);
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
   * Rallies to the place chosen by the HQ.
   * @return Place to explore to.
   * @throws GameActionException
   */
  private MapLocation findExploreLocation() throws GameActionException {
    return messagingSystem.readRallyPoint();
  }

  private void act() throws GameActionException {

    switch (mode) {
      case COMBAT:
        // if (!RC.isActive()) return;
        //micro.micro();
        // if (!NathanMicro.luge(mover)) {
        //micro.micro();
        // }
        NathanMicro.luge(mover);
        break;
      case MOVE:
        hybrid.move(target);
        break;
      case FARM:
        hybrid.sneak(target);
        break;
      case EXPLORE:
        hybrid.move(target);
        break;
      case BUILD_PASTURE:
        int d = currentLocation.distanceSquaredTo(target);

        // if we're there build a noise tower
        if (d == 0) {
          if (!RC.isActive()) break;
          RC.construct(RobotType.NOISETOWER);
          // RC.setIndicatorString(1, "Building Noise Tower");
          break;
        } else if (d <= 2) {
          if (!RC.isActive()) break;
          // if noise tower has been built build a pasture
          if (!RC.canMove(currentLocation.directionTo(target))) {
            RobotInfo targetInfo = RC.senseRobotInfo((Robot) RC.senseObjectAtLocation(target));
            if ((targetInfo.type == RobotType.SOLDIER && targetInfo.constructingRounds < 10 && targetInfo.constructingType == RobotType.NOISETOWER)
                || targetInfo.type == RobotType.NOISETOWER) {

              // System.out.println("I should build a pasture near " + target);
              if (buildPrecisePastureLoc == null) {
                // System.out.println("precise pasture loc = " + buildPrecisePastureLoc);
                buildPrecisePastureLoc = getBestPastrLocAdjacentTo(target);
              }

              if (currentLocation.equals(buildPrecisePastureLoc)) {
                // System.out.println("now building pastr at " + currentLocation);
                if (buildingSecondPastr) {
                  messagingSystem.writeMessage(MessageType.BUILDING_SECOND_SIMULTANEOUS_PASTURE,
                      target.x, target.y);
                } else {
                  messagingSystem.writeBuildingPastureMessage(target);
                }
                // RC.setIndicatorString(1, "Building Pasture");
                RC.construct(RobotType.PASTR);
              } else {
                hybrid.move(buildPrecisePastureLoc);
              }
              break;
            }
          }
        }
        hybrid.move(target);
        break;
      case DEFEND_PASTURE:
        boolean inRange = currentLocation.distanceSquaredTo(target) < 30;
        if (inRange) {
          hybrid.sneak(target);
        } else {
          hybrid.move(target);
        }
        break;
      default:
        break;
    }
  }


  private boolean isAdjacentNoiseTowerBeingMade(MapLocation m) throws GameActionException {
    Direction d = Direction.NORTH;
    MapLocation loc = m.add(d);

    for(int i = 0; i < 8; i++) {
      GameObject sensed = RC.senseObjectAtLocation(loc);
      if (sensed != null) {
        RobotInfo targetInfo = RC.senseRobotInfo((Robot) sensed);
        if((targetInfo.type == RobotType.SOLDIER && targetInfo.constructingRounds < 10 && targetInfo.constructingType == RobotType.NOISETOWER)
            || targetInfo.type == RobotType.NOISETOWER) return true;
      }
      d.rotateLeft();
      loc = m.add(d);
    }
    return false;
  }

  private MapLocation getBestPastrLocAdjacentTo(MapLocation loc) {
    // System.out.println("checking around NT at " + loc);
    MapLocation[] adjLocs = MapLocation.getAllMapLocationsWithinRadiusSq(loc, 2);
    MapLocation best = null;
    double bestCowEstimate = 0;
    for (int i = adjLocs.length; --i >= 1;) {
      MapLocation loc2 = adjLocs[i];
      if (loc2.equals(loc)
          || !RC.senseTerrainTile(loc2).isTraversableAtHeight(RobotLevel.ON_GROUND)
          || loc2 == ALLY_HQ)
        continue;
      double cowEstimate = PastureFinder.estimateCowGrowth(loc2, 8, 1);
      // System.out.println("cowEstimate at " + adjLocs[i] + " is " + cowEstimate);
      if (cowEstimate > bestCowEstimate) {
        best = loc2;
        bestCowEstimate= cowEstimate;
      }
    }

    return best;
  }

}
