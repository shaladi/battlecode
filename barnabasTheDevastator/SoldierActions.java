package barnabasTheDevastator;

import java.util.Random;

import battlecode.common.Clock;
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
	
	public static void combatStrategy(RobotController rc, int band) throws GameActionException {
		// Look if any enemies are nearby
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
		
		if(enemyRobots.length > 0) {
			RobotInfo closestEnemyInfo = null;
			double closestEnemyDistance = 10000;
			double totalEnemyHP = 0;
			// Find enemy with lowest HP
			for(Robot enemy : enemyRobots) {
				RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
				int distanceToEnemy = rc.getLocation().distanceSquaredTo(enemyInfo.location);
				if(enemyInfo.type != RobotType.HQ && distanceToEnemy <= closestEnemyDistance) {
					closestEnemyDistance = distanceToEnemy;
					closestEnemyInfo = enemyInfo;
					totalEnemyHP += enemyInfo.health;
				}
			}
			if(closestEnemyInfo != null) {
				if(rc.isActive()) {
					if(rc.canAttackSquare(closestEnemyInfo.location)) {
						if(rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc.getTeam()).length == 0 && rc.getHealth() < totalEnemyHP) {
							// Send distress signal
							SoldierActions.sendDistressSignal(rc, band);
							if(rc.getLocation().distanceSquaredTo(closestEnemyInfo.location) <= 2) {
								// Suicide if outnumbered and alone
								rc.selfDestruct();
							} else {
								SoldierActions.moveToLocation(rc, closestEnemyInfo.location);
							}
						} else {
							// Attack the enemy
							rc.attackSquare(closestEnemyInfo.location);
						}
					} else {
						// Move toward enemy
						SoldierActions.moveToLocation(rc, closestEnemyInfo.location);
					}
				}
			}
		}
	}
	
	public static void issueMoveToPastrCommand(RobotController rc, int band, MapLocation location) throws GameActionException {
		int commandChannel = band + 1;
		int locationChannel = band + 2;
		
		// Cancel outstanding commands
		SoldierActions.issueStandbyCommand(rc, band);
		rc.yield();
		
		rc.broadcast(commandChannel, Comm.MOVE_TO_PASTR);
		rc.broadcast(locationChannel, Comm.locToInt(location));
	}
	
	public static void issueMoveToLocationCommand(RobotController rc, int band, MapLocation location) throws GameActionException {
		int commandChannel = band + 1;
		int locationChannel = band + 2;
		
		// Cancel outstanding commands
		SoldierActions.issueStandbyCommand(rc, band);
		rc.yield();
		
		rc.broadcast(commandChannel, Comm.MOVE_TO_LOCATION);
		rc.broadcast(locationChannel, Comm.locToInt(location));
	}
	
	public static void issueStandbyCommand(RobotController rc, int band) throws GameActionException {
		int commandChannel = band + 1;
		int locationChannel = band + 2;
		
		rc.broadcast(commandChannel, Comm.STANDBY);
		rc.broadcast(locationChannel, Comm.END_OF_COMMAND);
	}
	
	public static void issueFollowMeCommand(RobotController rc, int band) throws GameActionException {
		int commandChannel = band + 1;
		int locationChannel = band + 2;
		
		// Cancel outstanding commands
		SoldierActions.issueStandbyCommand(rc, band);
		rc.yield();
		
		rc.broadcast(commandChannel, Comm.FOLLOW_THE_LEADER);
		rc.broadcast(locationChannel, Comm.locToInt(rc.getLocation()));
	}
	
	public static void updateFollowMeCommand(RobotController rc, int band) throws GameActionException {
		int locationChannel = band + 2;
		
		rc.broadcast(locationChannel, Comm.locToInt(rc.getLocation()));
	}
	
	public static void moveToLocation(RobotController rc, MapLocation location) throws GameActionException {
		if(rc.isActive()) {
			Direction directionToMove = rc.getLocation().directionTo(location);
			BasicPathing.tryToMove(directionToMove, true, rc);
		}
	}
	
	public static void moveToRandomPastr(RobotController rc, Random random, int squadNumber, MapLocation[] pastrs) throws GameActionException {
		MapLocation target = pastrs[random.nextInt(pastrs.length)];
		SoldierActions.issueMoveToPastrCommand(rc, squadNumber, target);
		SoldierActions.moveToLocation(rc, target);
	}
	
	public static void verifyStandingPastrMove(RobotController rc, int band) throws GameActionException {
		MapLocation target = Comm.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		if(SoldierActions.isValidPastr(rc, target, rc.getTeam().opponent()) || SoldierActions.isValidPastr(rc, target, rc.getTeam())) {
			// Continue move command to target
			SoldierActions.moveToLocation(rc, target);
		} else {
			SoldierActions.issueStandbyCommand(rc, band);
		}
	}
	
	public static void verifyStandingMove(RobotController rc, int band) throws GameActionException {
		MapLocation target = Comm.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		MapLocation currentLocation = rc.getLocation();
		if(currentLocation.equals(target) || currentLocation.isAdjacentTo(target)) {
			SoldierActions.issueStandbyCommand(rc, band);
		} else {
			// Continue move command to target
			SoldierActions.moveToLocation(rc, target);
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
	
	public static void broadcastVitality(RobotController rc, int band) throws GameActionException {
		int vitalityChannel = band + Comm.VITALITY_SUBCHANNEL;
		rc.broadcast(vitalityChannel, Clock.getRoundNum());
	}
	
	public static boolean isLeaderAlive(RobotController rc, int band) throws GameActionException {
		int vitalityChannel = band + Comm.VITALITY_SUBCHANNEL;
		int roundsSinceLastVitalityBroadcast = Clock.getRoundNum() - rc.readBroadcast(vitalityChannel);
		return roundsSinceLastVitalityBroadcast <= 5;
	}
	
	public static void sendDistressSignal(RobotController rc, int band) throws GameActionException {
		int distressChannel = band + Comm.DISTRESS_SUBCHANNEL;
		rc.broadcast(distressChannel, Comm.locToInt(rc.getLocation()));
	}
	
	public static boolean isDistressSignal(RobotController rc, int band) throws GameActionException {
		int distressChannel = band + Comm.DISTRESS_SUBCHANNEL;
		return rc.readBroadcast(distressChannel) != 0;
	}
	
	public static void answerDistressSignal(RobotController rc, int band) throws GameActionException {
		int distressChannel = band + Comm.DISTRESS_SUBCHANNEL;
		MapLocation locationOfDistress = Comm.intToLoc(rc.readBroadcast(distressChannel));
		
		// Clear distress signal
		rc.broadcast(distressChannel, 0);
		
		// Cancel all outstanding commands
		SoldierActions.issueStandbyCommand(rc, band);
		rc.yield();
		
		// Move to location of distress
		SoldierActions.issueMoveToLocationCommand(rc, band, locationOfDistress);
		SoldierActions.moveToLocation(rc, locationOfDistress);
	}
}