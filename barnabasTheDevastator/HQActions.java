package barnabasTheDevastator;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * This class contains static methods for HQ actions
 */
public class HQActions {
	public static void createNewSquad(RobotController rc, int squadSize, int squadNumber) throws GameActionException {
		// Tell idle soldiers to pay attention
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.TUNE_IN);
		
		// Tell soldiers at attention to tune to the squadNumber band
		rc.broadcast(Comm.RESPONDING_SOLDIER_CHANNEL, squadNumber * 100);
		
		// Tell only squadSize # of idle soldiers to join squad
		rc.broadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL, squadSize);
	}
	
	public static void spawnSoldiers(RobotController rc) throws GameActionException {
		if(rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			for(int i=0; i<8; i++){
				Direction trialDir = RobotPlayer.allDirections[i];
				if(rc.canMove(trialDir)){
					rc.spawn(trialDir);
					break;
				}
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
	
	private static boolean isSquadActive(RobotController rc, int squadNumber) throws GameActionException {
		int vitalityChannel = (squadNumber * 100) + Comm.VITALITY_SUBCHANNEL;
		int roundsSinceLastVitalityBroadcast = Clock.getRoundNum() - rc.readBroadcast(vitalityChannel);
		return roundsSinceLastVitalityBroadcast <= 5;
	}
	
	public static int numberOfActiveSquads(RobotController rc, int numberOfSpawnedSquads) throws GameActionException {
		int numberOfActiveSquads = 0;
		for(int i = numberOfSpawnedSquads; i > 0; i--) {
			if(isSquadActive(rc, i)) {
				numberOfActiveSquads++;
			}
		}
		return numberOfActiveSquads;
	}
}