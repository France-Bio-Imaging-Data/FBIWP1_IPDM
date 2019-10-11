/**
 * 
 */
package plugins.ofuica.metro;

import java.util.EventListener;

/**
 * @author osvaldo
 */

public interface ContextListener extends EventListener {
   
   public void contextChanged(Context context, ContextEvent e);
}
