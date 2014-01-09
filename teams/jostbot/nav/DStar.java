package jostbot.nav;

import battlecode.common.MapLocation;

public class DStar {
	public final static int MINE_MOVE_COST = 13;
	public final static int NORMAL_MOVE_COST = 1;
	
	public final MapLocation target;
	
	public DStar(MapLocation target) {
		this.target = target;
	}
	
	
	
}
