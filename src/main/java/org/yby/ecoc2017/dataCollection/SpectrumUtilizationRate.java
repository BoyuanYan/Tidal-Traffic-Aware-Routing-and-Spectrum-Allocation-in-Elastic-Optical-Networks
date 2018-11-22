package org.yby.ecoc2017.dataCollection;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;

import java.util.Set;

/**
 * calculate utilization Rate
 * @author yby
 */
public final class SpectrumUtilizationRate<E extends EonEdge> {

    public static final double EMPTY_EDGE = -1;
    private static SpectrumUtilizationRate ourInstance = new SpectrumUtilizationRate();

    public static SpectrumUtilizationRate getInstance() {
        return ourInstance;
    }

    private SpectrumUtilizationRate() {
    }

    /**
     * calculate utilization Rate for one graph.
     * Notification: the size of slots contained by a E can be variable.
     * @param graph
     * @return utilization Rate or EMPTY_EDGE
     */
    public double calculateUR(SimpleWeightedGraph<EonVertex, E> graph) {
        return calculateUR(graph.edgeSet());
    }

    /**
     * calculate utilization Rate for one edgeSet.
     * Notification: the size of slots contained by a E can be variable.
     * @param edgeSet
     * @return utilization Rate or EMPTY_EDGE
     */
    public double calculateUR(Set<E> edgeSet) {
        double totalSlotNum = 0.0d;
        double occupiedSlotNum = 0.0d;
        if (!edgeSet.isEmpty()) {
            for (E edge : edgeSet) {
                totalSlotNum = totalSlotNum + edge.getSlots().size();
                occupiedSlotNum = occupiedSlotNum + edge.occupiedSlotsNum();
            }
            return occupiedSlotNum / totalSlotNum;
        } else {
            return EMPTY_EDGE;
        }
    }


    /**
     * calculate utilization Rate for one edge.
     * @param edge
     * @return utilization Rate or EMPTY_EDGE.
     */
    public double calculateUR(E edge) {
        if (edge == null) {
            return EMPTY_EDGE;
        }
        double size = edge.getSlots().size();
        double occupied = edge.occupiedSlotsNum();
        return occupied / size;
    }
}
