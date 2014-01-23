package repoMan;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Noise{
	
	public static void herdCows(RobotController rc) throws GameActionException{
		MapLocation origin = rc.getLocation();
		for(int r = 10; r > 5; r--){
			for(int i=0; i<8; i++){
				MapLocation target = origin.add(Direction.values()[i], r);
				while (!rc.isActive()){
					rc.yield();
				}
					rc.attackSquare(target);
			}

		}
	}
}