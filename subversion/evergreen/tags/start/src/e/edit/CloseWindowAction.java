package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import e.gui.*;

/**
The ETextArea close window action.
*/
public class CloseWindowAction extends ETextAction {
    public static final String ACTION_NAME = "Close";

    public CloseWindowAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        window.closeWindow();
    }

    public boolean isEnabled() {
        return (getFocusedComponent() != null);
    }
}
