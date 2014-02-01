package mergebot.utils;

import battlecode.common.*;

public class ClothingManager extends Utils {

	public ClothingManager() {
		super();
	}
	void getDressed() {
		try {
			RC.wearHat();
		} catch (GameActionException e) {
      // e.printStackTrace();
		}
	}

}
