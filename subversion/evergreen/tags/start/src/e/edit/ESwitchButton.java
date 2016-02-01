package e.gui;

import java.awt.*;
import java.awt.event.*;

import e.edit.*;

public class ESwitchButton extends EButton implements ActionListener {
    private ETextWindow window;
    
    public ESwitchButton(ETextWindow window) {
        super("");
        this.window = window;
        addActionListener(this);
    }

    public void paint(Graphics g) {
        g.setColor(getGlyphColor());
        paintGlyph(g, 5, 5);
    }

    public Color getGlyphColor() {
        return pressed ? Color.GRAY : Color.BLACK;
    }

    public void paintGlyph(Graphics g, int x, int y) {
        final int d = 2;
        
        g.fillRect(x, y + 1 + d, 6*d, d);

        // Top left
        g.fillRect(x + 1, y + 2, d, d);
        g.fillRect(x + 2, y + 1, d, d);

        // Top right
        g.fillRect(x + 9, y + 2, d, d);
        g.fillRect(x + 8, y + 1, d, d);

        // Bottom left
        g.fillRect(x + 2, y + 1 + 2*d, d, d);
        g.fillRect(x + 1, y + 2*d, d, d);

        // Bottom right
        g.fillRect(x + 8, y + 1 + 2*d, d, d);
        g.fillRect(x + 9, y + 2*d, d, d);
    }
    
    public void actionPerformed(ActionEvent e) {
        window.switchToCounterpart();
    }
}
