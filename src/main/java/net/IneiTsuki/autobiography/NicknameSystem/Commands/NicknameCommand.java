package net.IneiTsuki.autobiography.NicknameSystem.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.IneiTsuki.autobiography.Autobiography;
import net.IneiTsuki.autobiography.NicknameSystem.Config.NicknameConfig;
import net.IneiTsuki.autobiography.NicknameSystem.Storage.NicknameStorage;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

public class NicknameCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("nickname")
                .then(CommandManager.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return Command.SINGLE_SUCCESS;

                            String nickname = StringArgumentType.getString(context, "name");
                            UUID playerUUID = player.getUuid();

                            // Check if nicknames are enabled
                            if (!NicknameConfig.get().enableNicknames) {
                                context.getSource().sendFeedback(() -> Text.literal("§cNicknames are disabled on this server!"), false);
                                return Command.SINGLE_SUCCESS;
                            }

                            // Validate nickname
                            if (!isValidNickname(nickname)) {
                                context.getSource().sendFeedback(() -> Text.literal("§cInvalid nickname! Must be " +
                                        NicknameConfig.get().minLength + "-" + NicknameConfig.get().maxLength +
                                        " characters and contain only letters, numbers, and underscores."), false);
                                return Command.SINGLE_SUCCESS;
                            }

                            // Ensure nickname is unique
                            Map<UUID, String> nicknames = Autobiography.getNicknames();
                            if (nicknames.containsValue(nickname)) {
                                context.getSource().sendFeedback(() -> Text.literal("§cThat nickname is already taken!"), false);
                                return Command.SINGLE_SUCCESS;
                            }

                            // Apply nickname
                            nicknames.put(playerUUID, nickname);
                            NicknameStorage.saveNicknames();

                            // Update the player's scoreboard with the new nickname
                            Autobiography.setNickname(player, nickname);  // Calls the method in Autobiography.java

                            // Inform the player
                            context.getSource().sendFeedback(() -> Text.literal("§aYour nickname is now " + nickname), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                // Clear Own Nickname
                .then(CommandManager.literal("clear")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return Command.SINGLE_SUCCESS;

                            UUID playerUUID = player.getUuid();
                            Map<UUID, String> nicknames = Autobiography.getNicknames();

                            if (!nicknames.containsKey(playerUUID)) {
                                context.getSource().sendFeedback(() -> Text.literal("§cYou don’t have a nickname set!"), false);
                                return Command.SINGLE_SUCCESS;
                            }

                            // Remove nickname
                            nicknames.remove(playerUUID);
                            NicknameStorage.saveNicknames();

                            // Reset name tag on scoreboard
                            Autobiography.setNickname(player, player.getGameProfile().getName());  // Set back to original name

                            context.getSource().sendFeedback(() -> Text.literal("§aYour nickname has been reset!"), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }

    private static boolean isValidNickname(String nickname) {
        if (nickname.length() < NicknameConfig.get().minLength || nickname.length() > NicknameConfig.get().maxLength) {
            return false;
        }
        if (!nickname.matches("^[a-zA-Z0-9_]+$")) {
            return false;
        }
        for (String banned : NicknameConfig.get().bannedWords) {
            if (nickname.toLowerCase().contains(banned.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
