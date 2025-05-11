package ivory.host.util.server;

import lombok.Data;

import java.awt.*;

@Data
public class TextSegment {

    String text;
    Color color;
    boolean bold;
    boolean italic;

    public TextSegment(String text, Color color, boolean bold, boolean italic) {
        this.text = text;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
    }
}
