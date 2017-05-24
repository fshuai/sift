package sift;

import match.Match;
import match.MatchKeys;
import render.RenderImage;
import scale.KDFeaturePoint;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by fshuai on 2017/4/21.
 */
public class Main {
    static {
        System.setProperty(ModifiableConst._TOWPNTSCALAMINUS, "8.0");
        System.setProperty(ModifiableConst._SLOPEARCSTEP, "5");
        System.setProperty(ModifiableConst._TOWPNTORIENTATIONMINUS, "0.05");
    }

    public static void drawImage(BufferedImage logo, BufferedImage model,
                                 String file, List<Match> ms) throws Exception {
        int lw = logo.getWidth();
        int lh = logo.getHeight();

        int mw = model.getWidth();
        int mh = model.getHeight();

        BufferedImage outputImage = new BufferedImage(lw + mw, lh + mh,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = outputImage.createGraphics();
        g.drawImage(logo, 0, 0, lw, lh, null);
        g.drawImage(model, lw, lh, mw, mh, null);
        g.setColor(Color.GREEN);
        for (Match m : ms) {
            KDFeaturePoint fromPoint = m.fp1;
            KDFeaturePoint toPoint = m.fp2;
            g.drawLine((int) fromPoint.x, (int) fromPoint.y, (int) toPoint.x
                    + lw, (int) toPoint.y + lh);
        }
        g.dispose();
        FileOutputStream fos = new FileOutputStream(file);
        ImageIO.write(outputImage, "JPEG", fos);
        fos.close();
    }

    public static void main(String[] args) throws Exception {

        BufferedImage img = ImageIO.read(new File(
                "f:/image/test01.jpg"));
        RenderImage ri = new RenderImage(img);
        SIFT sift = new SIFT();
        //int n0=sift.detectFeatures(ri.toPixelFloatArray(null));
        List<KDFeaturePoint> al = sift.getGlobalKDFeaturePoints();

        BufferedImage img1 = ImageIO.read(new File(
                "f:/image/test02.jpg"));
        RenderImage ri1 = new RenderImage(img1);
        SIFT sift1 = new SIFT();
        //int n1=sift1.detectFeatures(ri1.toPixelFloatArray(null));
        List<KDFeaturePoint> al1 = sift1.getGlobalKDFeaturePoints();

        List<Match> ms = MatchKeys.findMatchesBBF(al1, al);
        ms = MatchKeys.filterMore(ms);
        //ms = MatchKeys.filterFarMatchL(ms, img.getWidth(), img.getHeight());
        //ms = MatchKeys.filterFarMatchR(ms, img1.getWidth(), img.getHeight());
        drawImage(img1, img, "f:/image/result.jpg", ms);
       // System.out.println("n0:"+n0);
       // System.out.println("n1:"+n1);
    }
}
