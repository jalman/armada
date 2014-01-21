package sandboxplayer.soldiers;

import static sandboxplayer.soldiers.SoldierUtils.luge;
import static sandboxplayer.utils.Utils.*;
import sandboxplayer.RobotBehavior;
import sandboxplayer.RobotPlayer;
import sandboxplayer.messaging.MessageHandler;
import sandboxplayer.messaging.MessagingSystem.MessageType;
import sandboxplayer.nav.Mover;
import battlecode.common.*;

public class SoldierBehavior extends RobotBehavior {

	public static boolean shouldjosh;

  enum Role {
    LEFTLEFT, LEFTRIGHT, RIGHTLEFT, RIGHTRIGHT, PASTR, NONE
  };

  enum Mode {
    GOING_TO_MIDDLE, SWEEP_OUT, RETURN_HOME, STAND_RICH_LOC, FIND_PASTR_LOC,
    BUILD_PASTR, ACQUIRE_TARGET, ENGAGE, BIRTH_DECIDE_MODE, DEFEND_PASTR
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

  public final MapLocation[] cornerPastrLocs = {new MapLocation(2, 2),
      new MapLocation(MAP_WIDTH - 3, 2), new MapLocation(MAP_WIDTH - 3, MAP_HEIGHT - 3),
      new MapLocation(2, MAP_HEIGHT - 3)};

  public SoldierBehavior() {
    role = Role.NONE;
    changeMode(Mode.BIRTH_DECIDE_MODE);
    
    try {
		shouldjosh = RC.readBroadcast(JOSHBOT_CHANNEL) == 1;
	} catch (GameActionException e) {
		e.printStackTrace();
	}
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

  public boolean joshbotbuild() throws GameActionException {
	  if(!RC.isActive()) return true;

    MapLocation spot = ALLY_HQ.add(ALLY_HQ.directionTo(ENEMY_HQ).opposite());
    MapLocation spot2 = ALLY_HQ.add(ALLY_HQ.directionTo(spot).rotateLeft());
    if (spot.equals(currentLocation)) {
			RC.construct(RobotType.NOISETOWER);
		} else {
			GameObject atspot = RC.canSenseSquare(spot) ? RC.senseObjectAtLocation(spot) : null;
      Direction move = currentLocation.directionTo(spot);
			if(atspot != null) {
        if (spot2.equals(currentLocation)) {
					RC.construct(RobotType.PASTR);
				}
				GameObject atspot2 = RC.canSenseSquare(spot2) ? RC.senseObjectAtLocation(spot2) : null;
				if(atspot2 == null) {
          move = currentLocation.directionTo(spot2);
				}
				else return false;
			}
			int count = 0;
			while(!RC.canMove(move) && count < 9) {
				move = move.rotateLeft();
				count++;
			}
			RC.move(move);
		}

		return true;
  }

  @Override
  public void run() throws GameActionException {
    if (luge()) { // luge = micro
      // set mode to ATTACK or something
      return;
      // send message?
    } else if(shouldjosh) {
    	if(!joshbotbuild()) {
    		shouldjosh = false;
    	}
    } else if (isIdle()) {
      if (ENEMY_MILK > ALLY_MILK
          || (ENEMY_PASTR_COUNT >= ALLY_PASTR_COUNT && ENEMY_PASTR_COUNT > 0)) {
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
        } else if (!initialSweep && ALLY_PASTR_COUNT == 0) {
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

          MapLocation bestCorner = null;
          int maxDist = 10000000;
          if(ALLY_HQ.distanceSquaredTo(ENEMY_HQ) <= 200) {
            for (int i = 3; i >= 0; i--) {
              int dist = ALLY_HQ.distanceSquaredTo(cornerPastrLocs[i]);
              if (dist < maxDist) {
                bestCorner = cornerPastrLocs[i];
                maxDist = dist;
              }
              dest = bestCorner;
            }
          } else {
            dest = ENEMY_HQ;
          }
          changeMode(Mode.FIND_PASTR_LOC);
        } else if (ALLY_PASTR_COUNT > ENEMY_PASTR_COUNT) {
          dest = ALLY_PASTR_LOCS[(robotNum + currentRound) % ALLY_PASTR_LOCS.length];
          changeMode(Mode.DEFEND_PASTR);
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
      // case GOING_TO_MIDDLE:
      // if (mover.arrived()) {
      // decideNextMode();
      // } else {
      // mover.movePushHome();
      // }
      // break;
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
          // RC.setIndicatorString(1, "cows here: " + currentCowsHere);
          if (currentCowsHere < 500) {
            mover.setTarget(roleLocList[roleIndex]);
            changeMode(Mode.SWEEP_OUT);
          }
        }
        break;
      case FIND_PASTR_LOC:
        // "gradient ascent": for each direction to move, find cow growth
        // of squares in radius^2 2 (instead of 5...). Move in direction of greatest thereof.

        double bestGrowth = -1,
        hereGrowth = COW_GROWTH[curX][curY];
        if (hereGrowth == 0) {
          mover.move();
          break;
        }

        MapLocation bestGrowthLoc = null;
        for (int i = 7; i >= 0; i--) {
          MapLocation squareToCheck = currentLocation.add(DIRECTIONS[i]);
          double curGrowth = COW_GROWTH[squareToCheck.x][squareToCheck.y];
          hereGrowth += curGrowth;
          if (squareToCheck.x < 2 || squareToCheck.x >= MAP_WIDTH - 2
              || squareToCheck.y < 2 || squareToCheck.y >= MAP_WIDTH - 2
              || RC.senseTerrainTile(squareToCheck) == TerrainTile.VOID) {
            continue;
          }
          for (int j = 7; j >= 0; j--) {
            MapLocation neighboringSquareToCheck = currentLocation.add(DIRECTIONS[i]);
            curGrowth += COW_GROWTH[neighboringSquareToCheck.x][neighboringSquareToCheck.y];
          }
          if (curGrowth > bestGrowth) {
            bestGrowth = curGrowth;
            bestGrowthLoc = squareToCheck;
          }
        }

        if (hereGrowth >= bestGrowth) {
          changeMode(Mode.BUILD_PASTR);
        } else {
          mover.setTarget(bestGrowthLoc);
          mover.move();
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
      case DEFEND_PASTR:
        if (!mover.arrived()) {
          mover.sneak();
        }
        break;
      case ACQUIRE_TARGET:
        int mindistance = 10000000;
        MapLocation pastrLoc,
        pastrTarget = null;
        for (int i = ENEMY_PASTR_LOCS.length - 1; i >= 0; i--) {
          pastrLoc = ENEMY_PASTR_LOCS[i];
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

  private boolean isIdle() {
    return (mode == Mode.SWEEP_OUT || mode == Mode.RETURN_HOME || mode == Mode.STAND_RICH_LOC || mode == Mode.DEFEND_PASTR);
  }

  private MapLocation findRichSquare() {
    double maxCows = currentCowsHere + 50 * COW_GROWTH[curX][curY] + 300;// favor staying here
    double curCows;

    MapLocation current, best = currentLocation;
    MapLocation[] toCheck = MapLocation.getAllMapLocationsWithinRadiusSq(currentLocation,
        RobotType.SOLDIER.sensorRadiusSquared);
    for (int i=toCheck.length-1; i>=0; i--) {
      try {
        current = toCheck[i];
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

    // RC.setIndicatorString(1, "max nearby cows: " + maxCows + " at " + best);
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
