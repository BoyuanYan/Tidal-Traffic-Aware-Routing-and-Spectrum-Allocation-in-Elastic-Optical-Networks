package org.yby.ecoc2017.defragAlgorithm.icocn2017_2;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.patel.ShortestPathDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.*;

/**
 * 用K算法进行重构
 */
public class KLEAlg<E extends EonEdge>  extends ShortestPathDefragmentationAlg<E> {
    public KLEAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        super(graph, currentServices);
    }


    int serviceShiftTime;
    int checkedServiceNum;

    /**
     *
     * @param lastService 不是被阻塞的业务，而是触发前处理的最后一个离去的业务，所以时间应该从入参业务的离去时间算起。
     */
    @Override
    public void defragment(Service lastService) {
        // 获取被重构业务的个数
        int num = getCurrentServices().size();
        // Step 1
        // 按照被占用slot的降序排序
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
        Iterator<ServiceAssignment<E>> iteratorLong = orderedSA.iterator();
        Iterator<ServiceAssignment<E>> iteratorShort = orderedSA.iterator();

        checkedServiceNum = num;
        Calendar seperationTime = Calendar.getInstance();
        // 计算阈值时间分界点。
        seperationTime.setTimeInMillis(
                lastService.getEndTime().getTimeInMillis() + Configuration.restTimeInMs);
        KShortestPathAlgorithm<EonVertex, E> kShortestPathAlgorithm =
                new KShortestPaths<EonVertex, E>(getGraph(), Configuration.k);

        // defragment
        while (iteratorLong.hasNext()) {
            ServiceAssignment<E> serviceAssignment = iteratorLong.next();
            if (serviceAssignment.getService().getEndTime().after(seperationTime)) {
                int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
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
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                boolean isShift = false;
                for (GraphPath<EonVertex, E> graphPath : paths) {
                    List<E> newPath = graphPath.getEdgeList();
                    int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(newPath,
                            serviceAssignment.getService().getRequiredSlotNum());
                    if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                            shiftSlotStartIndex < minOccupiedSlotIndex) {
                        // if there is enough resource for shifting.
                        BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex, newPath);
                        addServiceShiftTime(newPath.size(), lastService.getEndTime());
                        isShift = true;
                        break;
                    } else {
                        // if no available choice, then restore
//                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                    }
                }
                if (!isShift) {
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            }
        }

        // 第二次循环
        // defragment
        while (iteratorShort.hasNext()) {
            ServiceAssignment<E> serviceAssignment = iteratorShort.next();
            if (!serviceAssignment.getService().getEndTime().after(seperationTime)) {
                int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
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
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                boolean isShift = false;
                for (GraphPath<EonVertex, E> graphPath : paths) {
                    List<E> newPath = graphPath.getEdgeList();
                    int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(newPath,
                            serviceAssignment.getService().getRequiredSlotNum());
                    if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                            shiftSlotStartIndex < minOccupiedSlotIndex) {
                        // if there is enough resource for shifting.
                        BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex, newPath);
                        addServiceShiftTime(newPath.size(), lastService.getEndTime());
                        isShift = true;
                        break;
                    } else {
                        // if no available choice, then restore
//                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                    }
                }
                if (!isShift) {
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            }
        }

    }

    /**
     * 获取本次重构的业务搬移次数
     * @return
     */
    int getShiftTime(){
        return serviceShiftTime;
    }

    /**
     * 获取本次重构，需要进行重构的业务数量，注意，并不是实际上被重构的业务数量。
     * @return
     */
    int getCheckedServiceNum() {
        return checkedServiceNum;
    }
}
