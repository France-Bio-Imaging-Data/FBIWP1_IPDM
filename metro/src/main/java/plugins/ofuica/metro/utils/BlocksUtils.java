package plugins.ofuica.metro.utils;

import java.util.Iterator;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;

/**
 * 
 * @author osvaldo
 */

public final class BlocksUtils {
   
   public static BlockDescriptor getBlockDescriptorByName(final WorkFlow wf, 
         final String name) {
      
      final Iterator<BlockDescriptor> iter = wf.iterator();
      
      while( iter.hasNext() ) {
         final BlockDescriptor d = iter.next();
         
         if ( d.getDefinedName().equals(name) ) {
            return d;
         }
      }
      
      return null;
   }
   
   public static Var<?> getInputVariableByName(final BlockDescriptor descriptor, 
         final String name) {
      
      for ( final Var<?> v : descriptor.inputVars ) {
         if ( v.getName().equals(name) ) {
            return v;
         }
      }
      
      return null;
   }
}
