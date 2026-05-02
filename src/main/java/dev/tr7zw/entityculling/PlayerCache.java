package dev.tr7zw.entityculling;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerCache implements Listener {

	private final Map<Player, Set<Location>> hiddenBlocks = new ConcurrentHashMap<>();
	private final Map<Player, Set<Integer>> hiddenEntitiesID = new ConcurrentHashMap<>();

	public void setHidden(Player player, Location loc, boolean hidden) {
		Set<Location> blocks = this.hiddenBlocks.computeIfAbsent(player, p -> ConcurrentHashMap.newKeySet());
		if (!hidden) {
			blocks.remove(loc);
		} else {
			blocks.add(loc);
		}
	}

	public void setHidden(Player player, Entity entity, boolean hidden) {
		Set<Integer> ids = this.hiddenEntitiesID.computeIfAbsent(player, p -> ConcurrentHashMap.newKeySet());
		if (!hidden) {
			ids.remove(entity.getEntityId());
		} else {
			ids.add(entity.getEntityId());
		}
	}

	public boolean isHidden(Player player, Location loc) {
		Set<Location> blocks = this.hiddenBlocks.get(player);
		if (blocks == null) {
			return false;
		}
		return blocks.contains(loc);
	}

	public boolean isEntityHidden(Player player, int entityId) {
		Set<Integer> ids = this.hiddenEntitiesID.get(player);
		if (ids == null) {
			return false;
		}
		return ids.contains(entityId);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.hiddenBlocks.remove(event.getPlayer());
		this.hiddenEntitiesID.remove(event.getPlayer());
	}

	@EventHandler
	public void onDeath(EntityRemoveEvent event) {
		for (Set<Integer> hidden : this.hiddenEntitiesID.values()) {
			hidden.remove(event.getEntity().getEntityId());
		}
	}

	@EventHandler
	public void onUnload(PlayerChunkUnloadEvent event) {
		Set<Integer> entities = this.hiddenEntitiesID.get(event.getPlayer());
		if (entities != null) {
			Arrays.stream(event.getChunk().getEntities())
					.map(Entity::getEntityId).toList()
					.forEach(entities::remove);
		}
	}
}