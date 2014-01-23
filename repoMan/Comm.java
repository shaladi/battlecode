package repoMan;

public class Comm {
	// Idle Soldier Channel Commands
	public static int STANDBY = -9999;
	public static int TUNE_IN = -9998;

	// Responding Soldier Channel Commands
	public static int MOVE_TO_PASTR = -9997;
	public static int BUILD_PASTR = -9996;
	public static int BUILD_TOWER = -9995;
	public static int END_OF_COMMAND = -9994;
	public static int MOVE_TO_LOCATION = -9993;
	public static int FOLLOW_THE_LEADER = -9992;

	static final int IDLE_SOLDIER_CHANNEL = 50; // Channel that idle soldiers will listen to for a command
	static final int RESPONDING_SOLDIER_CHANNEL = 51; // Channel that directs soldiers to tune to a specific band
	static final int RESPONDING_SOLDIER_TALLY_CHANNEL = 52; // Channel which decrements as number of responding soldiers increases

	// Squad band subchannels
	public static final int SIGN_IN_SUBCHANNEL = 0;
	public static final int COMMAND_SUBCHANNEL = 1;
	public static final int LOCATION_SUBCHANNEL = 2;
	public static final int VITALITY_SUBCHANNEL = 3;
	public static final int DISTRESS_SUBCHANNEL = 4;


	//Defensive channels
	public static int GOOD_PASTR_CHANNEL = 53;
	public static int BUILD_PASTR_CHANNEL = 54;
	public static int GOOD_NOISE_CHANNEL = 55;
	public static int BUILD_PASTR_TALLY_CHANNEL = 56;


	/**
	 * @param subchannel The subchannel within the current band to check
	 * @return Whether or not the player is tuned to the subchannel specified
	 */
	public static boolean isOnSubchannel(int tunedChannel, int subchannel) {
		return ((tunedChannel - subchannel) % 100 == 0);
	}
}