package barnabasTheDevastator;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer {
	static RobotController rc;
	static int ID;
	static Random random = new Random();
	static Direction allDirections[] = Direction.values();
	
	// HQ Data
	static int currentNumberOfSquads;
	
	// Soldier Data
	static int tunedChannel;
	static boolean isLeader;
	
	public static void run(RobotController rcin) {
		// Initialization code
		rc = rcin;
		ID = rc.getRobot().getID();
		random.setSeed(ID);
		try {
			if(rc.getType() == RobotType.HQ) {
				// Execute HQ Code
				initHQ();
			} else if(rc.getType() == RobotType.SOLDIER) {
				// Execute Soldier Code
				initSoldier();
			}
			rc.yield();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		// Looped code
		while(true) {
			try {
				if(rc.getType() == RobotType.HQ) {
					// Execute HQ Code
					runHQ();
				} else if(rc.getType() == RobotType.SOLDIER) {
					// Execute Soldier Code
					runSoldier();
				}
				rc.yield();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*	HQ Code	*/
	private static void initHQ() throws GameActionException {
		//MapManager.assessMap(rc);
		//MapManager.printMap();
		currentNumberOfSquads = 0;
		isLeader = false;
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.STANDBY);
	}

	private static void runHQ() throws GameActionException {
		// Debug
		rc.setIndicatorString(0, "Number of Soldiers to request: " + rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL));
		
		HQActions.tryToShoot(rc);
		
		// Check if ready to make a new squad
		if(rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) == 0) {
			currentNumberOfSquads++;
			
			// Issue a new squad creation request
			HQActions.createNewSquad(rc, SoldierActions.squadSize, currentNumberOfSquads);
		}
		
		// Spawn Soldiers
		HQActions.spawnSoldiers(rc);
	}
	
	/*	Soldier Code	*/
	private static void initSoldier() {
		// Have new soldiers listen to the idle soldier channel
		tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
	}
	
	private static void runSoldier() throws GameActionException {
		rc.setIndicatorString(0, "Tuned to channel " + tunedChannel);
		
		// If nearby an enemy, shoot it
		SoldierActions.tryToShoot(rc);
		
		if(tunedChannel == Comm.IDLE_SOLDIER_CHANNEL) {
			// If standing by, pay attention to Idle Soldier Channel
			if(rc.readBroadcast(Comm.IDLE_SOLDIER_CHANNEL) == Comm.TUNE_IN) {
				tunedChannel = Comm.RESPONDING_SOLDIER_CHANNEL;
			}
			
		} else if(tunedChannel == Comm.RESPONDING_SOLDIER_CHANNEL) {
			if(rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) > 0) {
				// If tuned in for command, change channel to specified band
				tunedChannel = rc.readBroadcast(Comm.RESPONDING_SOLDIER_CHANNEL);
				rc.broadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL, rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) - 1);
			} else {
				// Go back to the idle channel
				tunedChannel = rc.readBroadcast(Comm.IDLE_SOLDIER_CHANNEL);
			}
		} else {
			int squadNumber = tunedChannel / 100;
			int band = squadNumber * 100;
			int signInSubchannel = 0;
			int commandSubchannel = 1;
			int locationSubchannel = 2;
			
			// Soldier is tuned into a squad band
			if(isOnSubchannel(signInSubchannel)) {
				if(rc.readBroadcast(tunedChannel) == 0) {
					// If this squad has no leader, become the leader
					isLeader = true;
					rc.broadcast(tunedChannel, 1);
					tunedChannel = band + commandSubchannel;
					rc.broadcast(tunedChannel, Comm.STANDBY);
				} else {
					tunedChannel = band + commandSubchannel;
				}
			}
			
			/*
			 * Strategy:
			 * 		If enemy pastures are present, attack them.
			 *      If not, protect our pastrs.
			 *      If we have none, wander around, search for enemies to attack
			 */
			if(isLeader) {
				MapLocation[] pastrsToMoveTo = rc.sensePastrLocations(rc.getTeam().opponent());
				if(pastrsToMoveTo.length == 0) {
					// If no enemy PASTRs, move to protect our PASTRs
					pastrsToMoveTo = rc.sensePastrLocations(rc.getTeam());
				}
				if(pastrsToMoveTo.length > 0) {
					if(rc.readBroadcast(band + commandSubchannel) == Comm.STANDBY) {
						// Select new target to move to
						SoldierActions.moveToRandomPastr(rc, random, squadNumber, pastrsToMoveTo);
					} else if(rc.readBroadcast(band + commandSubchannel) == Comm.MOVE_TO_LOCATION){
						MapLocation target = Comm.intToLoc(rc.readBroadcast(band + locationSubchannel));
						if(isValidPastr(target, rc.getTeam().opponent())) {
							// Continue move command to target
							SoldierActions.moveToLocation(rc, squadNumber, target);
						} else {
							SoldierActions.issueStandbyCommand(rc, squadNumber);
						}
					}
				} else {
					SoldierActions.wander(rc, random);
					SoldierActions.issueMoveCommand(rc, squadNumber, rc.getLocation());
				}
			} else {
				// Follower strategy
				if(random.nextDouble() < 0.01) {
					if(rc.isActive()) {
						rc.construct(RobotType.PASTR);
					}
				} else {
					if(isOnSubchannel(commandSubchannel)) {
						if(rc.readBroadcast(tunedChannel) == Comm.STANDBY) {
							// Do nothing
						} else if(rc.readBroadcast(tunedChannel) == Comm.MOVE_TO_LOCATION) {
							tunedChannel = band + locationSubchannel;
						}
					} else if(isOnSubchannel(locationSubchannel)) {
						int channelData = rc.readBroadcast(band + locationSubchannel);
						if(channelData != Comm.END_OF_COMMAND) {
							MapLocation target = Comm.intToLoc(channelData);
							SoldierActions.moveToLocation(rc, squadNumber, target);
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param subchannel The subchannel within the current band to check
	 * @return Whether or not the player is tuned to the subchannel specified
	 */
	private static boolean isOnSubchannel(int subchannel) {
		return ((tunedChannel - subchannel) % 100 == 0);
	}
	
	private static boolean isValidPastr(MapLocation location, Team team) {
		for(MapLocation pastrLocation : rc.sensePastrLocations(team)) {
			if (pastrLocation.equals(location)) {
				return true;
			}
		}
		return false;
	}
}