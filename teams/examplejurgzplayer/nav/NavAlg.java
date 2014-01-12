package examplejurgzplayer.nav;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public abstract class NavAlg {
	protected MapLocation curLoc = null, finish = null;
	
	public static final int NORMAL_MOVE_COST = 1;
	
	abstract public void recompute();
	
	abstract public void recompute(MapLocation finish);
	
	/*
	 * Return the next direction to [attempt to] move in.
	 * 
	 */
	abstract public Direction getNextDir();
}
