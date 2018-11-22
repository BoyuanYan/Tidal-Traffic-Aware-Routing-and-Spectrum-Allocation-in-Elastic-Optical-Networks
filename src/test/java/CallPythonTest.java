import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by yby on 2017/4/4.
 */
public class CallPythonTest {
    public static final Logger log = LoggerFactory.getLogger(CallPythonTest.class);


    /**
     * http://alvinalexander.com/java/edu/pj/pj010016
     * http://commons.apache.org/proper/commons-exec/
     * ProcessBuilder
     */
    @Test
    public void interoprateTest() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("python","/home/yby/ecoc2017/pyForEcoc/drawTest.py");
//        processBuilder.directory(new File("~/ecoc2017/pyForEcoc"));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder result = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
                log.info(processBuilder.command().toString() + " --->: " + line);
            }
        } catch (IOException e) {
            log.warn("failed to read output from process", e);
        } finally {
            //IOUtils.closeQuietly(reader);
        }
        process.waitFor();
        int exit = process.exitValue();
        if (exit != 0) {
            throw new IOException("failed to execute:" + processBuilder.command() + " with result:" + result);
        } else {
            log.info("python process has been shutdown normally. ");
        }
    }
}
