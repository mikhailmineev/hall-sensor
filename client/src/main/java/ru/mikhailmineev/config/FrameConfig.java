/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.config;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

/**
 * A config file component for frame or dialog location storage. You have to
 * sync config on exit, this class does not sync config
 *
 * @author Михаил
 */
public class FrameConfig implements WindowStateListener, ComponentListener, WindowListener {

    private int state;
    private Dimension size;
    private Point location;
    private boolean visible;

    /**
     * Does nothing, frame or dialog should be assigned via "loadOrDefaults"
     * method
     */
    public FrameConfig() {
    }

    /**
     *
     * @param frame - frame to record location and size of
     * @param relative - if true, remember whether window was opened before 
     * shutdown
     */
    public void loadOrDefaults(Frame frame, boolean relative) {
        if (size == null || location == null) {
            frame.setState(Frame.NORMAL);
            frame.pack();
            frame.setLocationRelativeTo(null);
            size = frame.getSize();
            if (frame.isShowing()) {
                location = frame.getLocationOnScreen();
            }
            state = frame.getExtendedState();
        } else {
            frame.setSize(size);
            frame.setLocation(location);
            frame.setExtendedState(state);
        }

        frame.addWindowStateListener(this);
        frame.addComponentListener(this);

        if (relative) {
            frame.addWindowListener(this);
            frame.setVisible(visible);
        }

    }

    /**
     *
     * @param dialog - dialog to record location and size of
     */
    public void loadOrDefaults(Dialog dialog) {
        if (size == null || location == null) {
            dialog.pack();
            dialog.setLocationRelativeTo(dialog.getParent());
            size = dialog.getSize();
            if (dialog.isShowing()) {
                location = dialog.getLocationOnScreen();
            }
        } else {
            dialog.setSize(size);
            dialog.setLocation(location);
        }

        dialog.addWindowStateListener(this);
        dialog.addComponentListener(this);
    }

    @Override
    public void windowStateChanged(WindowEvent e) {
        state = e.getNewState();
        //state = frame.getExtendedState();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        Class aClass = e.getComponent().getClass();
        if (Frame.class.isAssignableFrom(aClass)) {
            Frame frame = (Frame) e.getComponent();
            if (frame.getExtendedState() != Frame.MAXIMIZED_BOTH) {
                size = e.getComponent().getSize();
            }
        } else if (Dialog.class.isAssignableFrom(aClass)) {
            Dialog dialog = (Dialog) e.getComponent();
            size = e.getComponent().getSize();
        }

    }

    @Override
    public void componentMoved(ComponentEvent e) {
        Class aClass = e.getComponent().getClass();
        if (Frame.class.isAssignableFrom(aClass)) {
            Frame frame = (Frame) e.getComponent();
            if (frame.getExtendedState() != Frame.MAXIMIZED_BOTH && frame.isShowing()) {
                location = frame.getLocationOnScreen();
            }
        } else if (Dialog.class.isAssignableFrom(aClass)) {
            Dialog dialog = (Dialog) e.getComponent();
            if (dialog.isShowing()) {
                location = dialog.getLocationOnScreen();
            }
        }
    }

    @Override
    public void componentShown(ComponentEvent e) {
        visible = true;
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        //useless
    }

    @Override
    public void windowOpened(WindowEvent e) {
        //useless
    }

    @Override
    public void windowClosing(WindowEvent e) {
        visible = false;
    }

    @Override
    public void windowClosed(WindowEvent e) {
        //useless
    }

    @Override
    public void windowIconified(WindowEvent e) {
        //useless
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        //useless
    }

    @Override
    public void windowActivated(WindowEvent e) {
        //useless
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        //useless
    }
}
