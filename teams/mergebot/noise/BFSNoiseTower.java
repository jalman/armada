package mergebot.noise;

import mergebot.RobotBehavior;
import mergebot.utils.Utils;
import battlecode.common.*;
import static mergebot.utils.Utils.*;

abstract public class BFSNoiseTower extends RobotBehavior {
	
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };
	Direction[][] dir = new Direction[35][35];
  MapLocation[] queue = new MapLocation[1225];
  int[][] last = new int[35][35];
  int at = 1;
	
	MapLocation target = null;
	
	public BFSNoiseTower() {
    System.out.println("start " + Clock.getBytecodeNum());
	  
		
	  queue[0] = currentLocation;
	  dir[17][17] = Direction.OMNI;


      for(int i = 7; i >= 0; i--) {
        Direction d = directions[i];
        MapLocation loc = currentLocation.add(d);
        TerrainTile there = RC.senseTerrainTile(loc);
        if(!there.isTraversableAtHeight(RobotLevel.ON_GROUND)) continue;
        int x = loc.x - currentLocation.x;
        int y = loc.y - currentLocation.y;
        dir[x+17][y+17] = d.opposite();
        queue[at] = loc;
        at++;
      }
	  
	  for(int s = 1; s < at; s++) {
	    Direction initD = dir[queue[s].x - currentLocation.x + 17][queue[s].y - currentLocation.y + 17];
	    Direction aD;
	    Direction bD;
	    if(initD.isDiagonal()) {
	      aD = initD.rotateLeft().rotateLeft();
	      bD = initD.rotateRight();
	    } else {
        aD = initD.rotateLeft().rotateLeft().rotateLeft();
        bD = initD.rotateRight().rotateRight();
      }
	    for(Direction d = aD; d != bD; d = d.rotateLeft()) {
	      MapLocation loc = queue[s].add(d);
	      TerrainTile there = RC.senseTerrainTile(loc);
	      if(!there.isTraversableAtHeight(RobotLevel.ON_GROUND)) continue;
	      int x = loc.x - currentLocation.x;
	      int y = loc.y - currentLocation.y;
        if(x*x + y*y > 200) continue;
        
	      if(dir[x+17][y+17] == null) {
	        dir[x+17][y+17] = d.opposite();
	        queue[at] = loc;
	        at++;
	      }
	    }
	  }

    System.out.println("end " + Clock.getBytecodeNum());
    System.out.println("at " + at);
	  
	}

}
