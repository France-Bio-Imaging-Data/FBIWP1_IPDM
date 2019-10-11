/**
 * 
 */
package plugins.ofuica.metro.protocols;

import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.util.VarListener;
import plugins.ofuica.metro.client.Client;

/**
 * @author osvaldo
 */

public final class VarClient extends Var<Client> {
   
   public VarClient(String name, Client defaultValue) {
      this(name, defaultValue, null);
   }
   
   public VarClient(String name, Client defaultValue, VarListener<Client> defaultListener) {
      super(name, defaultValue, defaultListener);
   }
}
