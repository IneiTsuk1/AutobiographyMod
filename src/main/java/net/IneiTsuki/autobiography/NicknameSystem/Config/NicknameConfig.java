package net.IneiTsuki.autobiography.NicknameSystem.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NicknameConfig {
    private static final File CONFIG_FILE = new File("config/nickname_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger("nickname");
    private static NicknameConfig INSTANCE;

    public boolean enableNicknames = true;
    public int minLength = 3;
    public int maxLength = 16;
    public List<String> bannedWords = Arrays.asList("admin", "owner", "mod", "staff");

    public static NicknameConfig get() {
        if (INSTANCE == null) {
            loadConfig();
        }
        return INSTANCE;
    }

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            saveConfig(); // Save default config if file doesn't exist
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            INSTANCE = GSON.fromJson(reader, NicknameConfig.class);
            LOGGER.info("Nickname config loaded.");
        } catch (IOException e) {
            LOGGER.error("Failed to load nickname config!", e);
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(new NicknameConfig(), writer);
            LOGGER.info("Default nickname config saved.");
        } catch (IOException e) {
            LOGGER.error("Failed to save nickname config!", e);
        }
    }
}
