package net.iskaa303.simpleportals.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.entity.PortalEntity;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.iskaa303.simpleportals.entity.PortalWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

    /**
     * /simpleportals kill (uuid) — deletes a portal.
     * Requires cheats/op (level 2).
     * Tab-completes portal UUIDs, prioritizing the one the player is looking at.
     */
public final class PortalCommand {

    private static final SimpleCommandExceptionType INVALID_UUID = new SimpleCommandExceptionType(
            Component.literal("Invalid portal UUID"));
    private static final SimpleCommandExceptionType PORTAL_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("Portal not found"));

    private static final SuggestionProvider<CommandSourceStack> PORTAL_SUGGESTIONS = (ctx, builder) -> {
        CommandSourceStack source = ctx.getSource();
        // Get the UUID of the portal the player is looking at
        String lookingAt = getLookingAtPortalUuid(source);
        if (lookingAt != null) {
            builder.suggest(lookingAt);
        }
        // Suggest all portal UUIDs
        ServerLevel level = source.getLevel();
        PortalWorldData data = PortalWorldData.get(level);
        for (PortalEntity portal : data.getAllPortals()) {
            String uuid = portal.getUuid().toString();
            if (!uuid.equals(lookingAt)) {
                builder.suggest(uuid);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simpleportals")
                .requires(ctx -> ctx.hasPermission(2)) // op/cheats only
                .then(Commands.literal("kill")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .suggests(PORTAL_SUGGESTIONS)
                                .executes(PortalCommand::executeKill)
                        )
                )
        );
    }

    private static int executeKill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String uuidStr = StringArgumentType.getString(ctx, "uuid");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw INVALID_UUID.create();
        }

        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        PortalWorldData data = PortalWorldData.get(level);

        PortalEntity portal = data.getPortal(uuid);
        if (portal == null) {
            throw PORTAL_NOT_FOUND.create();
        }

        // Remove from world data
        data.removePortal(uuid);

        // Sync deletion to all players
        PortalSyncPayload payload = PortalSyncPayload.deletePortal(uuid);
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
        for (ServerPlayer player : level.players()) {
            player.connection.send(packet);
        }

        source.sendSuccess(() -> Component.literal("§cPortal deleted"), true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Raycast from the player's eyes through the look vector to find which portal
     * they are looking at. Returns the UUID string, or null.
     */
    private static String getLookingAtPortalUuid(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = 32.0;
        Vec3 to = from.add(look.scale(reach));

        ServerLevel level = source.getLevel();
        PortalWorldData data = PortalWorldData.get(level);

        double bestDist = Double.MAX_VALUE;
        String bestUuid = null;

        for (PortalEntity portal : data.getAllPortals()) {
            if (portal.rayIntersects(from, to)) {
                double dist = portal.getCentroid().distanceToSqr(from);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestUuid = portal.getUuid().toString();
                }
            }
        }
        return bestUuid;
    }
}
