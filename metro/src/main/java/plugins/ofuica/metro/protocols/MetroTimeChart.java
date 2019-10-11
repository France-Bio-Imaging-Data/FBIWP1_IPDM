/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarReferencingPolicy;
import plugins.ofuica.metro.chart.VarTimeChart;

/**
 * @author osvaldo
 */

public final class MetroTimeChart extends Plugin implements Block {
   private VarGenericArray<Object[]> timeArray_ = new VarGenericArray<>("time", Object[].class, new Object[1]);
   private VarGenericArray<Object[]> valueArray_ = new VarGenericArray<>("value", Object[].class, new Object[1]);
   private VarSequence figure_ = new VarSequence("figure", null);
   private VarTimeChart timeChart_ = new VarTimeChart("chart", null);
   
   public MetroTimeChart() {
      super();
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(timeArray_.getName(), timeArray_);
      inputMap.add(valueArray_.getName(), valueArray_);
      timeChart_.setReferencingPolicy(VarReferencingPolicy.NONE);
      inputMap.add(timeChart_.getName(), timeChart_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(figure_.getName(), figure_);
   }
   
   @Override
   public void run() {
      
      if ( timeArray_.size() != valueArray_.size() ) {
         System.out.println("passed data has not the same lenght");
         return;
      }
      
      Object[][] chartValues = {timeArray_.getValue(), valueArray_.getValue()};
      timeChart_.setValue(chartValues);
      figure_.setValue(timeChart_.getChartAsSequence());
   }
}
