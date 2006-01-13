package e.gui;

import com.apple.eawt.*;
import e.util.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * A simple "about box".
 */
public class AboutBox extends JDialog {
    private static final AboutBox INSTANCE = new AboutBox();
    
    private ImageIcon icon;
    private String applicationName;
    private String version;
    private ArrayList<String> copyrightLines = new ArrayList<String>();
    
    private AboutBox() {
        initMacOs();
        initIcon();
    }
    
    public static AboutBox getSharedInstance() {
        return INSTANCE;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public boolean isConfigured() {
        return (applicationName != null);
    }
    
    public void setImage(String filename) {
        this.icon = new ImageIcon(filename);
        if (GuiUtilities.isMacOs()) {
            // FIXME: 64x64 is the Mac OS standard, but we should support whatever GNOME does too.
            this.icon = new ImageIcon(ImageUtilities.scale(icon.getImage(), 64, 64, ImageUtilities.InterpolationHint.BICUBIC));
            // Apple's HIG says that these dialog icons should be the application icon.
            UIManager.put("OptionPane.errorIcon", icon);
            UIManager.put("OptionPane.informationIcon", icon);
            UIManager.put("OptionPane.questionIcon", icon);
            UIManager.put("OptionPane.warningIcon", icon);
        }
    }
    
    public void setVersion(String version, String build) {
        this.version = version;
        if (build != null) {
            version += " (" + build + ")";
        }
    }
    
    /**
     * Adds a line of copyright text. You can add as many as you like. ASCII
     * renditions of the copyright symbol are automatically converted to the
     * real thing.
     */
    public void addCopyright(String copyright) {
        copyrightLines.add(copyright.replaceAll("\\([Cc]\\)", "\u00a9"));
    }
    
    public void setVisible(boolean visible) {
        if (visible == true) {
            makeUi();
        }
        super.setVisible(visible);
    }
    
    private void makeUi() {
        // FIXME: add GNOME and Win32 implementations.
        makeMacUi();
        if (GuiUtilities.isMacOs() == false) {
            // GNOME and Win32 applications give their about boxes titles.
            setTitle("About " + applicationName);
        }
    }
    
    private void makeMacUi() {
        // http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/XHIGWindows/chapter_17_section_5.html#//apple_ref/doc/uid/20000961-TPXREF17
        
        // Mac OS font defaults.
        Font applicationNameFont = new Font("Lucida Grande", Font.BOLD, 14);
        Font versionFont = new Font("Lucida Grande", Font.PLAIN, 10);
        Font copyrightFont = new Font("Lucida Grande", Font.PLAIN, 10);
        
        // FIXME: code to get the right fonts for GNOME probably looks like this, but even Java 6 doesn't seem to get the right starting font.
        if (false) {
            final float PANGO_SCALE_SMALL = (1.0f / 1.2f);
            final float PANGO_SCALE_XX_LARGE = (1.2f * 1.2f * 1.2f);
            final Font gnomeBaseFont = UIManager.getFont("TextArea.font");
            final float baseSize = gnomeBaseFont.getSize2D();
            applicationNameFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_XX_LARGE).deriveFont(Font.BOLD);
            versionFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_SMALL);
            copyrightFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_SMALL);
        }
        
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(8, 12, 20, 12));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        Dimension spacerSize = new Dimension(1, 8);
        
        if (icon != null) {
            addLabel(panel, new JLabel(icon));
            panel.add(Box.createRigidArea(spacerSize));
            panel.add(Box.createRigidArea(spacerSize));
        }
        
        addLabel(panel, applicationNameFont, applicationName);
        panel.add(Box.createRigidArea(spacerSize));
        
        if (version != null) {
            addLabel(panel, versionFont, version);
            panel.add(Box.createRigidArea(spacerSize));
        }
        
        for (String copyright : copyrightLines) {
            addLabel(panel, copyrightFont, copyright);
        }
        
        setContentPane(panel);
        
        // Set an appropriate size.
        pack();
        // Disable the "maximize" button.
        setMaximumSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        // Stop resizing.
        setResizable(false);
        
        // Center on the display.
        // FIXME: use the visual center.
        setLocationRelativeTo(null);
    }
    
    private static void addLabel(JPanel panel, Icon icon) {
        addLabel(panel, new JLabel(icon));
    }
    
    private static void addLabel(JPanel panel, Font font, String text) {
        // FIXME: Mac OS actually uses selectable text components which is handy for copying & pasting version information.
        // FIXME: support HTML and automatically install code to change the mouse cursor when hovering over links, and use BrowserLauncher when a link is clicked?
        JLabel label = new JLabel(text);
        label.setFont(font);
        addLabel(panel, label);
    }
    
    private static void addLabel(JPanel panel, JLabel label) {
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.add(label);
    }
    
    private void initMacOs() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        initMacOsAboutMenu();
        initMacOsIcon();
    }
    
    private void initMacOsAboutMenu() {
        Application.getApplication().addApplicationListener(new ApplicationAdapter() {
            public void handleAbout(ApplicationEvent e) {
                AboutBox.getSharedInstance().setVisible(true);
                e.setHandled(true);
            }
        });
    }
    
    private void initMacOsIcon() {
        // FIXME: we need to look for the icon in a way that will work on Linux too.
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (key.startsWith("APP_ICON_")) {
                String icnsFilename = env.get(key);
                // FIXME: if we had a .icns reader for ImageIO, we wouldn't need to mess around like this.
                String pngFilename = icnsFilename.replaceAll("\\.icns$", "-128.png");
                setImage(pngFilename);
                return;
            }
        }
    }
    
    private void initIcon() {
        if (icon != null) {
            return;
        }
        
        String frameIconFilename = System.getProperty("org.jessies.frameIcon");
        if (frameIconFilename != null) {
            setImage(frameIconFilename);
        }
    }
    
    public static void main(String[] args) {
        GuiUtilities.initLookAndFeel();
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setApplicationName("Demonstration");
        aboutBox.setVersion("1.00", "374");
        aboutBox.addCopyright("Copyright (C) 2006, Elliott Hughes");
        aboutBox.setVisible(true);
    }
}
