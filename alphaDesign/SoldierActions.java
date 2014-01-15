package alphaDesign;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

/**
 * This class contains static methods for soldier actions
 */
public class SoldierActions {
	public static void wander(RobotController rc, Random random) throws GameActionException {
		if(rc.isActive()) {
			Direction chosenDirection = RobotPlayer.allDirections[random.nextInt(8)];
			if(rc.canMove(chosenDirection)) {
				rc.move(chosenDirection);
			}
		}
	}
	
	public static void tryToShoot(RobotController rc) throws GameActionException {
		// Look if any enemies are nearby
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc.getTeam().opponent());
		
		// Shoot at all nearby enemies
		for(Robot enemy : enemyRobots) {
			if(rc.isActive()) {
				rc.attackSquare(rc.senseRobotInfo(enemy).location);
			}
		}
	}
}