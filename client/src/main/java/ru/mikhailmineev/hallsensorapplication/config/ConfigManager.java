/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.hallsensorapplication.config;

import ru.mikhailmineev.config.ConfigManagerBehavior;
import ru.mikhailmineev.config.FileJsonManager;
import java.io.File;
import ru.mikhailmineev.config.PreventStartupException;

/**
 *
 * @author Михаил
 */
public class ConfigManager extends FileJsonManager<DischargerConfig> {

    public ConfigManager(File homedir) throws PreventStartupException {
        super(DischargerConfig.class, ConfigManagerBehavior.COMMON, new File(homedir, "config.json"), null);
    }

    public DischargerConfig getConfig() {
        return config;
    }

    @Override
    protected void createEmptyConfig() {
        config = new DischargerConfig();
    }

}
