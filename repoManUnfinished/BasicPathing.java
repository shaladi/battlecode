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
	
		public static boolean canMove(Direction dir, boolean selfAvoiding,RobotController rc, boolean bugAroundEnemies){
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
			return Soldier.canMove(rc,dir, bugAroundEnemies);
		}
	
		public static void snail(RobotController rc, MapLocation goal, boolean selfAvoiding, boolean sneak, boolean bugAroundEnemies) throws GameActionException{
			Direction chosenDirection = rc.getLocation().directionTo(goal);
			while(snailTrail.size()<2)
				snailTrail.add(new MapLocation(-1,-1));
			if(rc.isActive()){
				snailTrail.remove(0);
				snailTrail.add(rc.getLocation());
				Direction trialDir = chosenDirection;
				for(int i = 0; i < 8; i++){
					if(canMove(trialDir,selfAvoiding,rc, bugAroundEnemies)){
						if(!sneak){
							rc.move(trialDir);
						}
						else{
							rc.sneak(trialDir);
						}
//						snailTrail.remove(0);
//						snailTrail.add(rc.getLocation());
						break;
					}
					trialDir = trialDir.rotateRight();
				}
			}
		}


	//############## bug code ###################### 
	public static void bug(RobotController rc, MapLocation goal, Random random, boolean sneak, boolean bugAroundEnemies) throws GameActionException {

MapLocation myLoc = rc.getLocation();
		if(!myLoc.equals(goal)){
			rc.setIndicatorString(2, "Moving to: " + goal);
			Direction goaldir = myLoc.directionTo(goal);
			if(RobotPlayer.bugDir == null){
				if(Soldier.canMove(rc, goaldir, bugAroundEnemies)){
					if(rc.isActive()){
						if(!sneak){
							rc.move(goaldir);
						}
						else{
							rc.sneak(goaldir);
						}
					}
				}
				else if(myLoc.distanceSquaredTo(goal)<=4 && Soldier.isFollower(rc)){
					//You're close enough, do nothing, or simple move
					rc.setIndicatorString(2, "I'm close enough, simple moving. " + goal);
					simpleMove(rc, goal, bugAroundEnemies);
				}
				else{
					RobotPlayer.bugStart = myLoc;
					RobotPlayer.bugTarget = goal;
					//					if(RobotPlayer.isLeader || !Soldier.isFollower(rc)){
					RobotPlayer.bugLeft = isLeft(myLoc, myLoc.add(goaldir), goal);
					//					}else{
					//						int band = (RobotPlayer.tunedChannel/100)*100;
					//						int leaderBugLeftChannel = band+Comm.LEADER_BUGLEFT_SUBCHANNEL;
					//						int leaderBugLeft = rc.readBroadcast(leaderBugLeftChannel);
					//						if(leaderBugLeft > 0){
					//							RobotPlayer.bugLeft = true;
					//						}else{
					//							RobotPlayer.bugLeft = false;
					//						}
					//					}
					if(!RobotPlayer.bugLeft){
						RobotPlayer.bugDir = goaldir.rotateRight();
					}else{
						RobotPlayer.bugDir = goaldir.rotateLeft();
					}
				}
			}
			else{
				//				//Higher probability of leaving bug if getting further away from goal.
				double dist = myLoc.distanceSquaredTo(goal);
				double bugDist = RobotPlayer.bugStart.distanceSquaredTo(goal);
				double probDist = dist - bugDist;
				if(probDist <= 0){
					probDist = 1;
				}
				float prob = (float) Math.exp(-1000.0/probDist);
				//rc.setIndicatorString(2, "bugLeft is now" + RobotPlayer.bugLeft);

				if(!rc.getLocation().equals(RobotPlayer.bugStart)){
					RobotPlayer.bugMoved = true;
				}
				rc.setIndicatorString(2, "bugleft is now "+ RobotPlayer.bugLeft + " and bugDir is " + RobotPlayer.bugDir + " and goal is " + goal + " bugmoved: "+RobotPlayer.bugMoved);
				if(random.nextFloat() < prob ){
					RobotPlayer.bugDir = null;
					RobotPlayer.bugStart = null;
					RobotPlayer.bugTarget = null;
					RobotPlayer.bugLeft = !RobotPlayer.bugLeft;
					RobotPlayer.bugMoved = false;
				}
				if(RobotPlayer.bugMoved && Soldier.canMove(rc, myLoc.directionTo(goal), bugAroundEnemies)){
					RobotPlayer.bugDir = null;
					RobotPlayer.bugStart = null;
					RobotPlayer.bugTarget = null;
					RobotPlayer.bugMoved = false;
				}
				else{
					if(!RobotPlayer.bugLeft){
						//a proportional chance of just stopping bugging, just in case.
						if( RobotPlayer.bugMoved && (!RobotPlayer.bugTarget.equals(goal) || dist <= bugDist)){
							RobotPlayer.bugDir = null;
							RobotPlayer.bugStart = null;
							RobotPlayer.bugTarget = null;
							RobotPlayer.bugMoved = false;
						}
						else if(Soldier.canMove(rc, RobotPlayer.bugDir.rotateLeft(), bugAroundEnemies)){
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
						else if(Soldier.canMove(rc,RobotPlayer.bugDir, bugAroundEnemies)){
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
							while(!Soldier.canMove(rc,RobotPlayer.bugDir, bugAroundEnemies)){
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
						if(RobotPlayer.bugMoved && (!RobotPlayer.bugTarget.equals(goal) || dist <= bugDist)){
							RobotPlayer.bugDir = null;
							RobotPlayer.bugStart = null;
							RobotPlayer.bugTarget = null;
							RobotPlayer.bugMoved = false;

						}
						else if(Soldier.canMove(rc,RobotPlayer.bugDir.rotateRight(), bugAroundEnemies)){
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
						else if(Soldier.canMove(rc,RobotPlayer.bugDir, bugAroundEnemies)){
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
							while(!Soldier.canMove(rc,RobotPlayer.bugDir, bugAroundEnemies)){
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
//		//Initialize good variables.
//		MapLocation myLoc = rc.getLocation();
//		Direction goalDir = myLoc.directionTo(goal);
//
//		//If you're not bugging and can go straight in the goal direction, go ahead.
//		if(RobotPlayer.bugDir == null && Soldier.canMove(rc, goalDir, bugAroundEnemies)){
//			if(rc.isActive()){
//				if(sneak){
//					rc.sneak(goalDir);
//				}
//				else{
//					rc.move(goalDir);
//				}
//			}
//		}
//		//If you cannot go in the direction, then there's an obstacle infront of you, start bugging around it.
//		else{			
//			if(RobotPlayer.bugDir == null){
//				RobotPlayer.bugDir = goalDir.rotateLeft();
//				RobotPlayer.bugStartDist = myLoc.distanceSquaredTo(goal);
//				RobotPlayer.bugTarget = goal;
//				RobotPlayer.bugLeft = isLeft(myLoc, myLoc.add(goalDir), goal);
//				RobotPlayer.bugStart = myLoc;
//			}
//
//
//			//Conditions to stop bugging.
//			double dist = myLoc.distanceSquaredTo(goal);
//			double bugDist = RobotPlayer.bugStartDist;
//			double probDist = dist - bugDist;
//			if(probDist <= 0){
//				probDist = 1;
//			}
//			float prob = (float) Math.exp(-1000.0/probDist);
//			//rc.setIndicatorString(2, "bugLeft is now" + RobotPlayer.bugLeft);
//
//			if(!rc.getLocation().equals(RobotPlayer.bugStart)){
//				RobotPlayer.bugMoved = true;
//			}
//			rc.setIndicatorString(2, "bugleft is now "+ RobotPlayer.bugLeft + " and bugDir is " + RobotPlayer.bugDir + " and goal is " + goal + " bugmoved: "+RobotPlayer.bugMoved);
//			if(random.nextFloat() < prob ){
//				RobotPlayer.bugDir = null;
//				RobotPlayer.bugStart = null;
//				RobotPlayer.bugTarget = null;
//				RobotPlayer.bugMoved = false;
//			}
//			if(RobotPlayer.bugMoved && rc.getLocation().equals(RobotPlayer.bugStart)){
//				RobotPlayer.bugDir = null;
//				RobotPlayer.bugStart = null;
//				RobotPlayer.bugTarget = null;
//				RobotPlayer.bugMoved = false;
//			}
//
//			if(RobotPlayer.bugDir != null){
//
//
//
//				if(RobotPlayer.bugLeft){
//					if(!RobotPlayer.bugTarget.equals(goal) || (myLoc.distanceSquaredTo(goal) < RobotPlayer.bugStartDist)){
//						RobotPlayer.bugDir = null;
//						RobotPlayer.bugStartDist = 100000;
//						RobotPlayer.bugTarget = null;
//					} else{
//
//						//IF you can turn right, do so.
//						if(Soldier.canMove(rc, RobotPlayer.bugDir.rotateRight(), bugAroundEnemies)){
//							RobotPlayer.bugDir = RobotPlayer.bugDir.rotateRight();
//						}
//						//if you can move in the direction you're bugging in, go ahead.
//						if(Soldier.canMove(rc, RobotPlayer.bugDir, bugAroundEnemies)){
//							if(rc.isActive()){
//								if(sneak){
//									rc.sneak(RobotPlayer.bugDir);
//								}
//								else{
//									rc.move(RobotPlayer.bugDir);
//								}
//							}
//
//						}
//						else{
//							RobotPlayer.bugDir = RobotPlayer.bugDir.rotateLeft();
//						}
//					}
//				}
//				else{
//					if(!RobotPlayer.bugTarget.equals(goal) || (myLoc.distanceSquaredTo(goal) < RobotPlayer.bugStartDist)){
//						RobotPlayer.bugDir = null;
//						RobotPlayer.bugStartDist = 100000;
//						RobotPlayer.bugTarget = null;
//					} else{
//
//
//						//IF you can turn left, do so.
//						if(Soldier.canMove(rc, RobotPlayer.bugDir.rotateLeft(), bugAroundEnemies)){
//							RobotPlayer.bugDir = RobotPlayer.bugDir.rotateLeft();
//						}
//						//if you can move in the direction you're bugging in, go ahead.
//						if(Soldier.canMove(rc, RobotPlayer.bugDir, bugAroundEnemies)){
//							if(rc.isActive()){
//								if(sneak){
//									rc.sneak(RobotPlayer.bugDir);
//								}
//								else{
//									rc.move(RobotPlayer.bugDir);
//								}
//							}
//
//						}
//						else{
//							RobotPlayer.bugDir = RobotPlayer.bugDir.rotateRight();
//						}
//					}
//
//				}
//			}
//		}

	}

	public static boolean isLeft(MapLocation a, MapLocation b, MapLocation c){
		return ((b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)) > 0;
	}

	//	private static boolean shouldIgoLeft(RobotController rc, MapLocation goal) {
	//		//rc.setIndicatorString(2, "Should I go Left");
	//
	//		Direction rightOne = rc.getLocation().directionTo(goal).rotateRight();
	//		Direction rightTwo = rightOne.rotateRight();
	//		Direction leftOne = rc.getLocation().directionTo(goal).rotateLeft();
	//		Direction leftTwo = leftOne.rotateLeft();
	//		int leftVoids = 0;
	//		int rightVoids = 0;
	//		for(int i=1; i*i<rc.getType().sensorRadiusSquared*2; i++){
	//			MapLocation locRightOne = rc.getLocation().add(rightOne, i);
	//			TerrainTile locRightOneTile = rc.senseTerrainTile(locRightOne);
	//			if(!(locRightOneTile.equals(TerrainTile.NORMAL) || locRightOneTile.equals(TerrainTile.ROAD))){
	//				rightVoids++;
	//			}
	//			MapLocation locRightTwo = rc.getLocation().add(rightTwo, i);
	//			TerrainTile locRightTwoTile = rc.senseTerrainTile(locRightTwo);
	//			if(!(locRightTwoTile.equals(TerrainTile.NORMAL) || locRightTwoTile.equals(TerrainTile.ROAD))){
	//				rightVoids++;
	//			}
	//			MapLocation locLeftOne = rc.getLocation().add(leftOne, i);
	//			TerrainTile locLeftOneTile = rc.senseTerrainTile(locLeftOne);
	//			if(!(locLeftOneTile.equals(TerrainTile.NORMAL) || locLeftOneTile.equals(TerrainTile.ROAD))){
	//				leftVoids++;
	//			}
	//			MapLocation locLeftTwo = rc.getLocation().add(leftTwo, i);
	//			TerrainTile locLeftTwoTile = rc.senseTerrainTile(locLeftTwo);
	//			if(!(locLeftTwoTile.equals(TerrainTile.NORMAL) || locLeftTwoTile.equals(TerrainTile.ROAD))){
	//				leftVoids++;
	//			}
	//		}
	//		//System.out.println("Going left is : "+(leftVoids < rightVoids));
	//		rc.setIndicatorString(2, "Going left is : "+(leftVoids < rightVoids) + "  leftvoids: "+leftVoids + " rightVoids: "+rightVoids + " goaldir is:"+rc.getLocation().directionTo(goal) + " "+ rightOne + " "+leftOne );
	//		return leftVoids < rightVoids;
	//
	//
	//	}

	//########### simple move code #################### (Possibly the simplest navigation code ... kinda like the right way to implement bug... except not.)
	public static void simpleMove(RobotController rc, MapLocation goal, boolean bugAroundEnemies) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(goal);
		if(Soldier.canMove(rc,dir, bugAroundEnemies)){
			if(rc.isActive()){
				rc.move(dir);
			}
		}

		else{
			boolean turnLeft = isLeft(rc.getLocation(), rc.getLocation().add(dir), goal);
			if(turnLeft){
				Direction trialDir = dir.rotateLeft();
				while(!Soldier.canMove(rc,trialDir, bugAroundEnemies)){
					trialDir = trialDir.rotateLeft();
				}
				if(rc.isActive()){
					rc.move(trialDir);
				}
			}else{
				Direction trialDir = dir.rotateRight();
				while(!Soldier.canMove(rc,trialDir, bugAroundEnemies)){
					trialDir = trialDir.rotateRight();
				}
				if(rc.isActive()){
					rc.move(trialDir);
				}			
			}
		}

	}



}