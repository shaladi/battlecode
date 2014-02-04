package heinrichTheHerder;

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
import battlecode.common.TerrainTile;

public class Soldier{

	public static final int squadSize = 5;
	public static enum herdingStatus {towards, away, init}
	public static herdingStatus currentHerdingStatus;

	//TODO: later on change from random to prefer squares with high cow content. (or herd to one location and stand on it.)
	public static void wander(RobotController rc, Random random) throws GameActionException {
		if(rc.isActive()) {
			Direction chosenDirection = Direction.values()[random.nextInt(8)];
			
			double cows = rc.senseCowsAtLocation(rc.getLocation());
			float prob = (float) Math.exp(-cows/100.0);
			rc.setIndicatorString(2, "Cows are: " + cows + " Prob of moving is: " + prob );
			if(random.nextFloat() < prob && canMove(rc, chosenDirection)) {
				rc.sneak(chosenDirection);
			}
		}
	}

	public static boolean canMove(RobotController rc, Direction dir){
		return (rc.canMove(dir) && rc.getLocation().add(dir).distanceSquaredTo(rc.senseEnemyHQLocation()) > RobotType.HQ.attackRadiusMaxSquared);
	}

	public static boolean nearEnemies(RobotController rc){
		return rc.senseNearbyGameObjects(Robot.class, rc.getType().sensorRadiusSquared, rc.getTeam().opponent()).length > 0;
	}
	public static void combatStrategy(RobotController rc, int band, Random random) throws GameActionException {
		// Look if any enemies are nearby
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getType().sensorRadiusSquared, rc.getTeam().opponent());

		if(enemyRobots.length > 0) {
			RobotInfo closestEnemyInfo = null;
			RobotInfo lowestEnemyInfo = null;
			double closestEnemyDistance = 10000;
			double lowestHealth = 101;

			double totalEnemyHP = 0;
			// Find enemy with lowest HP and closest distance

			for(Robot enemy : enemyRobots) {
				RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
				int distanceToEnemy = rc.getLocation().distanceSquaredTo(enemyInfo.location);

				if(enemyInfo.type != RobotType.HQ && distanceToEnemy <= closestEnemyDistance) {
					closestEnemyDistance = distanceToEnemy;
					closestEnemyInfo = enemyInfo;
					if(enemyInfo.type.equals(RobotType.SOLDIER) && distanceToEnemy <= rc.getType().attackRadiusMaxSquared){
						//only consider soldiers for suicide move.
						//only consider robots that can shoot at you. i.e. within your attack radius
						totalEnemyHP += enemyInfo.health;
					}
				}

				//Don't care much for pastrs here. Only need to take care of soldiers.
				if(enemyInfo.type == RobotType.SOLDIER && enemyInfo.health < lowestHealth){
					lowestHealth = enemyInfo.health;
					lowestEnemyInfo = enemyInfo;
				}

			}

			//figure out which enemy to attack (Closest, or, LowestHitPoint);
			RobotInfo enemyToAttack = null;

			//TODO: perhaps better heuristics are needed.
			//perhaps if lowestHitPoint enemy is in attackRange, shoot at him. Or if all I need to do is land one hit, give it a shot.
			if(lowestEnemyInfo != null && rc.getLocation().distanceSquaredTo(lowestEnemyInfo.location) <= rc.getType().attackRadiusMaxSquared | lowestEnemyInfo.health <= 10){
				enemyToAttack = lowestEnemyInfo;
			}else{
				enemyToAttack = closestEnemyInfo;
			}

			if(enemyToAttack != null) {
				if(rc.isActive()) {
					if(rc.canAttackSquare(enemyToAttack.location)) {
						//TODO: Change to if total friendly health is less than total enemy health to avoid unnecessary suicide. 
						Robot[] friends = rc.senseNearbyGameObjects(Robot.class, rc.getType().sensorRadiusSquared, rc.getTeam());
						double alliedHealth = rc.getHealth();
						double fightAlliedHealth = rc.getHealth();
						for(int i=0; i<friends.length; i++){
							RobotInfo friend = rc.senseRobotInfo(friends[i]);
							alliedHealth += friend.health;
							if(rc.getLocation().distanceSquaredTo(friend.location) <= rc.getType().attackRadiusMaxSquared){
								fightAlliedHealth += friend.health;
							}
						}
						//Retreat if out numbered but backup is available.
						if(fightAlliedHealth < totalEnemyHP) {
							// Send distress signal
							Soldier.sendDistressSignal(rc, band);

							Direction retreatDir = rc.getLocation().directionTo(enemyToAttack.location).opposite();
							if(alliedHealth > totalEnemyHP && rc.getHealth() > 50 && canMove(rc, retreatDir)){
								// If you have enough health, and there's hope for you, Retreat in opposite direction.
								rc.move(retreatDir);
							}
							else if(rc.getLocation().distanceSquaredTo(enemyToAttack.location) <= 2) {
								// Suicide if outnumbered and alone
								rc.selfDestruct();
							} else {
								//TODO: Perhaps a simple move command will do.
								Soldier.moveToLocation(rc, enemyToAttack.location, random, false);
								//BasicPathing.simplemove(rc, enemyToAttack.location);
							}
						} else {
							//If an enemy is too close, send distress signal and run! He's trying to bomb you.
							//Stay at least 3 away from enemy if possible.
//							Direction runDir = rc.getLocation().directionTo(closestEnemyInfo.location).opposite();
//							if(rc.getLocation().distanceSquaredTo(closestEnemyInfo.location) < 3 && rc.canMove(runDir)){
//								Soldier.sendDistressSignal(rc, band);
//								rc.move(runDir);
//							}
//							else{
								// Attack the enemy
								rc.attackSquare(enemyToAttack.location);
//							}
						}
					} else {
						//TODO: when in combat, perhaps a simple move command will do. To reduce bytecode usage.
						//Move toward enemy (if pastr)
						
						if(enemyToAttack.type == RobotType.PASTR || enemyToAttack.type == RobotType.NOISETOWER || RobotPlayer.panic){
							Soldier.moveToLocation(rc, enemyToAttack.location, random, false);
						}else{
							MapLocation oneCloser = rc.getLocation().add(rc.getLocation().directionTo(enemyToAttack.location));
							if(oneCloser.distanceSquaredTo(enemyToAttack.location)>enemyToAttack.type.attackRadiusMaxSquared){
								Soldier.moveToLocation(rc, oneCloser, random, false);
						}
							
						}
						//BasicPathing.simplemove(rc, enemyToAttack.location);
					}
				}
			}
		}
	}

	public static void issueMoveToEnemyPastrCommand(RobotController rc, int band, MapLocation location) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int locationChannel = band + Comm.LOCATION_SUBCHANNEL;

		rc.broadcast(commandChannel, Comm.MOVE_TO_ENEMY_PASTR);
		rc.broadcast(locationChannel, VectorActions.locToInt(location));
	}
	
	public static void issueMoveToFriendlyPastrCommand(RobotController rc, int band, MapLocation location) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int locationChannel = band + Comm.LOCATION_SUBCHANNEL;

		rc.broadcast(commandChannel, Comm.MOVE_TO_FRIENDLY_PASTR);
		rc.broadcast(locationChannel, VectorActions.locToInt(location));
	}

	public static void issueMoveToLocationCommand(RobotController rc, int band, MapLocation location) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int locationChannel = band + Comm.LOCATION_SUBCHANNEL;

		rc.broadcast(commandChannel, Comm.MOVE_TO_LOCATION);
		rc.broadcast(locationChannel, VectorActions.locToInt(location));
	}

	public static void issueStandbyCommand(RobotController rc, int band) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int locationChannel = band + Comm.LOCATION_SUBCHANNEL;

		rc.broadcast(commandChannel, Comm.STANDBY);
		rc.broadcast(locationChannel, Comm.END_OF_COMMAND);
	}

	public static void issueFollowMeCommand(RobotController rc, int band) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int locationChannel = band + Comm.LOCATION_SUBCHANNEL;

		if(rc.readBroadcast(commandChannel) != Comm.WANDER) {
			rc.broadcast(commandChannel, Comm.WANDER);
		}

		rc.broadcast(locationChannel, 10100+VectorActions.locToInt(rc.getLocation()));
	}
	
	public static void issueHerdCommand(RobotController rc, int band, MapLocation pastrLocation) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int locationChannel = band + Comm.LOCATION_SUBCHANNEL;
		
		rc.broadcast(commandChannel, Comm.HERD);
		rc.broadcast(locationChannel, VectorActions.locToInt(pastrLocation));
	}

	public static void moveToLocation(RobotController rc, MapLocation location, Random random, boolean sneak) throws GameActionException {
		//rc.setIndicatorString(1, "Trying to move to " + location);
		BasicPathing.bug(rc, location, random, sneak);
	}

	/**
	 * Selects enemy PASTR which is closest to friendly HQ and issues a MoveToEnemyPastr command
	 * @param rc The RobotController of the player calling this function
	 * @param random The Random object associated with this player
	 * @param squadNumber The squad number of the player calling this function
	 * @param pastrs A list of available PASTRs
	 * @throws GameActionException
	 */
	public static void moveToEnemyPastr(RobotController rc, Random random, int squadNumber, MapLocation[] pastrs) throws GameActionException {
		MapLocation ourHqLocation = rc.senseHQLocation();
		MapLocation closestPastr = null;
		int closestDistance = Integer.MAX_VALUE;
		for(MapLocation pastr : pastrs) {
			int distanceToOurHq = pastr.distanceSquaredTo(ourHqLocation);
			if(distanceToOurHq <= closestDistance) {
				closestDistance = distanceToOurHq;
				closestPastr = pastr;
			}
		}
		Soldier.issueMoveToEnemyPastrCommand(rc, squadNumber, closestPastr);
		Soldier.moveToLocation(rc, closestPastr, random, false);
	}
	
	/**
	 * Selects friendly PASTR which is closest to enemy HQ and issues a MoveToFriendlyPastr command
	 * @param rc The RobotController of the player calling this function
	 * @param random The Random object associated with this player
	 * @param squadNumber The squad number of the player calling this function
	 * @param pastrs A list of available PASTRs
	 * @throws GameActionException
	 */
	public static void moveToFriendlyPastr(RobotController rc, Random random, int squadNumber, MapLocation[] pastrs) throws GameActionException {
		MapLocation theirHqLocation = rc.senseEnemyHQLocation();
		MapLocation closestPastr = null;
		int closestDistance = Integer.MAX_VALUE;
		for(MapLocation pastr : pastrs) {
			int distanceToTheirHq = pastr.distanceSquaredTo(theirHqLocation);
			if(distanceToTheirHq <= closestDistance) {
				closestDistance = distanceToTheirHq;
				closestPastr = pastr;
			}
		}
		Soldier.issueMoveToFriendlyPastrCommand(rc, squadNumber, closestPastr);
		Soldier.moveToLocation(rc, closestPastr, random, true);
	}

	public static void verifyStandingPastrMove(RobotController rc, int band, Random random, boolean isEnemyPASTR) throws GameActionException {
		MapLocation target = VectorActions.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		if(isEnemyPASTR && Soldier.isValidPastr(rc, target, rc.getTeam().opponent())) {
			// Continue move command to target
			Soldier.moveToLocation(rc, target, random, false);
		} else if(!isEnemyPASTR && Soldier.isValidPastr(rc, target, rc.getTeam())) {
			if(Soldier.isAtLocation(rc, target, true)) {
				// Start herding cows
				Soldier.issueHerdCommand(rc, band, target);
			} else {
				// Continue sneak command to target
				Soldier.moveToLocation(rc, target, random, true);
			}
		} else {
			Soldier.issueStandbyCommand(rc, band);
		}
	}

	public static void verifyStandingMove(RobotController rc, int band, Random random) throws GameActionException {
		MapLocation target = VectorActions.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		if(Soldier.isAtLocation(rc, target, true)) {
			Soldier.issueStandbyCommand(rc, band);
		} else {
			// Continue move command to target
			Soldier.moveToLocation(rc, target, random, false);
		}
	}
	
	public static void verifyStandingHerd(RobotController rc, int band, Random random, boolean isEnemyPastr) throws GameActionException {
		MapLocation pastrLocation = VectorActions.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		if(!isEnemyPastr && Soldier.isValidPastr(rc, pastrLocation, rc.getTeam())) {
			Soldier.moveToLocation(rc, pastrLocation, random, true);
		} else {
			Soldier.issueStandbyCommand(rc, band);
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
		return roundsSinceLastVitalityBroadcast <= 2;
	}

	public static void sendDistressSignal(RobotController rc, int band) throws GameActionException {
		int distressChannel = band + Comm.DISTRESS_SUBCHANNEL;
		rc.broadcast(distressChannel, VectorActions.locToInt(rc.getLocation()));
	}

	public static boolean isDistressSignal(RobotController rc, int band) throws GameActionException {
		int distressChannel = band + Comm.DISTRESS_SUBCHANNEL;
		return rc.readBroadcast(distressChannel) != 0;
	}

	public static void answerDistressSignal(RobotController rc, int band, Random random) throws GameActionException {
		int distressChannel = band + Comm.DISTRESS_SUBCHANNEL;
		MapLocation locationOfDistress = VectorActions.intToLoc(rc.readBroadcast(distressChannel));

		// Clear distress signal
		rc.broadcast(distressChannel, 0);

		// Move to location of distress
		Soldier.issueMoveToLocationCommand(rc, band, locationOfDistress);
		Soldier.moveToLocation(rc, locationOfDistress, random, false);
	}
	
	public static boolean isHQCommand(RobotController rc, int band) throws GameActionException {
		int HQCommandChannel = band + Comm.HQ_COMMAND_SUBCHANNEL;
		return rc.readBroadcast(HQCommandChannel) != Comm.STANDBY;
	}
	
	public static void followHQCommand(RobotController rc, int band) throws GameActionException {
		int HQCommandChannel = band + Comm.HQ_COMMAND_SUBCHANNEL;
		int HQCommand = rc.readBroadcast(HQCommandChannel);
		int HQLocationChannel = band + Comm.HQ_LOCATION_SUBCHANNEL;

		if(HQCommand == Comm.ALL_SQUADS_TO_LOCATION) {
			MapLocation target = VectorActions.intToLoc(rc.readBroadcast(HQLocationChannel));
			
			Soldier.issueMoveToLocationCommand(rc, band, target);
		}
		
		// Clear HQ Command
		rc.broadcast(HQCommandChannel, Comm.STANDBY);
		rc.broadcast(HQLocationChannel, 0);
	}
	
	public static boolean isFormingSquad(RobotController rc, int band) throws GameActionException {
		return rc.readBroadcast(Comm.RESPONDING_SOLDIER_CHANNEL) == band && rc.senseRobotCount() <= Soldier.squadSize;
	}
	
	public static void updateLeaderLocation(RobotController rc, int band) throws GameActionException {
		int commandChannel = band + Comm.COMMAND_SUBCHANNEL;
		int leaderLocationChannel = band + Comm.LEADER_LOCATION_SUBCHANNEL;
		int leaderLocation = VectorActions.locToInt(rc.getLocation());
		
		int command = rc.readBroadcast(commandChannel);
		if(command == Comm.WANDER || command == Comm.MOVE_TO_FRIENDLY_PASTR) {
			leaderLocation += 10100;
		}
		
		rc.broadcast(leaderLocationChannel, leaderLocation);
	}

	public static void shouldIpanicOrRush(RobotController rc) throws GameActionException {
		int panic = rc.readBroadcast(Comm.PANIC_CHANNEL);
		if(panic>0){
			RobotPlayer.panic = true;
		}
		int rush = rc.readBroadcast(Comm.RUSH_CHANNEL);
		if(rush>0){
			RobotPlayer.rush = true;
		}
	}
	
	public static boolean isAtLocation(RobotController rc, MapLocation location, boolean nearBy) {
		MapLocation myLocation = rc.getLocation();
		if(nearBy) {
			return myLocation.equals(location) || myLocation.isAdjacentTo(location);
		} else {
			return myLocation.equals(location);
		}
	}

	public static void herdCows(RobotController rc, int band, Random random, int herderIndex, boolean resetHerding) throws GameActionException {
		MapLocation pastrLocation = VectorActions.intToLoc(rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL));
		if(resetHerding) {
		    Soldier.currentHerdingStatus =  herdingStatus.init;
		    RobotPlayer.resetHerding = false;
		}
		
		if(herderIndex >= 0 && herderIndex <= 7) {
			MapLocation startingLocation = pastrLocation.add(Direction.values()[herderIndex], 3);
			TerrainTile tileAtStart = rc.senseTerrainTile(startingLocation);
			while(tileAtStart == TerrainTile.OFF_MAP || tileAtStart == TerrainTile.VOID) {
				// Move start closer to PASTR
				startingLocation = startingLocation.add(Direction.values()[(herderIndex + 4) % 8]);
				tileAtStart = rc.senseTerrainTile(startingLocation);
			}

			MapLocation awayLocation = startingLocation;
			for(int i = 1; i <= 8; i++) {
				// Try to expand
				MapLocation newAwayLocation = awayLocation.add(Direction.values()[herderIndex]);
				TerrainTile tileAtNewLocation = rc.senseTerrainTile(newAwayLocation);
				if(tileAtNewLocation != TerrainTile.OFF_MAP && tileAtNewLocation != TerrainTile.VOID) {
					awayLocation = newAwayLocation;
				} else {
					break;
				}
			}

			if(Soldier.currentHerdingStatus == herdingStatus.init) {
				rc.setIndicatorString(1, "Herding init " + herderIndex);
				if(isAtLocation(rc, startingLocation, false)) {
					Soldier.currentHerdingStatus = herdingStatus.away;
				} else {
					Soldier.moveToLocation(rc, startingLocation, random, true);
				}
			} else if(Soldier.currentHerdingStatus == herdingStatus.away) {
				rc.setIndicatorString(1, "Herding away " + herderIndex);
				if(isAtLocation(rc, awayLocation, false)) {
					Soldier.currentHerdingStatus = herdingStatus.towards;
				} else {
					Soldier.moveToLocation(rc, awayLocation, random, true);
				}
			} else if(Soldier.currentHerdingStatus == herdingStatus.towards) {
				rc.setIndicatorString(1, "Herding towards " + herderIndex);
				if(isAtLocation(rc, startingLocation, false)) {
					Soldier.currentHerdingStatus = herdingStatus.away;
				} else {
					Soldier.moveToLocation(rc, startingLocation, random, false);
				}
			}
		} else {
			// Sneak around PASTR
			Soldier.moveToLocation(rc, pastrLocation, random, true);
		}
	}
}