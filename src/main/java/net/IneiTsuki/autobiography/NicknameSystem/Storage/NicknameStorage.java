package net.IneiTsuki.autobiography.NicknameSystem.Storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.IneiTsuki.autobiography.Autobiography;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class NicknameStorage {
    private static final File FILE = new File("config/nicknames.json");
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<UUID, String>>() {}.getType();
    private static final Logger LOGGER = LogManager.getLogger(Autobiography.MOD_ID);

    public static void saveNicknames() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(Autobiography.getNicknames(), writer);
            LOGGER.info("Nicknames saved successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to save nicknames!", e);
        }
    }

    public static void loadNicknames() {
        if (!FILE.exists()) {
            LOGGER.warn("Nicknames file not found. Skipping load.");
            return;
        }

        try (FileReader reader = new FileReader(FILE)) {
            Map<UUID, String> data = GSON.fromJson(reader, TYPE);
            if (data != null) {
                Autobiography.getNicknames().putAll(data);
                LOGGER.info("Nicknames loaded successfully.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load nicknames!", e);
        }
    }
}
