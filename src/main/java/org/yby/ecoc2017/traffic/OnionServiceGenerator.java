package org.yby.ecoc2017.traffic;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

/**
 * 洋葱业务生成器。
 *
 */
public class OnionServiceGenerator extends SineServiceGenerator {
    private static final Logger log = LoggerFactory.getLogger(OnionServiceGenerator.class);

    private static final Set<Integer> level0 = Sets.newHashSet(12,13,16,17,22);
    private static final Set<Integer> level1 = Sets.newHashSet(9,10,11,14,15,18,21,23,24);
    private static final Set<Integer> level2 = Sets.newHashSet(6,7,8,19,20,25,26,27,28);
    private static final Set<Integer> level3 = Sets.newHashSet(1,2,3,4,5);

    private static final double rho_bias=140;
    public static double rho_0;
    public static double rho_1;
    public static double rho_2;
    public static double rho_3;

    /**
     * Notification: timeInterval must can be divided exactly by 60*24.
     *
     * @param timeInterval
     * @param rouBias
     * @param rouBusinessPeak
     * @param rouResidentialPeak
     * @param miu
     * @param minRequiredSlotNum
     * @param maxRequiredSlotNum
     * @param businessArea
     * @param residentialArea
     * @param vertexNum
     * @param startTime
     * @param days
     */
    public OnionServiceGenerator(int timeInterval, double rouBias, double rouBusinessPeak, double rouResidentialPeak, double miu, int minRequiredSlotNum, int maxRequiredSlotNum, ArrayList<Integer> businessArea, ArrayList<Integer> residentialArea, int vertexNum, Calendar startTime, int days) {
        super(timeInterval, rouBias, rouBusinessPeak, rouResidentialPeak, miu, minRequiredSlotNum, maxRequiredSlotNum, businessArea, residentialArea, vertexNum, startTime, days);
    }

    /**
     * 阶越函数
     * @param t
     * @return
     */
    private double epsilon(double t) {
        if (t>0) {
            return 1;
        } else if (t==0) {
            return 0.5;
        } else {
            return 0;
        }
    }

    /**
     * 合并集合为list。
     * @param in
     * @return
     */
    private ArrayList<Integer> combineIntList(Set<Integer>... in) {
        ArrayList<Integer> rtn = Lists.newArrayList();
        for (Set<Integer> st : in) {
            for (int i : st) {
                rtn.add(i);
            }
        }
        return rtn;
    }

    /**
     * 根据洋葱模型生成业务
     * 只生成6点到18点的business area的业务
     * @return
     */
    @Override
    public ArrayList<Service> generateServices() {
        int startIndex = 1;

        // get the start time of days, such as 2016-04-07 00:00:00.000
        ArrayList<Calendar> eachDayBegin = dayBegin(startTime, days);
        for (Calendar startPerDay : eachDayBegin) {
            // get every point to split one day to multiple parts.
            ArrayList<Calendar> intervalPoint = minutesBegin(startPerDay, timeInterval);
            for (int i=1; i<intervalPoint.size(); i++) {
                Calendar tmpStartTime = intervalPoint.get(i-1);
                Calendar tmpEndTime = intervalPoint.get(i);

                double t = getDoubleTime(tmpStartTime, tmpEndTime);
                double r0 = epsilon(t-6)*epsilon(18-t)*rho_0*(1+Math.sin(Math.PI/2.0d + Math.PI/6.0d * t));
                double r1 = epsilon(t-6)*epsilon(18-t)*rho_1*(1+Math.sin(Math.PI/2.0d + Math.PI/6.0d * t));
                double r2 = epsilon(t-6)*epsilon(18-t)*rho_2*(1+Math.sin(Math.PI/2.0d + Math.PI/6.0d * t));
                double r3 = epsilon(t-6)*epsilon(18-t)*rho_3*(1+Math.sin(Math.PI/2.0d + Math.PI/6.0d * t));

                // generate three service queues for business area, residential area, and cross area.
                if (rho_bias != 0) {
                    ServiceGenerator r_bias_generator = new ServiceGenerator(
                            combineIntList(level0, level1, level2, level3),
                            rho_bias, miu, tmpStartTime,
                            tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                    ArrayList<Service> biasServices = r_bias_generator.generateServices();
                    serviceQueue.addServiceList(biasServices);
                    startIndex = startIndex + biasServices.size();
                }
                if (r0 != 0) {
                    ServiceGenerator r0_generator = new ServiceGenerator(
                            combineIntList(level0),
                            r0, miu, tmpStartTime,
                            tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                    ArrayList<Service> biasServices = r0_generator.generateServices();
                    serviceQueue.addServiceList(biasServices);
                    startIndex = startIndex + biasServices.size();
                }
                if (r1 != 0) {
                    ServiceGenerator r1_generator = new ServiceGenerator(
                            combineIntList(level0, level1),
                            r1, miu, tmpStartTime,
                            tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                    ArrayList<Service> biasServices = r1_generator.generateServices();
                    serviceQueue.addServiceList(biasServices);
                    startIndex = startIndex + biasServices.size();
                }
                if (r2 != 0) {
                    ServiceGenerator r2_generator = new ServiceGenerator(
                            combineIntList(level0, level1, level2),
                            r2, miu, tmpStartTime,
                            tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                    ArrayList<Service> biasServices = r2_generator.generateServices();
                    serviceQueue.addServiceList(biasServices);
                    startIndex = startIndex + biasServices.size();
                }
                if (r3 != 0) {
                    ServiceGenerator r3_generator = new ServiceGenerator(
                            combineIntList(level0, level1, level2, level3),
                            r3, miu, tmpStartTime,
                            tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                    ArrayList<Service> biasServices = r3_generator.generateServices();
                    serviceQueue.addServiceList(biasServices);
                    startIndex = startIndex + biasServices.size();
                }


//                // residential area
//                ArrayList<Service> residentialServices;
//                if (tmpResidentialRou != 0) {
//                    ServiceGenerator residentialGenerator = new ServiceGenerator(residentialArea, tmpResidentialRou, miu,
//                            tmpStartTime, tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
//                    residentialServices = residentialGenerator.generateServices();
//                    if (Debug.ENABLE_DEBUG && residentialServices.isEmpty()) {
//                        log.warn("residential: no service arrival and leave between {} and {} with rou equals {}.",
//                                format.format(tmpStartTime.getTime()),
//                                format.format(tmpEndTime.getTime()),
//                                tmpResidentialRou);
//                    }
//                } else {
//                    residentialServices = Lists.newArrayList();
//                }
//
//                serviceQueue.addServiceList(residentialServices);
//                startIndex = startIndex + residentialServices.size();
//                // cross area
//                ServiceGenerator crossGenerator = new ServiceGenerator(businessArea, residentialArea, rouBias, miu,
//                        tmpStartTime, tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
//                ArrayList<Service> crossServices = crossGenerator.generateServices();
//                if (Debug.ENABLE_DEBUG && crossServices.isEmpty()) {
//                    log.warn("cross: no service arrival and leave between {} and {} with rou equals {}.",
//                            format.format(tmpStartTime.getTime()),
//                            format.format(tmpEndTime.getTime()),
//                            rouBias);
//                }
//                serviceQueue.addServiceList(crossServices);
//                startIndex = startIndex + crossServices.size();
            }
        }

        return serviceQueue.combineServiceList();
    }
}
