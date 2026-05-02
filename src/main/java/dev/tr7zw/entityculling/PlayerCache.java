package dev.tr7zw.entityculling;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;

import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class PlayerCache implements Listener {

	private final Map<Player, Set<Location>> hiddenBlocks = new HashMap<>();
	private final Map<Player, Set<Integer>> hiddenEntitiesID = new HashMap<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final WriteLock writeLock = lock.writeLock();
	private final ReadLock readLock = lock.readLock();

	public void setHidden(Player player, Location loc, boolean hidden) {
		try {
			this.writeLock.lock();
			Set<Location> blocks = this.hiddenBlocks.computeIfAbsent(player, p -> new HashSet<>());
			if (!hidden) {
				blocks.remove(loc);
			} else {
				blocks.add(loc);
			}
		} finally {
			this.writeLock.unlock();
		}
	}

	public void setHidden(Player player, Entity entity, boolean hidden) {
		try {
			this.writeLock.lock();
			Set<Integer> ids = this.hiddenEntitiesID.computeIfAbsent(player, p -> new HashSet<>());
			if (!hidden) {
				ids.remove(entity.getEntityId());
			} else {
				ids.add(entity.getEntityId());
			}
		} finally {
			this.writeLock.unlock();
		}
	}

	public boolean isHidden(Player player, Location loc) {
		try {
			this.readLock.lock();
			Set<Location> blocks = this.hiddenBlocks.get(player);
			if (blocks == null) {
				return false;
			}
			return blocks.contains(loc);
		} finally {
			this.readLock.unlock();
		}
	}

	public boolean isEntityHidden(Player player, int entityId) {
		try {
			this.readLock.lock();
			Set<Integer> ids = this.hiddenEntitiesID.get(player);
			if (ids == null) {
				return false;
			}
			return ids.contains(entityId);
		} finally {
			this.readLock.unlock();
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		try {
			this.writeLock.lock();
			this.hiddenBlocks.remove(event.getPlayer());
			this.hiddenEntitiesID.remove(event.getPlayer());
		} finally {
			this.writeLock.unlock();
		}
	}

	@EventHandler
	public void onDeath(EntityRemoveEvent event) {
		try {
			this.writeLock.lock();
			for (Set<Integer> hidden : this.hiddenEntitiesID.values()) {
				hidden.remove(event.getEntity().getEntityId());
			}
		} finally {
			this.writeLock.unlock();
		}
	}

	@EventHandler
	public void onUnload(PlayerChunkUnloadEvent event) {
		try {
			this.writeLock.lock();
			Set<Integer> entities = this.hiddenEntitiesID.get(event.getPlayer());
			if (entities != null) {
				Arrays.stream(event.getChunk().getEntities())
						.map(Entity::getEntityId).toList()
						.forEach(entities::remove);
			}
		} finally {
			this.writeLock.unlock();
		}
	}
}
