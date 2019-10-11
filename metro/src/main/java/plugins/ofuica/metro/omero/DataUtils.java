/**
 * 
 */
package plugins.ofuica.metro.omero;

import java.util.List;

import icy.type.DataType;
import omero.model.IObject;
import omero.model.PixelsTypeI;

/**
 * @author osvaldo
 */

public class DataUtils {
   
   public static PixelsTypeI getOMEDataTypeFromIcy(final DataType dataType,
         final List<IObject> supportedTypes) {
      
      for ( Object obj : supportedTypes ) {
         if ( obj instanceof PixelsTypeI ) {
            final PixelsTypeI type = (PixelsTypeI) obj;
            String name = "";
            String typeName = dataType.name();
            
            switch(typeName) {
               case "BYTE": name = "int8"; break;
               case "SHORT": name = "int16"; break;
               case "INT": name = "int32"; break;
               case "UBYTE": name = "uint8"; break;
               case "USHORT": name = "uint16"; break;
               case "UINT": name = "uint32"; break;
               case "FLOAT": name = "float"; break;
               case "DOUBLE": name = "double"; break;
               default: name = ""; break;
            }
            
            if ( type.getValue().getValue().equals(name) ) {
               return type;
            }
         }
      }
      
      return null; // not supported condition.
   }
}
