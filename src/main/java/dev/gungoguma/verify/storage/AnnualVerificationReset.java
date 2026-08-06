package dev.gungoguma.verify.storage;

import dev.gungoguma.verify.config.VerifyConfig;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class AnnualVerificationReset {
    private final File file;
    private final VerifyConfig config;
    private final VerificationStore store;

    public AnnualVerificationReset(File dataFolder, VerifyConfig config, VerificationStore store) {
        this.file = new File(dataFolder, "verification-meta.yml");
        this.config = config;
        this.store = store;
    }

    public boolean resetIfNeeded(LocalDate today) throws IOException {
        if (!isOnOrAfterResetDate(today)) {
            return false;
        }

        FileConfiguration metadata = YamlConfiguration.loadConfiguration(file);
        int lastResetYear = metadata.getInt("lastResetYear", 0);
        if (lastResetYear >= today.getYear()) {
            return false;
        }

        store.deleteAll();
        metadata.set("lastResetYear", today.getYear());
        metadata.save(file);
        return true;
    }

    private boolean isOnOrAfterResetDate(LocalDate today) {
        LocalDate resetDate = LocalDate.of(today.getYear(), config.resetMonth(), config.resetDay());
        return !today.isBefore(resetDate);
    }
}
