package org.yby.ecoc2017.dataCollection;

import com.google.common.collect.Lists;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.defragAlgorithm.BasicDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;

import java.util.ArrayList;

/**
 * Created by yby on 2017/4/11.
 * slots的被分配次数。
 */
public class SlotsOccupiedTime {

    private static SlotsOccupiedTime ourInstance = new SlotsOccupiedTime();

    public static SlotsOccupiedTime getInstance() {
        return ourInstance;
    }

    private SlotsOccupiedTime() {
    }

    /**
     * calculate occupied time of every slots
     * @param graph graph
     * @param slotsNum number of slots per link
     * @param <V> Vertex extends EonVertex
     * @param <E> Edge extends EonEdge
     * @return list of SOT
     */
    public <V extends EonVertex, E extends EonEdge> int[] calculateSOT(
                                                    SimpleWeightedGraph<V, E> graph, int slotsNum) {
        int[] rtn = new int[slotsNum];
        for (int i=0; i<slotsNum; i++) {
            rtn[i] = 0;
        }
        // initialization

        for (E edge :graph.edgeSet()) {
            for (int index=0; index<edge.getSlots().size(); index++) {
                rtn[index] = rtn[index] + edge.getSlots().get(index) .getOccupiedNum();
            }
        }

        return rtn;
    }


}
