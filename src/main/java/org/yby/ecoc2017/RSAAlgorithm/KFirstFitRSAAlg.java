package org.yby.ecoc2017.RSAAlgorithm;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.Timestamp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 使用k算法进行RSA的算路。
 */
public class KFirstFitRSAAlg extends FirstFitRSAAlg {
    private static final Logger log = LoggerFactory.getLogger(KFirstFitRSAAlg.class);
    private KShortestPathAlgorithm<EonVertex, EonEdge> kShortestPathAlgorithm;
    final int k;


    public KFirstFitRSAAlg(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue,
                           ArrayList<Timestamp> servicesOrderedQueue, int k) {
        super(graph, servicesQueue, servicesOrderedQueue);
        this.k=k;
        kShortestPathAlgorithm = new KShortestPaths<>(graph,k);
    }

    /**
     * 从k算法算出来的k条路中选出比较合适的那一条
     * @param paths
     * @param serviceToBeAssigned
     * @return 如果没有可用的，则返回Right为UNAVAILABLE的数值
     */
    private Pair<GraphPath<EonVertex, EonEdge>, Integer> filter(
                                                            List<GraphPath<EonVertex, EonEdge>> paths,
                                                            Service serviceToBeAssigned) {
        // 先按照hop跳数升序排序
        paths.sort(new Comparator<GraphPath<EonVertex, EonEdge>>() {
            @Override
            public int compare(GraphPath<EonVertex, EonEdge> o1, GraphPath<EonVertex, EonEdge> o2) {
                if (o1.getLength() > o2.getLength()) {
                    return 1;
                } else if (o1.getLength() < o2.getLength()){
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        for (GraphPath<EonVertex, EonEdge> path : paths) {
            int subscript = findFirstAvailableSlot(path.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
            if (subscript != BasicRSAAlg.UNAVAILABLE) {
                return new Pair<>(path, subscript);
            }
        }
        return new Pair<>(null, BasicRSAAlg.UNAVAILABLE);
    }

    @Override
    public void allocate() {
        for (int index=0; index<getServicesOrderedQueue().size(); index++) {
            Timestamp timestamp = getServicesOrderedQueue().get(index);
            Service serviceToBeAssigned = getServicesQueue().get(timestamp.getServiceIndex()-1);
            // if this timestamp is start-time.
            if (timestamp.isStartTime()) {
                EonVertex src = new EonVertex(serviceToBeAssigned.getSource());
                EonVertex dst = new EonVertex(serviceToBeAssigned.getDestination());
                // 使用k算法计算路径
                List<GraphPath<EonVertex, EonEdge>> paths = kShortestPathAlgorithm.getPaths(src, dst);
                // 过滤出较好的那一条，其实就是根据hop排序，哈哈哈哈
                Pair<GraphPath<EonVertex, EonEdge>, Integer> better = filter(paths, serviceToBeAssigned);
//                GraphPath<EonVertex, EonEdge> path = dijkstraShortestPath.getPath(src, dst);
//                int subscript = findFirstAvailableSlot(path.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
                // if there exists enough spectrum resource.
                if (better.getSecond() != BasicRSAAlg.UNAVAILABLE) {
                    execAllocation(better.getFirst().getEdgeList(),
                            better.getSecond(),
                            serviceToBeAssigned);
                } else {
                    // if there isn't enough spectrum resource at specific shortest path.
                    handleAllocationFail(serviceToBeAssigned, index);
                }
            } else {
                // if this timestamp is end-time.
                handleServiceLeave(serviceToBeAssigned.getIndex());
            }
        }
    }



}
