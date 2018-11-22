package org.yby.ecoc2017.boot;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jgrapht.alg.util.Pair;

import java.util.*;

/**
 * One Eon Network parameters, included by class EonNetParams.
 * Notification: The index of vertexes start from 1.
 * Created by yby on 2017/4/1.
 */
public class SingleEonNet {

    public ArrayList<Integer> businessArea;
    public ArrayList<Integer> residentialArea;

    public String name;
    public int vertexNum;
    public int edgeNum;
    public List<Pair<Integer, Integer>> edges;

    public SingleEonNet(ArrayList<Integer> businessArea, ArrayList<Integer> residentialArea, String name,
                        int vertexNum, int edgeNum, List<String> edges) {
        this.businessArea = businessArea;
        this.residentialArea = residentialArea;
        this.name = name;
        this.vertexNum = vertexNum;
        this.edgeNum = edgeNum;
        this.edges = parse(edges);
    }

    public Set<Integer> getBusinessAreaSet() {
        HashSet<Integer> rtn = Sets.newHashSetWithExpectedSize(businessArea.size());
        for (int i : businessArea) {
            rtn.add(i);
        }
        return rtn;
    }

    public Set<Integer> getResidentialArea() {
        HashSet<Integer> rtn = Sets.newHashSetWithExpectedSize(residentialArea.size());
        for (int i : residentialArea) {
            rtn.add(i);
        }
        return rtn;
    }

    /**
     * parse string with format "source-destination" to Pair<Source, Destination>.
     * @param edgesStr unhandled string
     * @return handled data
     */
    private List<Pair<Integer, Integer>> parse(List<String> edgesStr) {
        List<Pair<Integer, Integer>> rtn = Lists.newArrayListWithCapacity(edgesStr.size());
        for (String str : edgesStr) {
            String[] sd = str.split("-");
            Pair<Integer, Integer> pair = new Pair<>(Integer.parseInt(sd[0]), Integer.parseInt(sd[1]));
            rtn.add(pair);
        }
        return rtn;
    }
}
