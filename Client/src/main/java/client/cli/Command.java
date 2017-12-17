package client.cli;

import javax.ws.rs.client.WebTarget;

public interface Command {
    void run(WebTarget target);
}
