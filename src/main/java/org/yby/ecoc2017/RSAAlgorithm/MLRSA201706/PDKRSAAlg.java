package org.yby.ecoc2017.RSAAlgorithm.MLRSA201706;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.RSAAlgorithm.FirstFitRSAAlg;
import org.yby.ecoc2017.boot.Bootstrap;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.traffic.Timestamp;
import org.yby.ecoc2017.utils.Debug;

import java.util.*;

/**
 * 进行
 */
public class PDKRSAAlg extends FirstFitRSAAlg {

    private static final Logger log = LoggerFactory.getLogger(PDKRSAAlg.class);
    // 表示预测准确度，百分平方均值误差
    public static double mape;

    public static int predictionIntervalInMins =30;
    static final int minToMs = 60*1000;
    final int k;
    final double alpha;
    private int weightedCount=0;
    private int defaultCount=0;
    private int samePathCount=0;
    private int bothBlockCount=0;

    private KShortestPathAlgorithm<EonVertex, EonEdge> kShortestPathAlgorithm;
    private DijkstraShortestPath<EonVertex, EonEdge> dijkstraShortestPath;
    private Map<EonEdge, Double> futureMap;

    public PDKRSAAlg(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue,
                     ArrayList<Timestamp> servicesOrderedQueue, int k, double alpha) {
        super(graph, servicesQueue, servicesOrderedQueue);
        this.k=k;
        this.alpha=alpha;
        dijkstraShortestPath = new DijkstraShortestPath<>(graph);
        kShortestPathAlgorithm = new KShortestPaths<>(graph, k);
    }


    /**
     * 恢复边权为默认值1.
     */
    private void restoreWeight() {
        for (EonEdge edge : getGraph().edgeSet()) {
            getGraph().setEdgeWeight(edge, 1);
        }
    }

    /**
     * 更新图的边权为入参所指。
     * @param weights
     */
    private void updateWeight(Map<EonEdge, Double> weights) {
        for (Map.Entry<EonEdge, Double> entry : weights.entrySet()) {
            double p  = entry.getValue();
            double c = entry.getKey().occupiedSlotsNum();
            //TODO 此处还没有log(d+1)作为分母
            double edgeWeight = c+alpha*p;
            getGraph().setEdgeWeight(entry.getKey(), edgeWeight);
        }
    }

    /**
     * 判断两条路由是否经过同样的点序列。
     * @param defaultPath
     * @param weightedPath
     * @return
     */
    private boolean isEqual(GraphPath<EonVertex, EonEdge> defaultPath, GraphPath<EonVertex, EonEdge> weightedPath) {
        if (defaultPath.getLength() != weightedPath.getLength()) {
            return false;
        } else {
            for (EonVertex vertex : defaultPath.getVertexList()) {
                boolean isContained = false;
                for (EonVertex wVertex : weightedPath.getVertexList()) {
                    if (wVertex.equals(vertex)) {
                        isContained = true;
                    }
                }
                if (!isContained) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 比较按照最小跳数的一条路径和最小weight计算出来的k条路径的优劣。
     * @param defaultPath
     * @param defaultSubscript
     * @param weightedPaths
     * @param weightedSubscripts
     * @return
     */
    private Pair<GraphPath<EonVertex, EonEdge>, Integer> compare(GraphPath<EonVertex, EonEdge> defaultPath,
                                                                 int defaultSubscript,
                                                                 List<GraphPath<EonVertex, EonEdge>> weightedPaths,
                                                                 List<Integer> weightedSubscripts) {
        for(int i=0; i<weightedPaths.size(); i++) {
            Pair<GraphPath<EonVertex, EonEdge>, Integer> result =
                    compare(defaultPath, defaultSubscript,weightedPaths.get(i), weightedSubscripts.get(i));
            // 如果返回的是weightedPath，则跳出循环，直接返回
            if (!result.getFirst().equals(defaultPath)) {
                return result;
            }
        }
        return new Pair<>(defaultPath, defaultSubscript);
    }

    /**
     * 比较按照最小跳数和最小weight计算出来的两条路径的优劣。
     * @param defaultPath
     * @param defaultSubscript
     * @param weightedPath
     * @param weightedSubscript
     * @return
     */
    private Pair<GraphPath<EonVertex, EonEdge>, Integer> compare(GraphPath<EonVertex, EonEdge> defaultPath,
                                                                 int defaultSubscript,
                                                                 GraphPath<EonVertex, EonEdge> weightedPath,
                                                                 int weightedSubscript) {
        if (defaultSubscript == BasicRSAAlg.UNAVAILABLE && weightedSubscript != BasicRSAAlg.UNAVAILABLE) {
            weightedCount++;
            return new Pair<>(weightedPath, weightedSubscript);
        }
        if (weightedSubscript == BasicRSAAlg.UNAVAILABLE && defaultSubscript != BasicRSAAlg.UNAVAILABLE) {
            defaultCount++;
            return new Pair<>(defaultPath, defaultSubscript);
        }
        if (weightedSubscript == BasicRSAAlg.UNAVAILABLE && defaultSubscript == BasicRSAAlg.UNAVAILABLE) {
            bothBlockCount++;
            return new Pair<>(defaultPath, defaultSubscript);
        }

        // 如果两条路径上都有可以分配的资源的话
        // 1 比较跳数
        double defaultHop = defaultPath.getLength();
        double weightedHop = weightedPath.getLength();
        double difference = weightedHop - defaultHop;
        // 跳数差不可能为负数，因为根据default weight的D算法计算出来的就是跳数最短的路径
        if (difference <= 0) {
            if (difference != 0) {
                throw new RuntimeException("根据default weight计算出来的最短路不是最小跳数的路径");
            }
            // 判断两条路是否完全一致。
            if (!isEqual(defaultPath, weightedPath)) {
                // 如果不一致，说明走的是不同的路径。
                weightedCount++;
                return new Pair<>(weightedPath, weightedSubscript);
            } else {
                // 如果一致，说明走的是同样的路径。
                samePathCount++;
                return new Pair<>(weightedPath, weightedSubscript);
            }
        } else if (difference <=2) {
            // 如果多1跳，则default必须不少于3跳；如果多2跳，则default必须不少于6跳；如果多2跳以上，说明绕路太远，不予考虑。
            if (difference/defaultHop >= 0.33334) {
                defaultCount++;
                return new Pair<>(defaultPath, defaultSubscript);
            } else {
                // 2 比较最小可用slot编号
                double slotDiff = weightedSubscript - defaultSubscript;
                if (slotDiff <= Bootstrap.getInstance().netParams.slotNum * 0.2d) {
                    // 如果分配差值没有超过总slots可用数目的20%，说明差别在可接受范围内。
                    weightedCount++;
                    return new Pair<>(weightedPath, weightedSubscript);
                } else {
                    defaultCount++;
                    return new Pair<>(defaultPath, defaultSubscript);
                }
            }
        } else {
            defaultCount++;
            return new Pair<>(defaultPath, defaultSubscript);
        }
    }

    /**
     * 返回距离当前时间点到predictionIntervalInMins分钟后，这段时间内的到达和离去的业务流。
     * @param total
     * @param index
     * @return
     */
    protected List<Timestamp> subList(ArrayList<Timestamp> total, int index) {

        Calendar tmp = Calendar.getInstance();
        tmp.setTimeInMillis(total.get(index).getTime().getTimeInMillis() + predictionIntervalInMins*minToMs);

        int end = index+1;
        while (total.get(end).getTime().before(tmp)) {
            end++;
            if (end == total.size()) {
                break;
            }
        }
        if (end == index+1) {
            throw new RuntimeException("only one or no service between predictionInterval.");
        }
        /* Returns a view of the portion of this list between the specified
           {@code fromIndex}, inclusive, and {@code toIndex}, exclusive. */
        List<Timestamp> list = total.subList(index, end);
        return list;

    }

    /**
     * 生成EonEdge根据D算法将要被分配后产生的边上的负载map。
     * @param orderedList
     * @return
     */
    protected Map<EonEdge, Double> generateOccupiedSlotsNum(List<Timestamp> orderedList) {
        // initialize occupiedSlotsNum
        Map<EonEdge, Double> occupiedSlotsNum = Maps.newHashMap();
        for (EonEdge edge : getGraph().edgeSet()) {
            occupiedSlotsNum.put(edge, 0.0d);
        }


        // collect current services
        for (ServiceAssignment<EonEdge> servAssign : getCurrentServices().values()) {
            execVirtualServiceAssignment(occupiedSlotsNum, servAssign.getPath(),
                    true, servAssign.getService().getRequiredSlotNum());
        }

        // collect future services
        // key represents service index, value represents path of services
        Map<Integer, List<EonEdge>> futureService = Maps.newHashMap();
        for (int i=0; i<orderedList.size(); i++) {
            List<EonEdge> path;
            Timestamp timestamp = orderedList.get(i);
            int serviceIndex = timestamp.getServiceIndex();
            if (timestamp.isStartTime()) {
                Service service = getServicesQueue().get(serviceIndex-1);
                path = dijkstraShortestPath.getPath(
                        new EonVertex(service.getSource()), new EonVertex(service.getDestination()))
                        .getEdgeList();
                futureService.put(serviceIndex, path);
                execVirtualServiceAssignment(occupiedSlotsNum, path, true, service.getRequiredSlotNum());
            } else {
                int requiredSlotsNum = 0;
                if (getCurrentServices().containsKey(serviceIndex)) {
                    path = getCurrentServices().get(serviceIndex).getPath();
                    requiredSlotsNum = getServicesQueue().get(serviceIndex-1).getRequiredSlotNum();
                } else if (futureService.containsKey(serviceIndex)) {
                    path = futureService.get(serviceIndex);
                    // must be serviceIndex-1
                    requiredSlotsNum = getServicesQueue().get(serviceIndex-1).getRequiredSlotNum();
                    futureService.remove(serviceIndex);
                } else if (getBlockedServices().containsKey(serviceIndex)) {
                    // Do nothing.
                    path = null;
                } else {
                    throw new RuntimeException(
                            "service which neither belongs to current services nor future services leaves.");
                }
                execVirtualServiceAssignment(occupiedSlotsNum, path, false, requiredSlotsNum);

            }
        }

        if (Debug.ENABLE_DEBUG) {
            checkNoLessThanZero(occupiedSlotsNum);
        }
        return occupiedSlotsNum;
    }

    /**
     * verify all double value in input parameters is not less than 0.
     * @param occupiedSlotsNum condition about occupied slots number
     */
    protected void checkNoLessThanZero(Map<EonEdge, Double> occupiedSlotsNum) {
        for (double d : occupiedSlotsNum.values()) {
            if (d < 0 ) {
                throw new RuntimeException("less than zero!");
            }
        }
    }

    /**
     *
     * @param occupiedSlotsNum
     * @param path
     * @param isPlus
     */
    protected void execVirtualServiceAssignment(Map<EonEdge, Double> occupiedSlotsNum,
                                                List<EonEdge> path, boolean isPlus, int requiredSlotsNum) {
        if (path != null) {
            if (isPlus) {
                for (EonEdge edge : path) {
                    int num = requiredSlotsNum;
                    occupiedSlotsNum.put(edge, occupiedSlotsNum.get(edge) + num);
                }
            } else {
                for (EonEdge edge : path) {
                    int num = requiredSlotsNum;
                    occupiedSlotsNum.put(edge, occupiedSlotsNum.get(edge) - num);
                }
            }
        } else {
            // blocked service leave, do nothing.
        }
    }


    @Override
    public void allocate() {
        // 执行预测的时间点
        Calendar predictionTimePoint = Calendar.getInstance();
        predictionTimePoint.setTimeInMillis(getServicesOrderedQueue().get(0).getTime().getTimeInMillis()-1);

        for (int index=0; index<getServicesOrderedQueue().size(); index++) {
            Timestamp timestamp = getServicesOrderedQueue().get(index);
            Service serviceToBeAssigned = getServicesQueue().get(timestamp.getServiceIndex()-1);

            while (timestamp.getTime().after(predictionTimePoint)) {
                // 把时间往后推移一个预测时间间隔
                predictionTimePoint.setTimeInMillis(predictionTimePoint.getTimeInMillis() + predictionIntervalInMins*minToMs);
                List<Timestamp> future = subList(getServicesOrderedQueue(), index);
                // 获取未来的负载情况
                futureMap = generateOccupiedSlotsNum(future);
            }

            // 引入百分平方均值误差
            futureMap = introduceMAPE(futureMap, mape);

            // if this timestamp is start-time.
            if (timestamp.isStartTime()) {
                EonVertex src = new EonVertex(serviceToBeAssigned.getSource());
                EonVertex dst = new EonVertex(serviceToBeAssigned.getDestination());
                // 边权均为1的最短路
                GraphPath<EonVertex, EonEdge> defaultPath = dijkstraShortestPath.getPath(src, dst);
                int defaultSubscript = findFirstAvailableSlot(defaultPath.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
                // 边权根据当前流量负载和未来流量负载结合成的最优路径
                updateWeight(futureMap);
                List<GraphPath<EonVertex, EonEdge>> weightedPaths = kShortestPathAlgorithm.getPaths(src, dst);
                // 先按照hop-count的升序排序
                weightedPaths.sort(new Comparator<GraphPath<EonVertex, EonEdge>>() {
                    @Override
                    public int compare(GraphPath<EonVertex, EonEdge> o1, GraphPath<EonVertex, EonEdge> o2) {
                        if (o1.getLength() > o2.getLength()) {
                            return 1;
                        } else if (o1.getLength() < o2.getLength()) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                });
                List<Integer> weightedSubscripts = Lists.newArrayListWithExpectedSize(weightedPaths.size());
                for (GraphPath<EonVertex, EonEdge> weightedPath : weightedPaths) {
                    int weightedSubscript = findFirstAvailableSlot(
                            weightedPath.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
                    weightedSubscripts.add(weightedSubscript);
                }
                // 通过比较，找出最好的那个
                Pair<GraphPath<EonVertex, EonEdge>, Integer> better = compare(defaultPath,defaultSubscript,
                                                                                weightedPaths, weightedSubscripts);
                // 恢复边权
                restoreWeight();

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
        log.info("一共选用default路径{}次",defaultCount);
        log.info("一共选用weighted路径{}次。", weightedCount);
        log.info("计算出来同样的路径{}次", samePathCount);
        log.info("同时没有计算出可行路径{}次", bothBlockCount);
    }

    /**
     * 以mape概率左右摇摆
     * @param futureMap
     * @param mape
     */
    public Map<EonEdge, Double> introduceMAPE(Map<EonEdge, Double> futureMap, double mape) {
        assert mape >=0 && mape <=1;
        if (mape == 0) {
            return futureMap;
        }
        Map<EonEdge, Double> rtn = Maps.newHashMap();
        for (Map.Entry<EonEdge, Double> entry : futureMap.entrySet()) {
            double value = entry.getValue();
            double diff = value * Math.random() * mape * 2; // 左右摇摆的幅度应该是(-mape*2, mape*2)
            if (Math.random() > 0.5) {
                value = value + diff;
            } else {
                value = value - diff;
            }
            rtn.put(entry.getKey(), value);
        }
        return rtn;
    }
}
