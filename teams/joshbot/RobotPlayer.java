package joshbot;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	static Random rand;
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		
		while(true) {
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
					System.out.println("HQ Exception");
				}
			}
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						MapLocation spot = rc.senseHQLocation().add(Direction.EAST, 5);
						rc.setIndicatorString(2, spot.toString());
						if(spot.equals(rc.getLocation())) {
							rc.construct(RobotType.PASTR);
						} else {
							GameObject atspot = rc.canSenseSquare(spot) ? rc.senseObjectAtLocation(spot) : null;
							Direction move = Direction.OMNI;
							move = rc.getLocation().directionTo(spot);
							if(atspot != null) {
								if(spot.distanceSquaredTo(rc.getLocation()) <= 5) move = move.opposite();
								
							}
							int count = 0;
							while(!rc.canMove(move) && count < 9) {
								move = move.rotateLeft();
								count++;
							}
							if(rc.canMove(move)) rc.move(move);
							rc.setIndicatorString(1, move.toString());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			rc.yield();
		}
	}
}
