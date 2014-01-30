package hybridmover2.utils;

import static hybridmover2.utils.Utils.*;

import java.util.Arrays;

import battlecode.common.MapLocation;

/**
 * An (ordered) set of map locations.
 * @author vlad
 *
 */
public class LocSet {
  public boolean[][] has = new boolean[MAP_WIDTH][MAP_HEIGHT];
  public int index[][] = new int[MAP_WIDTH][MAP_HEIGHT];
  public MapLocation[] locs = new MapLocation[MAP_SIZE];
  public int size = 0;

  public LocSet(MapLocation... sources) {
    for (MapLocation loc : sources) {
      insert(loc);
    }
  }

  public void insert(MapLocation loc) {
    has[loc.x][loc.y] = true;
    index[loc.x][loc.y] = size;
    locs[size++] = loc;

    if (!get(getIndex(loc)).equals(loc)) {
      System.out.println("BUG in LocSet!");
    }
  }

  public boolean contains(MapLocation loc) {
    return has[loc.x][loc.y];
  }

  public int getIndex(MapLocation loc) {
    return index[loc.x][loc.y];
  }

  public MapLocation get(int i) {
    return locs[i];
  }

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    MapLocation[] temp = Arrays.copyOf(locs, size);
    return Arrays.toString(temp);
  }
}