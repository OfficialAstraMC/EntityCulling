package dev.tr7zw.entityculling;

import com.github.retrooper.packetevents.PacketEvents;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

import org.bukkit.plugin.java.JavaPlugin;

import dev.tr7zw.entityculling.occlusionculling.BlockChangeListener;

public class CullingPlugin extends JavaPlugin {

	private BlockChangeListener blockChangeListener;
	private PlayerCache cache;

	@Override
	public void onLoad() {
		PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
		PacketEvents.getAPI().load();
	}

	@Override
	public void onEnable() {
		PacketEvents.getAPI().init();
		this.blockChangeListener  = new BlockChangeListener(this);
		this.cache = new PlayerCache();
		this.getServer().getPluginManager().registerEvents(this.blockChangeListener, this);
		this.getServer().getPluginManager().registerEvents(this.cache, this);
		this.getServer().getScheduler().runTaskTimerAsynchronously(
				this,
				new CullTask(this.blockChangeListener, this.cache),
				1,
				1
		);
		PacketEvents.getAPI().getEventManager().registerListener(new CullingPacketListener(this.cache));
	}

	@Override
	public void onDisable() {
		PacketEvents.getAPI().terminate();
	}

	public BlockChangeListener getBlockChangeListener() {
		return this.blockChangeListener;
	}

	public PlayerCache getCache() {
		return this.cache;
	}
}