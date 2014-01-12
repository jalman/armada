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
					if (rc.isActive()) {
						Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 16, rc.getTeam().equals(Team.A) ? Team.B : Team.A);
						if(nearbyEnemies.length == 0 && rc.senseRobotCount() < 25) {
							Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							while(rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) != null) toEnemy = toEnemy.rotateLeft();
							rc.spawn(toEnemy);
						} else if (nearbyEnemies.length > 0){
							rc.setIndicatorString(0, nearbyEnemies.length + "");
							int attackindex = (int)(Math.random() * nearbyEnemies.length);
							RobotInfo tokill = rc.senseRobotInfo(nearbyEnemies[attackindex]);
							MapLocation killplace = tokill.location;
							if(rc.canAttackSquare(killplace)) {
								rc.attackSquare(killplace);
							}
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
						MapLocation spot2 = spot.add(rc.senseHQLocation().directionTo(spot).rotateLeft());
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
						for(b = 1; b <= 20; b++) {
							TerrainTile check = rc.senseTerrainTile(rc.getLocation().add(directions[a], b));
							if(check == TerrainTile.OFF_MAP || check == TerrainTile.VOID) {
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
}
