package core;

public class DownloadFileRequest extends JGroupRequest {
    private String address;
    private String fileName;

    public DownloadFileRequest(String fileName, String address) {
        this.address = address;
        this.type = RequestType.DownloadFile;
        this.fileName = fileName;
    }

    public String getAddress() {
        return address;
    }

    public String getFileName() {
        return fileName;
    }
}
