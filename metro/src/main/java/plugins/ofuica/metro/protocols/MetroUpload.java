/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * MetroUpload Block.
 * 
 * Allows you to storage a sequence from Icy's environment to server side.
 * 
 * @author osvaldo
 */

public final class MetroUpload extends Plugin implements Block {
   
   private VarSession session_ = new VarSession("session", null);
   private VarSequence sequence_ = new VarSequence("sequence" , null);
   private VarBoolean idrefType_ = new VarBoolean("id ref project/dataset", false);
   private VarLong idref_ = new VarLong("id reference", 0);
   private VarLong id_ = new VarLong("id", 0);
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      inputMap.add(sequence_.getName(), sequence_);
      inputMap.add(idrefType_.getName(), idrefType_);
      inputMap.add(idref_.getName(), idref_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(id_.getName(), id_);
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null )
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      
      if ( sequence_.getValue() == null )
         throw new VarException(sequence_, "Sequence should be not null.");
   }
   
   @Override
   public void run() {
      
      validateInputVariables();
      try {
         
         final Session session = session_.getValue();
         final Client client = session.getClient();
         Long id = -1L;
         
         if ( idrefType_.getValue() )
            id = client.uploadSequence(idref_.getValue().toString(), sequence_.getValue());
         
         else
            id = client.uploadSequence(idref_.getValue(), sequence_.getValue());
         
         id_.setValue(id);
         
      } catch (ClientException e) {
         e.printStackTrace();
      }
   }
}
