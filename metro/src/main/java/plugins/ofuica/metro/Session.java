/**
 * 
 */
package plugins.ofuica.metro;

import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarListener;
import plugins.ofuica.metro.client.Client;

/**
 * It class defines a session.
 * 
 * One session can have only one client and only one status.
 * 
 * If you need manage more than one client, you should create a lots of sessions.
 * 
 * @author osvaldo
 */

public class Session {
   
   private String id_;
   private VarString status_ = new VarString("status", "");
   private Client client_ = null;
   
   public Session() {
      this("no_named_session");
   }
   
   public Session(String id) {
      id_ = id;
   }
   
   public void AddActionListener(VarListener<String> listener) {
      status_.addListener(listener);
   }
   
   public String getId() {
      return id_;
   }
   
   public VarString getStatus() {
      return status_;
   }
   
   public Client getClient() {
      return client_;
   }
   
   public void setClient(Client client) {
      client_ = client;
   }
   
   public void setId(String newValue) {
      id_ = newValue;
   }
}
