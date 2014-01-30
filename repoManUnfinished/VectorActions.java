package repoMan;

import battlecode.common.MapLocation;

public class VectorActions{
	
	public static MapLocation mldivide(MapLocation loc, int divisor){
		return new MapLocation(loc.x/divisor, loc.y/divisor);
	}

	public static MapLocation mladd(MapLocation a, MapLocation b) {
		return new MapLocation(a.x+b.x, a.y+b.y);
	}
	
	public static MapLocation mlsubtract(MapLocation a, MapLocation b) {
		return new MapLocation(a.x-b.x, a.y-b.y);
	}
	
	public static int locToInt(MapLocation loc){
		return loc.x*100+loc.y;
	}
	
	public static MapLocation intToLoc(int code){
		return new MapLocation(code/100, code%100);
	}
	
}