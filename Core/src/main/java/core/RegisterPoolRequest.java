package core;

public class RegisterPoolRequest extends JGroupRequest {
   public RegisterPoolRequest(String address) {
      this.address = address;
      this.type = RequestType.RegisterPool;
   }
}
