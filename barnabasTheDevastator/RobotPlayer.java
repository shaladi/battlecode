package barnabasTheDevastator;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	static RobotController rc;
	static int ID;
	static Random random = new Random();
	static Direction allDirections[] = Direction.values();
	
	// HQ Data
	static int numberOfSpawnedSquads;
	static int numberOfActiveSquads;	// Equal to the number of alive leaders
	
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
		numberOfSpawnedSquads = 0;
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.STANDBY);
	}

	private static void runHQ() throws GameActionException {
		// Debug
		rc.setIndicatorString(0, "Number of Squads Created: " + numberOfSpawnedSquads);
		rc.setIndicatorString(1, "Number of Active Squads: " + numberOfActiveSquads);
		
		HQActions.tryToShoot(rc);
		
		// Find the number of active squads
		numberOfActiveSquads = HQActions.numberOfActiveSquads(rc, numberOfSpawnedSquads);
		
		// Check if ready to make a new squad
		if(rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) == 0) {
			numberOfSpawnedSquads++;
			
			// Issue a new squad creation request
			HQActions.createNewSquad(rc, SoldierActions.squadSize, numberOfSpawnedSquads);
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
		int band = (tunedChannel / 100) * 100;
		rc.setIndicatorString(0, "Tuned to channel " + tunedChannel);
		
		// If nearby an enemy, shoot it
		SoldierActions.combatStrategy(rc, band);
		
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
				tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
			}
		} else {
			// Soldier is tuned into a squad band
			if(Comm.isOnSubchannel(tunedChannel, Comm.SIGN_IN_SUBCHANNEL)) {
				if(rc.readBroadcast(tunedChannel) == 0) {
					// If this squad has no leader, become the leader
					isLeader = true;
					rc.broadcast(tunedChannel, 1);
					tunedChannel = band + Comm.COMMAND_SUBCHANNEL;
					rc.broadcast(tunedChannel, Comm.STANDBY);
				} else {
					tunedChannel = band + Comm.COMMAND_SUBCHANNEL;
				}
			}
			
			/*
			 * Strategy:
			 * 		If enemy pastures are present, attack them.
			 *      If not, protect our pastrs.
			 *      If we have none, wander around, search for enemies to attack
			 */
			if(isLeader) {
				// Leader Strategy
				SoldierActions.broadcastVitality(rc, band);
				
				if(SoldierActions.isDistressSignal(rc, band)) {
					rc.setIndicatorString(1, "Answering Distress Signal");
					SoldierActions.answerDistressSignal(rc, band);
				} else {
					int outstandingCommand = rc.readBroadcast(tunedChannel);
					rc.setIndicatorString(1, "Current Command: " + outstandingCommand);

					MapLocation[] pastrsToMoveTo = rc.sensePastrLocations(rc.getTeam().opponent());
					if(pastrsToMoveTo.length == 0) {
						// If no enemy PASTRs, move to protect our PASTRs
						pastrsToMoveTo = rc.sensePastrLocations(rc.getTeam());
					}
					if(pastrsToMoveTo.length > 0) {
						if(outstandingCommand == Comm.STANDBY) {
							// Select new target to move to
							SoldierActions.moveToRandomPastr(rc, random, band, pastrsToMoveTo);
						} else if(outstandingCommand == Comm.MOVE_TO_PASTR){
							// Verify existing move command
							SoldierActions.verifyStandingPastrMove(rc, band);
						} else if(outstandingCommand == Comm.MOVE_TO_LOCATION) {
							// Verify if at target
							SoldierActions.verifyStandingMove(rc, band);
						} else if(outstandingCommand == Comm.FOLLOW_THE_LEADER) {
							// Select new target to move to
							SoldierActions.moveToRandomPastr(rc, random, band, pastrsToMoveTo);
						}
					} else {
						if(outstandingCommand == Comm.STANDBY) {
							// Wander
							SoldierActions.wander(rc, random);
							SoldierActions.issueFollowMeCommand(rc, band);
						} else if(outstandingCommand == Comm.MOVE_TO_PASTR){
							// Wander
							SoldierActions.wander(rc, random);
							SoldierActions.issueFollowMeCommand(rc, band);
						} else if(outstandingCommand == Comm.MOVE_TO_LOCATION) {
							// Verify if at target
							SoldierActions.verifyStandingMove(rc, band);
						} else if(outstandingCommand == Comm.FOLLOW_THE_LEADER) {
							// Wander
							SoldierActions.wander(rc, random);
							SoldierActions.updateFollowMeCommand(rc, band);
						}
					}	
				}
			} else {
				// Follower strategy
				// Check if leader is alive
				if(SoldierActions.isLeaderAlive(rc, band)) {
					if(Comm.isOnSubchannel(tunedChannel, Comm.COMMAND_SUBCHANNEL)) {
						int command = rc.readBroadcast(tunedChannel);
						if(command == Comm.STANDBY) {
							// Do nothing
						} else if(command == Comm.MOVE_TO_PASTR || command == Comm.MOVE_TO_LOCATION || command == Comm.FOLLOW_THE_LEADER) {
							tunedChannel = band + Comm.LOCATION_SUBCHANNEL;
						}
					} else if(Comm.isOnSubchannel(tunedChannel, Comm.LOCATION_SUBCHANNEL)) {
						int channelData = rc.readBroadcast(band + Comm.LOCATION_SUBCHANNEL);
						if(channelData != Comm.END_OF_COMMAND) {
							MapLocation target = Comm.intToLoc(channelData);
							rc.setIndicatorString(1, "Moving to " + target);
							SoldierActions.moveToLocation(rc, target);
						} else {
							tunedChannel = band + Comm.COMMAND_SUBCHANNEL;
						}
					}
				} else {
					// If leader is dead, go back to idle channel
					tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
				}
			}
		}
	}
	
}