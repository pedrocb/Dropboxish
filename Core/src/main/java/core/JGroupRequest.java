package core;

import java.io.Serializable;
import java.util.UUID;

public class JGroupRequest implements Serializable {
    protected RequestType type;
    protected String address;
    protected String id;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
