package core;

public class RegisterPoolRequest extends JGroupRequest {
   private String address;

   public RegisterPoolRequest(String address) {
      this.address = address;
      this.type = RequestType.RegisterPool;
   }

   public String getAddress() {
      return address;
   }
}
