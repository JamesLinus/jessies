package e.ptextarea;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A simple styler for patches.
 */
public class PPatchTextStyler extends PAbstractTextStyler {
    public PPatchTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    public List<PLineSegment> getTextSegments(int line) {
        int start = textArea.getLineStartOffset(line);
        int end = textArea.getLineEndOffsetBeforeTerminator(line);
        List<PLineSegment> result = new ArrayList<PLineSegment>();
        String lineText = textArea.getLineText(line);
        PStyle style = PStyle.NORMAL;
        if (lineText.startsWith("+")) {
            style = PStyle.PATCH_PLUS;
        } else if (lineText.startsWith("-")) {
            style = PStyle.PATCH_MINUS;
        }
        result.add(new PTextSegment(textArea, start, end, style));
        return result;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        // We have no language, so we have no keywords.
    }
}
