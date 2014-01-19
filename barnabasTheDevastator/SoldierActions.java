package barnabasTheDevastator;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * This class contains static methods for soldier actions
 */
public class SoldierActions {
	public static final int squadSize = 4;
	
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
			RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
			if(rc.isActive() && enemyInfo.type != RobotType.HQ) {
				rc.attackSquare(rc.senseRobotInfo(enemy).location);
			}
		}
	}
	
	public static void issueMoveCommand(RobotController rc, int squadNumber, MapLocation location) throws GameActionException {
		int band = squadNumber * 100;
		int commandChannel = band + 1;
		int locationChannel = band + 2;
		
		rc.broadcast(commandChannel, Comm.MOVE_TO_LOCATION);
		rc.broadcast(locationChannel, Comm.locToInt(location));
	}
	
	public static void issueStandbyCommand(RobotController rc, int squadNumber) throws GameActionException {
		int band = squadNumber * 100;
		int commandChannel = band + 1;
		int locationChannel = band + 2;
		
		rc.broadcast(commandChannel, Comm.STANDBY);
		rc.broadcast(locationChannel, Comm.END_OF_COMMAND);
	}
	
	public static void moveToLocation(RobotController rc, int squadNumber, MapLocation location) throws GameActionException {
		rc.setIndicatorString(1, "Trying to move to " + location);
		if(rc.isActive()) {
			Direction directionToMove = rc.getLocation().directionTo(location);
//			for(int i = 0; i < 8; i++) {
//				if(rc.canMove(directionToMove)) {
//					rc.move(directionToMove);
//					break;
//				}
//				directionToMove = directionToMove.rotateRight();
//			}
			BasicPathing.tryToMove(directionToMove, true, rc);
		}
	}
	
	public static void moveToRandomPastr(RobotController rc, Random random, int squadNumber, MapLocation[] pastrs) throws GameActionException {
		MapLocation target = pastrs[random.nextInt(pastrs.length)];
		SoldierActions.issueMoveCommand(rc, squadNumber, target);
		SoldierActions.moveToLocation(rc, squadNumber, target);
	}

}