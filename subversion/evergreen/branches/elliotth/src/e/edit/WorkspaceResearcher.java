package e.edit;

import java.awt.*;
import javax.swing.text.html.*;

public interface WorkspaceResearcher {
    /**
    * Look for something in a JTextComponent. Returns an HTML string
    * containing information about what it found. Should return
    * the empty string (not null) if it has nothing to say. The string passed
    * to the researcher is the selection, if there is one, or the line up to the
    * caret otherwise.
    */
    public String research(javax.swing.text.JTextComponent component, String string);
    
    /**
    * Invoked before research -- if we're researching an ETextWindow -- to
    * give the Researcher an opportunity to decline. This lets a Java researcher,
    * for example, not tell you about Java's Array class when you're working on
    * a Ruby program.
    */
    public boolean isSuitable(ETextWindow textWindow);
}
