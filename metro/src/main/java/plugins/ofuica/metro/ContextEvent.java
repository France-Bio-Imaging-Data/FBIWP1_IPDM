/**
 * 
 */
package plugins.ofuica.metro;

/**
 * It class defines the events that happen in a context.
 * 
 * @author osvaldo
 */

public class ContextEvent {
   
   public enum EventType {
       CREATED_SESSION, DESTROY_SESSION, UNKNOWN
   }
   
   private EventType type_ = EventType.UNKNOWN;
   
   public ContextEvent(EventType type) {
      type_ = type;
   }
   
   public EventType type() {
      return type_;
   }
}

