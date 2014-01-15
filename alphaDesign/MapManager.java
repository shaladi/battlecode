package alphaDesign;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MapManager {
	public static TerrainTile[][] map;
	public static int mapWidth;
	public static int mapHeight;
	
	public static void assessMap(RobotController rc) {
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
		map = new TerrainTile[mapWidth][mapHeight];
		for(int y = 0; y < mapHeight; y++) {
			for(int x = 0; x < mapWidth; x++) {
				map[x][y] = rc.senseTerrainTile(new MapLocation(x, y));
			}
		}
	}
	
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