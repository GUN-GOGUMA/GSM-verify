package dev.gungoguma.verify.storage;

import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlVerificationStore implements VerificationStore {
    private final File file;
    private FileConfiguration config;

    public YamlVerificationStore(File dataFolder) throws IOException {
        this.file = new File(dataFolder, "verified-users.yml");
        load();
    }

    @Override
    public synchronized boolean isVerified(UUID uuid) {
        return findByUuid(uuid).isPresent();
    }

    @Override
    public synchronized Optional<VerifiedUser> findByUuid(UUID uuid) {
        ConfigurationSection section = config.getConfigurationSection("users." + uuid);
        if (section == null) {
            return Optional.empty();
        }

        return Optional.of(readUser(uuid, section));
    }

    @Override
    public synchronized Optional<VerifiedUser> findByDiscordId(String discordId) {
        ConfigurationSection users = config.getConfigurationSection("users");
        if (users == null) {
            return Optional.empty();
        }

        for (String key : users.getKeys(false)) {
            ConfigurationSection section = users.getConfigurationSection(key);
            if (section == null || !discordId.equals(section.getString("discordId"))) {
                continue;
            }

            return Optional.of(readUser(UUID.fromString(key), section));
        }

        return Optional.empty();
    }

    @Override
    public synchronized void save(VerifiedUser user) throws IOException {
        String path = "users." + user.uuid();
        config.set(path + ".discordId", user.discordId());
        config.set(path + ".name", user.name());
        config.set(path + ".flag", user.flag());
        config.set(path + ".studentId", user.studentId());
        config.set(path + ".roleType", user.roleType().name());
        config.set(path + ".verifiedAt", user.verifiedAt().toString());
        persist();
    }

    @Override
    public synchronized boolean deleteByUuid(UUID uuid) throws IOException {
        if (!config.contains("users." + uuid)) {
            return false;
        }

        config.set("users." + uuid, null);
        persist();
        return true;
    }

    @Override
    public synchronized void deleteAll() throws IOException {
        config.set("users", null);
        persist();
    }

    private void load() throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create plugin data folder: " + parent);
        }

        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Could not create verification store: " + file);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    private void persist() throws IOException {
        config.save(file);
    }

    private VerifiedUser readUser(UUID uuid, ConfigurationSection section) {
        String roleTypeName = section.getString("roleType", RoleType.STUDENT.name());
        String verifiedAtText = section.getString("verifiedAt", Instant.EPOCH.toString());
        return new VerifiedUser(
            uuid,
            section.getString("discordId", ""),
            section.getString("name", ""),
            section.isInt("flag") ? section.getInt("flag") : null,
            section.getString("studentId", null),
            RoleType.valueOf(roleTypeName),
            Instant.parse(verifiedAtText)
        );
    }
}
