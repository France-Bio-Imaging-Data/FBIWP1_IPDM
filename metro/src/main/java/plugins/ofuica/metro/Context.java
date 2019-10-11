/**
 * 
 */
package plugins.ofuica.metro;

import java.util.ArrayList;
import java.util.List;

/**
 * The application is using just one main context in general.
 * But it is also possible be using multiples context if someone needs.
 * 
 * One context could have many sessions.
 * 
 * One context by work-flow (protocol)
 * 
 * @author osvaldo
 */

public class Context {
   private final String name_;
   private List<Session> sessions_ = new ArrayList<>();
   private final List<ContextListener> listeners_ = new ArrayList<>();
   
   public Context(String name) {
      name_ = name;
   }
   
   public Context() {
      this("unnamed");
   }
   
   public List<Session> getSessions() {
      
      final List<Session> ret = new ArrayList<>();
      
      for ( final Session session : sessions_ ) {
         ret.add(session);
      }
      
      return ret;
   }
   
   public String getName() {
      return name_;
   }
   
   public Session createSession(String id) {
      final Session ret = new Session(id);
      sessions_.add(ret);
      
      for ( ContextListener cl : listeners_ ) {
         cl.contextChanged(this, new ContextEvent(ContextEvent.EventType.CREATED_SESSION));
      }
      
      return ret;
   }
   
   public Session createSession() {
      final Session ret = new Session();
      sessions_.add(ret);
      
      for ( ContextListener cl : listeners_ ) {
         cl.contextChanged(this, new ContextEvent(ContextEvent.EventType.CREATED_SESSION));
      }
      
      return ret;
   }
   
   public void destroySession(String id) {
      Object obj = null;
      
      for ( final Session session : sessions_ ) {
         if ( session.getId().equals(id) ) {
            obj = session;
            break;
         }
      }
      
      if ( obj != null ) {
         sessions_.remove(obj);
      }
   }
   
   public void destroySession(Session session) {
      sessions_.remove(session);
   }
   
   public void addContextListener(ContextListener listener) {
      listeners_.add(listener);
   }
   
   public void removeContextListener(ContextListener listener) {
      listeners_.remove(listener);
   }
}
