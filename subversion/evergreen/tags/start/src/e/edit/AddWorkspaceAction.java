package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * Opens a dialog where the user can specify a name and root directory for a new workspace,
 * which will then be created.
 */
public class AddWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Add Workspace...";
    
    private JTextField nameField = new JTextField("", 40);
    
    public AddWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        FilenameChooserField filenameChooserField = new FilenameChooserField(JFileChooser.DIRECTORIES_ONLY);
        filenameChooserField.setCompanionNameField(nameField);
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Name:", nameField);
        formPanel.addRow("Root Directory:", filenameChooserField);
        boolean okay = FormDialog.show(Edit.getFrame(), "Add Workspace", formPanel);
        
        if (okay == false) {
            return;
        }
        
        String message = FileUtilities.checkDirectoryExistence(filenameChooserField.getPathname());
        if (message != null) {
            Edit.showAlert("Open Quickly", message);
            return;
        }
        
        Edit.createWorkspace(nameField.getText(), filenameChooserField.getPathname());
    }
}
