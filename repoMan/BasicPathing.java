package repoMan;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BasicPathing{

	static ArrayList<MapLocation> snailTrail = new ArrayList<MapLocation>();

	//############## snail code ######################

	public static boolean canMove(Direction dir, boolean selfAvoiding,RobotController rc){
		//include both rc.canMove and the snail Trail requirements
		if(selfAvoiding){
			MapLocation resultingLocation = rc.getLocation().add(dir);
			for(int i=0;i<snailTrail.size();i++){
				MapLocation m = snailTrail.get(i);
				if(!m.equals(rc.getLocation())){
					if(resultingLocation.isAdjacentTo(m)||resultingLocation.equals(m)){
						return false;
					}
				}
			}
		}
		//if you get through the loop, then dir is not adjacent to the icky snail trail
		return rc.canMove(dir);
	}

	public static void snail(Direction chosenDirection,boolean selfAvoiding,RobotController rc) throws GameActionException{
		while(snailTrail.size()<2)
			snailTrail.add(new MapLocation(-1,-1));
		if(rc.isActive()){
			snailTrail.remove(0);
			snailTrail.add(rc.getLocation());
			Direction trialDir = chosenDirection;
			for(int i = 0; i < 8; i++){
				if(canMove(trialDir,selfAvoiding,rc)){
					rc.move(trialDir);
					//snailTrail.remove(0);
					//snailTrail.add(rc.getLocation());
					break;
				}
				trialDir = trialDir.rotateRight();
			}
		}
	}


	//############## bug code ###################### 
	public static void bug(RobotController rc, MapLocation goal, Random random, boolean sneak) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if(!myLoc.equals(goal)){
			Direction goaldir = myLoc.directionTo(goal);
			if(RobotPlayer.bugDir == null){
				if(rc.canMove(goaldir)){
					if(rc.isActive()){
						if(!sneak){
							rc.move(goaldir);
						}
						else{
							rc.sneak(goaldir);
						}
					}
				}
				else{
					RobotPlayer.bugStart = myLoc;
					RobotPlayer.bugTarget = goal;
					if(!RobotPlayer.bugLeft){
						RobotPlayer.bugDir = goaldir.rotateRight();
					}else{
						RobotPlayer.bugDir = goaldir.rotateLeft();
					}
				}
			}
			else{
				//Higher probability of leaving bug if getting further away from goal.
				double dist = myLoc.distanceSquaredTo(goal);
				double bugDist = RobotPlayer.bugStart.distanceSquaredTo(goal);
				double probDist = dist - bugDist;
				if(probDist <= 0){
					probDist = 1;
				}
				float prob = (float) Math.exp(-500.0/probDist);
				rc.setIndicatorString(2, "probability is now " + prob + " and bugleft is now "+ RobotPlayer.bugLeft + " and target is " + RobotPlayer.bugTarget + " and goal is " + goal);
				//rc.setIndicatorString(2, "bugLeft is now" + RobotPlayer.bugLeft);
				if(random.nextFloat() < prob ){
					RobotPlayer.bugDir = null;
					RobotPlayer.bugStart = null;
					RobotPlayer.bugTarget = null;
					RobotPlayer.bugLeft = !RobotPlayer.bugLeft;
				}
				else{
					if(!RobotPlayer.bugLeft){
						//a proportional chance of just stopping bugging, just in case.
						if( !RobotPlayer.bugTarget.equals(goal) | dist < bugDist){
							RobotPlayer.bugDir = null;
							RobotPlayer.bugStart = null;
							RobotPlayer.bugTarget = null;
						}
						else if(rc.canMove(RobotPlayer.bugDir.rotateLeft())){
							RobotPlayer.bugDir = RobotPlayer.bugDir.rotateLeft();
							if(rc.isActive()){
								if(!sneak){
									rc.move(RobotPlayer.bugDir);
								}
								else{
									rc.sneak(RobotPlayer.bugDir);
								}
							}
						}
						else if(rc.canMove(RobotPlayer.bugDir)){
							if(rc.isActive()){
								if(!sneak){
									rc.move(RobotPlayer.bugDir);
								}
								else{
									rc.sneak(RobotPlayer.bugDir);
								}
							}
						}
						else {
							while(!rc.canMove(RobotPlayer.bugDir)){
								RobotPlayer.bugDir = RobotPlayer.bugDir.rotateRight();
							}	
							if(rc.isActive()){
								if(!sneak){
									rc.move(RobotPlayer.bugDir);
								}
								else{
									rc.sneak(RobotPlayer.bugDir);
								}
							}
						}	
					}
					else{
						//bug left.
						if( !RobotPlayer.bugTarget.equals(goal) | dist < bugDist){
							RobotPlayer.bugDir = null;
							RobotPlayer.bugStart = null;
							RobotPlayer.bugTarget = null;
							
						}
						else if(rc.canMove(RobotPlayer.bugDir.rotateRight())){
							RobotPlayer.bugDir = RobotPlayer.bugDir.rotateRight();
							if(rc.isActive()){
								if(!sneak){
									rc.move(RobotPlayer.bugDir);
								}
								else{
									rc.sneak(RobotPlayer.bugDir);
								}
							}
						}
						else if(rc.canMove(RobotPlayer.bugDir)){
							if(rc.isActive()){
								if(!sneak){
									rc.move(RobotPlayer.bugDir);
								}
								else{
									rc.sneak(RobotPlayer.bugDir);
								}
							}
						}
						else {
							while(!rc.canMove(RobotPlayer.bugDir)){
								RobotPlayer.bugDir = RobotPlayer.bugDir.rotateLeft();
							}	
							if(rc.isActive()){
								if(!sneak){
									rc.move(RobotPlayer.bugDir);
								}
								else{
									rc.sneak(RobotPlayer.bugDir);
								}
							}
						}
					}
				}
			}
		}
	}

	//########### simple move code #################### (Possibly the simplest navigation code ... kinda like the right way to implement bug... except not.)

	public static void simplemove(RobotController rc, MapLocation goal) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(goal);
		if(rc.canMove(dir)){
			if(rc.isActive()){
				rc.move(dir);
			}
		}

		else{
			Direction trialDir = dir.rotateLeft();
			while(!rc.canMove(trialDir)){
				trialDir = trialDir.rotateLeft();
			}
			if(rc.isActive()){
				rc.move(trialDir);
			}
		}

	}



}