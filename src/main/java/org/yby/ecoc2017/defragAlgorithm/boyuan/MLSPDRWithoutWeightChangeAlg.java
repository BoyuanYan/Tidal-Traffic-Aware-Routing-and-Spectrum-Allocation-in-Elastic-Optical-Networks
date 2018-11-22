package org.yby.ecoc2017.defragAlgorithm.boyuan;

import com.google.common.collect.Lists;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.patel.MaxSlotDescendComparator;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.*;

/**
 * Created by yby on 2017/4/10.
 * 没有边权值的改变,但是有区域的划分.
 */
public class MLSPDRWithoutWeightChangeAlg<E extends EonEdge> extends MLSPDRAlg<E> {
    public MLSPDRWithoutWeightChangeAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices, Map<E, Double> occupiedSlotsNum) {
        super(graph, currentServices, occupiedSlotsNum);
    }


    private static final Logger log = LoggerFactory.getLogger(MLSPDRWithoutWeightChangeAlg.class);
    /**
     * 按照权值重路由的版本
     */
//    @Override
//    public void defragment(Service blockedService) {
//        // sort
//        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
//
//        // change weights
//        changeEdgeWeight();
////        DijkstraShortestPath<EonVertex, E> dijkstraShortestPath = new DijkstraShortestPath<>(getGraph());
//
//        int k = 3;
//        KShortestPathAlgorithm<EonVertex, E> kShortestPathAlgorithm = new KShortestPaths<EonVertex, E>(getGraph(), k);
//
//        int count=0;
//
//        // defragment
//        Iterator<ServiceAssignment<E>> iterator = orderedSA.iterator();
//        while (iterator.hasNext()) {
//            ServiceAssignment<E> serviceAssignment = iterator.next();
//            int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
//
//            EonVertex source = new EonVertex(serviceAssignment.getService().getSource());
//            EonVertex destination = new EonVertex(serviceAssignment.getService().getDestination());
////            List<E> newPath = dijkstraShortestPath.getPath(source, destination).getEdgeList();
//            List<GraphPath<EonVertex, E>> paths = kShortestPathAlgorithm.getPaths(source, destination);
//            paths.sort(new Comparator<GraphPath<EonVertex, E>>() {
//                @Override
//                public int compare(GraphPath<EonVertex, E> o1, GraphPath<EonVertex, E> o2) {
//                    if (o1.getWeight() > o2.getWeight()) {
//                        return 1;
//                    } else if (o1.getWeight() < o2.getWeight()) {
//                        return -1;
//                    } else {
//                        return 0;
//                    }
//                }
//            });
//
//            BasicRSAAlg.tempReleaseService(serviceAssignment);
//            boolean isShift = false;
//
//            for (GraphPath<EonVertex,E> graphPath : paths) {
//                List<E> newPath = graphPath.getEdgeList();
//                int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(newPath,
//                        serviceAssignment.getService().getRequiredSlotNum());
//                if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
//                        shiftSlotStartIndex < minOccupiedSlotIndex) {
//                    // if there is enough resource for shifting.
//                    BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex, newPath);
//                    addServiceShiftTime(newPath.size(), blockedService.getStartTime());
//                    isShift = true;
//                    count++;
//                    break;
//                } else {
//                    // if no available choice, then restore
////                    BasicRSAAlg.tempAllocateService(serviceAssignment);
//                }
//            }
//
//            if (!isShift) {
//                BasicRSAAlg.tempAllocateService(serviceAssignment);
//            }
//        }
//
//        // restore weights
//        restoreEdgeWeight();
////        log.info("the number of shifting services is {}.", count);
//    }


    @Override
    public void defragment(Service blockedService) {
        // sort
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();

        int k = 2;
        KShortestPathAlgorithm<EonVertex, E> kShortestPathAlgorithm = new KShortestPaths<EonVertex, E>(getGraph(), k);

        List<ServiceAssignment<E>> residentialServiceList = Lists.newArrayList();
        for (int index=0; index< orderedSA.size(); index++) {
            ServiceAssignment<E> serviceAssignment = orderedSA.get(index);
            EonVertex source = new EonVertex(serviceAssignment.getService().getSource());
            EonVertex destination = new EonVertex(serviceAssignment.getService().getDestination());
            List<GraphPath<EonVertex, E>> paths = kShortestPathAlgorithm.getPaths(source, destination);
            paths.sort(new Comparator<GraphPath<EonVertex, E>>() {
                @Override
                public int compare(GraphPath<EonVertex, E> o1, GraphPath<EonVertex, E> o2) {
                    if (o1.getWeight() > o2.getWeight()) {
                        return 1;
                    } else if (o1.getWeight() < o2.getWeight()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
            for (GraphPath<EonVertex, E> graphPath : paths) {
                List<E> newPath = graphPath.getEdgeList();
                // 3 - business, 2 - cross, 1 - residential
                int privilege = EdgeWeightDescendComparator.calPrivilege(serviceAssignment);
                if (privilege == 2) {
                    // TODO 如果是cross业务，就要查验是否需要搬移residential业务以避让出空闲频谱资源来了。
                    BasicRSAAlg.tempReleaseService(serviceAssignment);
                    // 为了在后面应用getCrossShiftStartIndex这个方法,需要暂时更新serviceAssignment的path成员.
                    List<E> oldPath = serviceAssignment.getPath();
                    serviceAssignment.setPath(newPath);
                    int crossShiftSlotStartIndex = getCrossShiftStartIndex(serviceAssignment, blockedService);
                    serviceAssignment.setPath(oldPath);
                    if (crossShiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE) {
                        // 调整cross业务
                        BasicRSAAlg.shiftService(serviceAssignment, crossShiftSlotStartIndex, newPath);
                        addServiceShiftTime(serviceAssignment.getPath().size(),
                                blockedService.getStartTime());
                        // 一旦调整成功,则挑出paths的循环
                        break;
                    } else {
                        BasicRSAAlg.tempAllocateService(serviceAssignment);
                    }
                } else if (privilege == 1) {
                    // 如果是residential业务，先留存不做处理。因为cross业务的处理会影响到residential业务的排序
                    residentialServiceList.add(serviceAssignment);
                    // 留存后续处理,也要先跳出paths循环.
                    break;
                } else if (privilege == 3) {
                    // TODO 如果不是cross业务，该怎么往下压，还怎么往下压。
                    int minOccupiedSlotIndex = serviceAssignment.getStartIndex();

                    BasicRSAAlg.tempReleaseService(serviceAssignment);
                    int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(newPath,
                            serviceAssignment.getService().getRequiredSlotNum());
                    if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                            shiftSlotStartIndex < minOccupiedSlotIndex) {
                        // if there is enough resource for shifting.
                        BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex, newPath);
                        if (blockedService != null) {// 这一步判断是为了阈值重构触发
                            addServiceShiftTime(serviceAssignment.getPath().size(),
                                    blockedService.getStartTime());
                        }
                        // 一旦调整成功,就跳出paths的循环
                        break;
                    } else {
                        // if no available choice, then restore
                        BasicRSAAlg.tempAllocateService(serviceAssignment);
                    }
                } else {
                    // 不可能是除了1,2,3以外的取值了.
                }
            }
        }
        // TODO 下面的部分还没有进行多路径的处理.
        // 首先，进行排序，还是按照占用slot编号的降序进行排序。
        residentialServiceList.sort(new MaxSlotDescendComparator<E>());
        // 开始处理residential业务

        for (int residentialIndex=0; residentialIndex<residentialServiceList.size(); residentialIndex++) {
            ServiceAssignment<E> serviceAssignment = residentialServiceList.get(residentialIndex);
            EonVertex source = new EonVertex(serviceAssignment.getService().getSource());
            EonVertex destination = new EonVertex(serviceAssignment.getService().getDestination());
            List<GraphPath<EonVertex, E>> paths = kShortestPathAlgorithm.getPaths(source, destination);
            for (GraphPath<EonVertex, E> graphPath : paths) {
                int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
                List<E> newPath = graphPath.getEdgeList();
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(newPath,
                        serviceAssignment.getService().getRequiredSlotNum());
                if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                        shiftSlotStartIndex < minOccupiedSlotIndex) {
                    // if there is enough resource for shifting.
                    BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex, newPath);
                    if (blockedService != null) {// 这一步判断是为了阈值重构触发
                        addServiceShiftTime(serviceAssignment.getPath().size(),
                                blockedService.getStartTime());
                    }
                    // 如果调整成功,则跳出paths的循环
                    break;
                } else {
                    // if no available choice, then restore
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            }
        }
    }
}
