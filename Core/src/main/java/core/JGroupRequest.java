package core;

import java.io.Serializable;

public class JGroupRequest implements Serializable {
    protected RequestType type;
    protected long timestamp;

    public enum RequestType {
        UploadFile,
        RegisterPool
    }
    public JGroupRequest() {
        this.timestamp = System.currentTimeMillis() / 1000L;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }
}
