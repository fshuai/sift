package match;

import kdtree.KDTree;
import scale.KDFeaturePoint;
import sift.ModifiableConst;

import java.util.*;

/**
 * Created by fshuai on 2017/4/21.
 */
public class MatchKeys {
    private static class _mylist extends ArrayList<Match> {

        private static final long serialVersionUID = -1672787720681683109L;

        public void removeRange(int formIndex, int toIndex) {
            super.removeRange(formIndex, toIndex);
        }
    }

    public static List<Match> findMatchesBBF(List<KDFeaturePoint> keys1, List<KDFeaturePoint> keys2) {
        return findMatchesBBF(keys1, KDTree.createKDTree(keys2));
    }

    public static List<Match> findMatchesBBF(List<KDFeaturePoint> keys1, KDTree kd) {
        List<Match> matches = new _mylist();
        for (KDFeaturePoint kp : keys1) {
            ArrayList<KDTree.BestEntry> kpNNList = kd.nearestNeighbourListBBF(kp, 2, 40);
            if (kpNNList.size() < 2) throw (new IllegalArgumentException("BUG: less than two neighbours!"));
            KDTree.BestEntry be1 = (KDTree.BestEntry) kpNNList.get(0);
            KDTree.BestEntry be2 = (KDTree.BestEntry) kpNNList.get(1);
            if ((be1.getDist() / be2.getDist()) > ModifiableConst.getTowPntDistRatio()) {
                continue;
            }
            // XXX:
            KDFeaturePoint kpN = (KDFeaturePoint) be1.getNeighbour();
            if (Math.abs(kpN.orientation - kp.orientation) > ModifiableConst.getTowPntOrientationMinus()) {
                continue;
            }
            // if (Math.abs(kpN.getScale() - kp.getScale()) > ModifiableConst.getTowPntScaleMinus()) continue;
            matches.add(new Match(kp, kpN, be1.getDist(), be2.getDist()));
        }
        return (matches);
    }



    public static ArrayList<Match> filterMore(List<Match> matches) {
        Map<KDFeaturePoint, Integer> map = new HashMap<KDFeaturePoint, Integer>();

        for (Match m : matches) {
            Integer kp1V = map.get(m.fp1);
            int lI = (kp1V == null) ? 0 : (int) kp1V;
            map.put(m.fp1, lI + 1);
            Integer kp2V = map.get(m.fp2);
            int rI = (kp2V == null) ? 0 : (int) kp2V;
            map.put(m.fp2, rI + 1);
        }
        ArrayList<Match> survivors = new ArrayList<Match>();
        for (Match m : matches) {
            Integer kp1V = map.get(m.fp1);
            Integer kp2V = map.get(m.fp2);
            if (kp1V <= 1 && kp2V <= 1) survivors.add(m);
        }
        return (survivors);
    }

    public static void filterNBest(ArrayList<Match> matches, int bestQ) {
        Collections.sort(matches, new Match.MatchWeighter());
        if (matches.size() > bestQ) {
            ((_mylist) matches).removeRange(bestQ, matches.size() - bestQ);
        }
    }

    public static List<Match> filterFarMatchL(List<Match> matches, float minX, float minY) {
        int arcStep = ModifiableConst.getSolpeArcStep();
        if (matches.size() <= 1) return matches;
        int[] ms = new int[90 / arcStep]; //
        for (Match m : matches) {
            float r = (float)(Math.atan((m.fp2.y + minY - m.fp1.y) / (m.fp2.x + minX - m.fp1.x)) * 360
                    / (2 * Math.PI));
            m.slopeArc = (int) r / arcStep * arcStep; //
            if (m.slopeArc < 0) m.slopeArc += 90;
            ms[m.slopeArc / arcStep] = ms[m.slopeArc / arcStep] + 1;
        }
        int count = 0;
        int idx = 0;
        for (int i = 0; i < ms.length; i++) {//
            if (ms[i] > count) {
                count = ms[i];
                idx = i;
            }
        }
        idx = idx * arcStep;
        ArrayList<Match> survivors = new ArrayList<Match>();
        for (Match m : matches) {
            if (m.slopeArc == idx) survivors.add(m);
        }
        return survivors;
    }

    public static List<Match> filterFarMatchR(List<Match> matches, float minX, float minY) {
        int arcStep = ModifiableConst.getSolpeArcStep();
        if (matches.size() <= 1) return matches;
        int[] ms = new int[90 / arcStep]; //
        for (Match m : matches) {
            float r = (float)(Math.atan((m.fp1.y - (m.fp2.y + minY)) / (m.fp1.x + minX - m.fp2.x)) * 360
                    / (2 * Math.PI));
            m.slopeArc = ((int) r / arcStep * arcStep); //
            if (m.slopeArc < 0) m.slopeArc += 90;
            ms[m.slopeArc / arcStep] = ms[m.slopeArc / arcStep] + 1;
        }
        int count = 0;
        int idx = 0;
        for (int i = 0; i < ms.length; i++) {//
            if (ms[i] > count) {
                count = ms[i];
                idx = i;
            }
        }
        idx = idx * arcStep;
        ArrayList<Match> survivors = new ArrayList<Match>();
        for (Match m : matches) {
            if (m.slopeArc == idx) survivors.add(m);
        }
        return survivors;

    }
}
