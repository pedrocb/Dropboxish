package core;

public class UploadFileRequest extends JGroupRequest {
    private String fileName;

    public UploadFileRequest(String fileName) {
        this.fileName = fileName;
        this.type = RequestType.UploadFile;
    }
}
