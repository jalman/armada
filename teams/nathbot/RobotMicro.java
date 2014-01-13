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

		Robot[] nearbyTeam = rc.senseNearbyGameObjects(Robot.class, 25, player);
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 25, enemy);
		Robot[] enemiesInRange = rc.senseNearbyGameObjects(Robot.class, 10, enemy);
		
		MapLocation loc = rc.getLocation();
		
		rc.setIndicatorString(0, "team " + nearbyTeam.length + " enemy " + nearbyEnemies.length + "," + Clock.getRoundNum());
		
		if (nearbyTeam.length+1 >= nearbyEnemies.length) {
			if ((nearbyEnemies.length == 0 || nearbyTeam.length-1 >= nearbyEnemies.length) && enemiesInRange.length == 0) {
				//navigate towards where we want to go
				rc.setIndicatorString(1, "moving forward");
				return false;
			}
			else { //attack!
				rc.setIndicatorString(1, "attacking");
				String s = "";
				for (int i=0; i<enemiesInRange.length; ++i) {
					s += enemiesInRange[i].getID() + ",";
					try {
						if (rc.isActive()) {
							RobotInfo tokill = rc.senseRobotInfo(enemiesInRange[i]);
							MapLocation killplace = tokill.location;
							if(rc.canAttackSquare(killplace) && tokill.type != RobotType.HQ) {
								rc.attackSquare(killplace);
								break;
							}
						}
					}
					catch (GameActionException e) {
						e.printStackTrace();
						System.out.println("exception!1");
					}
				}
				rc.setIndicatorString(2, s);
				return true;
			}
		}
		else {
			rc.setIndicatorString(1, "retreating" + "," + Clock.getRoundNum());
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
				rc.setIndicatorString(2, "" + rc.isActive() + "," + rc.canMove(newDir) + "," + newDir.dx + "," + newDir.dy + "," + Clock.getRoundNum());
				if (rc.isActive() && newDir != Direction.NONE && newDir != Direction.OMNI) {
					if (rc.canMove(newDir)) {
						rc.move(newDir);
					}
					else if (rc.canMove(newDir.rotateLeft())) {
						rc.setIndicatorString(2, "" + newDir.rotateLeft().dx + "," + newDir.rotateLeft().dy);
						rc.move(newDir.rotateLeft());
					}
					else if (rc.canMove(newDir.rotateRight())) {
						rc.setIndicatorString(2, "" + newDir.rotateRight().dx + "," + newDir.rotateRight().dy);
						rc.move(newDir.rotateRight());
					}
				}
			}
			catch (GameActionException e) {
				e.printStackTrace();
				System.out.println("exception!3");
			}
		}
		if (nearbyEnemies.length > 0) { }
		return true;
	}
}
