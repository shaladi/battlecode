package heinrichTheHerder;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Comm {
	// Idle Soldier Channel Commands
	public static int STANDBY = -9999;
	public static int TUNE_IN = -9998;

	// Responding Soldier Channel Commands
	public static int MOVE_TO_ENEMY_PASTR = -9997;
	public static int MOVE_TO_FRIENDLY_PASTR = -9990;
	public static int BUILD_PASTR = -9996;
	public static int BUILD_TOWER = -9995;
	public static int END_OF_COMMAND = -9994;
	public static int MOVE_TO_LOCATION = -9993;
	public static int WANDER = -9992;
	public static int HERD = -9989;

	static final int IDLE_SOLDIER_CHANNEL = 50; // Channel that idle soldiers will listen to for a command
	static final int RESPONDING_SOLDIER_CHANNEL = 51; // Channel that directs soldiers to tune to a specific band
	static final int RESPONDING_SOLDIER_TALLY_CHANNEL = 52; // Channel which decrements as number of responding soldiers increases

	// Squad band subchannels
	public static final int SIGN_IN_SUBCHANNEL = 0;
	public static final int COMMAND_SUBCHANNEL = 1;
	public static final int LOCATION_SUBCHANNEL = 2;
	public static final int VITALITY_SUBCHANNEL = 3;
	public static final int DISTRESS_SUBCHANNEL = 4;
	public static final int HQ_COMMAND_SUBCHANNEL = 5;
	public static final int HQ_LOCATION_SUBCHANNEL = 6;
	public static final int LEADER_LOCATION_SUBCHANNEL = 7;
	public static final int HERDER_ROOT_SUBCHANNEL = 50; // Subchannels 50-57 reserved


	//Defensive channels
	public static int GOOD_PASTR_CHANNEL = 53;
	public static int BUILD_PASTR_CHANNEL = 54;
	public static int GOOD_NOISE_CHANNEL = 55;
	public static int BUILD_PASTR_TALLY_CHANNEL = 56;
	public static final int PANIC_CHANNEL = 57;
	public static final int RUSH_CHANNEL = 58;
	
	// General HQ -> Leader Commands
	public static int ALL_SQUADS_TO_LOCATION = -9991;


	/**
	 * @param subchannel The subchannel within the current band to check
	 * @return Whether or not the player is tuned to the subchannel specified
	 */
	public static boolean isOnSubchannel(int tunedChannel, int subchannel) {
		return ((tunedChannel - subchannel) % 100 == 0);
	}
	
	/**
	 * Creates a Max Distress command to specified number of squads to move to a location
	 * @param rc The RobotController of the player making the command
	 * @param numberOfSquads The number of squads that should respond to this command
	 * @param location The location to move to
	 * @throws GameActionException
	 */
	public static void issueSquadMoveCommand(RobotController rc, int numberOfSquads, MapLocation location) throws GameActionException {
		// Issue command to squads
		int squadToTry = 1;
		int numberOfSquadsCalled = 0;
		while(numberOfSquadsCalled < numberOfSquads) {
			int band = squadToTry * 100;
			if(rc.readBroadcast(band) == 0) {
				// Squad has not been started. We've hit the end of the spawned squad bands
				break;
			}
			if(Comm.isSquadActive(rc, squadToTry)) {
				int squadHQCommandChannel = band + Comm.HQ_COMMAND_SUBCHANNEL;
				int squadHQLocationChannel = band + Comm.HQ_LOCATION_SUBCHANNEL;
				rc.broadcast(squadHQCommandChannel, Comm.ALL_SQUADS_TO_LOCATION);
				rc.broadcast(squadHQLocationChannel, VectorActions.locToInt(location));
				numberOfSquadsCalled += 1;
			}
			squadToTry += 1;
		}
	}
	
	/**
	 * Checks vitality of squad leader
	 * @param rc The RobotController of the player making this method call
	 * @param squadNumber The squad to check
	 * @return if the squad is active
	 * @throws GameActionException
	 */
	public static boolean isSquadActive(RobotController rc, int squadNumber) throws GameActionException {
		int vitalityChannel = (squadNumber * 100) + Comm.VITALITY_SUBCHANNEL;
		int roundsSinceLastVitalityBroadcast = Clock.getRoundNum() - rc.readBroadcast(vitalityChannel);
		return roundsSinceLastVitalityBroadcast <= 5;
	}
}