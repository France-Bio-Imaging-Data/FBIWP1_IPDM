import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.InputStream;

public class IcyProtocolPublisher
{
   // generate the command line and launch it 
   private void applyWorkflow(String[] protocolArgs) 
   {
      final String[] pluginCmd = {"java", "-jar", "icy.jar", "-hl", "-x", "plugins.adufour.protocols.Protocols" };
      String[] CommandLine = new String[pluginCmd.length + protocolArgs.length];
      System.arraycopy(pluginCmd, 0, CommandLine, 0, pluginCmd.length);
      System.arraycopy(protocolArgs, 0, CommandLine, pluginCmd.length, protocolArgs.length);
      
      try {
         System.setOut(new PrintStream(new FileOutputStream("/tmp/test.txt")));
         System.setErr(new PrintStream(new FileOutputStream("/tmp/test_err.txt")));
         
         System.out.println("executing: " );
         for ( String str : CommandLine ) {
            System.out.print(" " + str + " ");
         }
         
         System.out.println("");
         Process process = Runtime.getRuntime().exec(CommandLine);
         
         try {
            process.waitFor();
            String line = "";
            InputStream stderr = process.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stderr));
            while ( (line = br.readLine()) != null)
               System.out.println(line);
            
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         
         System.out.println("exit value: " + process.exitValue());
         
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      return;
   }
   
   private static String[] createProtocolsArgument(String filepath) throws IOException
   {
      final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)));
      final List<String> args = new ArrayList<String>();
      
      while( br.ready() ) {
         String line = br.readLine();
         String[] arr = line.split("=");
         
         if (arr.length == 2) {
            
            if ( arr[0].equals("RecordIds") ) {
               line = "" + arr[0] + "=" + arr[1].replace(",", " ") + "";
            }
            
            args.add(line);
         }
      }
      
      br.close();
      return args.toArray(new String[args.size()]);
   }
   
   public static void main(String[] args) throws IOException
   {
      final String inputFile = System.getenv("InputFile");
      
      if ( inputFile == null ) {
         throw new IOException("i could not find inputFile: " + inputFile);
      }
      
      final String[] arguments = createProtocolsArgument(inputFile);
      IcyProtocolPublisher s = new IcyProtocolPublisher();
      s.applyWorkflow(arguments);
   }
}
