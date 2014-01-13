package sandboxplayer.nav;
/*package sandboxplayer.nav;

import battlecode.common.*;
import sandboxplayer.RobotBehavior;
import sandboxplayer.nav.*;

public class NavSystem {
	private RobotController rc;
	private RobotBehavior rb;
	//public NavType navtype;
	public MapLocation myLoc;
	private MapLocation currentDest;
	private NavAlg navAlg;
	private Direction d;
	
	public NavSystem(RobotBehavior rb) { 
		this.rb = rb;
		this.rc = rb.rc;
		//this.navtype = NavType.BUG; //NavType.BUG_STRAIGHT_DIG; 
		this.navAlg = NavType.BUG.navAlg;
		this.myLoc = rc.getLocation();
		this.currentDest = null;
		//this.lastDirectionMoved = Direction.NONE;
	}
	
	public void changeNavType(NavType navtype) {
		this.navAlg = navtype.navAlg;
	}

	public Direction navToward(MapLocation dest) {
		//int bc = Clock.getBytecodesLeft();
		myLoc = rc.getLocation();		
		
		if(dest == null || myLoc.equals(dest)) {
			return Direction.NONE;
		}
		
		if(dest == currentDest) {
			d = navAlg.getNextDir();
		}
		if(dest == null || myLoc.equals(dest)) {
			return Direction.NONE;
		}
		// Direction d = navTowardLongRange2(dest); // just for now
		//System.out.println("Direction: " + d.toString() + ". Bytecodes used by navToward = " + Integer.toString(bc-Clock.getBytecodesLeft()));
	}
	 public Direction navTowardAStar1(MapLocation dest) {
		if(dest.equals(currentDest)) {
			Direction d = astar1.getNextDir();
			//System.out.println("RECOMPUTED0 astar1 returns direction " + d.toString());
			if(d == Direction.OMNI || d == Direction.NONE || !rc.canMove(d)) {
				System.out.println("RECOMPUTED1 astar1 returns direction " + d.toString());
				astar1.recompute();
				d = astar1.getNextDir();	// hopefully the new d is valid.....
			}
			return d;
		} else {
			currentDest = dest;
			astar1.recompute(dest);
			return astar1.getNextDir();
		}
	}

	public Direction navTowardAStar2(MapLocation dest) {
		if(dest.equals(currentDest)) {
			Direction d = astar2.getNextDir();
			//System.out.println("RECOMPUTED0 astar1 returns direction " + d.toString());
			if(d == Direction.OMNI || d == Direction.NONE || !rc.canMove(d)) {
				System.out.println("RECOMPUTED1 astar1 returns direction " + d.toString());
				astar2.recompute();
				d = astar2.getNextDir();
			}
			return d;
		} else {
			currentDest = dest;
			astar2.recompute(dest);
			return astar2.getNextDir();
		}
	}
}***/