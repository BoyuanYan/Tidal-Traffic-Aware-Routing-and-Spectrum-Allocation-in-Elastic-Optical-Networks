package org.yby.ecoc2017;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.builder.UndirectedWeightedGraphBuilderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.RSAAlgorithm.FirstFitRSAAlg;
import org.yby.ecoc2017.RSAAlgorithm.KFirstFitRSAAlg;
import org.yby.ecoc2017.RSAAlgorithm.MLRSA201706.PDKRSAAlg;
import org.yby.ecoc2017.RSAAlgorithm.MLRSA201706.PDRSAAlg;
import org.yby.ecoc2017.boot.Bootstrap;
import org.yby.ecoc2017.boot.EonNetParams;
import org.yby.ecoc2017.boot.SingleEonNet;
import org.yby.ecoc2017.dataCollection.ServiceBlockingProbability;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonSlot;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.OnionServiceGenerator;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * 只进行耗时测试
 */
public class TimeCost {

    private static final Logger log = LoggerFactory.getLogger(TimeCost.class);

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static int days = 1;
    private static int dotsPerDay = 24;

    /**
     * 第一个参数表示rho
     * 第二个参数表示预测准确度浮动
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // 解析rho
        int rho = Integer.parseInt(args[0]);
        OnionServiceGenerator.rho_0 = rho;
        OnionServiceGenerator.rho_1 = rho-10;
        OnionServiceGenerator.rho_2 = rho-20;
        OnionServiceGenerator.rho_3 = rho-30;


        EonNetParams params = Bootstrap.getInstance().netParams;
        System.out.println(Calendar.getInstance().getTime().toLocaleString());

        /********************************** initialize parameters ******************************/
        Calendar startTime = Calendar.getInstance();
        startTime.clear();
        startTime.set(2017, 6, 1, 6, 30, 21);

        for (int i = 0; i < params.networks.size(); i++) {
            SingleEonNet net = params.networks.get(i);
            log.info("/******************************* {} ***************************/", net.name);
            // 在OnionServiceGenerator中，businessArea，residentialArea，rouBias都是无用的
            OnionServiceGenerator generator = new OnionServiceGenerator(params.timeInterval,
                    params.rouBias,
                    params.rouBusinessPeak,
                    params.rouResidentialPeak,
                    params.miu,
                    params.minRequiredSlotNum,
                    params.maxRequiredSlotNum,
                    net.businessArea,
                    net.residentialArea,
                    net.vertexNum,
                    startTime,
                    days);
            ArrayList<Service> services = generator.generateServices();
            ArrayList<Timestamp> serviceTimestamp = generator.generateServicesOrder(services);

            log.info("The start time of service is {}, the end time of service is {}.",
                    format.format(services.get(0).getStartTime().getTime()),
                    format.format(services.get(services.size() - 1).getStartTime().getTime()));

            /************************** use RSA algorithm *****************************************/
            int ksp[] = {1,2,3,4,5,6,7,8,9,10};
            ArrayList<SimpleWeightedGraph<EonVertex, EonEdge>> network = parseNets();
            Set<Integer> wholeVertexes = generateVertexes(net.vertexNum);
            for (int m=0; m<ksp.length; m++) {
                int k_value = ksp[m];
                FirstFitRSAAlg ffRSA;
                if (k_value==1) {
                    ffRSA = new FirstFitRSAAlg(network.get(i), services, serviceTimestamp);
                } else {
                    ffRSA = new KFirstFitRSAAlg(network.get(i), services, serviceTimestamp, k_value);
                }
                checkSlotsEmpty(network.get(i));
                double sim_start = Calendar.getInstance().getTimeInMillis();
                ffRSA.allocate();
                double sim_end = Calendar.getInstance().getTimeInMillis();
                log.info("************************************************************");
                outputData(ffRSA, startTime, days, wholeVertexes, "whole", k_value + " RSA");
                double total_bp = ServiceBlockingProbability.getInstance().calculateBP(
                        ffRSA.getPassedServices().size(), ffRSA.getBlockedServices().size());
                log.info("name {}, cost time {}, bp {}", k_value+"-RSA", sim_end - sim_start, total_bp);
            }
        }
        System.out.println(Calendar.getInstance().getTime().toLocaleString());
    }

    /**
     *
     * @param RSA
     * @param startTime
     * @param days
     * @param vertexes
     * @param area
     */
    private static void outputData(
            BasicRSAAlg RSA, Calendar startTime, int days, Set<Integer> vertexes, String area, String algName) {
        ServiceBlockingProbability serviceBP = ServiceBlockingProbability.getInstance();

        int year = startTime.get(Calendar.YEAR);
        int month = startTime.get(Calendar.MONTH);
        int day = startTime.get(Calendar.DAY_OF_MONTH);
        Calendar zeroStartTime = Calendar.getInstance();
        zeroStartTime.set(year, month, day, 6, 0, 0);
        zeroStartTime.set(Calendar.MILLISECOND, 0);

        Calendar zeroEndTime = Calendar.getInstance();
        zeroEndTime.setTimeInMillis(zeroStartTime.getTimeInMillis());
//        zeroEndTime.add(Calendar.DAY_OF_YEAR, days);
        zeroEndTime.add(Calendar.HOUR_OF_DAY, 12);
        int dividedNum = days*dotsPerDay;
        ArrayList<Double> bps = serviceBP.calculateBP(zeroStartTime, zeroEndTime, RSA.getPassedServices(),
                RSA.getBlockedServices(), dividedNum, vertexes);

        log.info("The start and end of bp is {} and {} in {}.",
                format.format(zeroStartTime.getTime()),
                format.format(zeroEndTime.getTime()),
                algName);
        log.info("The BPs of {} area in {} are : ", area, algName);
        // output
        for (double bp : bps) {
            System.out.print(bp+",");
        }
        System.out.println("");
    }



    /**
     * check if there exists slots that are not released after RSA and Defragmentation algorithm.
     * @param graph graph
     */
    private static void checkSlotsEmpty(SimpleWeightedGraph<EonVertex, EonEdge> graph) {
        for (EonEdge edge : graph.edgeSet()) {
            for (EonSlot slot : edge.getSlots()) {
                if (slot.isOccupied()) {
                    throw new RuntimeException("there is a slot that isn't released after RSA and " +
                            "defragmentation algorithm at least.");
                }
            }
        }
    }



    private static Set<Integer> generateVertexes(int num) {
        Set<Integer> rtn = Sets.newHashSet();
        for (int i=1; i<=num; i++) {
            rtn.add(i);
        }
        return rtn;
    }


    /**
     *
     * @return
     */
    public static ArrayList<SimpleWeightedGraph<EonVertex, EonEdge>> parseNets() {
        List<SingleEonNet> netsParams = Bootstrap.getInstance().netParams.networks;
        ArrayList<SimpleWeightedGraph<EonVertex, EonEdge>> list =
                Lists.newArrayListWithCapacity(netsParams.size());
        for (int i=0; i<netsParams.size(); i++) {
            UndirectedWeightedGraphBuilderBase builderBase = SimpleWeightedGraph.builder(EonEdge.class);
            SingleEonNet eonNet = netsParams.get(i);
            ArrayList<EonVertex> vertexList = Lists.newArrayListWithCapacity(eonNet.vertexNum);
            // add vertexes
            for (int vertexIndex=1; vertexIndex<=eonNet.vertexNum; vertexIndex++) {
                EonVertex vertex = new EonVertex(vertexIndex, true);
                builderBase.addVertex(vertex);
                vertexList.add(vertex);
            }
            // add edges
            for (Pair<Integer,Integer> edge : eonNet.edges) {
                builderBase.addEdge(vertexList.get(edge.getFirst()-1),
                        vertexList.get(edge.getSecond()-1),
                        1.0);
            }

            list.add((SimpleWeightedGraph<EonVertex, EonEdge>) builderBase.build());
        }
        return list;
    }
}
