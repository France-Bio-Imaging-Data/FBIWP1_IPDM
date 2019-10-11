/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.io.IOException;

import icy.common.exception.UnsupportedFormatException;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import icy.sequence.SequenceIdImporter;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarListener;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.icy.Loader3;
import plugins.ofuica.metro.omero.OmeroClient;
import plugins.ofuica.metro.omero.OmeroImporter;
import plugins.ofuica.metro.openimadis.OpenImadisClient;
import plugins.ofuica.metro.openimadis.OpenImadisImporter;

/**
 * Retrieves a sequence stored in the server side and represent in Icy's as sequence.
 * 
 * @author osvaldo
 */

public final class MetroGetSequence extends Plugin implements Block {
   
   private VarSession session_ = new VarSession("session", null);
   private VarLong id_;
   private VarSequence sequence_;
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      inputMap.add("id", id_ = new VarLong("id", 0));
      
      session_.addListener(new VarListener<Session> () {
         @Override
         public void valueChanged(Var<Session> source, Session oldValue, Session newValue) {
         }
         
         @Override
         public void referenceChanged(Var<Session> source, Var<? extends Session> oldReference,
               Var<? extends Session> newReference) {
         }
      });
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add("sequence" , sequence_ = new VarSequence("sequence", null));
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null )
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      
      if ( id_.getValue().longValue() <= 0 ) 
         throw new VarException(id_, "Image id should be major than 0");
   }
   
   @Override
   public void run() {
      
      validateInputVariables();
      final Session session = session_.getValue();
      final Client client = session.getClient();
      
      if ( client == null )
         throw new VarException(session_, "I could not find a client in the selected session.");
      
      SequenceIdImporter importer = null;
      
      if ( client instanceof OpenImadisClient )
         importer = OpenImadisImporter.create(client);
      
      else if ( client instanceof OmeroClient )
         importer = OmeroImporter.create(client);
      
      else {
      }
      
      try {
         if ( importer == null )
            throw new VarException(sequence_, "Error I could not get a sequence.");
         
         boolean opened = importer.open(id_.getValueAsString(), 0);
         
         if (opened) {
            final Sequence sequence =  Loader3.loadSequence(importer);
            
            if ( sequence != null )
               sequence_.setValue(sequence);
            else
               throw new VarException(sequence_, "Error I could not get a sequence");
         }
      } catch (UnsupportedFormatException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
