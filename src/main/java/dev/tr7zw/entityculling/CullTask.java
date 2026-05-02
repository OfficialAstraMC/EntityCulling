package dev.tr7zw.entityculling;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;

import dev.tr7zw.entityculling.occlusionculling.OcclusionCullingInstance;
import dev.tr7zw.entityculling.occlusionculling.BlockChangeListener.ChunkCoords;
import org.bukkit.util.Vector;

public class CullTask implements Runnable {

	private static final AxisAlignedBB BLOCK_AABB = new AxisAlignedBB(
			0d,
			0d,
			0d,
			1d,
			1d,
			1d
	);
	private static final AxisAlignedBB ENTITY_AABB = new AxisAlignedBB(
			0d,
			0d,
			0d,
			1d,
			2d,
			1d
	);

	private final CullingPlugin instance;
	private final OcclusionCullingInstance culling = new OcclusionCullingInstance();

	public CullTask(CullingPlugin pl) {
		this.instance = pl;
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.culling.resetCache();
			for (int x = -3; x <= 3; x++) {
				for (int y = -3; y <= 3; y++) {
					Location loc = player.getLocation().add(x * 16, 0, y * 16);
					ChunkCoords coords = this.instance.blockChangeListener.getChunkCoords(loc);
					if (this.instance.blockChangeListener.isInLoadedChunk(coords)) {
						// --- Block culling ---
						BlockState[] tiles = this.instance.blockChangeListener.getChunkTiles(coords);
						if (tiles != null) {
							for (BlockState block : tiles) {
								boolean canSee = this.culling.isAABBVisible(block.getLocation(), BLOCK_AABB,
										player.getEyeLocation(), false);
								boolean hidden = this.instance.cache.isHidden(player, block.getLocation());

								if (hidden && canSee) {
									this.instance.cache.setHidden(player, block.getLocation(), false);
									player.sendBlockChange(block.getLocation(), block.getBlockData());
								} else if (!hidden && !canSee) {
									this.instance.cache.setHidden(player, block.getLocation(), true);
									player.sendBlockChange(block.getLocation(), Material.BARRIER, (byte) 0);
								}
							}
						}

						// --- Entity culling ---
						Entity[] entities = this.instance.blockChangeListener.getChunkEntities(coords);
						if (entities != null) {
							for (Entity entity : entities) {
								boolean canSee = this.culling.isAABBVisible(entity.getLocation(), ENTITY_AABB, player.getEyeLocation(), true);
								boolean hidden = this.instance.cache.isEntityHidden(player, entity.getEntityId());

								if (hidden && canSee) {
									this.instance.cache.setHidden(player, entity, false);
									if (!(entity instanceof Player)) {
										sendSpawnPacket(player, entity);
									}
								} else if (!hidden && !canSee) {
									// Hide: send destroy packet
									if (!(entity instanceof Player)
											&& !(entity instanceof ExperienceOrb)
											&& !(entity instanceof Painting)) {
										this.instance.cache.setHidden(player, entity, true);
										sendDestroyPacket(player, entity);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void sendDestroyPacket(Player player, Entity entity) {
		WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entity.getEntityId());
		PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
	}

	private void sendSpawnPacket(Player player, Entity entity) {
		PacketWrapper<?> packet = this.createSpawnPacket(entity);
		PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
		WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(
				entity.getEntityId(),
				SpigotConversionUtil.getEntityMetadata(entity)
		);
		PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
	}

	private PacketWrapper<?> createSpawnPacket(Entity entity) {
		org.bukkit.Location loc = entity.getLocation();
		Vector3d position = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
		com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType =
				EntityTypes.getByName(entity.getType().getKey().getKey());
		Vector bukkitVelocity = entity.getVelocity();
		Vector3d velocity = new Vector3d(bukkitVelocity.getX(), bukkitVelocity.getY(), bukkitVelocity.getZ());
		com.github.retrooper.packetevents.protocol.world.Location peLoc = new com.github.retrooper.packetevents.protocol.world.Location(position, loc.getYaw(), loc.getPitch());
		return new WrapperPlayServerSpawnEntity(
				entity.getEntityId(),
				entity.getUniqueId(),
				entityType,
				peLoc,
				loc.getYaw(), // headYaw
				0,
				velocity
		);
	}
}