package ru.mikhailmineev.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Михаил
 */
public class PreventSeveralApplications {

    private static final Logger logger = LogManager.getLogger();

    public static boolean lockInstance(final File workingdirectory) {
        final File file = new File(workingdirectory, "lock");
        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        } catch (Exception e) {
                            logger.error("Unable to remove lock file: " + file.getAbsolutePath(), e);
                        }
                    }
                });
                return true;
            }
        } catch (Exception e) {
            logger.error("Unable to create and/or lock file: " + file.getAbsolutePath(), e);
        }
        return false;
    }

}
