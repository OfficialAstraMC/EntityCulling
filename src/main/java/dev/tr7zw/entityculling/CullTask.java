package dev.tr7zw.entityculling;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import dev.tr7zw.entityculling.occlusionculling.BlockChangeListener;
import dev.tr7zw.entityculling.occlusionculling.BlockChangeListener.ChunkCoords;
import dev.tr7zw.entityculling.occlusionculling.OcclusionCullingInstance;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class CullTask implements Runnable {

    private static final BoundingBox BLOCK_AABB = new BoundingBox(
            0d,
            0d,
            0d,
            1d,
            1d,
            1d
    );
    private static final BoundingBox ENTITY_AABB = new BoundingBox(
            0d,
            0d,
            0d,
            1d,
            2d,
            1d
    );

    private final BlockChangeListener blockChangeListener;
    private final PlayerCache cache;
    private final OcclusionCullingInstance culling;

    public CullTask(BlockChangeListener blockChangeListener, PlayerCache cache) {
        this.blockChangeListener = blockChangeListener;
        this.cache = cache;
        this.culling = new OcclusionCullingInstance(blockChangeListener);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.culling.resetCache();
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    Location loc = player.getLocation().add(x * 16, 0, y * 16);
                    ChunkCoords coords = this.blockChangeListener.getChunkCoords(loc);
                    if (this.blockChangeListener.isInLoadedChunk(coords)) {
                        // --- Block culling ---
                        BlockState[] tiles = this.blockChangeListener.getChunkTiles(coords);
                        if (tiles != null) {
                            for (BlockState blockState : tiles) {
                                boolean canSee = this.culling.isBoundingBoxVisible(blockState.getLocation(), BLOCK_AABB,
                                        player.getEyeLocation(), false);
                                boolean hidden = this.cache.isHidden(player, blockState.getLocation());

                                if (hidden && canSee) {
                                    this.cache.setHidden(player, blockState.getLocation(), false);
                                    player.sendBlockChange(blockState.getLocation(), blockState.getBlockData());
                                    if (blockState instanceof TileState tileState) {
                                        player.sendBlockUpdate(blockState.getLocation(), tileState);
                                    }
                                } else if (!hidden && !canSee) {
                                    this.cache.setHidden(player, blockState.getLocation(), true);
                                    player.sendBlockChange(blockState.getLocation(), Material.BARRIER.createBlockData());
                                }
                            }
                        }

                        // --- Entity culling ---
                        Entity[] entities = this.blockChangeListener.getChunkEntities(coords);
                        if (entities != null) {
                            for (Entity entity : entities) {
                                if (this.shouldNotHide(entity)) {
                                    continue;
                                }
                                boolean isClose = player.getLocation().distance(entity.getLocation()) <= 16.0;
                                boolean isAABVisible = this.culling.isBoundingBoxVisible(
                                        entity.getLocation(),
                                        ENTITY_AABB,
                                        player.getEyeLocation(),
                                        true
                                );
                                boolean canSee = isClose || isAABVisible;
                                boolean hidden = this.cache.isEntityHidden(player, entity.getEntityId());
                                if (hidden && canSee) {
                                    this.cache.setHidden(player, entity, false);
                                    if (!(entity instanceof Player) && entity.isValid()) {
                                        sendSpawnPacket(player, entity);
                                    }
                                } else if (!hidden && !canSee) {
                                    this.cache.setHidden(player, entity, true);
                                    sendDestroyPacket(player, entity);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldNotHide(Entity entity) {
        return entity instanceof Player
                || entity instanceof ExperienceOrb
                || entity instanceof Display;
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