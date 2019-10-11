/**
 * 
 */
package plugins.ofuica.metro.chart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import icy.sequence.Sequence;
import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.adufour.vars.lang.Var;

/**
 * Defines the view of chart in time compatible an used in protocols.
 * 
 * @author osvaldo
 */

public final class TimeChartViewer extends SwingVarEditor<Object[][]> {
   
   public TimeChartViewer(Var<Object[][]> variable) {
      super(variable);
   }
   
   @Override
   protected JComponent createEditorComponent() {
      
      final JFreeChart chart = ChartFactory.createTimeSeriesChart(
          "TimeSeries Chart", "Date", "Value", null, true, true, false);
      
      chart.setBackgroundPaint(new Color(0.0F, 0.0F, 0.0F, 0.0F));
      
      final ChartPanel panel = new ChartPanel(chart);
      final XYPlot plot = chart.getXYPlot();
      
      plot.getRangeAxis(0).setAutoRange(true);
      plot.getDomainAxis(0).setAutoRange(true);
      plot.getRangeAxis(0).setAxisLineVisible(true);
      plot.getDomainAxis(0).setAxisLineVisible(false);
      plot.setDomainCrosshairPaint(Color.red);
      plot.setRangeCrosshairPaint(Color.red);
      plot.setDomainCrosshairVisible(true);
      plot.setDomainCrosshairLockedOnData(false);
      plot.setRangeCrosshairVisible(true);
      plot.setRangeCrosshairLockedOnData(false);
      
      final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
      renderer.setBaseShapesVisible(true);
      renderer.setBaseShapesFilled(true);
      
      final DateAxis axis = (DateAxis) plot.getDomainAxis();
      axis.setDateFormatOverride(new SimpleDateFormat("dd-MM-yyyy"));
      axis.setAutoTickUnitSelection(false);
      axis.setVerticalTickLabels(true);
      return panel;
   }
   
   @Override
   public JComponent getEditorComponent() {
      return (ChartPanel) super.getEditorComponent();
   }
   
   @Override
   protected void activateListeners() {
   }
   
   @Override
   protected void deactivateListeners() {
   }
   
   @Override
   protected void updateInterfaceValue() {
      final JFreeChart chart = ((ChartPanel) getEditorComponent()).getChart();
      final TimeSeriesCollection dataset = new TimeSeriesCollection();
      
      if ( !(variable.getValue() instanceof Object[][][]) )
         return;
      
      final Object[][][] arr = (Object[][][]) variable.getValue();
      
      if ( arr[0].length < 2 )
         return;
      
      final TimeSeries series1 = new TimeSeries("Series1");
      
      for (int i = 0 ; i < arr[0][0].length ; i++ ) {
         final Object obj0 = arr[0][0][i];
         final Object obj1 = arr[0][1][i];
         
         if ( obj0 instanceof Date || obj1 instanceof Double ) {
            final Date date = (Date) obj0;
            series1.addOrUpdate(new Day(date.getDate(), date.getMonth(), date.getYear()+1900), (Double) obj1);
         }
      }
      
      dataset.addSeries(series1);
      chart.getXYPlot().setDataset(dataset);
   }
   
   public Sequence getChartAsSequence() {
      final JFreeChart chart = ((ChartPanel) getEditorComponent()).getChart();
      final int width = 800;
      final int height = 600;
      final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g2 = img.createGraphics(); 
      chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));
      g2.dispose();
      final Sequence sequence = new Sequence();
      sequence.setImage(0, 0, img);
      return sequence;
   }
   
   @Override
   public double getComponentHorizontalResizeFactor() {
      return 1.0;
   }
   
   @Override
   public double getComponentVerticalResizeFactor() {
      return 1.0;
   }
}
