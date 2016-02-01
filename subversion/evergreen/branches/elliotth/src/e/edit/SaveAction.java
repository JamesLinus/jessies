package e.edit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
The ETextArea save action.
*/
public class SaveAction extends ETextAction {
    public static final String ACTION_NAME = "Save";

    public SaveAction() {
        super(ACTION_NAME);
    }
    
    public boolean isEnabled() {
        return super.isEnabled() && (getFocusedTextWindow() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        getFocusedTextWindow().save();
    }
}
