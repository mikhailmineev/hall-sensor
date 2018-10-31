/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.config;

import ru.mikhailmineev.config.exceptions.ConfigSyncFailException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * File config storage class, stores config in JSON format. Suppotrs different
 * ways to initialize default config, file or class
 *
 * @author Михаил
 * @param <T> - Type of config core class
 */
public abstract class FileJsonManager<T> {

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.serializeNulls();
        GSON = builder.create();
    }
    private static final Gson GSON;
    private final File configfile;

    protected static final Logger logger = LogManager.getLogger();

    protected T config = null;

    /**
     *
     * @param type - Type of config core class
     * @param behavior - see ConfigManagerBehavior
     * @param configfile - file location, where to store config file
     * @param recoverConfigClasspath - path inside application, where default
     * config is stored. Should be in proper JSON format. If null, default
     * config should be created in createEmptyConfig()
     * @throws PreventStartupException
     */
    public FileJsonManager(Class<T> type, ConfigManagerBehavior behavior, File configfile, String recoverConfigClasspath) throws PreventStartupException {

        this.configfile = configfile;

        try {
            if (behavior == ConfigManagerBehavior.FORCE_DEFAULT_CONFIG) {
                throw new Exception("Forced to load default config");
            }

            //config not created
            if (!configfile.exists()) {
                configfile.createNewFile();
                logger.info("No config file, creating new");
                createDefaultConfig(type, recoverConfigClasspath);
                return;
            }

            //first stage, try to find log file
            String jsontext = FileUtils.readFileToString(configfile);

            //config is empty;
            if ("".equals(jsontext.trim())) {
                logger.warn("Config file is empty, recreating");
                createDefaultConfig(type, recoverConfigClasspath);
            }
            config = (T) GSON.fromJson(jsontext, type);
        } catch (Exception ex0) {
            logger.error("Failed to load config file, trying to use default", ex0);
            createDefaultConfig(type, recoverConfigClasspath);
        }
    }

    /**
     * Create default config there assigning it to "config" variable
     */
    protected abstract void createEmptyConfig();

    private void createDefaultConfig(Class<T> type, String recoverConfigClasspath) throws PreventStartupException {
        if (recoverConfigClasspath != null) {
            logger.info("Creating config from sample file");
            String jsontext = getTextFromClasspath(recoverConfigClasspath);
            config = (T) GSON.fromJson(jsontext, type);
        } else {
            logger.info("Creating empty config");
            createEmptyConfig();
        }
    }

    ;

    private String getTextFromClasspath(String recoverConfigClasspath) throws PreventStartupException {
        InputStream inputStreamReader = null;
        String jsontext = null;
        //failed to load file trying to get default from jar

        try {
            inputStreamReader = FileJsonManager.class
                    .getResource(recoverConfigClasspath).openStream();
            jsontext = IOUtils.toString(inputStreamReader);
        } catch (IOException ex1) {
            logger.fatal("Failed to load config file", ex1);
            throw new PreventStartupException();
        } finally {
            //just close stream, nothing important
            try {
                inputStreamReader.close();
            } catch (IOException ex1) {
                logger.error("Failed to close config stream", ex1);
            }

            syncConfigQuietly(jsontext);

        }
        return jsontext;
    }

    private void syncConfigQuietly(String jsontext) {
        //try to write default config to file
        try {
            FileUtils.writeStringToFile(configfile, jsontext);
        } catch (IOException ex) {
            logger.error("Failed to write default config to file", ex);
        }
    }

    /**
     * Sync config to local file
     *
     * @throws ConfigSyncFailException
     */
    public void syncConfig() throws ConfigSyncFailException {
        try {
            FileUtils.writeStringToFile(configfile, getConfigText());
        } catch (IOException ex) {
            logger.error("Failed to sync config", ex);
            throw new ConfigSyncFailException();
        }
    }

    /**
     * Get JSON string of your config
     *
     * @return
     */
    public String getConfigText() {
        return GSON.toJson(config);
    }

    /**
     * Get absolute path of your local config file
     *
     * @return
     */
    public String getConfigFilePath() {
        return configfile.getAbsolutePath();
    }

}
