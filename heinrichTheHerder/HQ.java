package heinrichTheHerder;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Direction;
import battlecode.common.TerrainTile;

public class HQ{
	
	
	//This function will spawn a soldier in the first possible direction.
	public static void tryToSpawn(RobotController rc) throws GameActionException{
		for(Direction dir:Direction.values()){
			if(rc.senseRobotCount() < GameConstants.MAX_ROBOTS && rc.isActive() && Soldier.canMove(rc,dir)){
				rc.spawn(dir);
				break;
			}
			
		}
	}
	
	//This function will set people in panic if needed.
	
	public static void shouldWeAllpanicOrRush(RobotController rc) throws GameActionException {
		boolean panic = false;
		boolean rush = false;
		
		//panic and be agressive if the enemy has more milk and there's not enough time to stop them. 
		double ourMilk = rc.senseTeamMilkQuantity(rc.getTeam());
		double enemyMilk = rc.senseTeamMilkQuantity(rc.getTeam().opponent());
		double roundNum = Clock.getRoundNum();
		if(enemyMilk > 8000000){
			panic = true;
		}else if(roundNum > 1650 && enemyMilk > ourMilk){
			panic = true;
		}else if(ourMilk > 7500000 && enemyMilk < 5000000){
			rush = true;
		}else{
			rc.setIndicatorString(2, "Okay, Nothing Now.");
			rc.broadcast(Comm.PANIC_CHANNEL, 0);
			rc.broadcast(Comm.RUSH_CHANNEL, 0);
		}
		if(panic){
			rc.setIndicatorString(2, "PANIC!!!!!!");
			rc.broadcast(Comm.PANIC_CHANNEL, 1);
			RobotPlayer.panic = true;
		}
		if(rush){
			rc.setIndicatorString(2, "Rush To The End");
			rc.broadcast(Comm.RUSH_CHANNEL, 1);
			RobotPlayer.rush = true;
		}
	}
	
	
	//This function will try to shoot if any enemies in attack range. 
	
	public static void tryToShoot(RobotController rc) throws GameActionException {
		// Look if any enemies are nearby
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc.getTeam().opponent());
		// Shoot at first nearby enemy
		if(enemyRobots.length > 0 && rc.isActive()){
			rc.attackSquare(rc.senseRobotInfo(enemyRobots[0]).location);
		}
	}
	
public static void createNewSquad(RobotController rc, int squadSize, int squadNumber) throws GameActionException {
		
		// Tell idle soldiers to pay attention
		rc.broadcast(Comm.IDLE_SOLDIER_CHANNEL, Comm.TUNE_IN);
		
		// Tell soldiers at attention to tune to the squadNumber band
		rc.broadcast(Comm.RESPONDING_SOLDIER_CHANNEL, squadNumber * 100);
		
		// Tell only squadSize # of idle soldiers to join squad
		rc.broadcast(Comm.RESPONDING_SOLDIER_TALLY_CHANNEL, squadSize);
		
		// Initialize the squad's HQ Command Channel to standby
		int squadHQCommandChannel = (squadNumber * 100) + Comm.HQ_COMMAND_SUBCHANNEL;
		rc.broadcast(squadHQCommandChannel, Comm.STANDBY);
	}


	public static void buildNewPastr(RobotController rc, int index) throws GameActionException {
				
				//Get noise tower location
				MapLocation loc = MapManager.goodPastrLocations.get(index);
				for(int i=0; i<8; i++){
					MapLocation noiseLoc = loc.add(Direction.values()[i]);
					if(!rc.senseTerrainTile(noiseLoc).equals(TerrainTile.VOID) && !rc.senseTerrainTile(noiseLoc).equals(TerrainTile.OFF_MAP)){
						loc = noiseLoc;
						break;
					}
				}		
				// Send out location where to build pastr
				rc.broadcast(Comm.BUILD_PASTR_CHANNEL, 1);
				rc.broadcast(Comm.GOOD_PASTR_CHANNEL, VectorActions.locToInt(MapManager.goodPastrLocations.get(index)));
				rc.broadcast(Comm.GOOD_NOISE_CHANNEL, VectorActions.locToInt(loc));
		
	}
	
	public static int numberOfActiveSquads(RobotController rc, int numberOfSpawnedSquads) throws GameActionException {
		int numberOfActiveSquads = 0;
		for(int i = numberOfSpawnedSquads; i > 0; i--) {
			if(Comm.isSquadActive(rc, i)) {
				numberOfActiveSquads++;
			}
		}
		return numberOfActiveSquads;
	}
}