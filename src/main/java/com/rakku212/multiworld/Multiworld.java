package com.rakku212.multiworld;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import com.rakku212.multiworld.api.MultiworldApi;
import com.rakku212.multiworld.command.MultiworldCommandExecutor;
import com.rakku212.multiworld.internal.persistence.DatabaseManager;
import com.rakku212.multiworld.internal.persistence.ManagedWorldRepository;
import com.rakku212.multiworld.internal.service.MultiworldApiImpl;
import com.rakku212.multiworld.internal.service.MultiworldService;

public final class Multiworld extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MultiworldApiImpl api;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);
        ManagedWorldRepository repository = new ManagedWorldRepository(databaseManager.getDataSource());
        MultiworldService service = new MultiworldService(this, repository);

        service.loadStoredWorlds();

        api = new MultiworldApiImpl(this, service);
        getServer().getServicesManager().register(MultiworldApi.class, api, this, ServicePriority.Normal);

        var cmd = getCommand("multiworld");
        if (cmd != null) {
            var executor = new MultiworldCommandExecutor(this, service);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().severe("plugin.yml にコマンド multiworld が定義されていません。");
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (databaseManager != null) {
            databaseManager.close();
            databaseManager = null;
        }
    }
}
