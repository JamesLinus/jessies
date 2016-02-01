package e.edit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
The ETextArea redo action.
*/
public class RedoAction extends ETextAction {
    public static final String ACTION_NAME = "Redo";

    public RedoAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            ETextArea target = getTextArea();
            target.setCaretPosition(target.getCaretPosition()); // Workaround for Java Bug Parade #4688560.
            target.getUndoManager().redo();
        } catch (CannotUndoException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isEnabled() {
        return super.isEnabled() && getTextArea().getUndoManager().canRedo();
    }
}
