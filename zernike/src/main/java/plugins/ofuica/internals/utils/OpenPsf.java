package plugins.ofuica.internals.utils;
import ij.*;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.FileInputStream;

public class OpenPsf {
    public static final int WU_BYTE = 0;
    public static final int WU_SHORT = 1;
    public static final int WU_FLOAT = 2;
    public static final int WU_USHORT = 4;
    public static final int WU_INT = 5;
    private static int getByte(int index, byte header[])
    {
        int value = header[index];
        if(value < 0)
            value += 256;
        return value;
    }
    
    private static int getInt(int index, byte header[])
    {
        int b1 = getByte(index, header);
        int b2 = getByte(index + 1, header);
        int b3 = getByte(index + 2, header);
        int b4 = getByte(index + 3, header);
        return b4 << 24 | b3 << 16 | b2 << 8 | b1;
    }

    public static ImageStack swapQuadrants(ImageStack ims)
    {
        ImageStack imstempo = new ImageStack(ims.getWidth(), ims.getHeight());
        for(int i = 0; i < ims.getSize(); i++)
        {
            int pos = (ims.getSize() / 2 + i) % ims.getSize() + 1;
            ImageProcessor ip = ims.getProcessor(pos);
            ImageProcessor tempo = ip.createProcessor(ip.getWidth(), ip.getHeight());
            int sizew = ip.getWidth() / 2;
            int sizeh = ip.getHeight() / 2;
            ip.setRoi(0, 0, sizew, sizeh);
            ImageProcessor t1 = ip.crop();
            tempo.insert(t1, sizew, sizeh);
            ip.setRoi(sizew, 0, sizew, sizeh);
            t1 = ip.crop();
            tempo.insert(t1, 0, sizeh);
            ip.setRoi(0, sizeh, sizew, sizeh);
            t1 = ip.crop();
            tempo.insert(t1, sizew, 0);
            ip.setRoi(sizew, sizeh, sizew, sizeh);
            t1 = ip.crop();
            tempo.insert(t1, 0, 0);
            imstempo.addSlice(String.valueOf(pos), tempo);
        }

        return imstempo;
    }

    private static int[] readHeader(String path)
    {
        int data[] = new int[4];
        byte header[] = new byte[16];
        try
        {
            FileInputStream in = new FileInputStream(path);
            in.read(header, 0, header.length);
            data[0] = getInt(0, header);
            data[1] = getInt(4, header);
            data[2] = getInt(8, header);
            data[3] = getInt(12, header);
            in.close();
        }
        catch(Exception e)
        {
            IJ.error((new StringBuilder("problem: ")).append(e.toString()).toString());
        }
        return data;
    }

    public static ImageStack readData(String pathDir, String pathName, boolean swap)
    {
        String path = (new StringBuilder(String.valueOf(pathDir))).append(File.separator).append(pathName).toString();
        int head[] = readHeader(path);
        FileInfo fi = new FileInfo();
        fi.width = head[0];
        fi.height = head[1];
        fi.nImages = head[2];
        int filet = 4;
        switch(head[3])
        {
        case 0: 
            filet = 0;
            break;

        case 2: 
            filet = 4;
            break;

        case 5: 
            filet = 3;
            break;

        case 1: 
            filet = 1;
            break;

        case 4: 
            filet = 2;
            break;
        }
        fi.fileType = filet;
        fi.intelByteOrder = true;
        fi.offset = 1024;
        fi.fileFormat = 1;
        fi.fileName = pathName;
        fi.directory = pathDir;
        FileOpener fo = new FileOpener(fi);
        ImageStack res = fo.open(false).getStack();
        if(swap)
            res = swapQuadrants(res);
        return res;
    }


}