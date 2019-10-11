/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.io.IOException;
import java.util.List;

import icy.common.exception.UnsupportedFormatException;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.sequence.Sequence;
import icy.sequence.SequenceIdImporter;
import plugins.adufour.blocks.lang.Batch;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.icy.Loader3;
import plugins.ofuica.metro.omero.OmeroImporter;
import plugins.ofuica.metro.openimadis.OpenImadisClient;
import plugins.ofuica.metro.openimadis.OpenImadisImporter;

/**
 * Concrete Batch process.
 * 
 * Creates a ready to use Batch process.
 * 
 * @author osvaldo
 */

public final class MetroBatch extends Batch {
   
   private VarSession session_;
   private VarSequence sequence_;
   private VarLong id_;
   private SequenceIdImporter importer_ = null;
   
   public MetroBatch() {
      super();
   }
   
   private boolean checkConnection() {
      if ( session_.getValue() == null ) {
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      }
      
      return true;
   }
   
   @Override
   public void initializeLoop() {
      super.initializeLoop();
      
      if ( !checkConnection() ) {
         new FailedAnnounceFrame("not connected instance");
      }
   }
   
   @Override
   public void beforeIteration() {
      super.beforeIteration();
      
      final Long id = Long.parseLong(getBatchElement().getValueAsString());
      final Session session = session_.getValue();
      
      if ( session == null ) {
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      }
      
      final Client client = session.getClient();
      importer_ = (client instanceof OpenImadisClient) ? OpenImadisImporter.create(client) : OmeroImporter.create(client);
      
      try {
         if ( importer_ == null ) 
            throw new VarException(sequence_, "Error I could not get a sequence.");
         
         boolean opened = importer_.open(id.toString(), 0);
         id_.setValue(id);
         
         if (opened) {
            final Sequence sequence =  Loader3.loadSequence(importer_);
            
            if ( sequence != null ) {
               sequence_.setValue(sequence);
            }
            
            else {
               throw new VarException(sequence_, "Error I could not get a sequence");
            }
         }
      } catch (UnsupportedFormatException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   @Override
   public void afterIteration() {
      super.afterIteration();
      try {
         importer_.close();
         System.gc();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   @Override
   public void declareLoopVariables(List<Var<?>> loopVariables) {
      super.declareLoopVariables(loopVariables);
      loopVariables.add(session_);
      loopVariables.add(sequence_);
      loopVariables.add(id_);
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      super.declareInput(inputMap);
      inputMap.add("session", session_ = new VarSession("session", null) );
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      super.declareOutput(outputMap);
      outputMap.add("sequence", sequence_ = new VarSequence("sequence", null));
      outputMap.add("id", id_ = new VarLong("id", 0));
   }
}
