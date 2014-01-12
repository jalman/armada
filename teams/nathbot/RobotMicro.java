package nathbot;

import battlecode.common.*;

import java.util.*;

public class RobotMicro {
	static Random rand;
	
	public static boolean luge(RobotController rc) {
		Team player = rc.getTeam();
		Team enemy = rc.getTeam().equals(Team.A) ? Team.B : Team.A;
		MapLocation origin = new MapLocation(0, 0);
		
		MapLocation myHQ = rc.senseHQLocation();
		MapLocation theirHQ = rc.senseEnemyHQLocation();

		Robot[] nearbyTeam = rc.senseNearbyGameObjects(Robot.class, 17, player);
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 17, enemy);
		
		MapLocation loc = rc.getLocation();
		
		if (nearbyTeam.length+1 >= nearbyEnemies.length) {
			if (nearbyEnemies.length == 0) {
				//navigate towards where we want to go
				return false;
			}
			else { //attack!
				for (int i=0; i<nearbyEnemies.length; ++i) {
					try {
						RobotInfo tokill = rc.senseRobotInfo(nearbyEnemies[i]);
						MapLocation killplace = tokill.location;
						if(rc.canAttackSquare(killplace) && tokill.type != RobotType.HQ) {
							rc.attackSquare(killplace);
							break;
						}
					}
					catch (GameActionException e) {
						e.printStackTrace();
						System.out.println("exception!1");
					}
				}
				return true;
			}
		}
		else {
			int dx = 0, dy = 0;
			for (int i=0; i<nearbyEnemies.length; ++i) {
				try {
					RobotInfo tokill = rc.senseRobotInfo(nearbyEnemies[i]);
					
					dx += tokill.location.x;
					dy += tokill.location.y;
				}
				catch (GameActionException e) {
					e.printStackTrace();
					System.out.println("exception!2");
				}
			}
			dx /= nearbyEnemies.length;
			dy /= nearbyEnemies.length;
			
			Direction newDir = loc.directionTo(new MapLocation(2*loc.x - dx, 2*loc.y - dy));
			
			try {
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
			catch (GameActionException e) {
				e.printStackTrace();
				System.out.println("exception!3");
			}
		}
		return true;
	}
}
