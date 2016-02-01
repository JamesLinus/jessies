package e.edit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
The ETextArea revert-to-saved action.
*/
public class RevertToSavedAction extends ETextAction {
    public static final String ACTION_NAME = "Revert to Saved";
    
    public RevertToSavedAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        getFocusedTextWindow().revertToSaved();
    }
    
    public boolean isEnabled() {
        if (super.isEnabled() == false) {
            return false;
        }
        
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            return false;
        }
        
        return textWindow.isDirty() || textWindow.isOutOfDateWithRespectToDisk();
    }
}
