package examplefuncsplayer;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	static Random rand;
	
	static boolean rotated = false;
	static char symmetry;
	static byte corner = 0;
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		
		while(true) {
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
							rc.spawn(toEnemy);
						}
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
				}
			}
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						int action = (rc.getRobot().getID()*rand.nextInt(101) + 50)%101;
						//Construct a PASTR
						if (action < 1 && rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 2) {
							rc.construct(RobotType.PASTR);
						//Attack a random nearby enemy
						} else if (action < 30) {
							Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
							if (nearbyEnemies.length > 0) {
								RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
								rc.attackSquare(robotInfo.location);
							}
						//Move in a random direction
						} else if (action < 80) {
							Direction moveDirection = directions[rand.nextInt(8)];
							if (rc.canMove(moveDirection)) {
								rc.move(moveDirection);
							}
						//Sneak towards the enemy
						} else {
							Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							if (rc.canMove(toEnemy)) {
								rc.sneak(toEnemy);
							}
						}
					}
				} catch (Exception e) {
					System.out.println("Soldier Exception");
				}
			}
			
			rc.yield();
		}
	}
	
	public static void getSymmetry(MapLocation ourBase, MapLocation opponentBase, char[][] rep)
	{
		boolean rot = false;
		//sets corner
		if(rep.length/2 > ourBase.y)
		{
			if(rep[0].length/2 > ourBase.x)
				corner = 1;
			else
				corner = 2;
		}
		else
		{
			if(rep[0].length/2 > ourBase.x)
				corner = 3;
			else
				corner = 4;
		}
		
		if(ourBase.x == opponentBase.x)
		{
			symmetry = 'h';
			for(int i = rep.length - 1; i > 0; i--){
				for(int j = rep.length/2; j > 0; j--)
				{
					if(rep[i][j] != rep[i][rep.length - j])
					{
						rot = true;
						break;
					}
				}
				if(rot)
					break;
			}
			rotated = rot;
		}
		else if(ourBase.y == opponentBase.y)
		{
			symmetry = 'v';
			for(int i = rep.length - 1; i > 0; i--){
				for(int j = rep.length/2; j > 0; j--)
				{
					if(rep[j][i] != rep[rep.length - j][i])
					{
						rot = true;
						break;
					}
				}
				if(rot)
					break;
			}
			rotated = rot;
		}
		else{
			if(corner == 1 || corner == 4)
			{
				symmetry = 'a';
				for(int i = rep.length-1; i > 0; i--)
				{
					for(int j = rep.length-i; j > 0; j--)
					{
						if(rep[i][j] != rep[rep.length - 1 - j][rep.length - 1 - i]){
							rot = true;
							break;
						}
					}
					if(rot)
						break;
				}
				rotated = rot;
			}
			else{
				symmetry = 't';
				for(int i = rep.length-1; i > 0; i--)
				{
					for(int j = rep.length-1; j > i; j--)
					{
						if(rep[i][j] != rep[j][i]){
							rot = true;
							break;
						}
					}
					if(rot)
						break;
				}
				rotated = rot;
			}
		}
	}
}
