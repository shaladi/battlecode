package alphaDesign;

import battlecode.common.MapLocation;

public class Comm {
	// Idle Soldier Channel Commands
	public static int STANDBY = -9999;
	public static int TUNE_IN = -9998;
	
	// Responding Soldier Channel Commands
	public static int MOVE_TO_LOCATION = -9997;
	public static int BUILD_PASTR = -9996;
	public static int BUILD_TOWER = -9995;
	
	static final int IDLE_SOLDIER_CHANNEL = 50; // Channel that idle soldiers will listen to for a command
	static final int RESPONDING_SOLDIER_CHANNEL = 51; // Channel that directs soldiers to tune to a specific band
	static final int RESPONDING_SOLDIER_TALLY_CHANNEL = 52; // Channel which tallies number of responding soldiers
	
	public static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	public static MapLocation intToLoc(int i){
		return new MapLocation(i/100, i%100);
	}
}