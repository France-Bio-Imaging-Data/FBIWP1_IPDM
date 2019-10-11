/**
 * 
 */
package plugins.ofuica.metro.omero;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.roi.BooleanMask2D;
import ome.model.units.BigResult;
import omero.gateway.model.EllipseData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.model.Length;
import omero.model.enums.UnitsLength;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPoint;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

/**
 * @author osvaldo
 */

class GeometriesBuilder {
   
   public icy.roi.ROI createShape(final ShapeData shape) {
      final float opacity = shape.getShapeSettings().getFill().getAlpha();
      
      icy.roi.ROI roi = null;
      
      if ( shape instanceof EllipseData ) {
         roi = createEllipse( (EllipseData) shape);
      }
      
      else if ( shape instanceof LineData ) {
         roi = createLine( (LineData) shape);
      }
      
      else if ( shape instanceof RectangleData ) {
         roi = createRectangle( (RectangleData) shape);
      }
      
      else if ( shape instanceof PolygonData ) {
         roi = createPolygon( (PolygonData) shape);
      }
      
      else if ( shape instanceof PointData ) {
         roi = createPoint( (PointData) shape);
      }
      
      else if (shape instanceof PolylineData ) {
         roi = createPolyline( (PolylineData) shape);
      }
      
      else if ( shape instanceof MaskData ) {
         roi = createMask( (MaskData) shape);
      }
      
      else {
         System.out.println("Warning: Roi's type not supported: " + shape.getClass().getName());
         return null;
      }
      
      roi.setOpacity(opacity);
      
      try {
         final Length strokeWidth = shape.getShapeSettings().getStrokeWidth(UnitsLength.PIXEL);
         roi.setStroke(strokeWidth.getValue());
      } catch (BigResult e) {
         e.printStackTrace();
      }
      
      return roi;
   }
   
   private icy.roi.ROI createMask(final MaskData shape) {
      final double x = shape.getX();
      final double y = shape.getY();
      final double w = shape.getWidth();
      final double h = shape.getHeight();
      
      // TODO: Get the data from the SHAPE attributes!.
      int maskSize = (int) (w*h);
      boolean[] booleanMask = new boolean[maskSize];
      
      int index = 0;
      for ( int i = 0; i < w ; i++ ) {
         for ( int j = 0 ; j < h ; j++ ) {
            booleanMask[index] = shape.getMaskAsBinaryArray()[i][j] > 0;
            index++;
         }
      }
      
      final BooleanMask2D mask = new BooleanMask2D(new Rectangle((int) x, (int) y,  (int) w, (int) h), booleanMask);
      final ROI2DArea roi = new ROI2DArea(mask);
      setupBasicFields(roi, shape);
      return roi;
   }
   
   private icy.roi.ROI createEllipse(final EllipseData shape) {
      
      final omero.model.AffineTransform transform = shape.getTransform();
      final Double cx = shape.getX();
      final Double cy = shape.getY();
      final Double rx = shape.getRadiusX();
      final Double ry = shape.getRadiusY();
      
      final AffineTransform at = new AffineTransform();
      at.setToIdentity();
      
      if ( transform != null && transform.isLoaded() ) {
// TODO: Delete this code when it is validated :).
//         final String[] transformArray = transform.replaceAll("matrix\\(", "").replaceAll("\\)", "").split(" ");
//         final double mms[] = new double[transformArray.length];
//         for ( int i = 0; i < transformArray.length ; i++ ) {
//            mms[i] = Double.parseDouble(transformArray[i]);
//         }
//         at.setTransform(mms[0], mms[1], mms[2], mms[3], mms[4], mms[5]);
         
         at.setTransform(transform.getA00().getValue(), 
                         transform.getA01().getValue(), 
                         transform.getA02().getValue(), 
                         transform.getA10().getValue(),
                         transform.getA11().getValue(),
                         transform.getA12().getValue());
      }
      
      final ROI2DEllipse e = new ROI2DEllipse(cx-rx, cy-ry, cx+rx, cy+ry);
      e.setPosition2D(new Point2D.Double(cx-rx, cy-ry));
      
      e.setName(shape.getText());
      setupBasicFields(e, shape);
      return e;
   }
   
   private void setupBasicFields(final icy.roi.ROI2D roi, final ShapeData shape) {
      final Integer t = shape.getT();
      final Integer z = shape.getZ();
      final Integer c = shape.getC();
      final Color fillColor = shape.getShapeSettings().getFill();
      
      roi.setColor(fillColor);
      roi.setC(c);
      roi.setT(t);
      roi.setZ(z);
   }
   
   private icy.roi.ROI createLine(final LineData shape) {
      
      final double y1 = shape.getY1();
      final double y2 = shape.getY2();
      final double x1 = shape.getX1();
      final double x2 = shape.getX2();
      
      final ROI2DLine roi = new ROI2DLine(x1, y1, x2, y2);
      setupBasicFields(roi, shape);
      return roi;
   }
   
   private icy.roi.ROI createRectangle(final RectangleData shape) {
      
      final double y = shape.getY();
      final double x = shape.getX();
      final double width = shape.getWidth();
      final double height = shape.getHeight();
      final double hw = width*0.5;
      final double hh = height*0.5;
      final ROI2DRectangle roi = new ROI2DRectangle(x-hw, y-hh, x+hw, y+hh);
      
      roi.setPosition2D(new Point2D.Double(x, y));
      setupBasicFields(roi, shape);
      return roi;
   }
   
   private icy.roi.ROI createPolygon(final PolygonData shape) {
      final List<Point2D> pts = new ArrayList<>();
      final List<Point2D.Double> points = shape.getPoints();
      
      for ( Point2D.Double point : points ) {
         pts.add(point);
      }
      
      final ROI2DPolygon roi = new ROI2DPolygon(pts);
      setupBasicFields(roi, shape);
      return roi;
   }
   
   private icy.roi.ROI createPolyline(final PolylineData shape) {
      final List<Point2D.Double> points = shape.getPoints();
      final List<Point2D> pts = new ArrayList<>(points);
      
      for ( final Point2D.Double point : points ) {
         pts.add(point);
      }
      
      final ROI2DPolyLine roi = new ROI2DPolyLine(pts);
      setupBasicFields(roi, shape);
      return roi;
   }
   
   private icy.roi.ROI createPoint(final PointData shape) {
      final double cx = shape.getX();
      final double cy = shape.getY();
      final ROI2DPoint roi = new ROI2DPoint(cx, cy);
      setupBasicFields(roi, shape);
      return roi;
   }
}
