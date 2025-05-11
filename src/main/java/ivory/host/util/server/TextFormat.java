package ivory.host.util.server;

import lombok.Data;

import java.awt.*;

@Data
public class TextFormat {

    Color color;
    boolean bold;
    boolean italic;

    public TextFormat() {
        color = Color.WHITE;
        bold = false;
        italic = false;
    }

    public TextFormat(TextFormat other) {
        this.color = other.color;
        this.bold = other.bold;
        this.italic = other.italic;
    }
}
