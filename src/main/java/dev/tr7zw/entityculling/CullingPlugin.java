package dev.tr7zw.entityculling;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tr7zw.entityculling.occlusionculling.BlockChangeListener;
import org.jspecify.annotations.NonNull;

public class CullingPlugin extends JavaPlugin {

	public static CullingPlugin INSTANCE;
	public BlockChangeListener blockChangeListener;
	public PlayerCache cache;

	@Override
	public void onLoad() {
		PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
		PacketEvents.getAPI().load();
	}

	@Override
	public void onEnable() {
		INSTANCE = this;
		PacketEvents.getAPI().init();
		this.blockChangeListener = new BlockChangeListener();
		this.cache = new PlayerCache();

		Bukkit.getPluginManager().registerEvents(this.blockChangeListener, this);
		Bukkit.getPluginManager().registerEvents(this.cache, this);
		Bukkit.getScheduler().runTaskTimerAsynchronously(INSTANCE, new CullTask(this), 1, 1);

		PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
			@Override
			public void onPacketSend(@NonNull PacketSendEvent event) {
				if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
					int entityId = new WrapperPlayServerSpawnEntity(event).getEntityId();
					Player player = event.getPlayer();
					if (cache.isEntityHidden(player, entityId)) {
						event.setCancelled(true);
					}
				} else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA
						|| event.getPacketType() == PacketType.Play.Server.ENTITY_HEAD_LOOK
						|| event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY
						|| event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION
						|| event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE
						|| event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
					int entityId = getEntityIdFromMovementPacket(event);
					Player player = event.getPlayer();
					if (cache.isEntityHidden(player, entityId)) {
						event.setCancelled(true);
					}
				}
			}
		});
	}

	@Override
	public void onDisable() {
		PacketEvents.getAPI().terminate();
	}

	private int getEntityIdFromMovementPacket(PacketSendEvent event) {
		if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
			return new WrapperPlayServerEntityMetadata(event).getEntityId();
		} else if (event.getPacketType() == PacketType.Play.Server.ENTITY_HEAD_LOOK) {
			return new WrapperPlayServerEntityHeadLook(event).getEntityId();
		} else if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
			return new WrapperPlayServerEntityVelocity(event).getEntityId();
		} else if (event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION) {
			return new WrapperPlayServerEntityRotation(event).getEntityId();
		} else if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
			return new WrapperPlayServerEntityRelativeMove(event).getEntityId();
		} else if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
			return new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
		}
		return -1;
	}

	public static void runTask(Runnable task) {
		if (INSTANCE.isEnabled()) {
			Bukkit.getScheduler().runTask(INSTANCE, task);
		}
	}
}