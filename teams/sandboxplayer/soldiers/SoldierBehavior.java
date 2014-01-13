package sandboxplayer.soldiers;

import static sandboxplayer.soldiers.SoldierUtils.luge;
import static sandboxplayer.utils.Utils.*;
import battlecode.common.*;
import sandboxplayer.RobotBehavior;
import sandboxplayer.RobotPlayer;
import sandboxplayer.messaging.MessageHandler;
import sandboxplayer.messaging.MessagingSystem.MessageType;
import sandboxplayer.nav.Mover;

public class SoldierBehavior extends RobotBehavior {

  enum Role {
    LEFTLEFT, LEFTRIGHT, RIGHTLEFT, RIGHTRIGHT, PASTR, NONE
  };

  enum Mode {
    GOING_TO_MIDDLE, SWEEP_OUT, RETURN_HOME, STAND_RICH_LOC, FIND_PASTR_LOC,
    BUILD_PASTR, ACQUIRE_TARGET, ENGAGE, BIRTH_DECIDE_MODE
  };

  // basic data
  int bornRound = Clock.getRoundNum();
  Mover mover = new Mover();

  // state machine stuff
  Mode mode;
  int roleIndex;
  Role role;

  /**
   * true if this is the first sweep attempt, false afterwards.
   */
  boolean initialSweep;

  /**
   * true if we're panicking to build pastures at the end
   */
  boolean panicPastrBuilding;

  /**
   * whether we're done building
   */
  boolean buildingFinished = false;
  /**
   * whether to sneak to where the building should be built
   */
  int sneakToBuildingLoc = 0;

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
    role = Role.NONE;
    changeMode(Mode.BIRTH_DECIDE_MODE);
  }

  @Override
  protected void initMessageHandlers() {
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
    // turn into a pasture or noise tower
    if (buildingFinished) {
      RobotPlayer.run(RC);
    }
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void run() throws GameActionException {
    if (luge()) { // luge = micro
      // set mode to ATTACK or something
      return;
      // send message?
    } else if (mode != Mode.ENGAGE && mode != Mode.ACQUIRE_TARGET && mode != Mode.BIRTH_DECIDE_MODE
        && mode != Mode.FIND_PASTR_LOC && mode != Mode.BUILD_PASTR) {
        if (ENEMY_MILK - ALLY_MILK > 50000 || ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT) {
          changeMode(Mode.ACQUIRE_TARGET);
        } else {
          // try to build pastures if late in game
          // probably deprecated (doesn't happen)
          if (currentRound > 1800 && ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT && !panicPastrBuilding) {
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
          } else if (!initialSweep) {
            // stand on rich squares
            MapLocation loc = findRichSquare();
            if (loc != null) {
              mover.setTarget(loc);
              changeMode(Mode.STAND_RICH_LOC);
            }
          }
        }
    }

    switch (mode) {
      case BIRTH_DECIDE_MODE:
        int robotNum = RC.senseRobotCount();
        if (ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT + 1
            || (robotNum + currentRound / 100 > 22 && ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT)) {
          roleIndex = ALLY_PASTR_COUNT % 4;
          role = Role.PASTR;
          changeMode(Mode.FIND_PASTR_LOC);
          dest = new MapLocation((ALLY_HQ.x + roleLocList[roleIndex].x) / 2,
              (ALLY_HQ.y + roleLocList[roleIndex].y) / 2);
        } else {
          roleIndex = robotNum % 4;
          role = roleList[roleIndex];

          changeMode(Mode.SWEEP_OUT);
          dest = roleLocList[roleIndex];

          initialSweep = true;
        }
        mover.setTarget(dest);
        mover.move();
        break;
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
