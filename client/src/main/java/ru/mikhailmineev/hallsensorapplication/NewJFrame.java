/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mikhailmineev.hallsensorapplication;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.Thread.sleep;
import java.net.URLDecoder;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import ru.mikhailmineev.config.ApplicationUtility;
import ru.mikhailmineev.config.PreventStartupException;
import ru.mikhailmineev.config.exceptions.ConfigSyncFailException;
import ru.mikhailmineev.hallsensorapplication.config.ConfigManager;
import ru.mikhailmineev.utils.PreventSeveralApplications;

/**
 *
 * @author Михаил
 */
public class NewJFrame extends javax.swing.JFrame {

    public static final String APPLICATION_NAME = "HallSensorApplication";
    private static NewJFrame instance;
    private static ZoneId ofOffset = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(3));
    public static DateTimeFormatter formatter
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss")
            .withLocale(Locale.UK)
            .withZone(ofOffset);

    public static NewJFrame getInstance() {
        return instance;
    }

    private static final int SIZE = 3;

    private static SerialPort serialPort;

    private final Writer writer;

    private final List<int[]> matrix = new ArrayList<>(1000);
    private boolean write = false;
    private boolean runtime = true;
    private Thread runtimer;
    private long mseconds;
    private static final Logger logger = LogManager.getLogger();
    private File workDir;
    private ConfigManager configmanager;
    private static final Object lock = new Object();

    static {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            logger.error("Uncaught exception!", e);
        });

    }

    /**
     * Creates new form NewJFrame
     */
    public NewJFrame(File workDir) {
        if (!PreventSeveralApplications.lockInstance(workDir)) {
            JOptionPane.showMessageDialog(this, "Приложение уже активно. Нельзя запустить дважды это приложение.", "", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        this.workDir = workDir;
        try {
            this.configmanager = new ConfigManager(workDir);
        } catch (PreventStartupException ex) {
        }
        initComponents();

        String port = configmanager.getConfig().getPort();
        int speed = configmanager.getConfig().getSpeed();
        connectPort.setText(port);
        connectSpeed.setText(Integer.toString(speed));

        recordButton.setEnabled(false);
        stopButton.setEnabled(false);
        zerosetbutton.setEnabled(false);
        zerotextx.setText("Ноль x=" + xzero);
        zerotexty.setText("Ноль y=" + yzero);
        zerotextz.setText("Ноль z=" + zzero);
        JLabel[] fields = {s1value, s2value, s3value};
        writer = new Writer(fields);
        dataset = new DefaultXYDataset();

        int avg = (int) avgsel.getValue();
        createDataset(avg);

        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        plotArea.add(chartPanel, BorderLayout.CENTER);

        configmanager.getConfig().getFrame().loadOrDefaults(this, false);
    }

    DefaultXYDataset dataset;

    private double[] x1;
    private double xzero = 0;
    private double[] y1;
    private double yzero = 0;
    private double[] z1;
    private double zzero = 0;
    //private double[] v1;
    //private double[] v2;
    private double[] tao;

    private void createDataset(int avg) {
        synchronized (lock) {
            while (dataset.getSeriesCount() > 0) {
                Comparable seriesKey = dataset.getSeriesKey(0);
                dataset.removeSeries(seriesKey);
            }
            int size = matrix.size() / avg;
            x1 = new double[size];
            y1 = new double[size];
            z1 = new double[size];
            //v1 = new double[matrix.size()];
            //v2 = new double[matrix.size()];
            tao = new double[size];
            double[][] vertical = {x1, y1, z1};
            double[] zeros = {xzero, yzero, zzero};

            for (int i = 0; i < size; i++) {
                double time = mseconds / 1000.0;
                int ticks = size;
                tao[i] = i * time / (double) ticks;
                //avg count
                int[] sum = new int[SIZE];
                for (int j = i * avg; j < (i + 1) * avg; j++) {
                    int[] row = matrix.get(j);
                    for (int k = 0; k < SIZE; k++) {
                        sum[k] += row[k];
                    }
                }

                for (int j = 0; j < SIZE; j++) {
                    vertical[j][i] = sum[j] * 5 / (1024.0 * avg);
                    //to zero
                    vertical[j][i] = vertical[j][i] - zeros[j];
                }
                //v1[i] = Math.sqrt(x1[i] * x1[i] + y1[i] * y1[i] + z1[i] * z1[i]);
                //v2[i] = Math.sqrt(x2[i] * x2[i] + y2[i] * y2[i] + z2[i] * z2[i]);
            }

            if (xline.isSelected()) {
                double[][] data = {tao, x1};
                dataset.addSeries("x", data);
            }
            if (yline.isSelected()) {
                double[][] data = {tao, y1};
                dataset.addSeries("y", data);
            }
            if (zline.isSelected()) {
                double[][] data = {tao, z1};
                dataset.addSeries("z", data);
            }
        }
    }

    private JFreeChart createChart(XYDataset dataset) {

        JFreeChart chart = ChartFactory.createXYLineChart("", "time", "voltage", dataset);
        XYPlot plot = (XYPlot) chart.getPlot();
        //plot.setBackgroundAlpha(0);
        plot.setBackgroundPaint(Color.white);
        plot.getDomainAxis().setAxisLinePaint(Color.black);
        plot.setDomainGridlinePaint(Color.black);
        plot.setRangeGridlinePaint(Color.black);
        plot.getRenderer().setSeriesPaint(0, Color.red);
        plot.getRenderer().setSeriesPaint(1, Color.blue);
        plot.getRenderer().setSeriesPaint(2, Color.green);
        plot.getRenderer().setSeriesPaint(3, Color.ORANGE);
        plot.getRenderer().setSeriesPaint(4, Color.PINK);
        plot.getRenderer().setSeriesPaint(5, Color.MAGENTA);
        //plot.setForegroundAlpha(0.5f);
        return chart;

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        jPanel1 = new JPanel();
        jLabel2 = new JLabel();
        s1value = new JLabel();
        jLabel3 = new JLabel();
        s2value = new JLabel();
        jLabel4 = new JLabel();
        s3value = new JLabel();
        jPanel2 = new JPanel();
        connectPort = new JTextField();
        jLabel1 = new JLabel();
        jLabel5 = new JLabel();
        connectSpeed = new JTextField();
        connectButton = new JButton();
        jPanel3 = new JPanel();
        recordButton = new JButton();
        stopButton = new JButton();
        jLabel8 = new JLabel();
        numrows = new JLabel();
        jLabel9 = new JLabel();
        recordtime = new JLabel();
        zerosetbutton = new JButton();
        zerotextx = new JLabel();
        zerotexty = new JLabel();
        zerotextz = new JLabel();
        plotPanel = new JPanel();
        plotArea = new JPanel();
        jPanel5 = new JPanel();
        jLabel10 = new JLabel();
        jButton1 = new JButton();
        xline = new JCheckBox();
        yline = new JCheckBox();
        zline = new JCheckBox();
        avgsel = new JSpinner();
        jMenuBar1 = new JMenuBar();
        jMenu1 = new JMenu();
        openfilebutton = new JMenuItem();
        savefilebutton = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Hall sensor");
        setMinimumSize(new Dimension(600, 300));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[] {0, 5, 0};
        layout.rowHeights = new int[] {0, 5, 0, 5, 0, 5, 0};
        getContentPane().setLayout(layout);

        jPanel1.setBorder(BorderFactory.createTitledBorder("Текущие значения"));
        GridBagLayout jPanel1Layout = new GridBagLayout();
        jPanel1Layout.columnWidths = new int[] {0, 5, 0};
        jPanel1Layout.rowHeights = new int[] {0, 5, 0, 5, 0};
        jPanel1.setLayout(jPanel1Layout);

        jLabel2.setText("Датчик x:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        jPanel1.add(jLabel2, gridBagConstraints);

        s1value.setText("-");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(s1value, gridBagConstraints);

        jLabel3.setText("Датчик y:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        jPanel1.add(jLabel3, gridBagConstraints);

        s2value.setText("-");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        jPanel1.add(s2value, gridBagConstraints);

        jLabel4.setText("Датчик z:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(jLabel4, gridBagConstraints);

        s3value.setText("-");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        jPanel1.add(s3value, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        jPanel2.setBorder(BorderFactory.createTitledBorder("Подключение"));
        GridBagLayout jPanel2Layout = new GridBagLayout();
        jPanel2Layout.columnWidths = new int[] {0, 5, 0};
        jPanel2Layout.rowHeights = new int[] {0, 5, 0, 5, 0};
        jPanel2.setLayout(jPanel2Layout);

        connectPort.setText("COM4");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 69;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(connectPort, gridBagConstraints);

        jLabel1.setText("COM порт:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        jPanel2.add(jLabel1, gridBagConstraints);

        jLabel5.setText("Скорость:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        jPanel2.add(jLabel5, gridBagConstraints);

        connectSpeed.setText("115200");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel2.add(connectSpeed, gridBagConstraints);

        connectButton.setText("Подключиться");
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel2.add(connectButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel3.setBorder(BorderFactory.createTitledBorder("Запись"));
        GridBagLayout jPanel3Layout = new GridBagLayout();
        jPanel3Layout.columnWidths = new int[] {0, 5, 0};
        jPanel3Layout.rowHeights = new int[] {0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0};
        jPanel3.setLayout(jPanel3Layout);

        recordButton.setText("Начать запись");
        recordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                recordButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(recordButton, gridBagConstraints);

        stopButton.setText("Остановить запись");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel3.add(stopButton, gridBagConstraints);

        jLabel8.setText("Число строк:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        jPanel3.add(jLabel8, gridBagConstraints);

        numrows.setText("-");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(numrows, gridBagConstraints);

        jLabel9.setText("Время записи:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        jPanel3.add(jLabel9, gridBagConstraints);

        recordtime.setText("-");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        jPanel3.add(recordtime, gridBagConstraints);

        zerosetbutton.setText("Выставить на ноль");
        zerosetbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                zerosetbuttonActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel3.add(zerosetbutton, gridBagConstraints);

        zerotextx.setText("jLabel11");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel3.add(zerotextx, gridBagConstraints);

        zerotexty.setText("jLabel11");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel3.add(zerotexty, gridBagConstraints);

        zerotextz.setText("jLabel12");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel3.add(zerotextz, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(jPanel3, gridBagConstraints);

        plotPanel.setBorder(BorderFactory.createTitledBorder("График"));
        plotPanel.setMinimumSize(new Dimension(200, 33));
        plotPanel.setLayout(new GridBagLayout());

        plotArea.setLayout(new BorderLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        plotPanel.add(plotArea, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(plotPanel, gridBagConstraints);

        jPanel5.setBorder(BorderFactory.createTitledBorder("График"));
        GridBagLayout jPanel5Layout = new GridBagLayout();
        jPanel5Layout.columnWidths = new int[] {0, 5, 0, 5, 0};
        jPanel5Layout.rowHeights = new int[] {0, 5, 0, 5, 0, 5, 0};
        jPanel5.setLayout(jPanel5Layout);

        jLabel10.setText("Усреднять каждые:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel5.add(jLabel10, gridBagConstraints);

        jButton1.setText("ОК");
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        jPanel5.add(jButton1, gridBagConstraints);

        xline.setSelected(true);
        xline.setText("Ось x");
        xline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                xlineActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel5.add(xline, gridBagConstraints);

        yline.setSelected(true);
        yline.setText("Ось y");
        yline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ylineActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel5.add(yline, gridBagConstraints);

        zline.setSelected(true);
        zline.setText("Ось z");
        zline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                zlineActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel5.add(zline, gridBagConstraints);

        avgsel.setModel(new SpinnerNumberModel(1, 1, null, 1));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel5.add(avgsel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        getContentPane().add(jPanel5, gridBagConstraints);

        jMenu1.setText("Файл");

        openfilebutton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
        openfilebutton.setText("Открыть");
        openfilebutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                openfilebuttonActionPerformed(evt);
            }
        });
        jMenu1.add(openfilebutton);

        savefilebutton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        savefilebutton.setText("Сохранить");
        savefilebutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                savefilebuttonActionPerformed(evt);
            }
        });
        jMenu1.add(savefilebutton);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void connectButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        serialPort = new SerialPort(connectPort.getText());
        try {
            //Открываем порт
            serialPort.openPort();
            //Выставляем параметрыconnectSpeed
            //1:SerialPort.BAUDRATE_9600
            int speed = Integer.parseInt(connectSpeed.getText());
            serialPort.setParams(speed,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            //Включаем аппаратное управление потоком
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN
                    | SerialPort.FLOWCONTROL_RTSCTS_OUT);
            //Устанавливаем ивент лисенер и маску
            serialPort.addEventListener(new NewJFrame.PortReader(), SerialPort.MASK_RXCHAR);
            //Отправляем запрос устройству
            //serialPort.writeString("Get data");
        } catch (SerialPortException ex) {
            String type = ex.getExceptionType();
            if (type.equals("Port busy")) {
                JOptionPane.showMessageDialog(this, "Порт " + connectPort.getText() + " занят!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            } else if (type.equals("Port not found")) {
                JOptionPane.showMessageDialog(this, "Порт " + connectPort.getText() + " не найден!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            } else {
                logger.error("Failed to tonnect to device", ex);
            }
        }

    }//GEN-LAST:event_connectButtonActionPerformed

    private void recordButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_recordButtonActionPerformed
        recordButton.setEnabled(false);
        stopButton.setEnabled(true);
        zerosetbutton.setEnabled(false);
        record();
    }//GEN-LAST:event_recordButtonActionPerformed

    private void record() {
        matrix.clear();
        write = true;
        runtime = true;
        runtimer = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (runtime) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
                mseconds = (System.currentTimeMillis() - start);
                recordtime.setText(String.valueOf(mseconds));
            }
        });
        runtimer.start();
    }

    private void stopButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        int avg = (int) avgsel.getValue();
        stop(avg);
        stopButton.setEnabled(false);
        recordButton.setEnabled(true);
        zerosetbutton.setEnabled(true);
    }//GEN-LAST:event_stopButtonActionPerformed

    private void stop(int avg) {
        write = false;
        runtime = false;
        runtimer.interrupt();
        numrows.setText(Integer.toString(matrix.size()));
        createDataset(avg);
    }

    private void xlineActionPerformed(ActionEvent evt) {//GEN-FIRST:event_xlineActionPerformed
        if (xline.isSelected()) {
            double[][] data = {tao, x1};
            dataset.addSeries("x", data);
        } else {
            dataset.removeSeries("x");
        }
    }//GEN-LAST:event_xlineActionPerformed

    private void ylineActionPerformed(ActionEvent evt) {//GEN-FIRST:event_ylineActionPerformed
        if (yline.isSelected()) {
            double[][] data = {tao, y1};
            dataset.addSeries("y", data);
        } else {
            dataset.removeSeries("y");
        }
    }//GEN-LAST:event_ylineActionPerformed

    private void zlineActionPerformed(ActionEvent evt) {//GEN-FIRST:event_zlineActionPerformed
        if (zline.isSelected()) {
            double[][] data = {tao, z1};
            dataset.addSeries("z", data);
        } else {
            dataset.removeSeries("z");
        }
    }//GEN-LAST:event_zlineActionPerformed

    private void jButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        int avg = (int) avgsel.getValue();
        createDataset(avg);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void zerosetbuttonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_zerosetbuttonActionPerformed
        recordButton.setEnabled(false);
        stopButton.setEnabled(false);
        zerosetbutton.setEnabled(false);
        xzero = 0;
        yzero = 0;
        zzero = 0;
        record();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            //this average forces co calculate for all numbers average
            int average = matrix.size() == 0 ? 1 : matrix.size();
            stop(average);
            xzero = x1[0];
            yzero = y1[0];
            zzero = z1[0];
            zerotextx.setText("Ноль x=" + xzero);
            zerotexty.setText("Ноль y=" + yzero);
            zerotextz.setText("Ноль z=" + zzero);
            int avg = (int) avgsel.getValue();
            createDataset(avg);
            stopButton.setEnabled(false);
            recordButton.setEnabled(true);
            zerosetbutton.setEnabled(true);
        }, "Zeroset second wait").start();

    }//GEN-LAST:event_zerosetbuttonActionPerformed

    private void openfilebuttonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_openfilebuttonActionPerformed
        try {
            JFileChooser f = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("CSV (разделители - запятые)", "csv");
            f.setFileFilter(filter);
            f.setCurrentDirectory(configmanager.getConfig().getDir());
            int status = f.showOpenDialog(this);
            configmanager.getConfig().setDir(f.getCurrentDirectory());
            if (JFileChooser.APPROVE_OPTION != status) {
                logger.info("Cancelled file opening");
                return;
            }

            File selectedFile = f.getSelectedFile();
            List readFileToString = FileUtils.readLines(selectedFile);
            String row4 = (String) readFileToString.get(4);
            String[] zeros = row4.split(";");//zero
            String row5 = (String) readFileToString.get(5);
            String average = row5.split(";")[1];//average
            int avg = Integer.parseInt(average);
            String row6 = (String) readFileToString.get(6);
            long measuretime = Long.parseLong(row6.split(";")[1]);//measure time
            String row7 = (String) readFileToString.get(7);
            int lines = Integer.parseInt(row7.split(";")[1]);//measure time

            //fill data
            matrix.clear();
            for (int i = 9; i < 9 + lines; i++) {
                String cols = (String) readFileToString.get(i);
                String[] cols2 = cols.split(";");
                int[] cols3 = new int[SIZE];
                for (int j = 0; j < SIZE; j++) {
                    cols3[j] = Integer.parseInt(cols2[j]);
                }
                matrix.add(cols3);
            }
            xzero = Double.parseDouble(zeros[1]);
            yzero = Double.parseDouble(zeros[2]);
            zzero = Double.parseDouble(zeros[3]);
            mseconds = measuretime;
            avgsel.setValue(avg);
            createDataset(avg);
            configmanager.syncConfig();
        } catch (IOException ex) {
            logger.error("Error occured during file open", ex);
        } catch (ConfigSyncFailException ex) {
            logger.error("Failed to sync config", ex);
        }

    }//GEN-LAST:event_openfilebuttonActionPerformed

    private void savefilebuttonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_savefilebuttonActionPerformed
        try {
            JFileChooser f = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("CSV (разделители - запятые)", "csv");
            f.setFileFilter(filter);
            f.setCurrentDirectory(configmanager.getConfig().getDir());
            int status = f.showSaveDialog(this);
            configmanager.getConfig().setDir(f.getCurrentDirectory());
            if (JFileChooser.APPROVE_OPTION != status) {
                logger.info("Cancelled file saving");
                return;
            }
            File selectedFile = f.getSelectedFile();
            String ext = FilenameUtils.getExtension(selectedFile.getAbsolutePath());
            if (!"csv".equals(ext)) {
                selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + ".csv");
            }

            int avg = (int) avgsel.getValue();

            StringBuilder data = new StringBuilder();
            data.append("\n");
            data.append("\n");
            data.append("\n");
            data.append(";x;y;z\n");
            data.append("zero;").append(xzero).append(";").append(yzero).append(";").append(zzero).append("\n");
            data.append("average;").append(avg).append("\n");
            data.append("measure time;").append(mseconds).append("\n");
            data.append("rows;").append(matrix.size()).append("\n");
            data.append("x;y;z\n");
            for (int i = 0; i < x1.length; i++) {
                int[] row = matrix.get(i);
                data.append(row[0]);
                data.append(";");
                data.append(row[1]);
                data.append(";");
                data.append(row[2]);
                data.append("\n");
            }
            FileUtils.writeStringToFile(selectedFile, data.toString());
        } catch (IOException ex) {
            logger.error("Error occured during file save", ex);
        }
    }//GEN-LAST:event_savefilebuttonActionPerformed

    private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            configmanager.syncConfig();
        } catch (ConfigSyncFailException ex) {
            logger.error("Failed to sync config", ex);
        }
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    private boolean called = false;

    private void msgReceived() {
        if (!called) {
            try {
                String port = connectPort.getText();
                int speed = Integer.parseInt(connectSpeed.getText());

                configmanager.getConfig().setPort(port);
                configmanager.getConfig().setSpeed(speed);
                configmanager.syncConfig();

            } catch (ConfigSyncFailException ex) {
                logger.error("Failed to sync config", ex);
            }

            called = true;
            recordButton.setEnabled(true);
            zerosetbutton.setEnabled(true);

        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        ApplicationUtility.forceUTF8(logger);
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());

        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        File workingDirectory = ApplicationUtility.getWorkingDirectory(APPLICATION_NAME);
        instance = new NewJFrame(workingDirectory);
        instance.setVisible(true);

    }

    private static class PortReader implements SerialPortEventListener {

        private String str;
        private int length = "000:000:000:".length();
        private int showdelay = 50;
        private int delaycount = 0;

        public PortReader() {
        }

        public void serialEvent(SerialPortEvent event) {

            if (event.isRXCHAR() && event.getEventValue() > 0) {
                instance.msgReceived();
                try {
                    //Получаем ответ от устройства, обрабатываем данные и т.д.
                    str += serialPort.readString();
                    String[] rows = str.split("\n");
                    str = "";

                    for (String row : rows) {
                        if (row.length() == length) {
                            //System.out.println(row);
                            String[] t = row.split(":");
                            int[] i = new int[SIZE];
                            for (int j = 0; j < SIZE; j++) {
                                try {
                                    i[j] = Integer.parseInt(t[j], 16);
                                } catch (NumberFormatException ex) {
                                }
                            }
                            if (showdelay == delaycount) {
                                instance.writer.setWrite(i);
                                delaycount = 0;
                            }
                            delaycount++;
                            if (instance.write) {
                                instance.matrix.add(i);
                            }

                        }
                    }

                    if (rows[rows.length - 1].length() < length) {
                        str += rows[rows.length - 1];
                    }

                } catch (SerialPortException ex) {
                    try {
                        serialPort.closePort();
                    } catch (SerialPortException ex1) {
                        logger.error("Failed close connetion after fail", ex);
                    } finally {
                        logger.error("Error with connection while parsing data", ex);
                    }
                } catch (Exception ex) {
                    try {
                        serialPort.closePort();
                    } catch (SerialPortException ex1) {
                        logger.error("Failed close connetion after fail", ex);
                    } finally {
                        logger.error("Unknown error while parsing data", ex);
                    }
                }
            }
        }
    }

    private static class Writer {

        private final List<int[]> array = new ArrayList<>(1000);
        private Object lock = new Object();

        public Writer(JLabel[] fields) {
            new Thread(() -> {

                while (true) {
                    long[] sum = new long[SIZE];
                    int count;

                    synchronized (lock) {
                        count = array.size();
                        for (int[] row : array) {
                            for (int i = 0; i < SIZE; i++) {
                                sum[i] += row[i];
                            }
                        }
                        array.clear();
                    }

                    for (int i = 0; i < SIZE; i++) {
                        fields[i].setText(Long.toString((long) (sum[i] / (double) count)));
                    }
                    try {
                        sleep(700);
                    } catch (InterruptedException ex) {
                    }
                }
            },
                    "writer").start();
        }

        public void setWrite(int[] write) {
            synchronized (lock) {
                array.add(write);
            }
        }

    }

    public static String getApplicationPath() {
        try {
            String path = NewJFrame.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            return decodedPath;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JSpinner avgsel;
    private JButton connectButton;
    private JTextField connectPort;
    private JTextField connectSpeed;
    private JButton jButton1;
    private JLabel jLabel1;
    private JLabel jLabel10;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel8;
    private JLabel jLabel9;
    private JMenu jMenu1;
    private JMenuBar jMenuBar1;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel5;
    private JLabel numrows;
    private JMenuItem openfilebutton;
    private JPanel plotArea;
    private JPanel plotPanel;
    private JButton recordButton;
    private JLabel recordtime;
    private JLabel s1value;
    private JLabel s2value;
    private JLabel s3value;
    private JMenuItem savefilebutton;
    private JButton stopButton;
    private JCheckBox xline;
    private JCheckBox yline;
    private JButton zerosetbutton;
    private JLabel zerotextx;
    private JLabel zerotexty;
    private JLabel zerotextz;
    private JCheckBox zline;
    // End of variables declaration//GEN-END:variables
}
