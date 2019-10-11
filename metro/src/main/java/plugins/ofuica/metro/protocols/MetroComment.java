/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * It allows you to create a commentary associated with your Image in the server side.
 * The commentary is always one string.
 * 
 * @author osvaldo
 */

public final class MetroComment extends Plugin implements Block {
   
   private VarSession session_ = new VarSession("session", null);
   private VarLong id_ = new VarLong("id", 0);
   private VarString commentary_ = new VarString("commentary", "");
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      inputMap.add(id_.getName(), id_);
      inputMap.add(commentary_.getName(), commentary_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null )
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
   }
   
   @Override
   public void run() {
      validateInputVariables();
      try {
         final Session session = session_.getValue();
         final Client client = session.getClient();
         client.commentImage(id_.getValue(), commentary_.getValue());
      } catch (ClientException e) {
         e.printStackTrace();
      }
   }
}
