package joshbot.nathbot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer {
	static Random rand;
	
	public static void run(RobotController rc) {
		Team player = rc.getTeam();
		Team enemy = rc.getTeam().equals(Team.A) ? Team.B : Team.A;
		MapLocation origin = new MapLocation(0, 0);
		
		MapLocation myHQ = rc.senseHQLocation();
		MapLocation theirHQ = rc.senseEnemyHQLocation();
		
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		
		while(true) {
			MapLocation loc = rc.getLocation();
			
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						Direction toEnemy = loc.directionTo(rc.senseEnemyHQLocation());
						if (rc.senseObjectAtLocation(loc.add(toEnemy)) == null) {
							rc.spawn(toEnemy);
						}
					}
					
					if(rc.isActive()) {
						Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 16, enemy);
						int attackindex = (int)(Math.random() * nearbyEnemies.length);
						if (attackindex > 0) {
							RobotInfo tokill = rc.senseRobotInfo(nearbyEnemies[attackindex]);
							MapLocation killplace = tokill.location;
							if(rc.canAttackSquare(killplace)) {
								rc.attackSquare(killplace);
							}
						}
					}
					
					
				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
			}
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
					RobotMicro.luge(rc);
					
					if (rc.isActive()) {
						int d = rc.readBroadcast(0);
						MapLocation[] m = rc.sensePastrLocations(enemy);
						rc.setIndicatorString(0, "" + d);
						if (d < 2) {
							if (m.length > 0 && rc.senseNearbyGameObjects(Robot.class, 100000000, player).length >= 6) {
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
									rc.broadcast(1, (myHQ.x + 2*theirHQ.x) / 3);
									rc.broadcast(2, (myHQ.y + 2*theirHQ.y) / 3);
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
}
