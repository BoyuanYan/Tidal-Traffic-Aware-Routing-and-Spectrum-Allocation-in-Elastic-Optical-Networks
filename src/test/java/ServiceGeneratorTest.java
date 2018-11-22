import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.Layer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceGenerator;
import org.yby.ecoc2017.traffic.ServiceQueue;
import org.yby.ecoc2017.traffic.Timestamp;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by yby on 2017/4/1.
 */
public class ServiceGeneratorTest {
    private static final Logger log = LoggerFactory.getLogger(ServiceGeneratorTest.class);

    @Test
    public void sortQueueTest() throws Exception {
        ArrayList<Integer> vertexList = new ArrayList<>(20);
        for (int i=0; i<20; i++) {
            vertexList.add(i+1);
        }

        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.HOUR, 1);
        ServiceGenerator generator = new ServiceGenerator(vertexList, 10, 3, startTime, endTime, 1, 5, 1);
        ArrayList<Service> services = generator.generateServices();

        generator.setStartTime(endTime);
        Calendar endTime2 = Calendar.getInstance();
        endTime2.setTimeInMillis(endTime.getTimeInMillis());
        endTime2.add(Calendar.HOUR, 1);
        generator.setEndTime(endTime2);
        generator.setRou(15);
        ArrayList<Service> services1 = generator.generateServices();

        ServiceQueue queue = new ServiceQueue();
        queue.addServiceList(services);
        queue.addServiceList(services1);

        List<Timestamp> sortedQueue = queue.sortQueue();

        StringBuilder builder = new StringBuilder("[");
        for(int i=0; i<sortedQueue.size(); i++) {
            builder.append(sortedQueue.get(i).getTime().getTimeInMillis()).append(",");
        }
        builder.append("]");
        log.info(builder.toString());

        for (int i=0; i<sortedQueue.size()-1; i++) {
            if (sortedQueue.get(i).getTime().after(sortedQueue.get(i+1).getTime())) {
                log.error("order error.");
            }
        }

    }

    @Test
    public void generateServicesTest() {
        ArrayList<Integer> vertexList = new ArrayList<>(20);
        for (int i=0; i<20; i++) {
            vertexList.add(i+1);
        }

        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.HOUR, 1);
        ServiceGenerator generator = new ServiceGenerator(vertexList, 10, 3, startTime, endTime, 1, 5, 1);
        ArrayList<Service> services = generator.generateServices();

        log.info("time scope is from {} to {}.", startTime.getTime().toLocaleString(), endTime.getTime().toLocaleString());
        log.info("the number of generated services is :{}", services.size());

        StringBuilder happenBuilder = new StringBuilder("[");
        StringBuilder endBuilder = new StringBuilder("[");
        for (Service service : services) {
            happenBuilder.append(service.getStartTime().getTimeInMillis()).append(",");
            endBuilder.append(service.getEndTime().getTimeInMillis()).append(",");
        }
        happenBuilder.append("]");
        endBuilder.append("]");
        log.info(happenBuilder.toString());
        log.info(endBuilder.toString());
    }

    @Test
    public void expoDist() {
        ExponentialDistribution exponentialDistribution = new ExponentialDistribution(0.2);
        double t = 60;
        double y = 0;
        ArrayList<Double> happendTime = new ArrayList<>();
        StringBuilder builder = new StringBuilder("[");
        while(y <= t) {
            double delta = exponentialDistribution.sample();
            y+=delta;
            happendTime.add(delta);
            builder.append(delta+",");
        }
        builder.append("]");
        System.out.println(builder.toString());
        System.out.println(happendTime.size());
    }

    @Test
    public void serviceTest() {
        PoissonDistribution poissonDistribution = new PoissonDistribution(5);
        int[] samples = poissonDistribution.sample(1000);
        double[] time = new double[1000];
        StringBuilder builder = new StringBuilder("[");
        for (int i=0; i<1000; i++) {
            if (samples[i] == 0) {
                samples[i] = 1;
            }
            builder.append(String.valueOf(1.0/samples[i]) + ",");
        }
        builder.append("]");
        System.out.println(builder.toString());

//        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
//        for(int i=0; i<1000; i++) {
//            dataset.addValue(((double)samples[i]), "s",String.valueOf(i));
//        }
//        JFreeChart chart = ChartFactory.createLineChart("test", "x", "y", dataset);
//        CategoryPlot plot = chart.getCategoryPlot();
////        IntervalMarker inter = new IntervalMarker(0, 1);
////        plot.addRangeMarker(inter, Layer.BACKGROUND);
//        FileOutputStream fos_jpg = null;
//        try {
//            fos_jpg = new FileOutputStream("D:\\BarChart.jpg");
//            ChartUtilities.writeChartAsJPEG(fos_jpg, 1.0f, chart, 400, 300, null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }finally {
//            try {
//                fos_jpg.close();
//            } catch (Exception e) {}
//        }

    }
}
