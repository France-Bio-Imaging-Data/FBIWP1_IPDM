// Decompiled by DJ v3.10.10.93 Copyright 2007 Atanas Neshkov  Date: 29/10/2009 05:10:27 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   RowColumnIndex.java

package plugins.ofuica.internals.jamlab;

import Jama.Matrix;

// Referenced classes of package jamlab:
//            JElmat

public class RowColumnIndex
{

    public RowColumnIndex()
    {
        _$6065 = null;
        _$6073 = null;
        _$6081 = null;
        _$6107 = null;
    }

    public RowColumnIndex(int r_index[], int c_index[], double values[])
    {
        _$6065 = r_index;
        _$6073 = c_index;
        _$6081 = values;
        _$6094 = r_index.length;
        _$6107 = new Matrix(JElmat.convertTo2D(values));
    }

    public int[] getRowIndex()
    {
        return _$6065;
    }

    public int[] getColumnIndex()
    {
        return _$6073;
    }

    public double[] getElementValues()
    {
        return _$6081;
    }

    public int getTotalElements()
    {
        return _$6094;
    }

    public Matrix getElementValuesInMatrix()
    {
        return _$6107;
    }

    private int _$6065[];
    private int _$6073[];
    private double _$6081[];
    private int _$6094;
    private Matrix _$6107;
}