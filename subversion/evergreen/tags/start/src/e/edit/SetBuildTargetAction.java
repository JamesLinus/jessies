package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import e.forms.*;
import e.gui.*;
import e.util.*;

/**
The ETextArea action to open file properties dialog where you can view
or alter the end of line string, indent string, and other stuff as we
think of it. (File encoding would be one possibility.)
*/
public class SetBuildTargetAction extends ETextAction {
    public static final String ACTION_NAME = "Set Build Target...";
    
    private JTextField buildTargetField = new JTextField("", 40);
    
    public SetBuildTargetAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        buildTargetField.setText(Parameters.getParameter("make.target"));
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Build Target:", buildTargetField);
        boolean okay = FormDialog.show(Edit.getFrame(), "Build Properties", formPanel);
        
        if (okay == false) {
            return;
        }
        
        System.setProperty("make.target", buildTargetField.getText());
    }
}
