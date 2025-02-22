package net.IneiTsuki.autobiography;

import net.IneiTsuki.autobiography.NicknameSystem.Commands.NicknameCommand;
import net.IneiTsuki.autobiography.NicknameSystem.Storage.NicknameStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Autobiography implements ModInitializer {
    public static final String MOD_ID = "nickname";
    private static final Map<UUID, String> nicknames = new HashMap<>();

    @Override
    public void onInitialize() {
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                NicknameCommand.register(dispatcher)
        );

        // Load nicknames when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> NicknameStorage.loadNicknames());

        // Save nicknames when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> NicknameStorage.saveNicknames());

        // Apply nicknames when players join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Check if the player already has a nickname
            if (!nicknames.containsKey(player.getUuid())) {
                String username = player.getGameProfile().getName();
                // If no nickname exists, use their username and apply it as a default nickname
                setNickname(player, username);
            } else {
                // If a nickname exists, apply it
                String nickname = nicknames.get(player.getUuid());
                setNickname(player, nickname);
            }
        });

        // Modify chat messages to use nicknames
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!(sender instanceof ServerPlayerEntity player)) return true;

            // Get player's nickname or default username
            String nickname = nicknames.getOrDefault(player.getUuid(), player.getGameProfile().getName());

            // Retrieve the message content correctly
            String chatContent = message.getContent().getString();

            // Create custom formatted chat message
            Text chatMessage = Text.of("§7<§a" + nickname + "§7> " + chatContent);

            // Ensure the server and player manager are non-null before broadcasting
            if (player.getServer() != null && player.getServer().getPlayerManager() != null) {
                player.getServer().getPlayerManager().broadcast(chatMessage, false);
            }

            // Cancel default chat event so our formatted message is used
            return false;
        });
    }

    public static void setNickname(ServerPlayerEntity player, String nickname) {
        if (player == null || player.getServer() == null || player.getServer().getScoreboard() == null) {
            return; // Safely exit if no scoreboard exists
        }

        Scoreboard scoreboard = player.getServer().getScoreboard();
        String playerName = player.getGameProfile().getName();
        String teamName = "nick_" + playerName;

        // Create a new team if it doesn't exist
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName); // Create a new team if one does not exist
        }

        // Set the player's prefix (nickname) and suffix (hide original name)
        team.setPrefix(Text.of("<" + nickname + "> "));  // This sets the visible nickname in the team
        team.setSuffix(Text.of(""));  // Empty suffix hides the player's actual name

        // Set the team's display name to an empty string, ensuring it doesn't show the player’s name in the tablist
        team.setDisplayName(Text.of(""));

        // Remove player from any existing team before adding to the new one
        Team existingTeam = scoreboard.getPlayerTeam(playerName);
        if (existingTeam != null) {
            existingTeam.removePlayer(playerName);  // Remove from any previous team
        }

        // Add player to the new team
        team.addPlayer(playerName);  // Add player to the team manually

        // Now update nametag and tablist for the player
        updateNametagForEveryone(player);  // Update the nametag for all players
        sendPlayerListUpdateToAllPlayers(player);  // Update tablist for all players
    }

    private static void updateNametagForEveryone(ServerPlayerEntity player) {
        if (player.getServer() == null || player.getServer().getPlayerManager() == null) return;

        // Send PlayerListS2CPacket to update the player's entry in the player list (tab)
        PlayerListS2CPacket playerListPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player);
        for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerManager().getPlayerList()) {
            onlinePlayer.networkHandler.sendPacket(playerListPacket); // Send packet to update player list (tab)
        }
    }

    private static void updateScoreboardTeam(ServerPlayerEntity player, String nickname, TextColor color) {
        if (player.getServer() == null) return; // Prevent crashes if server isn't initialized

        Scoreboard scoreboard = player.getServer().getScoreboard();
        if (scoreboard == null) return; // Prevent crashes if scoreboard isn't initialized

        String teamName = "nick_" + player.getUuid().toString().substring(0, 8); // Unique ID per player
        String playerName = player.getGameProfile().getName(); // Get actual username

        // Find and remove player from their existing team
        Team previousTeam = null;
        for (Team team : scoreboard.getTeams()) {
            if (team.getPlayerList().contains(playerName)) {
                previousTeam = team;
                break;
            }
        }
        if (previousTeam != null) {
            previousTeam.getPlayerList().remove(playerName); // Remove player from the previous team
        }

        // Create or get the team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
        }

        // Set the nickname with color and brackets using Style
        Text coloredNickname = Text.literal("<" + nickname + ">").setStyle(Style.EMPTY.withColor(color));
        Text formattedName = coloredNickname.copy().append(Text.literal(" ")); // Concatenate nickname and player name

        team.setPrefix(formattedName); // Set the prefix in the team

        // Add player to their new team
        team.getPlayerList().add(playerName);

        // Send the PlayerListS2CPacket to update the player's display name in the tab list
        sendPlayerListUpdateToAllPlayers(player);
    }

    // **Send the PlayerListS2CPacket to update the player list (tab)**
    private static void sendPlayerListUpdateToAllPlayers(ServerPlayerEntity player) {
        if (player.getServer() == null || player.getServer().getPlayerManager() == null) return; // Null check for player manager

        // Send PlayerListS2CPacket to update the player's entry in the player list (tab)
        PlayerListS2CPacket playerListPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player);
        for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerManager().getPlayerList()) {
            onlinePlayer.networkHandler.sendPacket(playerListPacket); // Send packet to update player list (tab)
        }
    }

    public static Map<UUID, String> getNicknames() {
        return nicknames;
    }
}
