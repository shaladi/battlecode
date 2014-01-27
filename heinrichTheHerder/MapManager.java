package heinrichTheHerder;

//import java.util.ArrayList;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;
//import battlecode.common.Direction;

public class MapManager{
	public static TerrainTile[][] map;
	public static int coarsenRate;
	public static int[][] coarseMap;
	public static boolean[][] myTerritory;
	public static double[][] cowGrowth;
	public static int mapWidth;
	public static int mapHeight;
	public static MapLocation HQLoc;
	public static MapLocation enemyHQLoc;
	public static ArrayList<MapLocation> goodPastrLocations;
	
	
	
	
	//This function creates and stores internal representation of map and cow growth.
	
	public static void assessMap(RobotController rc){
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
		HQLoc = rc.senseHQLocation();
		enemyHQLoc = rc.senseEnemyHQLocation();
		cowGrowth = rc.senseCowGrowth();
		map = new TerrainTile[mapWidth][mapHeight];
		myTerritory = new boolean[mapWidth][mapHeight];
		for(int y = 0; y < mapHeight; y++) {
			for(int x = 0; x < mapWidth; x++) {
				map[x][y] = rc.senseTerrainTile(new MapLocation(x, y));
				myTerritory[x][y] = HQLoc.distanceSquaredTo(new MapLocation(x,y))<enemyHQLoc.distanceSquaredTo(new MapLocation(x,y));
			}
		}
	}
	
	//This function creates pseudo voids around HQ to avoid missile fires.
//	
//	public static void voidEnemyHQ(RobotController rc){
//		int rad = rc.getType().attackRadiusMaxSquared;
//		ArrayList<MapLocation> voids = new ArrayList<MapLocation>();
//		voids.add(enemyHQLoc);
//		map[enemyHQLoc.x][enemyHQLoc.y] = TerrainTile.OFF_MAP;
//		while(voids.size()>0){
//			MapLocation cell = voids.remove(0);
//			for(int i=0; i<8; i++){
//				MapLocation consider = cell.add(Direction.values()[i]);
//				if(consider.distanceSquaredTo(enemyHQLoc)<rad && !(map[consider.x][consider.y].equals(TerrainTile.VOID) | map[consider.x][consider.y].equals(TerrainTile.OFF_MAP))){
//					map[consider.x][consider.y] = TerrainTile.VOID;
//					voids.add(consider);
//				}
//				
//			}
//		}
//	}
//	
	
	//This function calculates the best positions to build pastures.
	
	public static void getGoodPastrLocations(RobotController rc){
		//Penalize areas farther from HQ	
		//Perhaps a good strategy to think about here is to build away from enemyHQ instead of close to alliedHQ.
		double distanceToEnemy = HQLoc.distanceSquaredTo(enemyHQLoc);
		double penality = 10.0/(distanceToEnemy/2.0);
		double[] values = {0,0,0,0};
		goodPastrLocations = new ArrayList<MapLocation>();
		double nearHQCount = 0;
		for(int startx=0; startx < mapWidth; startx += 5){
			for(int starty=0; starty < mapHeight; starty+=5){
				MapLocation startLoc = new MapLocation(startx, starty);
				double currentCount = distanceToEnemy/2.0 - HQLoc.distanceSquaredTo(startLoc)*penality;
				MapLocation bestLoc = null;
				for(int x=0; x<5; x++){
					for(int y=0; y<5; y++){
						MapLocation loc = new MapLocation(startx+x, starty+y);
						if(loc.x < mapWidth && loc.y < mapHeight && myTerritory[loc.x][loc.y]){
							currentCount += cowGrowth[loc.x][loc.y];
							if(loc.distanceSquaredTo(HQLoc) <= RobotType.HQ.sensorRadiusSquared){
								nearHQCount += cowGrowth[loc.x][loc.y];
							}
							if(bestLoc==null){
								if(!map[loc.x][loc.y].equals(TerrainTile.VOID)){
									bestLoc = loc;
								}
							}
						}
					}
				}
				if (currentCount > 0){
					for(int x=0; x<4; x++){
						if(currentCount > values[x]){
							values[x] = currentCount;
							goodPastrLocations.add(x, bestLoc);
							break;
						}
					}
				}
			//This would be a nifty little tricky place to build a pastr since hq can offer protection.	
				if(nearHQCount > 5){
					for(int i=0; i<8; i++){
						MapLocation loc = HQLoc.add(Direction.values()[i]);
						if(!(map[loc.x][loc.y].equals(TerrainTile.VOID) || map[loc.x][loc.y].equals(TerrainTile.OFF_MAP))){
							goodPastrLocations.add(0, loc);
							break;
						}
					}
				}
			
			
			}
		}
//		System.out.println("GOOD PASTR LOCATIONS ARE: ");
//		System.out.println(Arrays.toString(goodPastrLocations));	
	}
	
	//This function creates a coarse version of our internal map representation;
	public static void coarsenMap(RobotController rc, int rate){
		coarsenRate = rate;
		coarseMap = new int[mapWidth/coarsenRate][mapHeight/coarsenRate];
		for(int startx=0; startx<mapWidth/coarsenRate; startx+=1){
			for(int starty=0; starty<mapHeight/coarsenRate; starty+=1){
				int voidCount = 0;
				for(int x=0; x<5; x++){
					for(int y=0; y<5; y++){
						MapLocation loc = new MapLocation(startx*mapWidth/coarsenRate+x, starty*mapHeight/coarsenRate+y);
						if(loc.x < mapWidth && loc.y < mapHeight){
							if(map[loc.x][loc.y].equals(TerrainTile.VOID)){
								voidCount += 1;
							}
							else if(map[loc.x][loc.y].equals(TerrainTile.ROAD)){
								voidCount -= 1;
							}
						}
						
					}
				}
				coarseMap[startx][starty] = voidCount;
			}
		}
	}
	
	//This function prints out the internal map representation.

	public static void printMap() {
		String rep = "";
		for(int y = 0; y < mapHeight; y++) {
			for(int x = 0; x < mapWidth; x++) {
				switch(map[x][y]) {
				case NORMAL:
					rep += "0 ";
					break;
				case ROAD:
					rep += "1 ";
					break;
				case VOID:
					rep += "X ";
					break;
				case OFF_MAP:
					rep += "N ";
					break;
				}
			}
			rep = rep.trim() + "\n";
		}
		System.out.println(rep);
	}	
	
	
}