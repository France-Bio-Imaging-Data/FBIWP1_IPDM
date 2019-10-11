/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import icy.common.exception.UnsupportedFormatException;
import icy.sequence.MetaDataUtil;
import icy.sequence.Sequence;
import icy.sequence.SequenceIdImporter;
import loci.formats.ome.OMEXMLMetadataImpl;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarIntegerArrayNative;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.icy.Loader3;
import plugins.ofuica.metro.omero.OmeroImporter;
import plugins.ofuica.metro.openimadis.OpenImadisClient;
import plugins.ofuica.metro.openimadis.OpenImadisImporter;

/**
 * The idea of this plugin loop is been able to process big-images using tiles.
 * 
 * @author osvaldo
 */

public final class MetroTilesLoop extends Loop {
   
   private VarSession session_;
   private VarLong guid_;
   private VarIntegerArrayNative tilesDimensions_;
   private VarSequence tile_;
   private VarInteger originX_;
   private VarInteger originY_;
   
   /* private private data */
   private SequenceIdImporter importer_ = null;
   private Long numberOfTiles_ = 0L;
   private long nrows_ = 0;
   private long ncols_ = 0;
   
   public MetroTilesLoop() {
      super();
   }
   
   @Override
   public void initializeLoop() {
      super.initializeLoop();
      // TODO:
      // initialize the importer!.
      // get the metadata not the full image.
      // organize the tiles structure (define the number of iterations)

      final Session session = session_.getValue();
      if ( session == null ) {
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      }
      
      if ( tilesDimensions_.getValue().length != 2 ) {
         throw new VarException(tilesDimensions_, "Dimensions of tiles array should be size equal to 2. (x and y) ");
      }
      
      final Client client = session.getClient();
      importer_ = (client instanceof OpenImadisClient) ? OpenImadisImporter.create(client):OmeroImporter.create(client);
      
      try {
         if ( importer_ == null ) 
            throw new VarException(tile_, "Error I could not get a sequence.");
         
         boolean opened = importer_.open(guid_.getValueAsString(), 0);
         int serie = 0;
         
         if (opened) {
            final OMEXMLMetadataImpl meta = importer_.getMetaData();
            long w = MetaDataUtil.getSizeX(meta, serie);
            long h = MetaDataUtil.getSizeY(meta, serie);
            ncols_ = w/tilesDimensions_.getValue()[0] + ((w%tilesDimensions_.getValue()[0] > 0) ? 1 : 0);
            nrows_ = h/tilesDimensions_.getValue()[1] + ((h%tilesDimensions_.getValue()[1] > 0) ? 1 : 0);
            numberOfTiles_ = ncols_ * nrows_;
         }
      } catch (UnsupportedFormatException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   @Override
   public void beforeIteration() {
      super.beforeIteration();
      final int cnt = getIterationCounter().getValue().intValue();
      int tx = tilesDimensions_.getValue()[0];
      int ty = tilesDimensions_.getValue()[1];
      
      int x = (int) (cnt%ncols_);
      int y = (int) (cnt/nrows_);
      
      originX_.setValue(x*tx);
      originY_.setValue(y*ty);
      
      // System.out.println("Iteration: " + cnt + " x: " + x*tx + ", y:" +y*ty + ", tx: " + tx + ", ty:" + ty);
      
      try {
         int serie = 0;
         final Sequence sequence = Loader3.loadSequence(importer_, serie, new Rectangle(x*tx, y*ty, tx, ty));
         tile_.setValue(sequence);
         
      } catch (UnsupportedFormatException | IOException e) {
         e.printStackTrace();
      }
   }
   
   @Override
   public void afterIteration() {
      System.gc();
   }
   
   @Override
   public boolean isStopConditionReached() {
      boolean stop = (getIterationCounter().getValue().longValue() >= numberOfTiles_.longValue());
      
      if (stop) {
         try {
            importer_.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      
      return stop;
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      super.declareInput(inputMap);
      inputMap.add("guid", guid_ = new VarLong("guid", 0));
      inputMap.add("session", session_ = new VarSession("session", null));
      inputMap.add("tiles dimensions", tilesDimensions_ = new VarIntegerArrayNative("tiles dimensions", null));
      inputMap.add("origin X", originX_ = new VarInteger("origin X", 0));
      inputMap.add("origin Y", originY_ = new VarInteger("origin Y", 0));
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      super.declareOutput(outputMap);
      outputMap.add("tiled sequence", tile_ = new VarSequence("tiled sequence", null));
   }
   
   @Override
   public void declareLoopVariables(List<Var<?>> loopVariables) {
      super.declareLoopVariables(loopVariables);
      loopVariables.add(guid_);
      loopVariables.add(session_);
      loopVariables.add(tilesDimensions_);
      loopVariables.add(originX_);
      loopVariables.add(originY_);
      loopVariables.add(tile_);
   }
}
