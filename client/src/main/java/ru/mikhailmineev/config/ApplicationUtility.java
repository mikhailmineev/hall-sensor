/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Михаил
 */
public class ApplicationUtility {

    public static void forceUTF8(Logger logger) {
        System.setProperty("file.encoding", "UTF-8");
        try {
            Field charset;
            charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (NoSuchFieldException ex) {
            logger.error("Failed to set UTF-8 encoding", ex);
        } catch (SecurityException ex) {
            logger.error("Failed to set UTF-8 encoding", ex);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to set UTF-8 encoding", ex);
        } catch (IllegalAccessException ex) {
            logger.error("Failed to set UTF-8 encoding", ex);
        }

    }

    public static OS getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.WINDOWS;
        }
        if (osName.contains("mac")) {
            return OS.MACOS;
        }
        if (osName.contains("linux")) {
            return OS.LINUX;
        }
        if (osName.contains("unix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    public static File getWorkingDirectory(String applicationname) {
        String userHome = System.getProperty("user.home", ".");
        File workingDirectory;
        if (OS.LINUX == getPlatform() || OS.SOLARIS == getPlatform()) {
            workingDirectory = new File(userHome, "." + applicationname + "/");
        } else if (OS.WINDOWS == getPlatform()) {
            String applicationData = System.getenv("APPDATA");
            String folder = applicationData != null ? applicationData : userHome;
            workingDirectory = new File(folder, applicationname + "/");
        } else if (OS.MACOS == getPlatform()) {
            workingDirectory = new File(userHome, "Library/Application Support/" + applicationname);
        } else {
            workingDirectory = new File(userHome, "." + applicationname + "/");
        }

        workingDirectory.mkdirs();
        return workingDirectory;
    }

    public static enum OS {
        WINDOWS, MACOS, SOLARIS, LINUX, UNKNOWN;
    }

    public static String getApplicationPath(Class mainclass) {
        try {
            String path = mainclass.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            return decodedPath;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static String fileSha1(File file) throws FileNotFoundException,
            IOException, NoSuchAlgorithmException {

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }

            return new HexBinaryAdapter().marshal(sha1.digest());
        }
    }

}
