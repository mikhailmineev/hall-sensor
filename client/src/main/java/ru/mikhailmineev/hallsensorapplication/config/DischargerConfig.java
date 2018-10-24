/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.hallsensorapplication.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ru.mikhailmineev.config.FrameConfig;

/**
 *
 * @author Михаил
 */
public class DischargerConfig {

    private String port = "COM3";
    private int speed = 115200;
    private String dir = System.getProperty("user.home");
    private FrameConfig frame;

    public DischargerConfig() {
        this.frame = new FrameConfig();
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int connectspeed) {
        this.speed = connectspeed;
    }

    public FrameConfig getFrame() {
        return frame;
    }

    public File getDir() {
        return new File(dir);
    }

    public void setDir(File dir) {
        this.dir = dir.getAbsolutePath();
    }
    
}
