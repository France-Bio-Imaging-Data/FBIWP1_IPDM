/**
 * 
 */
package plugins.ofuica.metro.omero;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.painter.Anchor2D;
import omero.gateway.model.EllipseData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.model.Length;
import omero.model.LengthI;
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

public class ROIUtils {
   
   /**
    * Creates a single shape and makes it visible.
    * @param shape
    * @return
    */
   private static ROIData createSingleShape(ShapeData shape) {
      final ROIData roiData = new ROIData();
      // shape.setVisible(true);
      roiData.addShapeData(shape);
      return roiData;
   }
   
   private static ROIData createRectangleROI(ROI2DRectangle roi) {
      
      final ROI2DRectangle roi2d = (ROI2DRectangle) roi;
      final List<Anchor2D> controlPoints = roi2d.getControlPoints();
      
      if ( controlPoints.size() == 4 ) {
         final Anchor2D ctrl1 = controlPoints.get(0);
         final Anchor2D ctrl2 = controlPoints.get(1);
         final Anchor2D ctrl3 = controlPoints.get(2);
         
         double w = ctrl2.getPositionX() - ctrl1.getPositionX();
         double h = ctrl3.getPositionY() - ctrl1.getPositionY();
         double x = roi2d.getPosition5D().getX();
         double y = roi2d.getPosition5D().getY();
        
         final RectangleData rectangle = new RectangleData(x, y, w, h);
         rectangle.setC(roi.getC());
         rectangle.setZ(roi.getZ());
         rectangle.setT(roi.getT());
         rectangle.setText(roi.getName());
         
         final Color rColor = roi.getColor();
         final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
               roi.getOpacity());
         final Length strokeLength = new LengthI(roi.getStroke(), UnitsLength.PIXEL);
         rectangle.getShapeSettings().setStrokeWidth(strokeLength);
         rectangle.getShapeSettings().setFill(color);
         return createSingleShape(rectangle);
      }
      
      return null;
   }
   
   private static ROIData createPointROI(int x, int y) {
      final PointData point = new PointData(x, y);
      return createSingleShape(point);
   }
   
   private static ROIData createEllipseROI(ROI2DEllipse roi) {
      final List<Anchor2D> controlPoints = roi.getControlPoints();
      
      if ( controlPoints.size() == 4) {
         final Anchor2D ctrl1 = controlPoints.get(0);
         final Anchor2D ctrl2 = controlPoints.get(1);
         final Anchor2D ctrl3 = controlPoints.get(2);
         
         double rx = (ctrl2.getPositionX() - ctrl1.getPositionX()) * 0.5f;
         double ry = (ctrl3.getPositionY() - ctrl1.getPositionY()) * 0.5f;
         
         double x = roi.getPosition5D().getX() + rx;
         double y = roi.getPosition5D().getY() + ry;
         
         final EllipseData ellipse = new EllipseData(x, y, rx, ry);
         
         ellipse.setC(roi.getC());
         ellipse.setZ(roi.getZ());
         ellipse.setT(roi.getT());
         ellipse.setText(roi.getName());
         
         final Color rColor = roi.getColor();
         final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
               roi.getOpacity());
         
         ellipse.getShapeSettings().setFill(color);
         ellipse.getShapeSettings().setStrokeWidth(new LengthI(roi.getStroke(), UnitsLength.PIXEL));
         return createSingleShape(ellipse);
      }
      
      return null;
   }
   
   private static ROIData createLineROI(ROI2DLine roi) {
      final List<Anchor2D> controlPoints = roi.getControlPoints();
      
      if ( controlPoints.size() == 2 ) {
         final Anchor2D ctrl1 = controlPoints.get(0);
         final Anchor2D ctrl2 = controlPoints.get(1);
         final LineData line = new LineData(ctrl1.getPositionX(), ctrl1.getPositionY(), ctrl2.getPositionX(), 
               ctrl2.getPositionY());
         line.setC(roi.getC());
         line.setZ(roi.getZ());
         line.setT(roi.getT());
         line.setText(roi.getName());
         
         final Color rColor = roi.getColor();
         final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
               roi.getOpacity());
         line.getShapeSettings().setFill(color);
         line.getShapeSettings().setStrokeWidth(new LengthI(roi.getStroke(), UnitsLength.PIXEL));
         return createSingleShape(line);
      }
      
      return null;
   }
   
   private static ROIData createPointROI(ROI2DPoint roi) {
      final List<Anchor2D> controlPoints = roi.getControlPoints();
      
      if ( controlPoints.size() == 1 ) {
         final Anchor2D ctrl1 = controlPoints.get(0);
         final PointData point = new PointData(ctrl1.getPositionX(), ctrl1.getPositionY());
         
         final Color rColor = roi.getColor();
         final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
               roi.getOpacity());
         
         point.setC(roi.getC());
         point.setZ(roi.getZ());
         point.setT(roi.getT());
         point.setText(roi.getName());
         point.getShapeSettings().setFill(color);
         point.getShapeSettings().setStrokeWidth(new LengthI(roi.getStroke(), UnitsLength.PIXEL));
         return createSingleShape(point);
      }
      
      return null;
   }
   
   private static ROIData createPolygonROI(ROI2DPolygon roi) {
      final List<Point2D.Double> points = new ArrayList<>();
      final List<Point2D.Double> points1 = new ArrayList<>(); // ? why did I put this ?
      final List<Point2D.Double> points2 = new ArrayList<>(); // ?
      final List<Integer> mask = new ArrayList<>(); // ?
      
      for (final Point2D pt : roi.getPoints()) {
         points.add(new Point2D.Double(pt.getX(), pt.getY()));
      }
      
      final PolygonData polygon = new PolygonData(points);
      
      polygon.setC(roi.getC());
      polygon.setZ(roi.getZ());
      polygon.setT(roi.getT());
      polygon.setText(roi.getName());
      
      final Color rColor = roi.getColor();
      final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
            roi.getOpacity());
      
      polygon.getShapeSettings().setFill(color);
      polygon.getShapeSettings().setStrokeWidth(new LengthI(roi.getStroke(), UnitsLength.PIXEL));
      polygon.getShapeSettings().setFillRule("");
      return createSingleShape(polygon);
   }
   
   private static ROIData createPolylineROI(ROI2DPolyLine roi) {
      final List<Point2D.Double> points = new ArrayList<>();
      final List<Point2D.Double> points1 = new ArrayList<>();
      final List<Point2D.Double> points2 = new ArrayList<>();
      final List<Integer> mask = new ArrayList<>();
      
      for (final Point2D pt : roi.getPoints()) {
         points.add(new Point2D.Double(pt.getX(), pt.getY()));
      }
      
      final PolylineData polyline = new PolylineData(points); // , points1, points2, mask);
      
      polyline.setC(roi.getC());
      polyline.setZ(roi.getZ());
      polyline.setT(roi.getT());
      polyline.setText(roi.getName());
      
      final Color rColor = roi.getColor();
      final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
            roi.getOpacity());
      
      polyline.getShapeSettings().setFill(color);
      polyline.getShapeSettings().setStrokeWidth(new LengthI(roi.getStroke(), UnitsLength.PIXEL));
      polyline.getShapeSettings().setFillRule("");
      return createSingleShape(polyline);
   }
   
   private static ROIData createAreaROI(ROI2DArea roi) {
      
      double x = roi.getPosition5D().getX();
      double y = roi.getPosition5D().getY();
      double cx = roi.getBounds5D().getSizeX();
      double cy = roi.getBounds5D().getSizeY();
      
      boolean bmask[] = roi.getBooleanMask(true).mask;
      byte[] mask = new byte[bmask.length];
      
      for (int i = 0 ; i < bmask.length ; i++ ) {
         mask[i] = (byte) (bmask[i] ? 1 : 0);
      }
      
      final MaskData maskArea = new MaskData(x, y, cx, cy, mask);
      maskArea.setMask(mask);
      maskArea.setC(roi.getC());
      maskArea.setZ(roi.getZ());
      maskArea.setT(roi.getT());
      maskArea.setText(roi.getName());
      
      final Color rColor = roi.getColor();
      final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
            roi.getOpacity());
      
      maskArea.getShapeSettings().setFill(color);
      maskArea.getShapeSettings().setStrokeWidth(new LengthI(roi.getStroke(), UnitsLength.PIXEL));
      return createSingleShape(maskArea);
   }
   
   public static ROIData IcyRoiToOmero(icy.roi.ROI roi) {
      
      if ( roi instanceof ROI2DEllipse ) {
         return createEllipseROI( (ROI2DEllipse) roi);
      } 
      
      else if ( roi instanceof ROI2DRectangle ) {
         return createRectangleROI( (ROI2DRectangle) roi);
      }
      
      else if ( roi instanceof ROI2DLine) { 
         return createLineROI( (ROI2DLine) roi);
      }
      
      else if ( roi instanceof ROI2DPoint ) {
         return createPointROI( (ROI2DPoint) roi);
      }
      
      else if ( roi instanceof ROI2DPolygon ) {
         return createPolygonROI( (ROI2DPolygon) roi);
      }
      
      else if ( roi instanceof ROI2DPolyLine ) {
         return createPolylineROI( (ROI2DPolyLine) roi);
      }
      
      else if ( roi instanceof ROI2DArea ) {
         return createAreaROI( (ROI2DArea) roi);
      }
      
      else {
         System.out.println("ROI type not implemented, it was not saved!. type: " + roi.getClassName());
      }
      
      return new ROIData();
   }
   
//   public static List<icy.roi.ROI> OmeroRoiToIcy(ROI roi) {
//      final GeometriesBuilder builder = new GeometriesBuilder();
//      return builder.createROIs(roi); 
//   }
}
