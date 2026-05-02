package dev.tr7zw.entityculling;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

class CullingPacketListener extends PacketListenerAbstract {

    private final PlayerCache cache;

    public CullingPacketListener(PlayerCache cache) {
        super(PacketListenerPriority.NORMAL);
        this.cache = cache;
    }

    @Override
    public void onPacketSend(@NonNull PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            int entityId = new WrapperPlayServerSpawnEntity(event).getEntityId();
            Player player = event.getPlayer();
            if (this.cache.isEntityHidden(player, entityId)) {
                event.setCancelled(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA
                || event.getPacketType() == PacketType.Play.Server.ENTITY_HEAD_LOOK
                || event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY
                || event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION
                || event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE
                || event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            int entityId = this.getEntityIdFromMovementPacket(event);
            Player player = event.getPlayer();
            if (this.cache.isEntityHidden(player, entityId)) {
                event.setCancelled(true);
            }
        }
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
}
