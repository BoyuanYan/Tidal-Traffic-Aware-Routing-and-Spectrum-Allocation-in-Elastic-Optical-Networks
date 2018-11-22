package org.yby.ecoc2017.defragAlgorithm.patel;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.UndirectedWeightedSubgraph;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.BasicDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * The idea of this defragAlgorithm is from <b>201103-OFC-Patel-Defragmentation of Transparent Flexible Optical WDM</b>.
 * Chanese Version：
 * 1、以占用波长序列号的降序排列当前存在于网络中的业务。
 * 2、从有序集合中选择第一个业务，并记录其占用的最低波长编号。
 * 3、构建一个辅助图。在该辅助图中，当且仅当原始图中两节点间有直连光路，并且该光路上有从最低波长编号开始的足够连续数量的波长资源时，该光路才被包含在辅助图中。
 * 4、对于被重构的业务，以最小跳数为准计算路由。
 * 5、从最低波长编号开始，如果路由上的可用波长资源满足连续性和一致性，则将该业务重分配到该路由上。如果这样满足条件的路由不存在，并且选用最低波长
 *    编号还没有高于该业务原本占用的波长资源的最低波长编号，则将最低波长编号+1,然后重复步骤3-5。如果选用最低波长编号已经等于该业务原本占用的波长
 *    资源的最低波长编号了，则进行步骤6。
 * 6、在有序业务集合中选择下一个业务，重复步骤2-5。
 * Created by yby on 2017/4/4.
 */
public class GreedyDefragmentationAlg<E extends EonEdge> extends BasicDefragmentationAlg<EonVertex, E> {

    public GreedyDefragmentationAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        super(graph, currentServices);
    }

    @Override
    public void defragment(Service blockedService) {
        // Step 1
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
        Iterator<ServiceAssignment<E>> iterator = orderedSA.iterator();
        // Step 2
        while (iterator.hasNext()) {
            ServiceAssignment<E> serviceAssignment = iterator.next();
            int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
            // Step 3
            UndirectedWeightedSubgraph<EonVertex, E> subgraph = abstractSubgraph(
                    getGraph(), serviceAssignment.getService().getRequiredSlotNum());
            // Step 4
            DijkstraShortestPath<EonVertex, E> dij = new DijkstraShortestPath<>(subgraph);
            GraphPath<EonVertex, E> path = dij.getPath(new EonVertex(serviceAssignment.getService().getSource()),
                    new EonVertex(serviceAssignment.getService().getDestination()));
            if (!path.getEdgeList().isEmpty()) {
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                // Step 5
                int minAvailSlotIndex = BasicRSAAlg.findFirstAvailableSlot(
                        path.getEdgeList(), serviceAssignment.getService().getRequiredSlotNum());
                // if there exists the path
                if (minAvailSlotIndex != BasicRSAAlg.UNAVAILABLE && minAvailSlotIndex<minOccupiedSlotIndex) {
                    BasicRSAAlg.shiftService(serviceAssignment, minAvailSlotIndex, path.getEdgeList());
                } else {
                    // if not, restore assignment of this service
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            }

        }
    }


    /**
     *
     * @param graph
     * @param requiredSlotNum
     * @return
     */
    private UndirectedWeightedSubgraph<EonVertex, E> abstractSubgraph(SimpleWeightedGraph<EonVertex, E> graph, int requiredSlotNum) {
        UndirectedWeightedSubgraph<EonVertex, E> subgraph = new UndirectedWeightedSubgraph<>(graph);
        for (E edge : graph.edgeSet()) {
            int minIndex = BasicRSAAlg.findFirstAvailableSlot(edge, requiredSlotNum);
            if (minIndex == BasicRSAAlg.UNAVAILABLE) {
                subgraph.removeEdge(edge);
            }
        }
        return subgraph;
    }
}
