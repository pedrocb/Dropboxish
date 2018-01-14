package core;

public class DeleteFileRequest extends JGroupRequest{
    private String fileName;

    public DeleteFileRequest(String address, String fileName){
        this.address = address;
        this.type = RequestType.DeleteFile;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
