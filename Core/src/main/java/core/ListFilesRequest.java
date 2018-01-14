package core;

public class ListFilesRequest extends JGroupRequest {

    public ListFilesRequest(String address) {
        this.address = address;
        this.type = RequestType.ListFiles;
    }
}
