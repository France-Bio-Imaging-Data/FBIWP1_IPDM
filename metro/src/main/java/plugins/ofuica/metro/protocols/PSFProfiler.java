/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.imagej.ImageJUtil;
import icy.plugin.abstract_.Plugin;
import ij.ImagePlus;
import metroloJ.resolution.PSFprofiler;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarDoubleArrayNative;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;

/**
 * PSFProfiler Block.
 * 
 * @author osvaldo
 */

public final class PSFProfiler extends Plugin implements Block {
   
   private VarSequence sequence_ = new VarSequence("sequence", null);
   private VarDoubleArrayNative resolutions_ = new VarDoubleArrayNative("resolutions", null);
   private VarString units_ = new VarString("units", null);
   private VarString xParams_ = new VarString("XParams", null);
   private VarString yParams_ = new VarString("YParams", null);
   private VarString zParams_ = new VarString("ZParams", null);
   
   private VarSequence xProfile = new VarSequence("xProfile", null);
   private VarSequence yProfile = new VarSequence("yProfile", null);
   private VarSequence zProfile = new VarSequence("zProfile", null);
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(sequence_.getName(), sequence_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(resolutions_.getName(), resolutions_);
      outputMap.add(units_.getName(), units_);
      outputMap.add(xParams_.getName(), xParams_);
      outputMap.add(yParams_.getName(), yParams_);
      outputMap.add(zParams_.getName(), zParams_);
      outputMap.add(xProfile.getName(), xProfile);
      outputMap.add(yProfile.getName(), yProfile);
      outputMap.add(zProfile.getName(), zProfile);
   }
   
   private void validateInputVariables() {
      if ( sequence_.getValue() == null )
         throw new VarException(sequence_, "sequence should be not null.");
   }
   
   @Override
   public void run() {
      
      validateInputVariables();
      final ImagePlus ip = ImageJUtil.convertToImageJImage(sequence_.getValue(), null);
      final PSFprofiler profiler = new PSFprofiler(ip);
      
      resolutions_.setValue(profiler.getResolutions());
      units_.setValue(profiler.getUnit());
      
      xParams_.setValue(profiler.getXParams());
      yParams_.setValue(profiler.getYParams());
      zParams_.setValue(profiler.getZParams());
      
      xProfile.setValue(ImageJUtil.convertToIcySequence(profiler.getXplot().getImagePlus(), null));
      yProfile.setValue(ImageJUtil.convertToIcySequence(profiler.getYplot().getImagePlus(), null));
      zProfile.setValue(ImageJUtil.convertToIcySequence(profiler.getZplot().getImagePlus(), null));
   }
}
