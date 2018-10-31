/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.config;

/**
 * Rules for config initialization
 *
 * @author Михаил
 */
public enum ConfigManagerBehavior {

    /**
     * Default behavior. Try to load local file, else try to load default config
     * in application, else initialize config class.
     */
    COMMON,
    /**
     * Reset local config. Try to load default config in application, else
     * initialize config class.
     */
    FORCE_DEFAULT_CONFIG

}
