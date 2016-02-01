package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

import e.forms.*;
import e.gui.*;
import e.util.*;

/** A demonstration of our text-editing component. */
public class Edit implements com.apple.eawt.ApplicationListener {
    private static com.apple.eawt.Application application;
    private static Edit instance;
    
    public static JFrame frame;
    private static JTabbedPane tabbedPane;
    private static TagsPanel tagsPanel;
    private static Advisor advisor;
    private static EStatusBar statusLine;
    private static Minibuffer minibuffer;
    private static JPanel statusArea;
    
    /** The global find history for all FindDialog instances. */
    private static EHistoryComboBoxModel findHistory = new ChronologicalComboBoxModel();
    
    public static Edit getInstance() {
        return instance;
    }
    
    /** Returns the frame Edit is using for its main window. */
    public static Frame getFrame() {
        return (Frame) frame;
    }
    
    public static EHistoryComboBoxModel getFindHistory() {
        return findHistory;
    }
    
    public static TagsPanel getTagsPanel() {
        return tagsPanel;
    }

    public static Advisor getAdvisor() {
        return advisor;
    }
    
    private static final int MAX_LINE_LENGTH = 72;

    public static String breakLongMessageLines(String message) {
        if (message.length() < MAX_LINE_LENGTH) {
            return message;
        }
        StringBuffer result = new StringBuffer(message);
        int chunkLength = 0;
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == '\n') {
                chunkLength = 0;
            } else {
                chunkLength++;
                if (chunkLength > MAX_LINE_LENGTH && result.charAt(i) == ' ') {
                    result.insert(i + 1, '\n');
                }
            }
        }
        return result.toString();
    }

    public static void showStatus(String status) {
        statusLine.setText(status);
    }

    public static void showAlert(String title, String message) {
        JOptionPane.showMessageDialog(Edit.getFrame(), breakLongMessageLines(message), title, JOptionPane.WARNING_MESSAGE);
    }

    public static boolean askQuestion(String title, String message, String continueText) {
        Object[] options = { continueText, "Cancel" };
        int option = JOptionPane.showOptionDialog(Edit.getFrame(), breakLongMessageLines(message), title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return (option == JOptionPane.YES_OPTION);
    }

    public static String askQuestion(String title, String message, String continueTextYes, String continueTextNo) {
        Object[] options = { continueTextYes, continueTextNo, "Cancel" };
        int option = JOptionPane.showOptionDialog(Edit.getFrame(), breakLongMessageLines(message), title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (option == JOptionPane.YES_OPTION) {
            return continueTextYes;
        } else if (option == JOptionPane.NO_OPTION) {
            return continueTextNo;
        } else {
            return "Cancel";
        }
    }

    public static void showDocument(String uri) {
        try {
            // FIXME: this is an ugly hack; where's the right place to sort this out?
            if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
                uri = "file:///" + uri.substring(6);
            }
            /* we could use a class provided by Apple on Mac OS, but it doesn't seem to offer any advantage.
            if (System.getProperty("os.name").indexOf("Mac") != -1) {
                System.err.println(uri);
                Class.forName("com.apple.eio.FileManager").getMethod("openURL", new Class[] { String.class }).invoke(null, new Object[] { uri });
                return;
            }*/
            Runtime.getRuntime().exec(new String[] {
                Parameters.getParameter("browser"), uri
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Edit.showAlert("Hyperlink", "Couldn't show '" + uri + "' (" + ex.getMessage() + ")");
        }
    }
    
    public static void moveToComponent(Component c) {
        c.requestFocus();
    }
    
    private static EDialog lastShownDialog;
    
    public static void hideDialog() {
        if (lastShownDialog != null && lastShownDialog.isVisible()) {
            lastShownDialog.setVisible(false);
        }
    }
    
    public static void showDialog(EDialog dialog) {
        hideDialog();
        
        Point location = frame.getLocationOnScreen();
        location.x += (frame.getWidth() - dialog.getWidth()) / 2;
        location.y += (frame.getHeight() - dialog.getHeight()) / 2;
        location.x = Math.max(0, location.x);
        location.y = Math.max(0, location.y);
        dialog.setLocation(location);
        
        dialog.setVisible(true);
        lastShownDialog = dialog;
        
        dialog.toFront();
    }
    
    private static Workspace defaultWorkspace;
    
    /** Extensions that shouldn't be opened by Edit. */
    private static String[] externalApplicationExtensions;
    
    public static boolean isFileForExternalApplication(String filename) {
        if (externalApplicationExtensions == null) {
            externalApplicationExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.externalApplicationExtensions", ""));
        }
        return FileUtilities.nameEndsWithOneOf(filename, externalApplicationExtensions);
    }
    
    public static void openFileWithExternalApplication(String filename) {
        try {
            Runtime.getRuntime().exec(new String[] {
                Parameters.getParameter("open.command"), filename
            });
        } catch (Exception ex) {
            Edit.showAlert("Run", "Couldn't open '" + filename + "' with an external application (" + ex.getMessage() + ")");
        }
    }
    
    /**
     * Sees if any of the "path.rewrite" configuration applies to the given filename,
     * and replaces the matching prefix with the appropriate substitution. Returns the
     * original filename if there's no applicable rewrite.
     */
    public static String processPathRewrites(String filename) {
        String from;
        for (int i = 0; (from = Parameters.getParameter("path.rewrite.from." + i)) != null; i++) {
            if (filename.startsWith(from)) {
                String to = Parameters.getParameter("path.rewrite.to." + i);
                String result = to + filename.substring(from.length());
                return result;
            }
        }
        return filename;
    }
    
    /**
     * Opens a file. If the file's already open, it receives the focus. The 'filename'
     * parameter is actually a grep-style filename:address string.
     * 
     * Returns the EWindow corresponding to the file opened, or null if
     * no file was opened or the file was passed to an external program.
     */
    public static EWindow openFile(String filename) {
        filename = FileUtilities.parseUserFriendlyName(filename);
        
        /* Special case for a URI. */
        if (FileUtilities.nameStartsWithOneOf(filename, FileUtilities.getArrayOfPathElements(Parameters.getParameter("url.prefixes", "")))) {
            Edit.showDocument(filename);
            return null;
        }
        
        if (filename.startsWith("telnet://")) {
            getCurrentWorkspace().addViewer(new ETelnetWindow(filename.substring(9)));
            return null;
        }
        
        filename = processPathRewrites(filename);
        
        /* Tidy up wrong-OS pathnames. Programs like jikes do this to us. */
        if (File.separatorChar == '\\') {
            filename = filename.replace('/', '\\');
        } else {
            filename = filename.replace('\\', '/');
        }
        
        /* Extract any address. */
        final String address;
        if (filename.indexOf(':', 2) != -1) {
            address = filename.substring(filename.indexOf(':', 2));
            filename = filename.substring(0, filename.indexOf(':', 2));
        } else {
            address = null;
        }
        
        Log.warn("Opening '" + filename + "' at '" + address + "'");
        
        /* Remove local-directory fluff. */
        if (filename.startsWith("./") || filename.startsWith(".\\")) {
            filename = filename.substring(2);
        }
        
        if (isFileForExternalApplication(filename)) {
            openFileWithExternalApplication(filename);
            return null;
        }
        
        /* Give up if the file doesn't exist. */
        if (FileUtilities.fileFromString(filename).exists() == false) {
            Edit.showAlert("Open", "File '" + filename + "' does not exist.");
            return null;
        }
        
        try {
            /*
             * Open the file a symbolic link points to, and not the link itself.
             */
            if (FileUtilities.isSymbolicLink(filename)) {
                //Edit.showAlert("Open", "Edit won't open a symbolic link, which '" + filename + "' appears to be.\nThe link target '" + canonicalFilename + "' will be opened instead.");
                String canonicalFilename = FileUtilities.fileFromString(filename).getCanonicalPath();
                if (address != null) {
                    canonicalFilename += address;
                }
                return Edit.openFile(canonicalFilename);
            }
            
            /*
             * Clean paths like a/b/../c/d. Let the Java API do this.
             */
            filename = FileUtilities.fileFromString(filename).getAbsolutePath();
        } catch (IOException ex) {
            /* harmless. */
        }
        
        /* Find which workspace this file is on/should be on, and make it visible. */
        Workspace workspace = getBestWorkspaceForFilename(filename);
        tabbedPane.setSelectedComponent(workspace);
        
        /* If the user already has this file open, we shouldn't open it again. */
        EWindow alreadyOpenWindow = workspace.findIfAlreadyOpen(filename, address);
        if (alreadyOpenWindow != null) {
            return alreadyOpenWindow;
        }
        
        /* Limit ourselves (rather arbitrarily) to files under half a gigabyte. That's quite a strain on us, at present. */
        final int KB = 1024;
        final int MB = 1024 * KB;
        long fileLength = FileUtilities.fileFromString(filename).length();
        if (fileLength > 512 * MB) {
            Edit.showAlert("Open", "Edit can't really handle files as large as '" + filename + "', which is " + fileLength + " bytes long. This file will not be opened.");
            return null;
        }
        
        /* Add an appropriate viewer for the filename to the chosen workspace. */
        return workspace.addViewerForFile(filename, address);
    }
    
    /** Returns an array of all the workspaces. */
    public static Workspace[] getWorkspaces() {
        Workspace[] result = new Workspace[tabbedPane.getTabCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (Workspace) tabbedPane.getComponentAt(i);
        }
        return result;
    }
    
    /** Returns the workspace whose root directory shares the longest common prefix with the given filename. */
    public static Workspace getBestWorkspaceForFilename(String filename) {
        Workspace[] workspaces = getWorkspaces();
        int bestIndex = 0;
        filename = FileUtilities.parseUserFriendlyName(filename);
        int bestLength = StringUtilities.lengthOfCommonDirectoryPrefix(filename, FileUtilities.parseUserFriendlyName(workspaces[0].getRootDirectory()));
        for (int i = 1; i < workspaces.length; i++) {
            int length = StringUtilities.lengthOfCommonDirectoryPrefix(filename, FileUtilities.parseUserFriendlyName(workspaces[i].getRootDirectory()));
            if (length > bestLength) {
                bestIndex = i;
                bestLength = length;
            }
        }
        return workspaces[bestIndex];
    }
    
    public static Workspace getCurrentWorkspace() {
        if (tabbedPane == null) {
            return null;
        }
        return (Workspace) tabbedPane.getSelectedComponent();
    }
    
    /**
     * Finds the appropriate index in tabbedPane for a workspace with the given name.
     * The idea is that sorting workspaces alphabetically will remove the urge to want
     * manual control over workspace order. Alphabetical order is sensible enough that
     * we shouldn't be upset by a workspace seeming to be in the wrong place, as was
     * so easily the case with the previous implicit chronological order.
     */
    public static int getWorkspaceIndexInTabbedPane(String name) {
        int index = 0;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            if (name.compareTo(title) <= 0) {
                break;
            }
            index++;
        }
        return index;
    }
    
    public static void createWorkspace(String name, String root) {
        boolean noNonEmptyWorkspaceOfThisNameExists = removeWorkspaceByName(name);
        if (noNonEmptyWorkspaceOfThisNameExists == false) {
            Edit.showAlert("Edit", "A non-empty workspace of the name '" + name + "' already exists.");
            return;
        }
        Workspace workspace = new Workspace(name, root);
        tabbedPane.insertTab(name, null, workspace, root, getWorkspaceIndexInTabbedPane(name));
        Edit.showStatus("Added workspace '" + name + "' (" + root + ")");
        if (defaultWorkspace == null) defaultWorkspace = workspace;
        frame.invalidate();
        frame.validate();
        frame.repaint();
    }
    
    /**
     * Removes any workspaces with the given name. Returns false if there was a workspace,
     * but it couldn't be removed because it had open files, true otherwise.
     */
    public static boolean removeWorkspaceByName(String name) {
        Workspace[] workspaces = getWorkspaces();
        for (int i = 0; i < workspaces.length; i++) {
            Workspace workspace = workspaces[i];
            if (workspace.getTitle().equals(name)) {
                if (workspace.isEmpty() == false) {
                    return false;
                }
                tabbedPane.remove(workspace);
            }
        }
        return true;
    }
    
    public static void removeCurrentWorkspace() {
        Workspace workspace = getCurrentWorkspace();
        if (workspace == null) {
            return;
        }
        if (workspace.isEmpty()) {
            tabbedPane.remove(workspace);
        } else {
            Edit.showAlert("Edit", "The workspace '" + workspace.getTitle() + "' is not empty.");
        }
    }
    
    public static void createWorkspaceForCurrentDirectory() {
        String currentDirectory = Parameters.getParameter("user.dir");
        String workspaceName = currentDirectory.substring(currentDirectory.lastIndexOf(File.separatorChar) + 1);
        createWorkspace(workspaceName, currentDirectory);
    }
    
    public void initWindowIcon() {
        ImageIcon icon = new ImageIcon(System.getProperty("env.EDIT_HOME") + File.separatorChar + "icon.gif");
        frame.setIconImage(icon.getImage());
    }
    
    public void handleAbout(com.apple.eawt.ApplicationEvent e) {
        Edit.showAlert("Edit", "Edit is free software. See the file COPYING for copying permission.");
    }
    
    public void handleOpenApplication(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleOpenApplication");
    }
    
    public void handleReOpenApplication(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleReOpenApplication");
    }
    
    public void handleOpenDocument(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleOpenDocument");
    }
    public void handleOpenFile(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleOpenFile");
    }
    
    public void handlePreferences(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handlePreferences");
    }
    
    public void handlePrintDocument(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handlePrintDocument");
    }
    public void handlePrintFile(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handlePrintFile");
    }
    
    /**
     * Attempts to quit Edit. All the workspaces are asked if it's safe for them to be
     * closed. Only if all workspaces agree that it's safe will Edit actually quit.
     */
    public void handleQuit(com.apple.eawt.ApplicationEvent e) {
        boolean isSafeToQuit = true;
        boolean onMacOS = (e != null);
        Workspace[] workspaces = getWorkspaces();
        for (int i = 0; i < workspaces.length; i++) {
            if (workspaces[i].getDirtyTextWindows().length != 0) {
                isSafeToQuit = false;
                // Ensure that the workspace in question is visible.
                tabbedPane.setSelectedIndex(i);
            }
        }
        
        if (onMacOS) {
            // Let Apple's library code know whether or not to terminate us when we return.
            e.setHandled(isSafeToQuit);
        }
        
        if (isSafeToQuit == false) {
            showAlert("Edit", "There are unsaved files. Please deal with them and try again.");
            return;
        }
        
        SpellingChecker.dumpKnownBadWordsTo(System.out);
        FormDialog.writeGeometriesTo(getDialogGeometriesPreferenceFilename());
        
        // We're definitely going to quit now...
        rememberOpenFiles();
        rememberOpenWorkspaces();
        rememberWindowSizeAndPosition();
        if (onMacOS == false) {
            System.exit(0);
        }
    }
    
    /** Returns the full pathname for the given preference file. */
    public static String getPreferenceFilename(String leafname) {
        return System.getProperty("preferencesDirectory") + File.separator + leafname;
    }
    
    public static String getDialogGeometriesPreferenceFilename() {
        return getPreferenceFilename("dialog-geometries");
    }
    
    public static String getWindowSizeAndPositionPreferenceFilename() {
        return getPreferenceFilename("window-size-and-position");
    }
    
    public static String getOpenFileListPreferenceFilename() {
        return getPreferenceFilename("open-file-list");
    }
    
    public static String getOpenWorkspaceListPreferenceFilename() {
        return getPreferenceFilename("open-workspace-list");
    }
    
    public void rememberWindowSizeAndPosition() {
        final String filename = getWindowSizeAndPositionPreferenceFilename();
        Point position = frame.getLocation();
        Dimension size = frame.getSize();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(filename));
            out.println(position.x);
            out.println(position.y);
            out.println(size.width);
            out.println(size.height);
        } catch (IOException ex) {
            Edit.showAlert("Edit", "Couldn't write window size and position " + filename);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void initWindowPosition() {
        final String filename = getWindowSizeAndPositionPreferenceFilename();
        int x = 0;
        int y = 0;
        int width = 800;
        int height = 730;
        if (FileUtilities.fileFromString(filename).exists()) {
            String[] lines = StringUtilities.readLinesFromFile(filename);
            if (lines.length == 4) {
                x = Integer.parseInt(lines[0]);
                y = Integer.parseInt(lines[1]);
                width = Integer.parseInt(lines[2]);
                height = Integer.parseInt(lines[3]);
            }
        }
        frame.setLocation(x, y);
        frame.setSize(width, height);
    }
    
    /** Writes a file containing a list of the currently-open files to Edit's preferences directory. */
    public void rememberOpenFiles() {
        Log.warn("Remembering open files in preparation for quit.");
        final String filename = getOpenFileListPreferenceFilename();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(filename));
            Workspace[] workspaces = getWorkspaces();
            for (int i = 0; i < workspaces.length; i++) {
                workspaces[i].writeFilenamesTo(out);
            }
        } catch (IOException ex) {
            Edit.showAlert("Edit", "Couldn't write list of open files to " + filename);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /** Opens all the files listed in the file we remembered them to last time we quit. */
    public void openRememberedFiles() {
        Edit.showStatus("Opening remembered files...");
        final String filename = getOpenFileListPreferenceFilename();
        if (FileUtilities.fileFromString(filename).exists() == false) {
            Edit.showStatus("No list of files to open");
            return; // It's not an error to not have any stored state.
        }
        String[] filenames = StringUtilities.readLinesFromFile(filename);
        for (int i = 0; i < filenames.length; i++) {
            Edit.openFile(filenames[i]);
        }
        Edit.showStatus((filenames.length == 0) ? "No files to open" : "Finished opening files");
    }
    
    /** Writes a file containing a list of the currently-open workspaces to Edit's preferences directory. */
    public void rememberOpenWorkspaces() {
        Log.warn("Remembering open workspaces in preparation for quit.");
        final String filename = getOpenWorkspaceListPreferenceFilename();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(filename));
            Workspace[] workspaces = getWorkspaces();
            for (int i = 0; i < workspaces.length; i++) {
                workspaces[i].writeDetailsTo(out);
            }
        } catch (IOException ex) {
            Edit.showAlert("Edit", "Couldn't write list of open workspaces to " + filename);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /** Opens all the workspaces listed in the file we remembered them to last time we quit. */
    public void openRememberedWorkspaces() {
        Log.warn("Opening remembered workspaces...");
        final String filename = getOpenWorkspaceListPreferenceFilename();
        if (FileUtilities.fileFromString(filename).exists() == false) {
            return; // It's not an error to not have any stored state.
        }
        
        String[] lines = StringUtilities.readLinesFromFile(filename);
        if (lines.length == 0) {
            return; // No remembered workspaces.
        }

        for (int i = 0; i < lines.length - 1; i += 2) {
            String name = lines[i];
            String root = lines[i + 1];
            Log.warn("Opening workspace '" + name + "' with root '" + root + "'");
            createWorkspace(name, root);

            File rootDirectory = FileUtilities.fileFromString(root);
            int which = tabbedPane.getTabCount() - 1;
            if (rootDirectory.exists() == false) {
                tabbedPane.setEnabledAt(which, false);
                tabbedPane.setToolTipTextAt(which, root + " doesn't exist.");
            } else if (rootDirectory.isDirectory() == false) {
                tabbedPane.setEnabledAt(which, false);
                tabbedPane.setToolTipTextAt(which, root + " isn't a directory.");
            }
        }
    }
    
    public void initWindowListener() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleQuit(null);
            }
        });
    }
    
    public void initWindowKeys() {
        // FIXME: do we need this or not? i thought we did, but when i came to add it because C-O wasn't always working, i found C-O had started to work (as far as i can tell) all the time. weird...
//        frame.getRootPane().registerKeyboardAction(this, "Close", KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, 0, true), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    
    public void initWindow() {
        initWindowPosition();
        initWindowIcon();
        initWindowListener();
        initWindowKeys();
    }
    
    public void initTagsPanel() {
        tagsPanel = new TagsPanel();
    }
    
    public void initAdvisor() {
        advisor = new Advisor();
    }
    
    public void startEditServer() {
        try {
            new EditServer();
        } catch (Throwable th) {
            Log.warn("Couldn't start EditServer", th);
        }
    }
    
    public void initPreferences() {
        Parameters.readPropertiesFile(getPreferenceFilename("edit.properties"));
    }
    
    public void initStatusArea() {
        statusLine = new EStatusBar();
        minibuffer = new Minibuffer();
        statusArea = new JPanel(new BorderLayout());
        statusArea.setBorder(new javax.swing.border.EmptyBorder(2, 2, 2, 2));
        statusArea.add(statusLine, BorderLayout.NORTH);
        statusArea.add(minibuffer, BorderLayout.SOUTH);
    }
    
    public static void showMinibuffer(MinibufferUser minibufferUser) {
        minibuffer.activate(minibufferUser);
    }
    
    public static void hideMinibuffer() {
        minibuffer.deactivate();
    }
    
    private Edit() {
        application = new com.apple.eawt.Application();
        application.addApplicationListener(this);

        Log.setApplicationName("Edit");
        frame = new JFrame("Edit");
        
        initPreferences();
        GuiUtilities.initLookAndFeel();
        FormDialog.readGeometriesFrom(getDialogGeometriesPreferenceFilename());
        
        /* FIXME: is there a better way round this two-stage menubar construction? */
        EditMenuBar menuBar = new EditMenuBar();
        frame.setJMenuBar(menuBar);
        menuBar.populate();
        
        initWindow();
        initTagsPanel();
        initAdvisor();
        initStatusArea();
        
        startEditServer();
        
        boolean atSide = (Parameters.getParameter("os.name").indexOf("Mac OS") != -1);
        UIManager.put("TabbedPane.useSmallLayout", Boolean.TRUE);
        tabbedPane = new JTabbedPane(atSide ? JTabbedPane.LEFT : JTabbedPane.TOP);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Edit.hideDialog();
                Edit.getTagsPanel().ensureTagsAreHidden();
            }
        });
        
        JSplitPane tagsAndAdvisorSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tagsPanel, advisor);
        tagsAndAdvisorSplitPane.setDividerLocation(0.8);
        tagsAndAdvisorSplitPane.setResizeWeight(0.8);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, tabbedPane, tagsAndAdvisorSplitPane);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusArea, BorderLayout.SOUTH);
        
        openRememberedWorkspaces();
        // If that didn't create any workspaces, give the user one for free...
        if (tabbedPane.getTabCount() == 0) {
            createWorkspaceForCurrentDirectory();
        }
        
        splitPane.setDividerLocation(0.8);
        splitPane.setResizeWeight(0.8);
        
        frame.setVisible(true);
        
        openRememberedFiles();
    }

    public static void main(String[] args) {
	instance = new Edit();
    }
}
