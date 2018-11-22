package org.yby.ecoc2017.defragAlgorithm.boyuan;

import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.FirstFitRSAAlg;
import org.yby.ecoc2017.SimulationPlotline;
import org.yby.ecoc2017.boot.Bootstrap;
import org.yby.ecoc2017.boot.EonNetParams;
import org.yby.ecoc2017.boot.SingleEonNet;
import org.yby.ecoc2017.dataCollection.ServiceBlockingProbability;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.*;
import org.yby.ecoc2017.utils.Debug;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * generate training data set in .csv format to be used for machine learning.
 * @author yby
 */
public class TrainingData {

    private static String FILE_SUFFIX = ".csv";
    private static String DATA_SUFFIX = ".data";
    private static String DATA_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static boolean APPEND = true;
    private static SimpleDateFormat format = new SimpleDateFormat(DATA_FORMAT);
    private static String dir_path = "J:/ecoc2017����/USLIKENET_�ı�bias��peak_200slots/";

    private static final Logger log = LoggerFactory.getLogger(TrainingData.class);

    // statistic interval is 2 minute.
    private static int interval = 2;


    public static void main(String[] args) {
        // http://www.cnblogs.com/growup/archive/2012/06/06/2538245.html
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        ArrayList<SimpleWeightedGraph<EonVertex, EonEdge>> nets = SimulationPlotline.parseNets();
        EonNetParams params = Bootstrap.getInstance().netParams;

        Calendar startTime = Calendar.getInstance();
        startTime.clear();
        startTime.set(2017, 6, 1, 6, 30, 21);
        int days = 1;
//        for (int date=1; date<2; date++){
            startTime.set(Calendar.DAY_OF_MONTH, 1);// 一号开始
            for (int i = 0; i < params.networks.size(); i++) {
                System.gc();
                SingleEonNet net = params.networks.get(i);
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
//                ArrayList<Service> services = generator.generateGlobalServices();
                ArrayList<Timestamp> serviceTimestamp = generator.generateServicesOrder(services);

                writeObjectsData(services, net.name, startTime, 1, (int)params.rouBias+"_"+(int)params.rouBusinessPeak+"_"+(int)params.rouResidentialPeak+"_Service");
                writeObjectsData(serviceTimestamp, net.name, startTime, 1, (int)params.rouBias+"_"+(int)params.rouBusinessPeak+"_"+(int)params.rouResidentialPeak+"_ServiceTimestamp");
                // pending
//                writeTrainingData(services, serviceTimestamp, net.vertexNum, net.name, startTime, days);

                /********************************************************************************************/

//                log.info("the size of service is {}.", services.size());
//            FirstFitRSAAlg ffRSA = new FirstFitRSAAlg(nets.get(i), services, serviceTimestamp);
//            ffRSA.allocate();
//
//            // data collection
//            double blocked = ffRSA.getBlockedServices().size();
//            double normal = ffRSA.getPassedServices().size();
//            log.info("blocked number is {}, total number is {}, bp is {}.", blocked, normal, blocked/(blocked+normal));
            }
//        }
    }

    public static void writeObjectsData(Object list, String netName, Calendar startTime,
                                         int days, String objName) {
        try {
            File output = new File(
                    netName + "_" + format.format(startTime.getTime()).split(" ")[0]
                            + "_" + days + "days_" +
                            objName + DATA_SUFFIX);
            if (!output.exists() || output.isFile()) {
                output.createNewFile();
            }

            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(output));
            outputStream.writeObject(list);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    /**
     *
     * @param services
     * @param serviceTimestamp
     * @param vertexNum
     * @param name
     */
    private static void writeTrainingData(ArrayList<Service> services, ArrayList<Timestamp> serviceTimestamp,
                                   int vertexNum, String name, Calendar startTime, int days) {
        try {
            File output = new File(name +"_"+days+"days" + FILE_SUFFIX);
            if (!output.exists() || output.isFile()) {
                output.createNewFile();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(output, APPEND));

            // initialization
            int[][] requiredNumMat= new int[vertexNum][vertexNum];
            for (int i=0; i<vertexNum; i++) {
                for (int j=0; j<vertexNum; j++) {
                    requiredNumMat[i][j] = 0;
                }
            }

            int year = startTime.get(Calendar.YEAR);
            int month = startTime.get(Calendar.MONTH);
            int day = startTime.get(Calendar.DAY_OF_MONTH);
            Calendar timePoint = Calendar.getInstance();
            timePoint.set(year, month, day, 0, 0, 0);
            timePoint.set(Calendar.MILLISECOND, 0);

            Calendar timeEnd = Calendar.getInstance();
            timeEnd.setTimeInMillis(timePoint.getTimeInMillis());
            timeEnd.add(Calendar.DAY_OF_YEAR, days);

            timePoint.add(Calendar.MINUTE, interval);

            for (Timestamp timestamp : serviceTimestamp) {

                while (timestamp.getTime().after(timePoint)) {
                    write(writer, requiredNumMat, timePoint);
                    timePoint.add(Calendar.MINUTE, interval);
                }
                Service service = services.get(timestamp.getServiceIndex()-1);
                handleService(service, timestamp, requiredNumMat);
            }

            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param writer
     * @param requiredNumMat
     * @param timePoint
     */
    private static void write(BufferedWriter writer, int[][] requiredNumMat, Calendar timePoint) throws Exception{
        int length = requiredNumMat.length;
        for (int i=0; i<length; i++) {
            for (int j=0; j<length; j++) {
                if (i != j) {
                    writer.write(String.valueOf(i+1) + ','
                            + String.valueOf(j+1) + ','
                            + format.format(timePoint.getTime()) + ','
                            + String.valueOf(requiredNumMat[i][j]) + "\n");
                }
            }

        }

        if (Debug.ENABLE_DEBUG) {
            int total = 0;
            for (int i=0; i<length; i++) {
                for (int j=0; j<length; j++) {
                    total = total + requiredNumMat[i][j];
                }
            }
            // log.info("total service num at {} is {}.", format.format(timePoint.getTime()), total);
        }
        writer.flush();
    }


    private static void handleService(Service service, Timestamp timestamp, int[][] requiredNumMat) {
//        Service service = services.get(timestamp.getServiceIndex()-1);
        if (Debug.ENABLE_DEBUG
                && !timestamp.getTime().equals(service.getStartTime())
                && !timestamp.getTime().equals(service.getEndTime())) {
            throw new RuntimeException("timestamp and service are not matched with each other!!!");
        }

        int sourceIndex = service.getSource();
        int destIndex = service.getDestination();
        if (timestamp.isStartTime()) {

            requiredNumMat[sourceIndex-1][destIndex-1] = requiredNumMat[sourceIndex-1][destIndex-1] + service.getRequiredSlotNum();
        } else {
            requiredNumMat[sourceIndex-1][destIndex-1] = requiredNumMat[sourceIndex-1][destIndex-1] - service.getRequiredSlotNum();
            if (Debug.ENABLE_DEBUG && requiredNumMat[sourceIndex-1][destIndex-1]<0) {
                throw new RuntimeException("required slot number is less than 0.");
            }
        }
    }

}
