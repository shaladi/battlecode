package multipleclassplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class HQ {
	private final int ID;

	public HQ() {
		this.ID = RobotPlayer.ID;
		initHQ();
	}

	private void initHQ() {
		
	}

	public void runHQ() throws GameActionException {
		RobotController rc = RobotPlayer.rc;
		// Spawn Soldiers
		for(Direction direction : RobotPlayer.allDirections) {
			if(rc.isActive() && rc.canMove(direction) && rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
				rc.spawn(direction);
			}
		}
	}
}