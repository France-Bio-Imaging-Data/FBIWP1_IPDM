/**
 * 
 */
package plugins.ofuica.metro.client;

/**
 * It class represents a client exception and holds information about the error from Exception (Throwable, etc).
 * 
 * @author osvaldo
 */

public class ClientException extends Exception {
   
   private static final long serialVersionUID = -7148784406259286114L;
   
   public ClientException(String message) {
      super(message);
   }
}
