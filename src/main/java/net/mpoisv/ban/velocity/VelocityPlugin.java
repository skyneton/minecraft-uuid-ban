package net.mpoisv.ban.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.mpoisv.ban.parse.YamlConfiguration;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Plugin(id = "UUID-BAN", name = "UUID-BAN", version = "1.4-SNAPSHOT", authors = "skyneton")
public class VelocityPlugin {
    private final ProxyServer server;
    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

    private YamlConfiguration loadConfig(Path path, String fileName) {
        return loadConfig(path, fileName, fileName);
    }
    private YamlConfiguration loadConfig(Path path, String fileName, String outFileName) {
        var file = new File(path.toFile(), outFileName);
        file.getParentFile().mkdirs();
        if(!file.exists()) {
            try(var input = getClass().getResourceAsStream("/" + fileName)) {
                Files.copy(Objects.requireNonNull(input), file.toPath());
            }catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        try {
            return YamlConfiguration.loadConfiguration(file);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
