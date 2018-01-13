package core;

public class UploadFileRequest extends JGroupRequest {
    private String fileName;

    public UploadFileRequest(String id, String fileName, String address) {
        this.id = id;
        this.fileName = fileName;
        this.type = RequestType.UploadFile;
        this.address = address;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
