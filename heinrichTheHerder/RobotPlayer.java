package heinrichTheHerder;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
//import battlecode.common.Robot;
import battlecode.common.RobotController;
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
	static MapLocation bugStart;
	static MapLocation bugTarget;
	static boolean bugLeft;
	static boolean panic;
	static boolean rush;
	static int soldierIndex;
	static boolean resetHerding = true;

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
		//MapManager.voidEnemyHQ(rc);
		//MapManager.printMap();
		MapManager.getGoodPastrLocations(rc);

		numberOfSpawnedSquads = 0;
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.STANDBY);

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
		
		//tell everyone whether or not to panic
		HQ.shouldWeAllpanicOrRush(rc);
		//If we're almost there, then rush to the end. Build a lot of pastrs.
		HQ.tryToShoot(rc);

		// Find the number of active squads
		numberOfActiveSquads = HQ.numberOfActiveSquads(rc, numberOfSpawnedSquads);

		//Check how many squads (robots) we've formed. If enemies have more pastures, we have will(almost certainly) make more pastures too.
		//If we're on panic, 50% chance for each spawned soldier to build a pasture.
		if((panic && random.nextFloat() < 0.5)||(rc.senseRobotCount() > Soldier.squadSize && random.nextFloat() < 0.3F/Math.pow(10.0,(rc.sensePastrLocations(rc.getTeam()).length - 2*rc.sensePastrLocations(rc.getTeam().opponent()).length)) || rc.readBroadcast(Comm.BUILD_PASTR_TALLY_CHANNEL)%2==1)){
			//Issue a new Pastr build command.
			HQ.buildNewPastr(rc, rc.sensePastrLocations(rc.getTeam()).length % MapManager.goodPastrLocations.size());

		}

		// Check if ready to make a new squad
		else {
			//First tell soldiers to stop building things.
			rc.broadcast(Comm.BUILD_PASTR_CHANNEL, 0);
			if(rc.readBroadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL) == 0) {

				numberOfSpawnedSquads++;

				// Issue a new squad creation request
				HQ.createNewSquad(rc, Soldier.squadSize, numberOfSpawnedSquads);
			}
		}

		// Spawn Soldiers
		HQ.tryToSpawn(rc);
	}

	//###################### COWBOY! (soldier) LOOP CODE #########################//
	private static void runSoldier(RobotController rc) throws GameActionException {
		int band = (tunedChannel / 100) * 100;
		rc.setIndicatorString(0, "tuned to channel: " + tunedChannel);
		//rc.setIndicatorString(2, "Number of friendlies: " + rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam()).length);	
		if(isLeader) {
			Soldier.broadcastVitality(rc, band);
			Soldier.updateLeaderLocation(rc, band);
			rc.setIndicatorString(1, "Current Command: " + rc.readBroadcast(band + Comm.COMMAND_SUBCHANNEL));
		}
		
		// If nearby an enemy, shoot it
		if (Soldier.nearEnemies(rc)) {

			Soldier.combatStrategy(rc, band, random);

		} else{
			//If panicking, No SNEAKING! NO WANDERING! GO FOR THE ATTACK AND FOR BUILD.
			Soldier.shouldIpanicOrRush(rc);
			
			if(tunedChannel == Comm.IDLE_SOLDIER_CHANNEL) {
				// If standing by, pay attention to Idle Soldier Channel
				if(rc.readBroadcast(Comm.IDLE_SOLDIER_CHANNEL) == Comm.TUNE_IN) {
					tunedChannel = Comm.RESPONDING_SOLDIER_CHANNEL;
				}

				//Check if need to build PASTR or NOISETOWER
				int build = rc.readBroadcast(Comm.BUILD_PASTR_CHANNEL);
				if(build > 0){
					int buildTally = rc.readBroadcast(Comm.BUILD_PASTR_TALLY_CHANNEL);
					if(buildTally%2==1){
						//If tally is even build pastr 
						tunedChannel = Comm.GOOD_PASTR_CHANNEL;
					}
					else{
						//if tally is odd build noise tower
						tunedChannel = Comm.GOOD_NOISE_CHANNEL;
					}
					//claim the work as done.
					rc.broadcast(Comm.BUILD_PASTR_TALLY_CHANNEL, buildTally+1);
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
			} 
			else if(tunedChannel == Comm.GOOD_PASTR_CHANNEL){
				int pastrLoc = rc.readBroadcast(Comm.GOOD_PASTR_CHANNEL);
				if(pastrLoc > 0){
					//System.out.println("I'm moving to build pastr.");
					//Soldier will move to build pastr
					MapLocation loc = VectorActions.intToLoc(pastrLoc);
					Soldier.moveToLocation(rc, loc, random, false);
					
					//If pastr's already built, go to idle.
					
					if(rc.getLocation().equals(loc)){
						if(rc.isActive()){
							rc.construct(RobotType.PASTR);
						}
					}else if(rc.canSenseSquare(loc) && rc.senseObjectAtLocation(loc) != null){
						//If pastr's already built, go to idle.
						tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
					}
				}
				else{
					tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
				}
			}
			else if(tunedChannel == Comm.GOOD_NOISE_CHANNEL){
				int noiseLoc = rc.readBroadcast(Comm.GOOD_NOISE_CHANNEL);
				if(noiseLoc > 0){
					//System.out.println("I'm moving to build noise tower.");
					//Soldier will move to build noisetower next to pastr
					MapLocation loc = VectorActions.intToLoc(noiseLoc);	
					Soldier.moveToLocation(rc, loc, random, false);
					
					
					if(rc.getLocation().equals(loc)){
						if(rc.isActive()){
							rc.construct(RobotType.NOISETOWER);
						}
					}else if(rc.canSenseSquare(loc) && rc.senseObjectAtLocation(loc) != null){
						//IF tower's already built, go to idle.
						tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
					}
				}
				else{
					tunedChannel = Comm.IDLE_SOLDIER_CHANNEL;
				}
			}
			else {

				// Soldier is tuned into a squad band
				if(Comm.isOnSubchannel(tunedChannel, Comm.SIGN_IN_SUBCHANNEL)) {
				    soldierIndex = rc.readBroadcast(tunedChannel);
				    rc.broadcast(tunedChannel, soldierIndex + 1);
					if(soldierIndex == 0) {
						// If this squad has no leader, become the leader
						isLeader = true;
						tunedChannel = band + Comm.COMMAND_SUBCHANNEL;
						rc.broadcast(tunedChannel, Comm.STANDBY);
					} else {
						tunedChannel = band + Comm.LEADER_LOCATION_SUBCHANNEL;
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
					
					if(Soldier.isFormingSquad(rc, band)) {
						// Wander around so that area around HQ is not congested
						Soldier.wander(rc, random);
						Soldier.issueFollowMeCommand(rc, band);
					} else if(Soldier.isHQCommand(rc, band)) {
						rc.setIndicatorString(1, "Following Order from HQ");
						Soldier.followHQCommand(rc, band);
					} else if(Soldier.isDistressSignal(rc, band)) {
						rc.setIndicatorString(1, "Answering Distress Signal");
						Soldier.answerDistressSignal(rc, band, random);
					} else {
						int outstandingCommand = rc.readBroadcast(tunedChannel);
						rc.setIndicatorString(1, "Current Command: " + outstandingCommand);

						MapLocation[] pastrsToMoveTo = rc.sensePastrLocations(rc.getTeam().opponent());
						boolean isEnemyPASTR = true;
						if(pastrsToMoveTo.length == 0) {
							// If no enemy PASTRs, move to protect our PASTRs
							pastrsToMoveTo = rc.sensePastrLocations(rc.getTeam());
							isEnemyPASTR = false;
						}
						if(pastrsToMoveTo.length > 0) {
							if(outstandingCommand == Comm.STANDBY) {
								// Select new target to move to
								if(isEnemyPASTR) {
									Soldier.moveToEnemyPastr(rc, random, band, pastrsToMoveTo);
								} else {
									Soldier.moveToFriendlyPastr(rc, random, band, pastrsToMoveTo);
								}
							} else if(outstandingCommand == Comm.MOVE_TO_ENEMY_PASTR) {
								// Verify existing move command
								Soldier.verifyStandingPastrMove(rc, band, random, isEnemyPASTR);
							} else if(outstandingCommand == Comm.MOVE_TO_FRIENDLY_PASTR) {
								// Verify existing move command
								Soldier.verifyStandingPastrMove(rc, band, random, isEnemyPASTR);
							} else if(outstandingCommand == Comm.MOVE_TO_LOCATION) {
								// Verify if at target
								Soldier.verifyStandingMove(rc, band, random);
							} else if(outstandingCommand == Comm.WANDER) {
								// Select new target to move to
								if(isEnemyPASTR) {
									Soldier.moveToEnemyPastr(rc, random, band, pastrsToMoveTo);
								} else {
									Soldier.moveToFriendlyPastr(rc, random, band, pastrsToMoveTo);
								}
							} else if(outstandingCommand == Comm.HERD) {
								Soldier.verifyStandingHerd(rc, band, random, isEnemyPASTR);
							}
						} else {						
							if(outstandingCommand == Comm.STANDBY) {
								// Wander
								Soldier.wander(rc, random);
								Soldier.issueFollowMeCommand(rc, band);
							} else if(outstandingCommand == Comm.MOVE_TO_ENEMY_PASTR){
								// Wander
								Soldier.wander(rc, random);
								Soldier.issueFollowMeCommand(rc, band);
							} else if(outstandingCommand == Comm.MOVE_TO_LOCATION) {
								// Verify if at target
								Soldier.verifyStandingMove(rc, band, random);
							} else if(outstandingCommand == Comm.WANDER) {
								// Wander
								Soldier.wander(rc, random);
								Soldier.issueFollowMeCommand(rc, band);
							}
						}	
					}
				} else {
					// Follower strategy = Follow the leader
					
					//IF you're a follower, and we're rushing, and there's no enemies in sight(already satisfied here), then you have 50% chance of just building a pastr.
					//If you're a follower, and we're panicking, there's a 0.5*0.5 chance of building a pastr.
					if((rush || (panic && random.nextFloat() < 0.5) ) && rc.senseCowsAtLocation(rc.getLocation()) > 100 && random.nextFloat() < 0.5){
						while(!rc.isActive()){
							rc.yield();
						}
						rc.construct(RobotType.PASTR);
					}
					
					
					// Check if leader is alive
					if(Soldier.isLeaderAlive(rc, band)) {
						int channelData = rc.readBroadcast(band + Comm.LEADER_LOCATION_SUBCHANNEL);
						int command = rc.readBroadcast(band + Comm.COMMAND_SUBCHANNEL);
						if(command == Comm.HERD) {
						    int herderIndex = soldierIndex - 1;
							Soldier.herdCows(rc, band, random, herderIndex, resetHerding);
						} else {
							resetHerding = true;
							// Follow the leader
							if(channelData > 10100){
								channelData -= 10100;
								MapLocation target = VectorActions.intToLoc(channelData);
								//rc.setIndicatorString(1, "Moving to " + target);
								Soldier.moveToLocation(rc, target, random, true);
							}else{
								MapLocation target = VectorActions.intToLoc(channelData);
								//rc.setIndicatorString(1, "Moving to " + target);
								Soldier.moveToLocation(rc, target, random, false);
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
	//###################### NOISETOWER LOOP CODE #####################//
	private static void runNoise(RobotController rc) throws GameActionException{
		Noise.herdCows(rc);
	}



}