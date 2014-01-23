package nathbot;


import java.util.*;

import battlecode.common.*;

public class RobotPlayer {
	static Random rand;

  static int numBots, numNoiseTowers, numPastrs, numSoldiers;
  static int SoldierID = 0;

  static final boolean[][] IN_RANGE = {
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

  static final int IN_RANGE_DIAMETER = IN_RANGE.length;
  static final int IN_RANGE_OFFSET = IN_RANGE.length / 2;

  static boolean attackDelay = false;

  static Team player, enemy;
  static MapLocation myHQ, theirHQ;

	public static void run(RobotController rc) {
    player = rc.getTeam();
    enemy = rc.getTeam().equals(Team.A) ? Team.B : Team.A;
		MapLocation origin = new MapLocation(0, 0);

    myHQ = rc.senseHQLocation();
    theirHQ = rc.senseEnemyHQLocation();

		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

		while(true) {
			MapLocation loc = rc.getLocation();

			if (rc.getType() == RobotType.HQ) {
				try {
          rc.setIndicatorString(1,
              "active? " + rc.isActive() + " rounds until active: " + rc.roundsUntilActive()
                  + " action delay: " + rc.getActionDelay());

					if(rc.isActive()) {
            tryHQAttack(rc);
					}

          // Check if a robot is spawnable and spawn one if it is
          if (rc.isActive() && rc.senseRobotCount() < 25) {
            Direction toEnemy = loc.directionTo(rc.senseEnemyHQLocation());
            if (rc.senseObjectAtLocation(loc.add(toEnemy)) == null) {
              rc.spawn(toEnemy);
            }
          }

				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (RobotMicro.luge(rc)) {
						if (rc.isActive()) {
							try {
								rc.yield();
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					if (rc.isActive()) {
						int d = rc.readBroadcast(0);
						MapLocation[] m = rc.sensePastrLocations(enemy);
						rc.setIndicatorString(0, "" + d);
						if (d < 2) {
							if (m.length > 0 && rc.senseNearbyGameObjects(Robot.class, 100000000, player).length >= 7) {
								int dist = 1000000000, ind = 0;
								for (int i=0; i<m.length; ++i) {
									int dd = (m[i].x - loc.x) * (m[i].x - loc.x) + (m[i].y - loc.y) * (m[i].y - loc.y);
									if (dd < dist) {
										dist = dd;
										ind = i;
									}
								}
								rc.broadcast(0, 2);
								rc.broadcast(1, m[ind].x);
								rc.broadcast(2, m[ind].y);
							}
							else {
								if (d == 0) {
									rc.broadcast(0, 1);
									rc.broadcast(1, (2*myHQ.x + 1*theirHQ.x) / 3);
									rc.broadcast(2, (2*myHQ.y + 1*theirHQ.y) / 3);
								}
								else if (d == 1 && Clock.getRoundNum() == 200) {
									rc.broadcast(1, (1*myHQ.x + 2*theirHQ.x) / 3);
									rc.broadcast(2, (1*myHQ.y + 2*theirHQ.y) / 3);
								}
							}
						}
						if (d >= 1) {
							MapLocation trg = new MapLocation(rc.readBroadcast(1), rc.readBroadcast(2));

							if (d == 2) {
								boolean stillThere = false;
								for (int i=0; i<m.length; ++i) {
									if (trg.x == m[i].x && trg.y == m[i].y) stillThere = true;
								}
								if (!stillThere) {
									rc.broadcast(0, 1);
									rc.broadcast(1, (myHQ.x + theirHQ.x) / 2);
									rc.broadcast(2, (myHQ.y + theirHQ.y) / 2);
									trg = new MapLocation( (myHQ.x + theirHQ.x) / 2, (myHQ.y + theirHQ.y) / 2 );
								}
							}

							Direction newDir = loc.directionTo(trg);

							if (rc.isActive() && newDir != Direction.NONE && newDir != Direction.OMNI) {
								if (rc.canMove(newDir)) {
									rc.move(newDir);
								}
								else if (rc.canMove(newDir.rotateLeft())) {
									rc.move(newDir.rotateLeft());
								}
								else if (rc.canMove(newDir.rotateRight())) {
									rc.move(newDir.rotateRight());
								}
							}
						}
					}
				} catch (Exception e) {
					System.out.println("Soldier Exception");
					e.printStackTrace();
				}
			}

			rc.yield();
		}
	}

  private static void tryHQAttack(RobotController rc) {
    MapLocation loc = rc.getLocation();
    Robot[] robots = rc.senseNearbyGameObjects(Robot.class, 25);
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
        info = rc.senseRobotInfo(robot);

        int x = info.location.x - loc.x + IN_RANGE_OFFSET;
        int y = info.location.y - loc.y + IN_RANGE_OFFSET;

        if (0 <= x && x < IN_RANGE_DIAMETER && 0 <= y && y < IN_RANGE_DIAMETER) {
          if (info.team == player) {
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

        MapLocation ml =
            new MapLocation(loc.x + i - IN_RANGE_OFFSET, loc.y + j - IN_RANGE_OFFSET);
        int val = rc.senseNearbyGameObjects(Robot.class, ml, 2, enemy).length + 1;
        val -= rc.senseNearbyGameObjects(Robot.class, ml, 2, player).length;

        if (val > attackWeight) {
          attackWeight = val;
          attackX = i;
          attackY = j;
        }
      }
    }

    if (attackX != 0 || attackY != 0) {
      try {
        MapLocation target =
            new MapLocation(loc.x + attackX - IN_RANGE_OFFSET, loc.y + attackY
                - IN_RANGE_OFFSET);
        rc.attackSquare(target);
        rc.setIndicatorString(1, "target: " + target);
        // RC.setIndicatorString(0, weight[attackX - 1][attackY - 1] + " " +
        // weight[attackX][attackY - 1] + " " + weight[attackX + 1][attackY - 1]);
        // RC.setIndicatorString(1, weight[attackX - 1][attackY] + " " +
        // weight[attackX][attackY]
        // + " " + weight[attackX + 1][attackY]);
        // RC.setIndicatorString(2, weight[attackX - 1][attackY + 1] + " " +
        // weight[attackX][attackY + 1] + " " + weight[attackX + 1][attackY + 1]);
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }

  }
  private static int attackWeight(RobotType type) {
    return (type == RobotType.HQ) ? 0 : 2;
  }
}
