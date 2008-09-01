package e.edit;

import e.gui.*;
import e.ptextarea.FileType;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import org.jdesktop.swingworker.SwingWorker;

public class Advisor extends JPanel {
    private static Advisor instance;
    
    private static ArrayList<WorkspaceResearcher> researchers = new ArrayList<WorkspaceResearcher>();
    
    /** The advice window. */
    private AdvisorHtmlPane advicePane = new AdvisorHtmlPane();
    
    private JFrame frame;
    
    public static synchronized Advisor getInstance() {
        if (instance == null) {
            instance = new Advisor();
        }
        return instance;
    }
    
    private Advisor() {
        setLayout(new BorderLayout());
        add(advicePane.makeToolBar(), BorderLayout.NORTH);
        add(advicePane, BorderLayout.CENTER);
        
        /*
        final JTextField textField = new JTextField();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                linkClicked(textField.getText());
            }
        });
        add(textField, BorderLayout.NORTH);
        */
    }
    
    /**
     * Initializes this class' expensive parts on a new low-priority thread.
     * We used to put off initialization until the researchers were actually needed, but that tends to be early on, and on the EDT.
     */
    public static void initResearchersOnBackgroundThread() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                initResearchersInParallel();
            }
        }, "initResearchersOnBackgroundThread");
        // Avoid inheriting the EDT's high priority.
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }
    
    private static void initResearchersInParallel() {
        synchronized (researchers) {
            ExecutorService executor = ThreadUtilities.newFixedThreadPool(6, "Researcher Initializer");
            executor.execute(new Runnable() {
                public void run() {
                    researchers.add(JavaResearcher.getSharedInstance());
                }
            });
            executor.execute(new Runnable() {
                public void run() {
                    researchers.add(ManPageResearcher.getSharedInstance());
                }
            });
            executor.execute(new Runnable() {
                public void run() {
                    researchers.add(new PerlDocumentationResearcher());
                }
            });
            executor.execute(new Runnable() {
                public void run() {
                    researchers.add(new PythonDocumentationResearcher());
                }
            });
            executor.execute(new Runnable() {
                public void run() {
                    researchers.add(new RubyDocumentationResearcher());
                }
            });
            executor.execute(new Runnable() {
                public void run() {
                    researchers.add(new StlDocumentationResearcher());
                }
            });
            executor.shutdown();
            try {
                executor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Log.warn("Failed to initialize researchers.", ex);
            }
        }
    }
    
    private static ArrayList<WorkspaceResearcher> getResearchers() {
        synchronized (researchers) {
            return researchers;
        }
    }
    
    /**
     * Invokes addWordsTo with 'words' on every known researcher whose isSuitable returns true for 'fileType'.
     */
    public void addWordsTo(FileType fileType, Set<String> words) {
        for (WorkspaceResearcher researcher : getResearchers()) {
            if (researcher.isSuitable(fileType)) {
                researcher.addWordsTo(words);
            }
        }
    }
    
    private synchronized JFrame getFrame() {
        if (frame == null) {
            final String frameTitle = "Evergreen Documentation Browser";
            frame = JFrameUtilities.makeSimpleWindow(frameTitle, this);
            frame.setSize(new Dimension(600, 500));
            JFrameUtilities.closeOnKeyStroke(frame, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false));
            // We use the frameTitle constant rather than asking the frame for its title so if we change the title, we don't break this code.
            JFrameUtilities.restoreBounds(frameTitle, frame);
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    JFrameUtilities.storeBounds(frameTitle, frame);
                }
            });
        }
        return frame;
    }
    
    public void setDocumentationVisible() {
        advicePane.clearAdvice();
        // Really, we want getFrame().toFront(), but GNOME breaks that, so we have to work around it.
        if (getFrame().isVisible()) {
            getFrame().setVisible(false);
        }
        getFrame().setVisible(true);
    }
    
    public synchronized void showDocumentation() {
        setDocumentationVisible();
        startResearch(ETextAction.getSearchTerm());
    }
    
    private class ResearchRunner extends SwingWorker<String, Object> {
        private String searchTerm;
        private ETextWindow textWindow;
        
        private ResearchRunner(String searchTerm) {
            this.searchTerm = searchTerm;
            this.textWindow = ETextAction.getFocusedTextWindow();
            advicePane.setTemporaryText("Searching for documentation on \"" + searchTerm + "\"...");
        }
        
        @Override
        protected String doInBackground() {
            FileType fileType = (textWindow != null) ? textWindow.getFileType() : null;
            StringBuilder newText = new StringBuilder();
            for (WorkspaceResearcher researcher : getResearchers()) {
                if (fileType == null || researcher.isSuitable(fileType)) {
                    String result = researcher.research(searchTerm, textWindow);
                    if (result != null && result.length() > 0) {
                        // We need to strip HTML and BODY tags if we're to concatenate HTML documents.
                        // We can't strip HEAD tags because they may have useful content.
                        // It's too hard to add BODY tags in appropriate places.
                        result = result.replaceAll("(?i)</?html>", "").replaceAll("(?i)</?body[^>]*>", "");
                        newText.append(result);
                    }
                }
            }
            
            if (newText.length() > 0) {
                return newText.toString();
            } else {
                return "No documentation found for \"" + searchTerm + "\".";
            }
        }
        
        @Override
        public void done() {
            String newText;
            try {
                newText = get();
            } catch (Exception ex) {
                // We could make more of an effort here, but this "shouldn't happen".
                // Effort should go to hardening the researchers rather than polishing the high-level error handling.
                // It's useful to let the user know we failed, though.
                newText = "<p>An error occurred while searching for \"" + searchTerm + "\". See log for full details.";
                Log.warn("ResearchRunner failed on \"" + searchTerm + "\"", ex);
            }
            setDocumentationText(newText);
        }
    }
    
    public void setDocumentationText(String content) {
        // We've probably stripped the HTML tag.
        if (content.startsWith("<html>") == false) {
            content = "<html><head><title></title></head><body bgcolor=#FFFFFF>" + content + "</body></html>";
        }
        // JEditorPane.setText is thread-safe.
        advicePane.setText(content);
    }
    
    private void startResearch(String text) {
        new ResearchRunner(text).execute();
    }
    
    public void linkClicked(String link) {
        new LinkClickRunner(link).execute();
    }
    
    private class LinkClickRunner extends SwingWorker<Object, Object> {
        private String link;
        
        private LinkClickRunner(String link) {
            this.link = link;
        }
        
        @Override
        protected Object doInBackground() {
            // Anything off the web or local HTML should be displayed in the documentation browser, rather than handed off to the platform's web browser.
            // Non-HTML files, though, need to be handed off so they're opened for editing.
            if (link.startsWith("http:") || link.matches("file:.*\\.html(#.*)?")) {
                advicePane.setPage(link);
                return null;
            }
            // Offer the link to each researcher.
            for (WorkspaceResearcher researcher : getResearchers()) {
                if (researcher.handleLink(link)) {
                    return null;
                }
            }
            // Hand it on to the file-opening code to work out what to do with it.
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Evergreen.getInstance().openFile(link);
                }
            });
            return null;
        }
    }
    
    public static String findToolOnPath(String tool) {
        ArrayList<String> availableTools = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, new String[] { "which", tool }, availableTools, errors);
        if (status != 0 || availableTools.size() == 0) {
            return null;
        }
        return availableTools.get(0);
    }
    
    /**
     * Adds all the unique words in the given collection to 'set'.
     */
    public synchronized static void extractUniqueWords(Collection<String> collection, Set<String> set) {
        for (String identifier : collection) {
            // FIXME: this turns "posix_openpt" into two words, so the spelling checker will accept "openpt" alone, rather than just in the identifier "posix_openpt" as intended. Maybe we should check blessed identifiers as a whole before we try break them into words, and then supply the spelling checker with the unique identifiers we bless, rather than just the list of words? (At the same time, passing all the words works well for Java source.)
            final String[] words = identifier.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase().split(" ");
            for (String word : words) {
                set.add(word);
            }
        }
    }
}
