package sift;

import io.KDFeaturePointListInfo;
import io.KDFeaturePointWriter;
import render.RenderImage;
import scale.KDFeaturePoint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fshuai on 2017/4/21.
 */
public class MakeSiftData {
    static {
        System.setProperty(ModifiableConst._TOWPNTSCALAMINUS, "8.0");
        System.setProperty(ModifiableConst._SLOPEARCSTEP, "5");
        System.setProperty(ModifiableConst._TOWPNTORIENTATIONMINUS, "0.05");

    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        File logoDir = new File("f:/image");
        File[] logoFiles = logoDir.listFiles(new FileFilter() {

            public boolean accept(File arg0) {
                return arg0.getName().endsWith(".png");
            }
        });
        ArrayList<Integer> list=new ArrayList<Integer>();
        int i = 0;
        for (File logoFile : logoFiles) {
            BufferedImage img = ImageIO.read(logoFile);
            RenderImage ri = new RenderImage(img);
            SIFT sift = new SIFT();
            //sift.detectFeatures(ri.toPixelFloatArray(null));
            List<KDFeaturePoint> al = sift.getGlobalKDFeaturePoints();
            KDFeaturePointListInfo info = new KDFeaturePointListInfo();
            info.setHeight(img.getHeight());
            info.setWidth(img.getWidth());
            info.setImageFile(logoFile.getName());
            info.setList(al);
            KDFeaturePointWriter.writeComplete("f:/image/" + logoFile.getName() + ".sift",
                    info);
            i++;
            System.out.println(i);
            if (i == 100) break;
        }
        System.out.println("total times:" + (System.currentTimeMillis() - start));
    }
}
