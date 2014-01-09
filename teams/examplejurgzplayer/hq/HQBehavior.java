package examplejurgzplayer.hq;

import static examplejurgzplayer.utils.Utils.*;
import battlecode.common.*;
import examplejurgzplayer.RobotBehavior;
import examplejurgzplayer.utils.Utils;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  int numBots, numNoiseTowers, numPastrs, numSoldiers;
  int SoldierID = 0;

  private final boolean[][] IN_RANGE = {
      {false, false, false, false, false, false, false, false, false},
      {false, false, true, true, true, true, true, false, false},
      {false, true, true, true, true, true, true, true, false},
      {false, true, true, true, true, true, true, true, false},
      {false, true, true, true, true, true, true, true, false},
      {false, true, true, true, true, true, true, true, false},
      {false, true, true, true, true, true, true, true, false},
      {false, false, true, true, true, true, true, false, false},
      {false, false, false, false, false, false, false, false, false}
  };

  final int IN_RANGE_DIAMETER = IN_RANGE.length;
  final int IN_RANGE_OFFSET = IN_RANGE.length / 2 - 1;

  private boolean attackDelay = false;

  public HQBehavior() {
}

  @Override
  public boolean beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    // RC.setIndicatorString(0, generators.size + " generators. " + Double.toString(actualFlux) +
    // " is pow");
    numBots = RC.senseNearbyGameObjects(Robot.class, currentLocation, 10000, ALLY_TEAM).length;
    return true;
  }

  @Override
  public void run() throws GameActionException {
    tryAttack();
    macro();
  }

  @Override
  public void endRound() {

  }

  private void tryAttack() {
    Robot[] robots = RC.senseNearbyGameObjects(Robot.class, 16);
    if (!attackDelay && robots.length > 0) {
      attackDelay = true;
      return;
    } else if (attackDelay && robots.length == 0) {
      attackDelay = false;
      return;
    }

    int[][] weight = new int[9][9];

    int[] enemiesX = new int[robots.length];
    int[] enemiesY = new int[robots.length];
    int numEnemies = 0;

    RobotInfo info;

    for (Robot robot : robots) {
      try {
        info = RC.senseRobotInfo(robot);

        int x = info.location.x - curX + IN_RANGE_OFFSET;
        int y = info.location.y - curY + IN_RANGE_OFFSET;

        if (0 <= x && x < IN_RANGE_DIAMETER && 0 <= y && y < IN_RANGE_DIAMETER) {
          if (info.team == ALLY_TEAM) {
            weight[x][y] = -attackWeight(info.type);

          } else {
            weight[x][y] = attackWeight(info.type);
            enemiesX[numEnemies] = x;
            enemiesY[numEnemies] = y;
            numEnemies++;
          }
        }

      } catch (GameActionException e) {
        e.printStackTrace();
      }

    }

    int attackX = 0;
    int attackY = 0;
    int attackWeight = -1000;

    if (Clock.getBytecodesLeft() > numEnemies * 500) {
      for (int n = 0; n < numEnemies; n++) {
        if (Clock.getBytecodesLeft() < 500) {
          break;
        }

        int[] iplaces = {enemiesX[n] - 1, enemiesX[n], enemiesX[n] + 1};
        int[] jplaces = {enemiesY[n] - 1, enemiesY[n], enemiesY[n] + 1};
        for (int i : iplaces) {
          for (int j : jplaces) {
            if (i <= 0 || i >= IN_RANGE_DIAMETER || j <= 0 || j >= IN_RANGE_DIAMETER) {
              continue;
            }


            if (!IN_RANGE[i][j])
              continue;


            int val = 0;
            val += weight[i - 1][j - 1];
            val += weight[i - 1][j + 1];
            val += weight[i + 1][j - 1];
            val += weight[i + 1][j + 1];
            val += weight[i][j - 1];
            val += weight[i][j + 1];
            val += weight[i - 1][j];
            val += weight[i + 1][j];
            val += weight[i][j] * 4;


            if (val > attackWeight) {
              attackWeight = val;
              attackX = i;
              attackY = j;
            }
          }
        }

      }
    } else {
      for (int n = 0; n < numEnemies; n++) {
        if (Clock.getBytecodesLeft() < 250) {
          break;
        }

        int i = enemiesX[n];
        int j = enemiesY[n];
        if (i <= 0 || i >= IN_RANGE_DIAMETER || j <= 0 || j >= IN_RANGE_DIAMETER) {
          continue;
        }


        if (!IN_RANGE[i][j])
          continue;

        MapLocation ml = new MapLocation(curX + i - IN_RANGE_OFFSET, curY + j - IN_RANGE_OFFSET);
        int val = RC.senseNearbyGameObjects(Robot.class, ml, 2, ENEMY_TEAM).length + 1;
        val -= RC.senseNearbyGameObjects(Robot.class, ml, 2, ALLY_TEAM).length;

        if (val > attackWeight) {
          attackWeight = val;
          attackX = i;
          attackY = j;
        }
      }
    }

    if (attackX != 0 || attackY != 0) {
      try {
        MapLocation target = new MapLocation(curX + attackX - IN_RANGE_OFFSET, curY + attackY
            - IN_RANGE_OFFSET);
        RC.attackSquare(target);
        RC.setIndicatorString(1, "target: " + target);
        // RC.setIndicatorString(0, weight[attackX - 1][attackY - 1] + " " +
        // weight[attackX][attackY - 1] + " " + weight[attackX + 1][attackY - 1]);
        // RC.setIndicatorString(1, weight[attackX - 1][attackY] + " " + weight[attackX][attackY]
        // + " " + weight[attackX + 1][attackY]);
        // RC.setIndicatorString(2, weight[attackX - 1][attackY + 1] + " " +
        // weight[attackX][attackY + 1] + " " + weight[attackX + 1][attackY + 1]);
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Handle upgrades and robots.
   */
  private void macro() {
    if (!RC.isActive()) return;

    boolean built = false;

    try {
      if (RC.isActive()) {
        built = buildSoldier();
      }
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
      // Spawn a Soldier
      for (int i = 0; i < 8; i++) {
        if (goodPlaceToMakeSoldier(dir)) {
          sendMessagesOnBuild();
          RC.spawn(dir);
          return true;
        }
        dir = dir.rotateRight();
      }
      // message guys to get out of the way??
    }
    return false;
  }

  private boolean goodPlaceToMakeSoldier(Direction dir) {
    return RC.canMove(dir);
  }


  private int attackWeight(RobotType type) {
    switch (type) {
      case SOLDIER:
        return 2;
      case HQ:
        return 0;
      default:
        return 2;
    }
  }

  private void sendMessagesOnBuild() throws GameActionException {
    // empty for now
  }
}
