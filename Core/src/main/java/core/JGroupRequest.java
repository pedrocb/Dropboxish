package core;

import java.io.Serializable;
import java.util.UUID;

public class JGroupRequest implements Serializable {
    protected RequestType type;
    protected long timestamp;
    protected String address;

    public enum RequestType {
        UploadFile,
        RegisterPool,
        DownloadFile,
        ListFiles,
        DeleteFile
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
