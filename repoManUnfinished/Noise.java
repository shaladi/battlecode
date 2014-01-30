package repoMan;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Noise{

	public static void herdCows(RobotController rc) throws GameActionException{
		MapLocation origin = rc.getLocation();
		for(int i=0; i<8; i++){
			for(int r = 300; r > 2; r--){
				MapLocation target = origin.add(Direction.values()[i], r);
				if(rc.canAttackSquare(target)){
					while (!rc.isActive()){
						rc.yield();
					}
					if(r > 10){
						rc.attackSquare(target);
					}
					else{
						rc.attackSquareLight(target);
					}
				}
			}

		}
	}
}