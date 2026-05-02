package dev.tr7zw.entityculling.occlusionculling;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.EnchantingTable;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import dev.tr7zw.entityculling.CullingPlugin;

public class BlockChangeListener implements Listener {

	public final Map<ChunkCoords, ChunkSnapshot> cachedChunkSnapshots = new ConcurrentHashMap<>();
	public final Map<ChunkCoords, BlockState[]> cachedChunkTiles = new ConcurrentHashMap<>();
	public final Map<ChunkCoords, Entity[]> cachedChunkEntities = new ConcurrentHashMap<>();
	private final CullingPlugin plugin;


	public BlockChangeListener(CullingPlugin plugin) {
		this.plugin = plugin;
		//Delay 1 tick to account for plugins such as multiverse
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			for (World world : Bukkit.getWorlds()) {
				for (Chunk chunk : world.getLoadedChunks()) {
					handleChunkLoadSync(chunk);
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		Chunk chunk = e.getBlock().getChunk();
		updateCachedChunkSync(new ChunkCoords(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), chunk);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		Chunk chunk = e.getBlock().getChunk();
		updateCachedChunkSync(new ChunkCoords(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), chunk);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent e) {
		handleChunkLoadSync(e.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent e) {
		Chunk chunk = e.getChunk();
		updateCachedChunkSync(new ChunkCoords(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), null);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent e) {
		handleExplosionSync(e.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent e) {
		handleExplosionSync(e.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSpawn(EntitySpawnEvent event) {
		Chunk chunk = event.getEntity().getLocation().getChunk();
		updateCachedChunkEntitiesSync(new ChunkCoords(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), chunk);
	}

	public void handleChunkLoadSync(Chunk loadedChunk) {
		updateCachedChunkSync(new ChunkCoords(loadedChunk.getWorld().getName(), loadedChunk.getX(), loadedChunk.getZ()),
				loadedChunk);
	}

	public void handleExplosionSync(List<Block> blockList) {
		Set<ChunkCoords> chunks = new HashSet<>();
		for (Block block : blockList) {
			int chunkX = (int) Math.floor(block.getX() / 16d);
			int chunkZ = (int) Math.floor(block.getZ() / 16d);
			chunks.add(new ChunkCoords(block.getWorld().getName(), chunkX, chunkZ));
		}
		for (ChunkCoords cc : chunks) {
			updateCachedChunkSync(cc, cc.getRealChunkSync());
		}
	}

	public void updateCachedChunkSync(final ChunkCoords cc, final Chunk chunk) {
		if (chunk == null) {
			this.cachedChunkSnapshots.remove(cc);
			this.cachedChunkTiles.remove(cc);
			this.cachedChunkEntities.remove(cc);
			return;
		}
		this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
			this.cachedChunkSnapshots.put(cc, chunk.getChunkSnapshot());
			this.cachedChunkTiles.put(cc, filterTiles(chunk.getTileEntities()));
			this.cachedChunkEntities.put(cc, chunk.getEntities());
		});
	}

	private BlockState[] filterTiles(BlockState[] tiles) {
		if (tiles.length == 0)
			return tiles;
		List<BlockState> list = new ArrayList<>(Arrays.asList(tiles)); // the arrays as list is not modifiable
		list.removeIf(state -> !(state instanceof Chest
				|| state instanceof Shulker
				|| state instanceof CreatureSpawner
				|| state instanceof EnchantingTable
				|| state instanceof Banner
				|| state instanceof Skull));
		return list.toArray(new BlockState[0]);
	}

	public void updateCachedChunkEntitiesSync(final ChunkCoords cc, final Chunk chunk) {
		if (chunk == null) {
			this.cachedChunkSnapshots.remove(cc);
			this.cachedChunkTiles.remove(cc);
			this.cachedChunkEntities.remove(cc);
			return;
		}
		this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
			this.cachedChunkEntities.put(cc, chunk.getEntities());
		});
	}

	public ChunkCoords getChunkCoords(Location loc) {
		int chunkX = (int) Math.floor(loc.getBlockX() / 16d);
		int chunkZ = (int) Math.floor(loc.getBlockZ() / 16d);
		return new ChunkCoords(loc.getWorld().getName(), chunkX, chunkZ);
	}

	@Deprecated
	public boolean isInLoadedChunk(Location loc) {
		return isInLoadedChunk(getChunkCoords(loc));
	}

	public boolean isInLoadedChunk(ChunkCoords cc) {
		return this.cachedChunkSnapshots.containsKey(cc);
	}

	@Deprecated
	public ChunkSnapshot getChunk(Location loc) {
		return getChunk(getChunkCoords(loc));
	}

	public ChunkSnapshot getChunk(ChunkCoords cc) {
		return this.cachedChunkSnapshots.get(cc);
	}

	@Deprecated
	public BlockState[] getChunkTiles(Location loc) {
		return getChunkTiles(getChunkCoords(loc));
	}

	public BlockState[] getChunkTiles(ChunkCoords cc) {
		return this.cachedChunkTiles.get(cc);
	}

	@Deprecated
	public Entity[] getChunkEntities(Location loc) {
		return getChunkEntities(getChunkCoords(loc));
	}

	public Entity[] getChunkEntities(ChunkCoords cc) {
		return this.cachedChunkEntities.get(cc);
	}

	public static class ChunkCoords {
		public String worldName;
		public int chunkX;
		public int chunkZ;

		public ChunkCoords(String worldName, int chunkX, int chunkZ) {
			this.worldName = worldName;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + chunkX;
			result = prime * result + chunkZ;
			result = prime * result + ((worldName == null) ? 0 : worldName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ChunkCoords other = (ChunkCoords) obj;
			if (this.chunkX != other.chunkX)
				return false;
			if (this.chunkZ != other.chunkZ)
				return false;
			if (this.worldName == null) {
				return other.worldName == null;
			}
			return this.worldName.equals(other.worldName);
		}

		public Chunk getRealChunkSync() {
			World world = Bukkit.getWorld(this.worldName);
			return world.getChunkAt(this.chunkX, this.chunkZ);
		}
	}
}