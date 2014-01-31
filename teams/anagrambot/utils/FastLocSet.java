package anagrambot.utils;

import battlecode.common.*;

public class FastLocSet {
    private static final int HASH = Math.max(GameConstants.MAP_MAX_WIDTH, GameConstants.MAP_MAX_HEIGHT);
    private int size = 0;
    private boolean[][] has = new boolean[HASH][HASH];

    public void add(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        if (!has[x][y]){
            size++;
            has[x][y] = true;
        }
    }

    public void remove(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        if (has[x][y]){
            size--;
            has[x][y] = false;
        }
    }

    public boolean contains(MapLocation loc) {
        return has[loc.x][loc.y];
    }

    public void clear() {
        has = new boolean[HASH][HASH];
    }
}