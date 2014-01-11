package examplejurgzplayer.soldiers;

import static examplejurgzplayer.soldiers.SoldierUtils.getHighestPriority;
import static examplejurgzplayer.soldiers.SoldierUtils.inRange;
import static examplejurgzplayer.utils.Utils.ALLY_HQ;
import static examplejurgzplayer.utils.Utils.ALLY_MILK;
import static examplejurgzplayer.utils.Utils.ALLY_PASTR_COUNT;
import static examplejurgzplayer.utils.Utils.COW_GROWTH;
import static examplejurgzplayer.utils.Utils.ENEMY_HQ;
import static examplejurgzplayer.utils.Utils.ENEMY_MILK;
import static examplejurgzplayer.utils.Utils.ENEMY_PASTR_COUNT;
import static examplejurgzplayer.utils.Utils.ENEMY_PASTR_LOCS;
import static examplejurgzplayer.utils.Utils.HQ_DX;
import static examplejurgzplayer.utils.Utils.HQ_DY;
import static examplejurgzplayer.utils.Utils.RC;
import static examplejurgzplayer.utils.Utils.curX;
import static examplejurgzplayer.utils.Utils.curY;
import static examplejurgzplayer.utils.Utils.currentCowsHere;
import static examplejurgzplayer.utils.Utils.currentLocation;
import static examplejurgzplayer.utils.Utils.currentRound;
import static examplejurgzplayer.utils.Utils.enemyRobots;
import static examplejurgzplayer.utils.Utils.messagingSystem;
import static examplejurgzplayer.utils.Utils.updateUnitUtils;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;
import examplejurgzplayer.RobotBehavior;
import examplejurgzplayer.RobotPlayer;
import examplejurgzplayer.messaging.MessageHandler;
import examplejurgzplayer.messaging.MessageType;
import examplejurgzplayer.nav.Mover;

public class SoldierBehavior extends RobotBehavior {

  enum Role {
    LEFTLEFT, LEFTRIGHT, RIGHTLEFT, RIGHTRIGHT, PASTR
  };

  enum Mode {
    GOING_TO_MIDDLE, SWEEP_OUT, RETURN_HOME, STAND_RICH_LOC, FIND_PASTR_LOC,
    BUILD_PASTR, ACQUIRE_TARGET, ENGAGE
  };

  // basic data
  int bornRound;
  Mover mover;

  // state machine stuff
  Mode mode;
  int roleIndex;
  Role role;


  boolean initialSweep; // true if this is the first sweep attempt, false aftewards.

  boolean panicPastrBuilding; // true if we're panicking to build pastrs at the end

  // building info
  boolean buildingFinished; // whether we're done building
  int sneakToBuildingLoc; // whether to sneak to where the building should be built

  // map locations
  MapLocation dest;

  static final MapLocation middle = new MapLocation(ALLY_HQ.x + HQ_DX / 2, ALLY_HQ.y + HQ_DY / 2);
  static final MapLocation leftleft = new MapLocation(ALLY_HQ.x + HQ_DX / 3 - HQ_DY / 3,
      ALLY_HQ.y + HQ_DY / 3 + HQ_DX / 3);
  static final MapLocation leftright = new MapLocation(ALLY_HQ.x + HQ_DX / 3 - HQ_DY / 5,
      ALLY_HQ.y + HQ_DY / 3 + HQ_DX / 5);
  static final MapLocation rightleft = new MapLocation(ALLY_HQ.x + HQ_DX / 3 + HQ_DY / 5,
      ALLY_HQ.y + HQ_DY / 3 - HQ_DX / 5);
  static final MapLocation rightright = new MapLocation(ALLY_HQ.x + HQ_DX / 3 + HQ_DY / 3,
      ALLY_HQ.y + HQ_DY / 3 - HQ_DX / 3);

  static final Role[] roleList = {Role.LEFTLEFT, Role.LEFTRIGHT, Role.RIGHTLEFT, Role.RIGHTRIGHT};
  static final MapLocation[] roleLocList = {leftleft, leftright, rightleft, rightright};

  public SoldierBehavior() {
    bornRound = Clock.getRoundNum();
    mover = new Mover();
    buildingFinished = false;
    sneakToBuildingLoc = 0;
    panicPastrBuilding = false;

    if (ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT + 1) {
      roleIndex = ALLY_PASTR_COUNT % 4;
      role = Role.PASTR;

      changeMode(Mode.FIND_PASTR_LOC);
      dest = new MapLocation((ALLY_HQ.x + roleLocList[roleIndex].x) / 2,
          (ALLY_HQ.y + roleLocList[roleIndex].y) / 2);
    } else {
      roleIndex = RC.senseRobotCount() % 4;
      role = roleList[roleIndex];

      changeMode(Mode.SWEEP_OUT);
      dest = roleLocList[roleIndex];

      initialSweep = true;
    }
    mover.setTarget(dest);
  }

  @Override
  protected void initMessageHandlers() {
    super.initMessageHandlers();

    handlers[MessageType.ATTACK_LOCATION.ordinal()] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        // TODO: attack!
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    updateUnitUtils();
    if (buildingFinished) {
      RobotPlayer.run(RC);
    }
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void run() throws GameActionException {
    if (currentLocation.distanceSquaredTo(ENEMY_HQ) <= 25) {
      mover.setTarget(currentLocation.subtract(currentLocation.directionTo(ENEMY_HQ)));
    }

    if (enemyRobots.length > 0) {
      attack();
    } else if (mode != Mode.ENGAGE && mode != Mode.ACQUIRE_TARGET) {
      if (ENEMY_MILK - ALLY_MILK > 50000 || ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT) {
        changeMode(Mode.ACQUIRE_TARGET);
      } else if (currentRound > 1800 && ENEMY_PASTR_COUNT > 0 && !panicPastrBuilding) {
        if (mode != Mode.BUILD_PASTR && mode != Mode.FIND_PASTR_LOC) {
          if (currentCowsHere < 150) {
            panicPastrBuilding = true;
            dest = ALLY_HQ.subtract(ALLY_HQ.directionTo(ENEMY_HQ));
            mover.setTarget(dest);
            sneakToBuildingLoc = 1;
            changeMode(Mode.FIND_PASTR_LOC);
          } else if (currentCowsHere < 400) {
            panicPastrBuilding = true;
            mover.setTarget(ALLY_HQ);
            changeMode(Mode.RETURN_HOME);
          }
        }
      } else if (!initialSweep) {
        MapLocation loc = findRichSquare();
        if (loc != null) {
          mover.setTarget(loc);
          changeMode(Mode.STAND_RICH_LOC);
        }
      }
    }

    switch (mode) {
      case GOING_TO_MIDDLE:
        if (mover.arrived()) {
          decideNextMode();
        } else {
          mover.movePushHome();
        }
        break;
      case SWEEP_OUT:
        if (mover.arrived()) {
          dest = new MapLocation((2 * ALLY_HQ.x + roleLocList[roleIndex].x) / 3,
              (2 * ALLY_HQ.y + roleLocList[roleIndex].y) / 3);
          mover.setTarget(dest);
          changeMode(Mode.RETURN_HOME);
        } else {
          mover.movePushHome();
        }
        break;
      case RETURN_HOME:
        if (mover.arrived()) {
          dest = roleLocList[roleIndex];
          mover.setTarget(dest);
          changeMode(Mode.SWEEP_OUT);

          initialSweep = false;
        } else {
          mover.movePushHome();
        }
        break;
      case STAND_RICH_LOC:
        if (!mover.arrived()) {
          mover.sneak();
        } else {
          RC.setIndicatorString(1, "cows here: " + currentCowsHere);
          if (currentCowsHere < 500) {
            mover.setTarget(roleLocList[roleIndex]);
            changeMode(Mode.SWEEP_OUT);
          }
        }
        break;
      case FIND_PASTR_LOC:
        if (mover.arrived()) {
          changeMode(Mode.BUILD_PASTR);
        } else {
          mover.execute(sneakToBuildingLoc);
        }
        break;
      case BUILD_PASTR:
        if (!RC.isConstructing() && RC.isActive()) {
          RC.construct(RobotType.PASTR);
        }

        if (RC.getConstructingRounds() == 0) {
          buildingFinished = true;
        }
        break;
      case ACQUIRE_TARGET:
        int mindistance = 10000000;
        MapLocation pastrTarget = null;
        for (MapLocation pastrLoc : ENEMY_PASTR_LOCS) {
          int d = currentLocation.distanceSquaredTo(pastrLoc);
          if (d < mindistance) {
            mindistance = d;
            pastrTarget = pastrLoc;
          }
        }

        if (pastrTarget == null) {
          mover.setTarget(ALLY_HQ);
          changeMode(Mode.RETURN_HOME);
        } else {
          mover.setTarget(pastrTarget);
          changeMode(Mode.ENGAGE);
          mover.move();
        }
        break;
      case ENGAGE:
        if (mover.arrived()) {
          changeMode(Mode.RETURN_HOME);
        } else {
          mover.move();
        }
        break;
      default:
        break;
    }
  }

  private void changeMode(Mode m) {
    RC.setIndicatorString(0, currentRound + ": " + m.toString() + " " + role.toString());
    mode = m;
  }

  private void attack() {
    try {
      if (RC.isActive()) {
        MapLocation loc = getHighestPriority(enemyRobots);
        if (inRange(loc)) {
          RC.attackSquare(loc);
        }
      }
    } catch (GameActionException e) {
      e.printStackTrace();
    }
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

  private void decideNextMode() {

  }

  @Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
  }
}
