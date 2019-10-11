/**
 * 
 */
package plugins.ofuica.metro.chart;

import icy.sequence.Sequence;
import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.util.VarListener;

/**
 * It represents a variable of the time's chart.
 * 
 * @author osvaldo
 */

public class VarTimeChart extends VarGenericArray<Object[][]> {
   
   private VarEditor<Object[][]> editor_ = null;
   
   public VarTimeChart(String name, Object[][] defaultValue) {
      super(name, Object[][].class, defaultValue);
   }
   
   public VarTimeChart(String name, Class<Object[][]> type, Object[][] defaultValue) {
      super(name, type, defaultValue);
   }
   
   public VarTimeChart(String name, Class<Object[][]> type, Object[][] defaultValue,
         VarListener<Object[][]> defaultListener) {
      super(name, type, defaultValue, defaultListener);
   }
   
   @Override
   public VarEditor<Object[][]> createVarEditor() {
      editor_ = new TimeChartViewer(this);
      return editor_;
   }
   
   public Sequence getChartAsSequence() {
      if ( editor_ != null && editor_ instanceof TimeChartViewer ) {
         TimeChartViewer chart = (TimeChartViewer) editor_;
         return chart.getChartAsSequence();
      }
      return null;
   }
}
