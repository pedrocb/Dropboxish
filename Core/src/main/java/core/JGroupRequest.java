package core;

import java.io.Serializable;

public class JGroupRequest implements Serializable {
    protected RequestType type;

    public enum RequestType {
        UploadFile,
        RegisterPool
    }
    public JGroupRequest() {

    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }
}
