package RSAAlg;

import com.google.common.collect.Lists;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.builder.UndirectedWeightedGraphBuilderBase;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.FirstFitRSAAlg;
import org.yby.ecoc2017.SimulationPlotline;
import org.yby.ecoc2017.dataCollection.ServiceBlockingProbability;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceGenerator;
import org.yby.ecoc2017.traffic.ServiceQueue;
import org.yby.ecoc2017.traffic.Timestamp;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * @author yby
 */
public class FirstFitRSAAlgTest {

    private static final Logger log = LoggerFactory.getLogger(FirstFitRSAAlgTest.class);
    private FirstFitRSAAlg alg;

//    @Before
    public void init() {
        UndirectedWeightedGraphBuilderBase builderBase = SimpleWeightedGraph.builder(EonEdge.class);
        SimpleWeightedGraph<EonVertex, EonEdge> graph = (SimpleWeightedGraph<EonVertex, EonEdge>)builderBase.build();
        alg = new FirstFitRSAAlg(graph, null, null);
    }

    @Test
    public void searchLowestAvaiIndexTest() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(3);
        list.add(4);
        list.add(6);
        list.add(7);
        list.add(8);
        list.add(10);
        list.add(12);
        list.add(13);
        list.add(14);
        list.add(15);

//        log.info("{}", alg.searchLowestAvaiIndex(list, 1));
//        log.info("{}", alg.searchLowestAvaiIndex(list, 2));
//        log.info("{}", alg.searchLowestAvaiIndex(list, 3));
//        log.info("{}", alg.searchLowestAvaiIndex(list, 4));
    }


    @Test
    public void firstFitRSATest() {
        ArrayList<SimpleWeightedGraph<EonVertex, EonEdge>> netList = SimulationPlotline.parseNets();

        double rou = 20;
        double miu = 2;
        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis(startTime.getTimeInMillis());
        endTime.add(Calendar.HOUR, 24);
        int minRequiredSlotNum = 1;
        int maxRequiredSlotNum = 5;
        int startIndex = 1;

        for (SimpleWeightedGraph<EonVertex, EonEdge> graph : netList) {
            ArrayList<Integer> avaiVertexes = generateVertexList(graph.vertexSet().size());
            ServiceGenerator generator = new ServiceGenerator(avaiVertexes, rou, miu, startTime, endTime,
                                                    minRequiredSlotNum, maxRequiredSlotNum, startIndex);
            ArrayList<Service> services = generator.generateServices();
            ServiceQueue serviceQueue = new ServiceQueue();
            serviceQueue.addServiceList(services);
            ArrayList<Timestamp> serviceOrderedQueue = serviceQueue.sortQueue();
            FirstFitRSAAlg ffRSA = new FirstFitRSAAlg(graph, services, serviceOrderedQueue);
            ffRSA.allocate();

            // data collection
            ArrayList<Double> bp = ServiceBlockingProbability.getInstance().calculateBP(startTime, endTime,
                    ffRSA.getPassedServices(), ffRSA.getBlockedServices(), 20, null);
            log.info("The BP of network is {}.", bp);
        }


    }

    private ArrayList<Integer> generateVertexList(int size) {
        ArrayList<Integer> rtn = Lists.newArrayList();
        for (int i=1; i<=size; i++) {
            rtn.add(i);
        }
        return rtn;
    }


    @Test
    public void smtTest() throws Exception{
        File file = new File("USLIKENET_2017-03-12_1days_MLSPDwithFirstFitRSA_SpectrumMigratingTime.data");
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        ArrayList<Pair<Calendar, Integer>> smt = (ArrayList<Pair<Calendar, Integer>>)inputStream.readObject();
        log.info("size of smt is : {}.", smt.size());
    }
}
