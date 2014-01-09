package examplejurgzplayer.soldiers;

import static examplejurgzplayer.soldiers.SoldierUtils.*;
import static examplejurgzplayer.utils.Utils.*;
import battlecode.common.*;
import examplejurgzplayer.RobotBehavior;
import examplejurgzplayer.RobotPlayer;
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
  boolean sneakToBuildingLoc; // whether to sneak to where the building should be built

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
    sneakToBuildingLoc = true;
    panicPastrBuilding = false;

    if (ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT + 1) {
      roleIndex = ALLY_PASTR_COUNT % 4;
      role = Role.PASTR;

      mode = Mode.FIND_PASTR_LOC;
      dest = new MapLocation((ALLY_HQ.x + roleLocList[roleIndex].x) / 2,
          (ALLY_HQ.y + roleLocList[roleIndex].y) / 2);
    } else {
      roleIndex = RC.senseRobotCount() % 4;
      role = roleList[roleIndex];

      mode = Mode.SWEEP_OUT;
      dest = roleLocList[roleIndex];

      initialSweep = true;
    }
    mover.setTarget(dest);
  }

  @Override
  public boolean beginRound() throws GameActionException {
    updateUnitUtils();
    updateSoldierUtils();

    if (buildingFinished) {
      RobotPlayer.run(RC);
    }
    return true;
  }

  @Override
  public void run() throws GameActionException {
    if (currentLocation.distanceSquaredTo(ENEMY_HQ) <= 15) {
      mover.setTarget(currentLocation.subtract(currentLocation.directionTo(ENEMY_HQ)));
    } else if (enemyRobots.length > 0) {
      attack();
    }

    if (mode != Mode.ENGAGE && mode != Mode.ACQUIRE_TARGET) {
      if (ENEMY_MILK - ALLY_MILK > 50000 || ENEMY_PASTR_COUNT > ALLY_PASTR_COUNT) {
        mode = Mode.ACQUIRE_TARGET;
      } else if (currentRound > 1800 && ENEMY_PASTR_COUNT == 0 && !panicPastrBuilding) {
        if (mode != Mode.BUILD_PASTR && mode != Mode.FIND_PASTR_LOC) {
          if (currentCowsHere < 150) {
            panicPastrBuilding = true;
            dest = ALLY_HQ.subtract(ALLY_HQ.directionTo(ENEMY_HQ));
            mover.setTarget(dest);
            sneakToBuildingLoc = false;
            mode = Mode.FIND_PASTR_LOC;
          } else if (currentCowsHere < 400) {
            panicPastrBuilding = true;
            dest = ALLY_HQ;
            mover.setTarget(dest);
            mode = Mode.RETURN_HOME;
          }
        }
      } else if (!initialSweep) {
        MapLocation loc = findRichSquare();
        if (loc != null) {
          mover.setTarget(loc);
        }
        mode = Mode.STAND_RICH_LOC;
      }
    }

    switch (mode) {
      case GOING_TO_MIDDLE:
        RC.setIndicatorString(0, "Going to middle");
        if (mover.arrived()) {
          decideNextMode();
        } else {
          mover.sneak();
        }
        break;
      case SWEEP_OUT:
        RC.setIndicatorString(0, "Sweep out " + role.toString());
        if (mover.arrived()) {
          dest = new MapLocation((2 * ALLY_HQ.x + roleLocList[roleIndex].x) / 3,
              (2 * ALLY_HQ.y + roleLocList[roleIndex].y) / 3);
          mover.setTarget(dest);
          mode = Mode.RETURN_HOME;
        } else {
          mover.sneak();
        }
        break;
      case RETURN_HOME:
        RC.setIndicatorString(0, "Return home");
        if (mover.arrived()) {
          dest = roleLocList[roleIndex];
          mover.setTarget(dest);
          mode = Mode.SWEEP_OUT;
          initialSweep = false;
        } else {
          mover.move();
        }
        break;
      case STAND_RICH_LOC:
        RC.setIndicatorString(0, "stand rich loc " + role.toString());
        if (!mover.arrived()) {
          mover.sneak();
        }
        break;
      case FIND_PASTR_LOC:
        RC.setIndicatorString(0, "find pastr loc " + role.toString());
        if (mover.arrived()) {
          mode = Mode.BUILD_PASTR;
        } else {
          mover.execute(sneakToBuildingLoc);
        }
        break;
      case BUILD_PASTR:
        RC.setIndicatorString(0, "build pastr " + role.toString());
        if (!RC.isConstructing() && RC.isActive()) {
          RC.construct(RobotType.PASTR);
        }

        if (RC.getConstructingRounds() == 0) {
          buildingFinished = true;
        }
        break;
      case ACQUIRE_TARGET:
        RC.setIndicatorString(0, "acquire target " + role.toString());
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
          mode = Mode.RETURN_HOME;
        } else {
          mover.setTarget(pastrTarget);
          mode = Mode.ENGAGE;
          mover.move();
        }
        break;
      case ENGAGE:
        RC.setIndicatorString(0, "engage " + role.toString());
        if(mover.arrived()) {
          mode = Mode.RETURN_HOME;
        } else {
          mover.move();
        }
        break;
      default:
        break;
    }
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

  public static final boolean[][] SOLDIER_SIGHT_RANGE = {
      {false, false, true, true, true, false, false},
      {false, true, true, true, true, true, false},
      {true, true, true, true, true, true, true},
      {true, true, true, true, true, true, true},
      {true, true, true, true, true, true, true},
      {false, true, true, true, true, true, false},
      {false, false, true, true, true, false, false}
  };
  public static final int SOLDIER_SIGHT_DIAMETER = SOLDIER_SIGHT_RANGE.length;
  public static final int SOLDIER_SIGHT_OFFSET = -3;

  private MapLocation findRichSquare() {
    double maxCows = currentCowsHere, curCows;

    MapLocation current, best = currentLocation;
    for (int x = 0; x < SOLDIER_SIGHT_DIAMETER; x++) {
      for (int y = 0; y < SOLDIER_SIGHT_DIAMETER; y++) {
        if (SOLDIER_SIGHT_RANGE[x][y]) {
          try {
            current = new MapLocation(curX + x + SOLDIER_SIGHT_OFFSET, curY + y
                + SOLDIER_SIGHT_OFFSET);
            curCows = RC.senseCowsAtLocation(current);
            if (curCows > maxCows && RC.senseObjectAtLocation(current) == null) {
              best = current;
              maxCows = curCows;
            }
          } catch (GameActionException e) {
            e.printStackTrace();
          }
        }
      }
    }

    RC.setIndicatorString(1, "max nearby cows: " + maxCows + " at " + best);
    if (maxCows < 300) {
      mode = Mode.RETURN_HOME;
    } else if (maxCows > 500) {
      return best;
    }

    return null;
  }

  private void decideNextMode() {

  }

  @Override
  public void endRound() {

  }
}
