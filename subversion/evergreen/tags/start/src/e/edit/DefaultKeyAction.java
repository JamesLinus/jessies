package e.edit;

import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

public class DefaultKeyAction extends ETextAction {
    public DefaultKeyAction() {
        super("default-key-action-with-electric-key-support");
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e) {
        ETextArea target = getTextArea();
        if (target == null || e == null) {
            return;
        }
        
        if ((! target.isEditable()) || (! target.isEnabled())) {
            javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(target);
            return;
        }
        
        String content = e.getActionCommand();
        int mod = e.getModifiers();
        if (content == null || content.length() < 1 || ((mod & ActionEvent.ALT_MASK) != (mod & ActionEvent.CTRL_MASK))) {
            return;
        }
        
        char c = content.charAt(0);
        if ((c < 0x20) || (c == 0x7F)) {
            return;
        }
        
        if (target.getIndenter().isElectric(c)) {
            CompoundEdit entireEdit = new CompoundEdit();
            target.getUndoManager().addEdit(entireEdit);
            try {
                target.replaceSelection(content);
                
                // If the electric character is the only thing on the line, correct the line's indentation.
                //if (target.getLineText(target.getLineOfOffset(target.getCaretPosition())).trim().length() == 1) {
                    target.correctIndentation(false);
                //}
            //} catch (BadLocationException ex) {
                // Can't happen.
            } finally {
                entireEdit.end();
            }
        } else if (content != null) {
            target.replaceSelection(content);
        } else {
            javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(target);
        }
    }
}
