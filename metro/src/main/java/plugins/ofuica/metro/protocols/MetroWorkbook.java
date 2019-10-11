/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarDoubleArrayNative;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.lang.VarWorkbook;

/**
 * @author osvaldo
 */

public final class MetroWorkbook extends Plugin implements Block {
   
   private VarWorkbook workbook_ = new VarWorkbook("Workbook", (Workbook) null);
   private VarString field_ = new VarString("field", null);
   private VarDoubleArrayNative output_ = new VarDoubleArrayNative("output", null);
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(workbook_.getName(), workbook_);
      inputMap.add(field_.getName(), field_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(output_.getName(), output_);
   }
   
   @Override
   public void run() {
      final String field = field_.getValue();
      
      final Workbook book = workbook_.getValue();
      final Sheet sheet = book.getSheet("moments");
      double[] arr = new double[sheet.getPhysicalNumberOfRows()-1];
      
      final Row row = sheet.getRow(0);
      int selectedCol = -1;
      
      final Iterator<Cell> iter = row.cellIterator();
      
      int i = 0;
      while(iter.hasNext()) {
         if ( iter.next().getStringCellValue().equals(field) ) {
            selectedCol = i;
            break;
         }
         i++;
      }
      
      for ( int x = 0 ; x < sheet.getPhysicalNumberOfRows()-1; x++ ) {
         final Row r = sheet.getRow(x+1);
         arr[x] = r.getCell(selectedCol).getNumericCellValue();
      }
      
      output_.setValue(arr);
   }
}
