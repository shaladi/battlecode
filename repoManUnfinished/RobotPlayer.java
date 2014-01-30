package repoMan;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
//import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class RobotPlayer{

	static int ID;
	static Random random = new Random();

	// HQ Data
	static int numberOfSpawnedSquads;
	static int numberOfActiveSquads;	// Equal to the number of alive leaders

	// Soldier Data
	static int tunedChannel;
	static boolean isLeader;
	static Direction bugDir;
	static int bugStartDist;
	static MapLocation bugTarget;
	static boolean bugLeft;
	static boolean panic;
	static boolean rush;
	public static RobotInfo[] nearbyEnemiesInfo;
	public static int myBuildLoc;
	public static MapLocation[] friendlyPastrs;
	public static MapLocation[] enemyPastrs;
	public static boolean bugMoved; 
	public static int targetLoc;
	public static MapLocation bugStart;
	public static int herderIndex;

	public static void run(RobotController rc){

		//Initialization Code

		try{

			initAll(rc);

			if(rc.getType().equals(RobotType.HQ)){

				initHQ(rc);

			}
			else if(rc.getType().equals(RobotType.SOLDIER)){

				initSoldier(rc);

			}

			rc.yield();

		}catch(Exception e){

			e.printStackTrace();

		}

		//Looping Code

		while(true){

			try{
				if(rc.getType().equals(RobotType.HQ)){

					runHQ(rc);

				}
				else if(rc.getType().equals(RobotType.SOLDIER)){

					runSoldier(rc);

				}
				else if(rc.getType().equals(RobotType.NOISETOWER)){

					runNoise(rc);

				}

				rc.yield();

			}catch(Exception e){

				e.printStackTrace();

			}

		}


	}

	//###################### Real work begins here #########################//
	private static void initAll(RobotController rc){
		ID = rc.getRobot().getID();
		random.setSeed(ID);
	}

	private static void initHQ(RobotController rc) throws GameActionException {
		MapManager.assessMap(rc);
		//MapManager.printMap();
		MapManager.getGoodPastrLocations(rc);

		numberOfSpawnedSquads = 0;
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.STANDBY);
		
		//start out squad size to be 5;
		rc.broadcast(Comm.SQUAD_SIZE_CHANNEL, 5);

	}

	private static void initSoldier(RobotController rc) throws GameActionException {
		// Have new soldiers listen to the idle soldier channel
		tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
	}

	//###################### HQ LOOP CODE #########################//
	private static void runHQ(RobotController rc) throws GameActionException {
		// Debug
		rc.setIndicatorString(0, "Number of Squads Created: " + numberOfSpawnedSquads);
		rc.setIndicatorString(1, "Number of Active Squads: " + numberOfActiveSquads);
		rc.setIndicatorString(2, "Total Robot Count:" + rc.senseRobotCount());
		
		//Set some global variables
		friendlyPastrs = rc.sensePastrLocations(rc.getTeam());
		enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
		
		//tell everyone whether or not to panic
		HQ.shouldWeAllpanicOrRush(rc);
		//If we're almost there, then rush to the end. Build a lot of pastrs.
		HQ.tryToShoot(rc);

		// Find the number of active squads
		numberOfActiveSquads = HQ.numberOfActiveSquads(rc, numberOfSpawnedSquads);
		

		boolean buildProb = random.nextFloat() < 0.1F/Math.pow(10.0,(friendlyPastrs.length - enemyPastrs.length));

		//Check how many squads (robots) we've formed. If enemies have more pastures, we have will(almost certainly) make more pastures too.
		//If we're on panic, 50% chance for each spawned soldier to build a pasture.
		if(rc.isActive() && friendlyPastrs.length < 4 && (((panic || rc.readBroadcast(Comm.BUILD_PASTR_TALLY_CHANNEL)%2==1) && random.nextFloat() < 0.5) || buildProb )){
			int index = friendlyPastrs.length % MapManager.goodPastrLocations.length;
			//Issue a new Pastr build command.
			HQ.buildNewPastr(rc, index);

		}

		// Check if ready to make a new squad
		else {
			if(rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) == 0) {
				rc.setIndicatorString(0, "CREATING SQUADS!!!!"+ " Squad Size is now: " + rc.readBroadcast(Comm.SQUAD_SIZE_CHANNEL));
				int squadSize = rc.readBroadcast(Comm.SQUAD_SIZE_CHANNEL);

				numberOfSpawnedSquads++;

				// Issue a new squad creation request
				HQ.createNewSquad(rc, squadSize, numberOfSpawnedSquads);
			}
		}

		// Spawn Soldiers
		HQ.tryToSpawn(rc);
	}

	//###################### COWBOY! (soldier) LOOP CODE #########################//
	private static void runSoldier(RobotController rc) throws GameActionException {
		int band = (tunedChannel / 100) * 100;
		rc.setIndicatorString(0, "tuned to channel: " + tunedChannel);
		
		if(isLeader) {
			Soldier.initializeLeader(rc, band);
		}

		// If nearby an enemy, shoot it
		if (tunedChannel != Comm.IDLE_SOLDIER_CHANNEL && tunedChannel != Comm.RESPONDING_SOLDIER_CHANNEL && Soldier.nearEnemies(rc)) {
			
			Soldier.combatStrategy(rc, band, random);

		} else{
			Soldier.nonCombatStrategy(rc, band, random, false);
		}
	}
	//###################### NOISETOWER LOOP CODE #####################//
	private static void runNoise(RobotController rc) throws GameActionException{
		Noise.herdCows(rc);
	}



}