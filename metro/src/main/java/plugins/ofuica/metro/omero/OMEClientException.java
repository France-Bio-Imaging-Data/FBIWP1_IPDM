/**
 * 
 */
package plugins.ofuica.metro.omero;

/**
 * @author osvaldo
 */

public class OMEClientException extends Exception {
   
   private static final long serialVersionUID = 5989860201456814671L;
   private final String message_;
   
   public OMEClientException(final String message) {
      this.message_ = message;
   }
   
   @Override
   public String getMessage() {
      return message_;
   }
}
