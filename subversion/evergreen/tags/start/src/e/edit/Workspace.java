package e.edit;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;

public class Workspace extends JPanel {
    private EColumn leftColumn = new EColumn();
    private EErrorsWindow errors = new EErrorsWindow("+Errors");

    private String title;
    private String rootDirectory;
    private OpenQuicklyDialog openQuicklyDialog;
    private FileDialog openDialog;
    private FileDialog saveAsDialog;
    private FindFilesDialog findFilesDialog;
    
    private ArrayList fileList;
    
    public Workspace(String title, String rootDirectory) {
        super(new BorderLayout());
        this.title = title;
        this.rootDirectory = rootDirectory;
        if (rootDirectory.endsWith(File.separator) == false) {
            rootDirectory += File.separator;
        }
        
        add(makeUI(), BorderLayout.CENTER);
        updateFileList();
    }
    
    /**
     * Fills the file list. It can take some time to scan for files, so we do
     * the job in the background. The constructor slaps in an empty placeholder
     * list until the real one comes along. This means that clients should
     * never cache the result from getFileList.
     */
    public void updateFileList() {
        new Thread(new Runnable() {
            public void run() {
                fileList = scanWorkspaceForFiles();
            }
        }).start();
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getRootDirectory() {
        return rootDirectory;
    }
    
    /**
     * Returns the current file list. Note that this can be updated, so you should never
     * cache this result across operations, but must cache it for the duration of any
     * single operation.
     * This method can return null, if the workspace hasn't been scanned yet.
     */
    public List getFileList() {
        return fileList;
    }
    
    public JComponent makeUI() {
        leftColumn.addComponent(errors);
        registerTextComponent(errors.getText());
        return leftColumn;
    }
    
    /** Tests whether this workspace is empty. A workspace is still considered empty if all it contains is an errors window. */
    public boolean isEmpty() {
        Component[] components = leftColumn.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof EErrorsWindow == false) {
                return false;
            }
        }
        return true;
    }
    
    /** Returns an array of this workspace's dirty text windows. */
    public ETextWindow[] getDirtyTextWindows() {
        ArrayList dirtyWindows = new ArrayList();
        Component[] components = leftColumn.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof ETextWindow) {
                ETextWindow window = (ETextWindow) components[i];
                if (window.isDirty()) {
                    dirtyWindows.add(window);
                }
            }
        }
        return (ETextWindow[]) dirtyWindows.toArray(new ETextWindow[dirtyWindows.size()]);
    }
    
    /** Write the names of the currently open files to the given PrintWriter, one per line. */
    public void writeFilenamesTo(PrintWriter out) {
        Component[] components = leftColumn.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof ETextWindow) {
                ETextWindow window = (ETextWindow) components[i];
                out.println(window.getFilename() + window.getAddress());
            }
        }
    }
    
    public void writeDetailsTo(PrintWriter out) {
        out.println(getTitle());
        out.println(getRootDirectory());
    }
    
    public EWindow findWindowByName(String name) {
        EWindow win = findWindowInColumnByName(leftColumn, name);
        //FIXME: if the file's not already open, we need to find it and open it.
        return win;
    }
    
    public EWindow findWindowInColumnByName(EColumn column, String name) {
        name = name.toLowerCase();
        Component[] cs = column.getComponents();
        for (int i = 0; i < cs.length; i++) {
            EWindow win = (EWindow) cs[i];
            if (win.getTitle().toLowerCase().endsWith(name)) {
                return win;
            }
        }
        return null;
    }
    
    public static boolean isAbsolute(String filename) {
        boolean windows = System.getProperty("os.name").indexOf("Windows") != -1;
        if (windows) {
            /* FIXME: is this a good test for Windows? What about \\ names? */
            return (filename.length() > 1) && (filename.charAt(1) == ':');
        } else {
            return (filename.length() > 0) && (filename.charAt(0) == '/');
        }
    }
    
    /** Returns the EWindow corresponding to the given file. If the file's open, shows the given address. */
    public EWindow findIfAlreadyOpen(String filename, String address) {
        /* Check we don't already have this open as a file or directory. */
        EWindow window = findWindowByName(filename);
        if (window!= null) {
            if (window.getHeight() < 2 * window.getTitleBar().getHeight()) {
                /* FIXME: we don't necessarily want this window to grab *all* the space. */
                window.expand();
            }
            if (address != null) {
                ETextWindow textWindow = (ETextWindow) window;
                textWindow.jumpToAddress(address);
            }
            Edit.moveToComponent(window);
            return window;
        }
        return null;
    }
    
    public void registerTextComponent(javax.swing.text.JTextComponent textComponent) {
        Edit.getAdvisor().registerTextComponent(textComponent);
    }
    
    public void unregisterTextComponent(javax.swing.text.JTextComponent textComponent) {
        Edit.getAdvisor().unregisterTextComponent(textComponent);
    }
    
    public EWindow addViewerForFile(String filename, String address) {
        Edit.showStatus("Opening " + filename + "...");
        EWindow window = null;
        if (filename.endsWith(".gif") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            window = addViewer(new EImageWindow(filename));
        } else if (FileUtilities.fileFromString(filename).isDirectory()) {
            window = addViewer(new EDirectoryWindow(filename));
        } else {
            try {
                ETextWindow newWindow = new ETextWindow(filename);
                registerTextComponent(newWindow.getText());
                window = addViewer(newWindow, address);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Edit.showStatus("Opened " + FileUtilities.getUserFriendlyName(filename));
        return window;
    }
    
    public EWindow addViewer(EWindow viewer) {
        addViewer(viewer, null);
        return viewer;
    }
    
    public EWindow addViewer(EWindow viewer, final String address) {
        EColumn column = leftColumn;
        column.addComponent(viewer);
        if (address != null) {
            final ETextWindow textWindow = (ETextWindow) viewer;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    textWindow.jumpToAddress(address);
                }
            });
        }
        Edit.moveToComponent(viewer);
        return viewer;
    }
    
    public synchronized void reportError(String context, final String error) {
        errors.append(error);
    }
    
    public EErrorsWindow getErrorsWindow() {
        return errors;
    }
    
    /**
     * Checks whether this workspace has unsaved files before performing some action.
     * Offers the user the choice of continuing without saving, continuing after saving, or
     * canceling.
     * Returns true if you should continue with your action, false if the user hit 'Cancel'.
     */
    public boolean prepareForAction(String activity, String unsavedDataPrompt) {
        ETextWindow[] dirtyWindows = getDirtyTextWindows();
        if (dirtyWindows.length > 0) {
            String choice = Edit.askQuestion(activity, unsavedDataPrompt, "Save All", "Don't Save");
            if (choice.equals("Cancel")) {
                return false;
            }
            boolean saveAll = choice.equals("Save All");
            if (saveAll) {
                for (int i = 0; i < dirtyWindows.length; i++) {
                    boolean savedOkay = dirtyWindows[i].save();
                    if (savedOkay == false) {
                        // Pretend the user canceled, because we shouldn't pretend that everything went okay.
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    public void scanDirectory(int prefixCharsToSkip, String directory, String[] ignoredExtensions, ArrayList result) {
        File dir = FileUtilities.fileFromString(directory);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
               if (FileUtilities.isIgnoredDirectory(file) == false) {
                   scanDirectory(prefixCharsToSkip, file.toString(), ignoredExtensions, result);
               }
            } else {
                if (FileUtilities.nameEndsWithOneOf(file.toString(), ignoredExtensions) == false && FileUtilities.isSymbolicLink(file) == false) {
                    result.add(file.toString().substring(prefixCharsToSkip));
                }
            }
        }
    }
    
    /**
     * Builds a list of files for Open Quickly. Doesn't bother indexing a workspace if there's no
     * suggestion it's a project (as opposed to a home directory, say): no Makefile or build.xml,
     * and no CVS or SCCS directories are good clues that we're not looking at software.
     */
    public ArrayList scanWorkspaceForFiles() {
        long start = System.currentTimeMillis();
        
        String[] ignoredExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.uninterestingExtensions", ""));

        ArrayList result = new ArrayList();
        
        // If a workspace is under the user's home directory, we're happy to scan it.
        File userHome = FileUtilities.fileFromString(System.getProperty("user.dir"));
        File workspaceRoot = FileUtilities.fileFromString(getRootDirectory());
        boolean isUnderUserHome;
        try {
            isUnderUserHome = workspaceRoot.equals(userHome) == false && workspaceRoot.getCanonicalPath().startsWith(userHome.getCanonicalPath());
        } catch (IOException ex) {
            return result;
        }
        
        // If we haven't already decided to scan a workspace, see if it contains interesting files that
        // suggest it should be scanned.
        File[] rootFiles = workspaceRoot.listFiles();
        if (isUnderUserHome || (rootFiles != null && isSensibleToScan(rootFiles))) {
            Log.warn("Scanning " + getRootDirectory() + " for interesting files.");
            int prefixCharsToSkip = FileUtilities.parseUserFriendlyName(getRootDirectory()).length() + (getRootDirectory().endsWith(File.separator) ? 0 : 1);
            scanDirectory(prefixCharsToSkip, getRootDirectory(), ignoredExtensions, result);
            Log.warn("Scan of " + getRootDirectory() + " took " + (System.currentTimeMillis() - start) + "ms; found " + result.size() + " files.");
            Edit.showStatus("Scan of '" + getRootDirectory() + "' complete (" + result.size() + " files)");
        } else {
            Log.warn("Skipping scanning of " + getRootDirectory() + " because it doesn't look like a project.");
        }
        
        return result;
    }
    
    /**
     * Tests whether the given directory contents suggest that we should be indexing this workspace.
     * The idea is that if it looks like a software project, it's worth the cost (whatever), but if it doesn't,
     * it may well be ridiculously expensive to scan. Particularly because we blindly follow symlinks.
     */
    public boolean isSensibleToScan(File[] contents) {
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].toString().matches(".*(CVS|SCCS|Makefile|makefile|build\\.xml)")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Shows the "Find File" dialog with the given String as the contents of the text field.
     * Use null to retain the current contents.
     */
    public void showFindFilesDialog(String pattern, String filenamePattern) {
        if (findFilesDialog == null) {
            findFilesDialog = new FindFilesDialog(this);
        }
        findFilesDialog.setPattern(pattern);
        findFilesDialog.setFilenamePattern(filenamePattern);
        findFilesDialog.showDialog();
    }
    
    /**
     * Shows the "Open Quickly" dialog with the given String as the contents of the text field.
     * Use null to retain the current contents.
     */
    public void showOpenQuicklyDialog(String filenamePattern) {
        if (openQuicklyDialog == null) {
            openQuicklyDialog = new OpenQuicklyDialog(this);
        }
        openQuicklyDialog.setFilenamePattern(filenamePattern);
        openQuicklyDialog.showDialog();
    }
    
    public void showOpenDialog() {
        if (openDialog == null) {
            openDialog = new FileDialog(Edit.getFrame(), "Open", FileDialog.LOAD);
            openDialog.setDirectory(getRootDirectory());
        }
        openDialog.show();
        String leafname = openDialog.getFile();
        if (leafname != null) {
            Edit.openFile(openDialog.getDirectory() + File.separator + leafname);
        }
    }

    /** Returns the chosen save-as name, or null. */
    public String showSaveAsDialog() {
        if (saveAsDialog == null) {
            saveAsDialog = new FileDialog(Edit.getFrame(), "Save As", FileDialog.SAVE);
            saveAsDialog.setDirectory(getRootDirectory());
        }
        saveAsDialog.show();
        String leafname = saveAsDialog.getFile();
        if (leafname == null) {
            return null;
        }
        return saveAsDialog.getDirectory() + File.separator + leafname;
    }
}
