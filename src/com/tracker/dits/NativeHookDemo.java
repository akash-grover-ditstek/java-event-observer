package com.tracker.dits;


import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeHookDemo extends JFrame implements ActionListener,
        NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener, WindowListener {
    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1541183202160543102L;

    private static long keyPress=0;
    private static long mouseClick=0;

    private static long lastMillisecond=0;
    /**
     * Menu Items
     */
    private final JMenuItem menuItemQuit, menuItemClear;

    /**
     * The text area to display event info.
     */
    private final JTextArea txtEventInfo;

    /**
     * Logging
     */
    private static final Logger log = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    /**
     * Instantiates a new native hook demo.
     */
    public NativeHookDemo() {
        lastMillisecond = System.currentTimeMillis()/1000;
        if(!SystemTray.isSupported()){
            System.out.println("System tray is not supported !!! ");
            System.exit(0);
        }

        // Set up the main window.
        setTitle("Mouse Keyboard Listener");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(600, 300);
        addWindowListener(this);

        JMenuBar menuBar = new JMenuBar();

        // Create the file menu.
        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menuFile);

        menuItemQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
        menuItemQuit.addActionListener(this);
        menuItemQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        menuItemQuit.getAccessibleContext().setAccessibleDescription("Exit the program");
        menuFile.add(menuItemQuit);

        // Create the view.
        JMenu menuView = new JMenu("View");
        menuView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(menuView);

        menuItemClear = new JMenuItem("Clear", KeyEvent.VK_C);
        menuItemClear.addActionListener(this);
        menuItemClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
        menuItemClear.getAccessibleContext().setAccessibleDescription("Clear the screen");
        menuView.add(menuItemClear);

        menuView.addSeparator();
        setJMenuBar(menuBar);

        // Create feedback area.
        txtEventInfo = new JTextArea();
        txtEventInfo.setEditable(false);
        txtEventInfo.setBackground(new Color(0xFF, 0xFF, 0xFF));
        txtEventInfo.setForeground(new Color(0x00, 0x00, 0x00));
        txtEventInfo.setText("");

        JScrollPane scrollPane = new JScrollPane(txtEventInfo);
        scrollPane.setPreferredSize(new Dimension(375, 125));
        add(scrollPane, BorderLayout.CENTER);

        // Disable parent logger and set the desired level.
        log.setUseParentHandlers(false);
        log.setLevel(Level.INFO);

        // Setup a generic ConsoleHandler
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        log.addHandler(handler);


        SystemTray systemTray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage("src/resource/logo.png");
        PopupMenu trayPopupMenu = new PopupMenu();
        MenuItem action = new MenuItem("Show");
        action.addActionListener(e -> restore());
        trayPopupMenu.add(action);
        MenuItem close = new MenuItem("Close");
        close.addActionListener(e -> {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex) {
                throw new RuntimeException(ex);
            }
            System.runFinalization();
            System.exit(0);

        });
        trayPopupMenu.add(close);

        TrayIcon trayIcon = new TrayIcon(image, "Mouse Keyboard Listener", trayPopupMenu);
        //adjust to default size as per system recommendation
        trayIcon.setImageAutoSize(true);

        try{
            systemTray.add(trayIcon);
        }catch(AWTException awtException){
            awtException.printStackTrace();
        }

        int interval = 2000;
        Timer timer = new Timer(interval, e -> {
            long currentMilliSeconds = System.currentTimeMillis()/1000;
            if((currentMilliSeconds-lastMillisecond)>20)
                snapShot();
        });

        timer.start();

        GlobalScreen.setEventDispatcher(new SwingDispatchService());

        setVisible(true);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuItemQuit) {
            this.dispose();
        } else if (e.getSource() == menuItemClear) {
            txtEventInfo.setText("");
        }
    }

    private void registerHook(){
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
        } catch (NativeHookException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @see NativeKeyListener#nativeKeyTyped(NativeKeyEvent)
     */
    public void nativeKeyTyped(NativeKeyEvent e) {
        keyPress++;
        if(keyPress%10==0){
            snapShot();
        }
        appendDisplay("Key Pressed " + keyPress);
    }

//    /**
//     * @see NativeMouseListener#nativeMouseClicked(NativeMouseEvent)
//     */
    public void nativeMouseClicked(NativeMouseEvent e) {
        mouseClick++;
        if(mouseClick%10==0){
            snapShot();
        }
        appendDisplay("Mouse Clicked " + mouseClick);
    }

    private void snapShot()  {
        try {
            String path = "/snapping/";
            File directory = new File(path);
            if (! directory.exists()){
                boolean result = directory.mkdir();
                if(result)
                    System.out.println(path + " Directory Created");
            }
            String ts = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
            String fileName = path + "SNAP"+ts+"_M"+mouseClick+"K"+keyPress;
            mouseClick=0;keyPress=0;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRectangle = new Rectangle(screenSize);
            Robot robot = new Robot();
            BufferedImage image = robot.createScreenCapture(screenRectangle);
            ImageIO.write(image, "png", new File(fileName + ".png"));
            lastMillisecond=System.currentTimeMillis()/1000;
            appendDisplay("Screenshot captured");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Write information about the <code>NativeInputEvent</code> to the text window.
     *
     * @param output appended to textEventInfo
     */
    private void appendDisplay(final String output) {
        txtEventInfo.append("\n" + output);

        try {
            //Clean up the history to reduce memory consumption.
            if (txtEventInfo.getLineCount() > 100) {
                txtEventInfo.replaceRange("", 0, txtEventInfo.getLineEndOffset(txtEventInfo.getLineCount() - 1 - 100));
            }

            txtEventInfo.setCaretPosition(txtEventInfo.getLineStartOffset(txtEventInfo.getLineCount() - 1));
        } catch (BadLocationException ex) {
            txtEventInfo.setCaretPosition(txtEventInfo.getDocument().getLength());
        }
    }

    /**
     * Unimplemented
     *
     * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    public void windowActivated(WindowEvent e) { /* Do Nothing */ }

    /**
     * Unimplemented
     *
     * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    public void windowClosing(WindowEvent e) { /* Do Nothing */ }

    /**
     * Unimplemented
     *
     * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
     */
    public void windowDeactivated(WindowEvent e) { /* Do Nothing */ }

    /**
     * Unimplemented
     *
     * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
     */
    public void windowDeiconified(WindowEvent e) { /* Do Nothing */ }

    /**
     * Unimplemented
     *
     * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    public void windowIconified(WindowEvent e) { /* Do Nothing */ }

    /**
     * Display information about the native keyboard and mouse along with any errors that may have
     * occurred.
     *
     * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    public void windowOpened(WindowEvent e) {
        // Return the focus to the window.
        requestFocusInWindow();

        // Please note that these properties are not available until after the GlobalScreen class is initialized.
        txtEventInfo.setText("Auto Repeat Rate: " + System.getProperty("jnativehook.key.repeat.rate"));
        appendDisplay("Auto Repeat Delay: " + System.getProperty("jnativehook.key.repeat.delay"));
        appendDisplay("Double Click Time: " + System.getProperty("jnativehook.button.multiclick.iterval"));
        appendDisplay("Pointer Sensitivity: " + System.getProperty("jnativehook.pointer.sensitivity"));
        appendDisplay("Pointer Acceleration Multiplier: " + System.getProperty("jnativehook.pointer.acceleration.multiplier"));
        appendDisplay("Pointer Acceleration Threshold: " + System.getProperty("jnativehook.pointer.acceleration.threshold"));

        try {
            txtEventInfo.setCaretPosition(txtEventInfo.getLineStartOffset(txtEventInfo.getLineCount() - 1));
        } catch (BadLocationException ex) {
            txtEventInfo.setCaretPosition(txtEventInfo.getDocument().getLength());
        }
        registerHook();
    }

    /**
     * Finalize and exit the program.
     *
     * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    public void windowClosed(WindowEvent e) {
        try {
            setExtendedState(JFrame.ICONIFIED);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void restore(){
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
    }

    /**
     * The demo project entry point.
     *
     * @param args unused.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(NativeHookDemo::new);
    }
}