package multipleclassplayer;

import java.util.HashMap;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	public static RobotController rc;
	public static int ID;
	public static Random rand = new Random();
	public static HQ hq;
	public static HashMap<Integer, Soldier> soldiers = new HashMap<Integer, Soldier>();
	public static Direction[] allDirections = {Direction.EAST,
												Direction.NORTH,
												Direction.NORTH_EAST,
												Direction.NORTH_WEST,
												Direction.SOUTH,
												Direction.SOUTH_EAST,
												Direction.SOUTH_WEST,
												Direction.WEST};
	
	public static void run(RobotController rcin) {
		rc = rcin;
		ID = rc.getRobot().getID();
		rand.setSeed(ID);

		
		while(true) {
			try {
				if(rc.getType() == RobotType.HQ) {
					// Run HQ Code
					if(hq == null) {
						hq = new HQ();
					}
					hq.runHQ();
				} else if(rc.getType() == RobotType.SOLDIER) {
					// Initialize new soldier
					if(!soldiers.containsKey(ID)) {
						// Initialize as AttackSoldier with specified probability
						Soldier newSoldier;
						if(rand.nextDouble() < 0.5) {
							newSoldier = new AttackSoldier(ID);
						} else {
							// Initialize as other type of soldier
							// TODO Make new soldier type
							newSoldier = new AttackSoldier(ID);
						}
						soldiers.put(ID, newSoldier);
					}
					
					// Run Soldier Code
					Soldier soldier = soldiers.get(ID);
					soldier.runSoldier();
				}
				rc.yield();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}