package sift;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import render.RenderImage;
import scale.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fshuai on 2017/4/21.
 * 面向用户的主操作接口
 */
public class SIFT implements Serializable{
    // 以下常数均是论文中推荐的参数值
    static float preprocSigma             = 1.5f;   // 用于处理被double后的图像预处理的模糊因子
    static float octaveSigma              = 1.6f;   // 用于处理每个8度空间图像的模糊因子
    static int minimumRequiredPixelsize = 32;    // 高斯金字塔中缩小时最小的尺寸
    static int scaleSpaceLevels         = 3;     // 每个8度空间需要获取极值点的差分图层，加上用于比较的上下层至少要有5个差分图像，所以至少要有6个高斯模糊图象。
    static float dogThresh                = 0.0075f; // 在差分图象上的极致点值最小值，防止大片的模糊后的点被选中，这个值越小选中的点越多。
    static float dValueLowThresh          = 0.008f; // 和周围点比较的差值，这个差值是经过导数运算的差值，不是直接比较的。论文中建议为0.03（page
    // 11），但获取的点数太少，这里修改为0.008
    static float maximumEdgeRatio         = 20.0f;  // 非角点的过虑比
    static float scaleAdjustThresh        = 0.50f;  // 尺度空间的精确点和真实图象上的离散点在投谢时需要调整，这个是最大调整范围，超这个值就可能是下一个点。
    static float peakRelThresh            = 0.8f;   //
    static int    relocationMaximum        = 4;

    public void detectFeatures(ImagePixelArray img,JavaSparkContext sc) {
        detectFeaturesDownscaled(img, -1, 1.0f,sc);
    }

    /**
     * @param img
     * @param preProcessMark 图象预处理的标记，小于0，img需要double,大于0时，说明图象的长和宽要half到这个尺寸以下，等于0则不预处理
     * @param startScale
     * @return
     */
    public static void detectFeaturesDownscaled(ImagePixelArray img, int preProcessMark, float startScale,JavaSparkContext sc) {

        if (preProcessMark < 0) {
            img = img.doubled();
            startScale *= 0.5;
        } else if (preProcessMark > 0) {
            while (img.width > preProcessMark || img.height > preProcessMark) {
                img = img.halved();
                startScale *= 2.0;
            }
        }
        if (preprocSigma > 0.0){
            GaussianArray gaussianPre = new GaussianArray(preprocSigma);
            img = gaussianPre.convolve(img);
        }

        Pyramid pyr = new Pyramid();
        pyr.buildOctaves(img, startScale, scaleSpaceLevels, octaveSigma, minimumRequiredPixelsize);

        globalFeaturePoints = new ArrayList<FeaturePoint>();
        // Generate featurePoints from each scalespace.
        JavaRDD<OctaveSpace> oct_rdd=sc.parallelize(pyr.octaves);
        System.out.println("octsize:"+oct_rdd.count());
        final JavaRDD<Integer> points_rdd=oct_rdd.map(new Function<OctaveSpace, Integer>(){
            public Integer call(OctaveSpace osp){
                ArrayList<ScalePeak> peaks = osp.findPeaks(dogThresh);// 寻找图片中的极值点
                //ArrayList<ScalePeak> peaksFilted = osp.filterAndLocalizePeaks(peaks, maximumEdgeRatio, dValueLowThresh,
                  //      scaleAdjustThresh, relocationMaximum);
                //osp.pretreatMagnitudeAndDirectionImgs();
                return peaks.size();
            }
        });
        points_rdd.foreach(new VoidFunction<Integer>() {
            public void call(Integer integer) throws Exception {
                System.out.println("num:"+integer);
            }
        });
        System.out.println("pointsize:"+points_rdd.first());
        //System.out.println("end:"+points_rdd.count());
        /*for (int on = 0; on < pyr.octaves.size(); ++on){
            OctaveSpace osp = pyr.octaves.get(on);

            ArrayList<ScalePeak> peaks = osp.findPeaks(dogThresh);// 寻找图片中的极值点
            ArrayList<ScalePeak> peaksFilted = osp.filterAndLocalizePeaks(peaks, maximumEdgeRatio, dValueLowThresh,
                    scaleAdjustThresh, relocationMaximum);

            // 先将要处理的图层上所有象素的梯度大小和方向计算出来
            osp.pretreatMagnitudeAndDirectionImgs();
            ArrayList<FeaturePoint> faturePoints = osp.makeFeaturePoints(peaksFilted, peakRelThresh, scaleSpaceLevels,
                    octaveSigma);
            osp.clear();
            globalFeaturePoints.addAll(faturePoints);
        }*/
    }

    public static ArrayList<FeaturePoint> getFeature(OctaveSpace osp){
        ArrayList<ScalePeak> peaks = osp.findPeaks(dogThresh);// 寻找图片中的极值点
        ArrayList<ScalePeak> peaksFilted = osp.filterAndLocalizePeaks(peaks, maximumEdgeRatio, dValueLowThresh,
                scaleAdjustThresh, relocationMaximum);

        // 先将要处理的图层上所有象素的梯度大小和方向计算出来
        osp.pretreatMagnitudeAndDirectionImgs();
        ArrayList<FeaturePoint> featurePoints = osp.makeFeaturePoints(peaksFilted, peakRelThresh, scaleSpaceLevels,
                octaveSigma);
        //osp.clear();
        return featurePoints;
    }

    private static List<FeaturePoint> globalFeaturePoints;
    private static List<KDFeaturePoint> globalKDFeaturePoints;

    public List<KDFeaturePoint> getGlobalKDFeaturePoints() {

        if (globalKDFeaturePoints != null) return (globalKDFeaturePoints);
        if (globalFeaturePoints == null) throw (new IllegalArgumentException("No featurePoints generated yet."));
        globalKDFeaturePoints = new ArrayList<KDFeaturePoint>();
        for (FeaturePoint fp : globalFeaturePoints) {
            globalKDFeaturePoints.add(new KDFeaturePoint(fp));
        }
        return globalKDFeaturePoints;
    }

    public static void main(String[] args) throws Exception{
        SparkConf conf=new SparkConf().setAppName("sift");
        JavaSparkContext sc=new JavaSparkContext(conf);

        BufferedImage img = ImageIO.read(new File(
                "/root/image/test01.jpg"));
        RenderImage ri = new RenderImage(img);
        SIFT sift = new SIFT();

        //sift.detectFeatures(ri.toPixelFloatArray(null),sc);
        sift.detectFeaturesDownscaled(ri.toPixelFloatArray(null), -1, 1.0f,sc);
        /*List<KDFeaturePoint> al = sift.getGlobalKDFeaturePoints();

        BufferedImage img1 = ImageIO.read(new File(
                "f:/image/test02.jpg"));
        RenderImage ri1 = new RenderImage(img1);
        SIFT sift1 = new SIFT();
        int n1=sift1.detectFeatures(ri1.toPixelFloatArray(null));
        List<KDFeaturePoint> al1 = sift1.getGlobalKDFeaturePoints();

        List<Match> ms = MatchKeys.findMatchesBBF(al1, al);
        ms = MatchKeys.filterMore(ms);*/
        //ms = MatchKeys.filterFarMatchL(ms, img.getWidth(), img.getHeight());
        //ms = MatchKeys.filterFarMatchR(ms, img1.getWidth(), img.getHeight());
        //drawImage(img1, img, "f:/image/result.jpg", ms);
        /*System.out.println("n0:"+n0);
        System.out.println("n1:"+n1);*/
    }
}