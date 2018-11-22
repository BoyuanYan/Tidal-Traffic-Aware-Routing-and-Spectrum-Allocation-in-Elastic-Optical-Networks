package org.yby.ecoc2017.dataCollection;

import com.google.common.collect.Lists;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.*;

/**
 * calculate blocking probability.
 * @author yby
 */
public final class ServiceBlockingProbability {
    public static final double EMPTY_SERVICE = 0;

    private static ServiceBlockingProbability ourInstance = new ServiceBlockingProbability();

    public static ServiceBlockingProbability getInstance() {
        return ourInstance;
    }

    private ServiceBlockingProbability() {
    }

    /**
     *
     * @param passedNum
     * @param blockedNum
     * @return
     */
    public double calculateBP(double passedNum, double blockedNum) {
        return blockedNum / (passedNum+blockedNum);
    }

    /**
     * calculate blocking probability range from startTime to endTime.
     * Notification: every service must be happened within startTime and endTime!!!
     * @param startTime start time
     * @param endTime end time
     * @param passedService passed Service map
     * @param blockedService blocked Service map
     * @param dividedNum divide (entTIme-startTime) by dividedNum, and get some timescopes.
     * @param vertexes source node or destination node included in vertexes set.
     * @return Blocking Probability
     */
    public <E extends EonEdge> ArrayList<Double> calculateBP(Calendar startTime, Calendar endTime,
                                                             TreeMap<Integer, ServiceAssignment<E>> passedService,
                                                             TreeMap<Integer, Service> blockedService, int dividedNum,
                                                             Set<Integer> vertexes) {
        Iterator<ServiceAssignment<E>> passedIterator = passedService.values().iterator();
        Iterator<Service> blockedIterator = blockedService.values().iterator();
        // 这里可能出现有余数的情况，不重要
        // unit is ms
        int timeInterval = (int)(endTime.getTimeInMillis() - startTime.getTimeInMillis()) / dividedNum + 1;
        // initialization
        double[] passed = new double[dividedNum];
        double[] blocked = new double[dividedNum];
        for (int i=0; i<dividedNum; i++) {
            passed[i] = 0;
            blocked[i] = 0;
        }
        // accumulate passed services
        Calendar tmp = Calendar.getInstance();
        tmp.setTimeInMillis(startTime.getTimeInMillis() + timeInterval);
        int index = 0;
        // 先找第一个在范围内的业务
        while(passedIterator.hasNext()) {
            Service service = passedIterator.next().getService();
            if (vertexes.contains(service.getSource()) && vertexes.contains(service.getDestination())) {
                if (service.getStartTime().after(startTime)) {
                    break;
                }
            }
        }
        // 把找到的这个算进去
        passed[0]=1;

        while(passedIterator.hasNext()) {
            Service service = passedIterator.next().getService();
            // make sure the service is from or to node of vertexes.
            if (vertexes.contains(service.getSource()) && vertexes.contains(service.getDestination())) {
                while (service.getStartTime().after(tmp)) {
                    index++;
                    tmp.add(Calendar.MILLISECOND, timeInterval);
                }
                if (index == dividedNum) {
                    // 如果相等，则表示已经走到了头
                    break;
                }
                passed[index] = passed[index] +1;
            }
        }
        // accumulate blocked services
        tmp.setTimeInMillis(startTime.getTimeInMillis() + timeInterval);
        index = 0;
        while(blockedIterator.hasNext()) {
            Service service = blockedIterator.next();
            if (vertexes.contains(service.getSource()) && vertexes.contains(service.getDestination())) {
                if (service.getStartTime().after(startTime)) {
                    break;
                }
            }
        }
        // 把找到的这个算进去
        blocked[0]=1;

        while(blockedIterator.hasNext()) {
            Service service = blockedIterator.next();
            // make sure the service is from or to node of vertexes.
            if (vertexes.contains(service.getSource()) && vertexes.contains(service.getDestination())) {
                while(service.getStartTime().after(tmp)) {
                    index++;
                    tmp.add(Calendar.MILLISECOND, timeInterval);
                }
                if (index == dividedNum) {
                    // 如果相等，则表示已经走到了头
                    break;
                }
                blocked[index] = blocked[index] +1;
            }
        }

        ArrayList<Double> rtn = Lists.newArrayListWithCapacity(dividedNum);
        for (int i=0; i<dividedNum; i++) {
            double total = blocked[i] + passed[i];
            if (total == 0) {
                rtn.add(EMPTY_SERVICE);
            } else {
                rtn.add(blocked[i]/total);
            }
        }

        return rtn;
    }

    /**
     * calculate blocking probability range from startTime to endTime.
     * Notification: every service must be happened within startTime and endTime!!!
     * @param startTime start time
     * @param endTime end time
     * @param passedService passed Service map
     * @param blockedService blocked Service map
     * @param dividedNum divide (entTIme-startTime) by dividedNum, and get some timescopes.
     * @param srcVertexes source node included in vertexes set.
     * @param dstVertexes destination node included in vertexes set.
     * @return Blocking Probability
     */
    public <E extends EonEdge> ArrayList<Double> calculateBP(Calendar startTime, Calendar endTime,
                                                             TreeMap<Integer, ServiceAssignment<E>> passedService,
                                                             TreeMap<Integer, Service> blockedService, int dividedNum,
                                                             Set<Integer> srcVertexes, Set<Integer> dstVertexes) {
        Iterator<ServiceAssignment<E>> passedIterator = passedService.values().iterator();
        Iterator<Service> blockedIterator = blockedService.values().iterator();
        // 这里可能出现有余数的情况，不重要
        // unit is ms
        int timeInterval = (int)(endTime.getTimeInMillis() - startTime.getTimeInMillis()) / dividedNum + 1;
        // initialization
        double[] passed = new double[dividedNum];
        double[] blocked = new double[dividedNum];
        for (int i=0; i<dividedNum; i++) {
            passed[i] = 0;
            blocked[i] = 0;
        }
        // accumulate passed services
        Calendar tmp = Calendar.getInstance();
        tmp.setTimeInMillis(startTime.getTimeInMillis() + timeInterval);
        int index = 0;
        while(passedIterator.hasNext()) {
            Service service = passedIterator.next().getService();
            // make sure the service is from or to node of vertexes.
            if ((srcVertexes.contains(service.getSource()) && dstVertexes.contains(service.getDestination())) ||
                    (srcVertexes.contains(service.getDestination()) && dstVertexes.contains(service.getSource()))) {
                while (service.getStartTime().after(tmp)) {
                    index++;
                    tmp.add(Calendar.MILLISECOND, timeInterval);
                }
                passed[index] = passed[index] +1;
            }
        }
        // accumulate blocked services
        tmp.setTimeInMillis(startTime.getTimeInMillis() + timeInterval);
        index = 0;
        while(blockedIterator.hasNext()) {
            Service service = blockedIterator.next();
            // make sure the service is from or to node of vertexes.
            if ((srcVertexes.contains(service.getSource()) && dstVertexes.contains(service.getDestination())) ||
                    (srcVertexes.contains(service.getDestination()) && dstVertexes.contains(service.getSource()))) {
                while(service.getStartTime().after(tmp)) {
                    index++;
                    tmp.add(Calendar.MILLISECOND, timeInterval);
                }
                blocked[index] = blocked[index] +1;
            }
        }

        ArrayList<Double> rtn = Lists.newArrayListWithCapacity(dividedNum);
        for (int i=0; i<dividedNum; i++) {
            double total = blocked[i] + passed[i];
            if (total == 0) {
                rtn.add(-1.0d);
            } else {
                rtn.add(blocked[i]/total);
            }
        }

        return rtn;
    }

}
