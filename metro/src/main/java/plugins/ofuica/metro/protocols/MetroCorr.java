/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.math.ArrayMath;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarDouble;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

/**
 * This block is a proxy to access to correlation calculations available in Icy kernel.
 * 
 * @author osvaldo
 */
public final class MetroCorr extends Plugin implements Block {
   
   private VarSequence input1_ = new VarSequence("input 1", null);
   private VarSequence input2_ = new VarSequence("input 2", null);
   private VarDouble rPearson_ = new VarDouble("r pearson", 0);
   private VarDouble correlation_ = new VarDouble("correlation", 0);
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(input1_.getName(), input1_);
      inputMap.add(input2_.getName(), input2_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(rPearson_.getName(), rPearson_);
      outputMap.add(correlation_.getName(), correlation_);
   }
   
   private void validateInputVariables() {
      if ( input1_.getValue() == null)
         throw new VarException(input1_, "You should asociate input1 with a sequence.");
      
      if ( input2_.getValue() == null)
         throw new VarException(input2_, "You should asociate input2 with a sequence.");
   }
   
   @Override
   public void run() {
      validateInputVariables();
      
      try {
         double rPearson = ArrayMath.correlationPearson(input1_.getValue().getFirstImage().getDataXYAsDouble(0),
               input2_.getValue().getFirstImage().getDataXYAsDouble(0));
         
         double corr = ArrayMath.correlation(input1_.getValue().getFirstImage().getDataXYAsDouble(0),
               input2_.getValue().getFirstImage().getDataXYAsDouble(0));
         
         rPearson_.setValue(rPearson);
         correlation_.setValue(corr);
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }
}
