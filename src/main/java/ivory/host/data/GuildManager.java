package ivory.host.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuildManager {

    Long guildID;

    String name;
    String ip;
    int port;

    boolean mcCommandEnabled;
    boolean bedrockCommandEnabled;

    boolean fakePortEnabled;
    boolean hidePort;

    public GuildManager(Long guildID, String name, String ip, int port) {
        this.guildID = guildID;
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.mcCommandEnabled = true;
        this.bedrockCommandEnabled = false;
        this.fakePortEnabled = false;
        this.hidePort = false;
    }

}
