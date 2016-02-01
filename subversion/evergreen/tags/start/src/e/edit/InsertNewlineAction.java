package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import e.util.*;

/**
The ETextArea action that inserts a newline and performs auto-indentation.
*/
public class InsertNewlineAction extends TextAction {
    public static final String ACTION_NAME = "insert-newline-and-auto-indent";

    public InsertNewlineAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        CompoundEdit entireEdit = new CompoundEdit();
        target.getUndoManager().addEdit(entireEdit);
        try {
            target.replaceSelection("\n");
            target.autoIndent();
        } finally {
            entireEdit.end();
        }
    }
}
