package barnabasTheDevastator;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

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
		
		// Suicide if outnumbered and alone
		if(rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc.getTeam()).length == 0 && enemyRobots.length > 0) {
			double totalEnemyHealth = 0;
			for(Robot enemy : enemyRobots) {
				totalEnemyHealth += rc.senseRobotInfo(enemy).health;
			}
			if(totalEnemyHealth > rc.getHealth()) {
				SoldierActions.moveToLocation(rc, rc.senseRobotInfo(enemyRobots[0]).location);
				if(rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent()).length > 0) {
					// Bid fairwell, cruel world
					rc.selfDestruct();
				}
			}
			return;
		}
		
		
		// Shoot enemy with lowest HP
		RobotInfo lowestHPEnemyInfo = null;
		double lowestHP = 200;
		for(Robot enemy : enemyRobots) {
			RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
			if(enemyInfo.health <= lowestHP) {
				lowestHP = enemyInfo.health;
				lowestHPEnemyInfo = enemyInfo;
			}
		}
		if(rc.isActive() && lowestHPEnemyInfo != null && lowestHPEnemyInfo.type != RobotType.HQ) {
			rc.attackSquare(lowestHPEnemyInfo.location);
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
	
	public static void moveToLocation(RobotController rc, MapLocation location) throws GameActionException {
		rc.setIndicatorString(1, "Trying to move to " + location);
		if(rc.isActive()) {
			Direction directionToMove = rc.getLocation().directionTo(location);
			BasicPathing.tryToMove(directionToMove, true, rc);
		}
	}
	
	public static void moveToRandomPastr(RobotController rc, Random random, int squadNumber, MapLocation[] pastrs) throws GameActionException {
		MapLocation target = pastrs[random.nextInt(pastrs.length)];
		SoldierActions.issueMoveCommand(rc, squadNumber, target);
		SoldierActions.moveToLocation(rc, target);
	}
	
	public static void verifyStandingPastrMove(RobotController rc, int squadNumber, int band) throws GameActionException {
		MapLocation target = Comm.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		if(SoldierActions.isValidPastr(rc, target, rc.getTeam().opponent())) {
			// Continue move command to target
			SoldierActions.moveToLocation(rc, target);
		} else {
			SoldierActions.issueStandbyCommand(rc, squadNumber);
		}
	}

	private static boolean isValidPastr(RobotController rc, MapLocation location, Team team) {
		for(MapLocation pastrLocation : rc.sensePastrLocations(team)) {
			if (pastrLocation.equals(location)) {
				return true;
			}
		}
		return false;
	}
}