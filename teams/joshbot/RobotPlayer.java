package joshbot;


import static joshbot.soldiers.SoldierUtils.getHighestPriority;
import static joshbot.soldiers.SoldierUtils.inRange;
import static joshbot.utils.Utils.*;
import battlecode.common.*;

import java.util.*;

import joshbot.utils.Utils;

public class RobotPlayer {
	static Random rand;
	public static final int[] yrangefornoise = { 20, 19, 19, 19, 19, 19, 19, 18, 18, 17, 17, 16, 16, 15, 14, 13, 12, 10,
		8, 6, 0 };
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		
		int a=0, b=20; //for noise
		double[][] cows = null; double[] cowsindir = new double[8];
		Utils.initUtils(rc);
		
		while(true) {
			Utils.updateUnitUtils();
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					tryAttack();
					if (rc.isActive()) {
						Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 16, rc.getTeam().equals(Team.A) ? Team.B : Team.A);
						if(nearbyEnemies.length == 0 && rc.senseRobotCount() < 25) {
							Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							while(rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) != null) toEnemy = toEnemy.rotateLeft();
							rc.spawn(toEnemy);
						}
					}
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						attack();
					}
					if (rc.isActive()) {
						MapLocation spot = rc.senseHQLocation().add(rc.senseHQLocation().directionTo(rc.senseEnemyHQLocation()).opposite());
						MapLocation spot2 = rc.senseHQLocation().add(rc.senseHQLocation().directionTo(spot).rotateLeft());
						if(spot.equals(rc.getLocation())) {
							rc.construct(RobotType.NOISETOWER);
						} else {
							GameObject atspot = rc.canSenseSquare(spot) ? rc.senseObjectAtLocation(spot) : null;
							Direction move = rc.getLocation().directionTo(spot);
							if(atspot != null) {
								if(spot2.equals(rc.getLocation())) {
									rc.construct(RobotType.PASTR);
								}
								GameObject atspot2 = rc.canSenseSquare(spot2) ? rc.senseObjectAtLocation(spot2) : null;
								rc.setIndicatorString(2, "asdf" + spot2);
								if(atspot2 == null) {
									move = rc.getLocation().directionTo(spot2);
									rc.setIndicatorString(2, "asdffdsa" + spot2);
								}
								else if(spot.distanceSquaredTo(rc.getLocation()) <= 24) move = move.opposite();
								if(atspot != null && atspot2 != null) joshbot.nathbot.RobotPlayer.run(rc);
							}
							int count = 0;
							while(!rc.canMove(move) && count < 9) {
								move = move.rotateLeft();
								count++;
							}
							if(rc.isActive() && rc.canMove(move)) rc.sneak(move);
							rc.setIndicatorString(1, move.toString());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (rc.getType() == RobotType.NOISETOWER) {
				try {

//					if(cows == null) {
//						cows = rc.senseCowGrowth();
//						for(int x = -20; x <= 20; x++) {
//							int range = yrangefornoise[Math.abs(x)];
//							for(int y = - range; y <= range; y++) {
//								cowsindir[Utils.getDirTowards(x,y)] += cows[curX+x][curY+y];
//							}
//						}
//					}
//					
					
					
					if(a%2 == 0) {
						MapLocation target = rc.getLocation().add(directions[a %8], b);
						if(rc.canAttackSquare(target)) rc.attackSquare(target);
						rc.yield();
						target = target.add(directions[(a+3) %8]).add(directions[(a+2) %8], 2);
						if(rc.canAttackSquare(target)) rc.attackSquare(target);
						rc.yield();
						target = target.add(directions[(a+6) %8], 6);
						if(rc.canAttackSquare(target)) rc.attackSquare(target);
					} else {
						MapLocation target = rc.getLocation().add(directions[a %8], b);
						if(rc.canAttackSquare(target)) rc.attackSquare(target);
						rc.yield();
						target = target.add(directions[(a+3) %8]).add(directions[(a+2) %8], 2);
						if(rc.canAttackSquare(target)) rc.attackSquare(target);
						rc.yield();
						target = target.add(directions[(a+6) %8], 6);
						if(rc.canAttackSquare(target)) rc.attackSquare(target);
					}
					
					if(b>4) b--;
					else {
						a++;
						a%=8;
						int c = a%2 == 0 ? 20 : 14;
						for(b = 1; b <= c; b++) {
							TerrainTile check = rc.senseTerrainTile(rc.getLocation().add(directions[a], b));
							if(check == TerrainTile.OFF_MAP) {
								b--;
								break;
							}
						}
					}
					
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
			
			rc.yield();
		}
	}
	
	  int numBots, numNoiseTowers, numPastrs, numSoldiers;
	  int SoldierID = 0;

	  private static final boolean[][] IN_RANGE = {
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

	  final static int IN_RANGE_DIAMETER = IN_RANGE.length;
	  final static int IN_RANGE_OFFSET = IN_RANGE.length / 2;

	  private static boolean attackDelay = false;
	  
	  private static int attackWeight(RobotType type) {
		    switch (type) {
		      case SOLDIER:
		        return 2;
		      case HQ:
		        return 0;
		      default:
		        return 2;
		    }
		  }
	  
	  private static void attack() {
		    try {
		      if (RC.isActive()) {
		        MapLocation loc = getHighestPriority(enemyRobots);
		        if (loc != null && inRange(loc)) {
		          RC.attackSquare(loc);
		        }
		      }
		    } catch (GameActionException e) {
		      e.printStackTrace();
		    }
		  }
	  
	  
	  private static void tryAttack() {
		    Robot[] robots = RC.senseNearbyGameObjects(Robot.class, 18);
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
}
