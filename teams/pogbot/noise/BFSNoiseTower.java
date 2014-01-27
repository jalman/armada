package pogbot.noise;

import pogbot.RobotBehavior;
import pogbot.utils.Utils;
import battlecode.common.*;
import static pogbot.utils.Utils.*;

abstract public class BFSNoiseTower extends RobotBehavior {
	

	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };
	Direction[][] dir = new Direction[35][35];
  MapLocation[] queue = new MapLocation[1225];
  int[][] last = new int[35][35];
  int at = 1;
	
	MapLocation target = null;
	
	public BFSNoiseTower() {
	  
		
	  queue[0] = currentLocation;
	  dir[17][17] = Direction.OMNI;
	  
	  for(int s = 0; s < at; s++) {
	    for(int i = 7; i >= 0; i--) {
	      MapLocation loc = queue[s].add(directions[i]);
	      TerrainTile there = RC.senseTerrainTile(loc);
	      if(there == TerrainTile.VOID || there == TerrainTile.OFF_MAP) continue;
	      int x = loc.x - currentLocation.x + 17;
	      if(x<0 || x >= 35 || loc.x >= MAP_WIDTH || loc.x < 0) continue;
	      int y = loc.y - currentLocation.y + 17;
        if(y<0 || y >= 35 || loc.y >= MAP_HEIGHT || loc.y < 0) continue;
        if((17-x)*(17-x) + (17-y)*(17-y) > 300) continue;
        
	      if(dir[x][y] == null) {
	        dir[x][y] = directions[(i+4)%8];
	        queue[at] = loc;
	        at++;
	      }
	    }
	  }
	  
	  System.out.println("at = " + at);
	  
	}

}
