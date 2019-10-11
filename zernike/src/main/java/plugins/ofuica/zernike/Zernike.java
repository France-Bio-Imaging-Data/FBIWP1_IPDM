/**
 * 
 */
package plugins.ofuica.zernike;

import java.awt.Dimension;

import javax.swing.JComponent;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import icy.imagej.ImageJUtil;
import icy.main.Icy;
import icy.sequence.Sequence;
import ij.ImagePlus;
import ij.ImageStack;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarChannel;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.gui.swing.WorkbookEditor;
import plugins.adufour.vars.lang.VarWorkbook;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.workbooks.Workbooks;
import plugins.ofuica.internals.calculate.MomentCalculator;
import plugins.ofuica.internals.calculate.PolynomialCalculator;
import plugins.ofuica.internals.dataTypes.CartCoord;
import plugins.ofuica.internals.utils.Mapper;
import plugins.ofuica.internals.utils.ZernikeUtil;

/**
 * @author osvaldo
 */

public class Zernike extends EzPlug implements Block {
   
   public enum MapperType { INTERNAL, EXTERNAL }
   
   private EzVarSequence sequence_ = new EzVarSequence("Sequence");
   private EzVarChannel channel_ = new EzVarChannel("Channel", sequence_.getVariable(), false);
   private EzVarInteger maxOrder_ = new EzVarInteger("Max order", 45, 1, Integer.MAX_VALUE, 1);
   private EzVarEnum<MapperType> mapper_  = new EzVarEnum<>("mapper", MapperType.values(), MapperType.INTERNAL);
   private EzVarBoolean estimatePsf_ = new EzVarBoolean("Estimate PSF", false);
   private EzVarSequence output_ = new EzVarSequence("output");
   private final VarWorkbook book_ = new VarWorkbook("Workbook", (Workbook) null);
   
   @Override
   protected void initialize() { 
      getUI().setActionPanelVisible(true);
      
      addEzComponent(sequence_);
      addEzComponent(channel_);
      addEzComponent(maxOrder_);
      addEzComponent(mapper_);
      addEzComponent(estimatePsf_);
      
      if ( book_.getValue() == null ) {
         final Workbook wb = Workbooks.createEmptyWorkbook();
         book_.setValue(wb);
      }
      
      final WorkbookEditor viewer = new WorkbookEditor(book_);
      viewer.setReadOnly(true);
      viewer.setEnabled(true);
      viewer.setFirstRowAsHeader(true);
      viewer.setOpenButtonVisible(false);
      viewer.setFirstRowAsHeader(true);
      JComponent jc = viewer.getEditorComponent();
      jc.setPreferredSize(new Dimension(400, 300));
      addComponent(jc);
   }
   
   void checkAllInputs() {
      if ( sequence_.getValue() == null ) {
         throw new VarException(sequence_.getVariable(), "Sequence is null.");
      }
   }
   
//   @Override
//   Because the time... we could forget this portion of code ?
//   protected void execute__() {
//      checkAllInputs();
//      int maxOrder = maxOrder_.getValue();
//      final Sequence seq = sequence_.getValue();
//      final CartCoord center = ZernikeUtil.getMassCenter(seq.getImage(0, (seq.getSizeZ()/2)+1), channel_.getValue());
//      final PolynomialCalculator poly = new PolynomialCalculator(seq.getWidth(), seq.getHeight(), center, 
//            Mapper.INTERNAL_MAPPING);
//      final TreeMap<MNPair<Integer>, IcyBufferedImage> sRpq = new TreeMap<MNPair<Integer>, IcyBufferedImage>();
//      final TreeMap<MNPair<Integer>, IcyBufferedImage> sVpq = new TreeMap<MNPair<Integer>, IcyBufferedImage>();
//      double cnt = poly.getVRpqc(sRpq, sVpq, maxOrder);
//      final VolumetricImage mims = ZernikeUtil.getMask(seq, ZernikeUtil.errode1p(sVpq.get(new MNPair<>(0,0,0)), 0.0));
//      final Sequence pool1 = new Sequence("mim");
//      pool1.addVolumetricImage(0, mims);
//      Icy.getMainInterface().addSequence(pool1);
//      final MomentCalculator mc = new MomentCalculator();
//      double[][] moments = mc.getPolynomsc(mims, sVpq, cnt);
//      System.out.println("Moments calculated....");
//      Sequence res = mc.invzerseZernikePO(moments, sRpq, sVpq, maxOrder);
//      System.out.println("estimate calculated....");
//      System.out.println("mass center: " + center);
//      System.out.println("cnt: " + cnt);
//   }
   
   protected void execute() {
      checkAllInputs();
      
      int maxOrder = maxOrder_.getValue();
      final Sequence seq = sequence_.getValue();
      final ImagePlus jimg = ImageJUtil.convertToImageJImage(seq, null);
      final ImageStack ims = jimg.getStack();
      final CartCoord center = new CartCoord(ZernikeUtil.getMassCenter(ims.getProcessor((ims.getSize()/ 2)+1)));
      int mapping = (mapper_.getValue() == MapperType.INTERNAL) ? Mapper.INTERNAL_MAPPING : Mapper.EXTERNAL_MAPPING;
      final PolynomialCalculator poly = new PolynomialCalculator(ims.getWidth(), ims.getHeight(),center, mapping);
      
      ImageStack sRpq = new ImageStack(ims.getWidth(), ims.getHeight());
      ImageStack sVpq = new ImageStack(ims.getWidth(), ims.getHeight());
      double cnt = poly.getVRpqc(sRpq, sVpq, maxOrder);
      
      final ImageStack mims = ZernikeUtil.getMask(ims, ZernikeUtil.errode1p(sVpq.getProcessor(1), 0.0));
      MomentCalculator mc = new MomentCalculator();
      
      double[][] moments = mc.getPolynomsc(mims, sVpq, cnt);
      int n_planes = moments.length;
      
      // generating workbook.
      final Workbook wb = Workbooks.createEmptyWorkbook();
      book_.setValue(wb);
      final Sheet sheet = book_.getValue().createSheet("zernike moments");
      final Row header = sheet.createRow(0);
      header.getCell(0).setCellValue("plan");
      
      for ( int i = 0 ; i <= maxOrder; i++ ) {
         String str = "m_" + i;
         header.getCell(i+1).setCellValue(str);
      }
      
      for (int k = 0 ; k < n_planes ; k++) {
         final Row row = sheet.createRow(k+1);
         
         for ( int m = 0 ; m <= maxOrder+1 ; m++) {
            if ( m == 0 )
               row.createCell(m).setCellValue(k);
            else 
               row.createCell(m).setCellValue(moments[k][m-1]);
         }
      }
      
      if ( estimatePsf_.getValue() ) {
         ImageStack res = mc.invzerseZernikePO(moments, sRpq, sVpq, maxOrder);
         ImagePlus imp = new ImagePlus("PSF Estimated", res);
         Sequence result = ImageJUtil.convertToIcySequence(imp, null);
         Icy.getMainInterface().addSequence(result);
         output_.setValue(result);
      }
   }
   
   @Override
   public void clean() {
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(sequence_.getVariable().getName(), sequence_.getVariable());
      inputMap.add(channel_.getVariable().getName(), channel_.getVariable());
      inputMap.add(maxOrder_.getVariable().getName(), maxOrder_.getVariable());
      inputMap.add(mapper_.getVariable().getName(), mapper_.getVariable());
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(output_.getVariable().getName(), output_.getVariable());
   }
}
