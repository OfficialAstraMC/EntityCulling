package dev.tr7zw.entityculling;

import org.bukkit.util.Vector;

public enum WrappedDirection {

	EAST(1, 0, 0),
	WEST(-1, 0, 0),
	SOUTH(0, 0, 1),
	NORTH(0, 0, -1),
	UP(0, 1, 0),
	DOWN(0, -1, 0);

	private final Vector v;

	WrappedDirection(int x, int y, int z) {
		this.v = new Vector(x, y, z);
    }


	public Vector getVector() {
		return v.clone();
	}

	public int getX() {
		return v.getBlockX();
	}

	public int getY() {
		return v.getBlockY();
	}

	public int getZ() {
		return v.getBlockZ();
	}
}
