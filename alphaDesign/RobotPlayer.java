package alphaDesign;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	static RobotController rc;
	static int ID;
	static Random random = new Random();
	static Direction allDirections[] = Direction.values();
	
	// HQ Data
	
	// Soldier Data
	static int tunedChannel;
	
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
		MapManager.assessMap(rc);
		//MapManager.printMap();
		
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.STANDBY);
	}

	private static void runHQ() throws GameActionException {
		// Spawn Soldiers
		if(rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			for(int i=0; i<8; i++){
				Direction trialDir = allDirections[i];
				if(rc.canMove(trialDir)){
					rc.spawn(trialDir);
					break;
				}
			}
		}
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
			// Wander around when idle
			SoldierActions.wander(rc, random);
			
			// If standing by, pay attention to Idle Soldier Channel
			if(rc.readBroadcast(Comm.IDLE_SOLDIER_CHANNEL) == Comm.TUNE_IN) {
				tunedChannel = Comm.RESPONDING_SOLDIER_CHANNEL;
				rc.broadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL, rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) + 1);
			}
			
		} else if(tunedChannel == Comm.RESPONDING_SOLDIER_CHANNEL) {
			// If tuned in for command, change channel to specified band
			tunedChannel = rc.readBroadcast(Comm.RESPONDING_SOLDIER_CHANNEL);
		}
	}
}