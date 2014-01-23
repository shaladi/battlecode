package garthTheGrouper;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

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
	public static MapLocation[] goodPastrLocations;
	
	
	
	
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
	
	
	//This function calculates the best positions to build pastures.
	
	public static void getGoodPastrLocations(RobotController rc){
		//Penalize areas farther from HQ	
		//Perhaps a good strategy to think about here is to build away from enemyHQ instead of close to alliedHQ.
		double distanceToEnemy = HQLoc.distanceSquaredTo(enemyHQLoc);
		double penality = 10.0/(distanceToEnemy/2.0);
		double[] values = {0,0,0,0};
		goodPastrLocations = new MapLocation[4];
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
							for(int y=2; y>=x; y--){
								values[y+1] = values[y];
								goodPastrLocations[y+1] = goodPastrLocations[y];
							}
							values[x] = currentCount;
							goodPastrLocations[x] = bestLoc;
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