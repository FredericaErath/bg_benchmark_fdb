/**
 * @description BGCoord for janusgraph
 * @date 2025/3/27 11:02
 * @version 1.0
 */

package edu.usc.bg;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.binary.TypeSerializerRegistry;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

public class JanusGraphBGCoord {

    private String workload;
    private double latency;
    private double perc;
    private double staleness;
    private int duration;
    private String directory;
    private int minimum;
    private int maximum;
    private String objective;
    private boolean validation;

    public static void main(String[] args) throws Exception {
        JanusGraphBGCoord coord = new JanusGraphBGCoord();
        coord.readCmdArgs(args);
        int res = coord.runBinarySearch();
        System.out.println("Result: " + res);
    }


    public boolean checkSLA(int count) {
        String satisLinePrefix = "[SatisfyingPerc] ";
        String staleLinePrefix = "staleness Perc (gran:user)=";

        double satisPerc = -1;
        double staleDataPerc = -1;

        File bgLog = new File(directory+"/BGMainClass-" + count + ".log");
        try (BufferedReader reader = new BufferedReader(new FileReader(bgLog))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(satisLinePrefix)) {
                    int index = line.indexOf(satisLinePrefix) + satisLinePrefix.length();
                    String numStr = line.substring(index).trim();
                    satisPerc = Double.parseDouble(numStr);
                    break;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading BG log: " + e.getMessage());
            return false;
        }
        System.out.println("satisPerc read from BGMainClass-" + count + " is: "+satisPerc);

        if(validation){
            File valLog = new File(directory+"/ValidationMainClass-" + count + ".log");
            try (BufferedReader reader = new BufferedReader(new FileReader(valLog))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(staleLinePrefix)) {
                        int index = line.indexOf(staleLinePrefix) + staleLinePrefix.length();
                        String numStr = line.substring(index).trim();
                        staleDataPerc = Double.parseDouble(numStr);
                        break;
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error reading Validation log: " + e.getMessage());
                return false;
            }
            System.out.println("Staleness read from ValidationMainClass-" + count + " is: "+staleDataPerc);
            // 3. check SLA
            return satisPerc >= perc && staleDataPerc <= staleness;
        }
        return satisPerc >= perc;
    }


    private double simulatePerformance(int threads) {
        return -Math.pow(threads - 50, 2) + 2500;
    }

    public int runBinarySearch() throws Exception {
        int left = minimum;
        int right = maximum;
        int bestValid = -1;
        int count = 0;

        while (left <= right) {
            int mid = (left + right) / 2;
            System.out.println("Testing, number of threads: T = " + mid);

            startClient(mid, count);
            boolean slaMet = checkSLA(count);
            System.out.println("threadcount = " + mid +
                    ", SLA " + (slaMet ? "meet" : "not meet"));

            if (slaMet) {
                // if meet certain sla:
                bestValid = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return bestValid;
    }

    public void startClient(int threads, int count) throws Exception {
        clearLogFiles();
        clearDB();
        Process bgProcess = startBGMainClass(threads);

        String bgLog = watchProcessOutput(bgProcess,
                "SHUTDOWN!!!",
                "BGMainClass");

        saveToFile(directory+"/BGMainClass-" + count +".log", bgLog);

        if (validation) {
            Process validationProcess = startValidationMainClass(threads);

            String validationLog = watchProcessOutput(validationProcess,
                    " of reads observed the value of ",
                    "Data was stale...",
                    "ValidationMainClass");

            saveToFile(directory+"/ValidationMainClass-"+count+".log", validationLog);
        }
    }

    private Process startBGMainClass(int threads) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-cp");
        commands.add("build/classes:lib/*");
        commands.add("edu.usc.bg.BGMainClass");

        commands.add("onetime");
        commands.add("-t");
        commands.add("edu.usc.bg.workloads.CoreWorkLoad");
        commands.add("-threads");
        commands.add(String.valueOf(threads));
        commands.add("-db");
        commands.add("janusgraph.JanusGraphClient");
        commands.add("-P");
        commands.add(workload);
        commands.add("-latency");
        commands.add(String.valueOf(latency));
        commands.add("-maxexecutiontime");
        commands.add(String.valueOf(duration));
        commands.add("-s");
        commands.add("true");

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);

        return pb.start();
    }

    private Process startValidationMainClass(int threads) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-cp");
        commands.add("build/classes:lib/*");
        commands.add("edu.usc.bg.validator.ValidationMainClass");

        commands.add("-t");
        commands.add("edu.usc.bg.workloads.CoreWorkLoad");
        commands.add("-threads");
        commands.add(String.valueOf(threads));
        commands.add("-db");
        commands.add("janusgraph.JanusGraphClient");
        commands.add("-P");
        commands.add(workload);
        commands.add("-latency");
        commands.add(String.valueOf(latency));
        commands.add("-s");
        commands.add("true");

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);

        return pb.start();
    }

    private void saveToFile(String fileName, String content) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            pw.print(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String watchProcessOutput(Process process, String... keywords) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            boolean running = true;

            while (running && (line = br.readLine()) != null) {
                sb.append(line).append("\n");
                System.out.println("[process output] " + line); // 也可注释掉

                boolean keywordMatched = false;

                for (String kw : keywords) {
                    if (line.contains(kw)) {
                        System.out.println("[detect the line] " + kw);
                        keywordMatched = true;
                        break;
                    }
                }

                if (keywordMatched) {
                    // Read two more lines
                    if(validation){
                        System.out.println("reading next two lines then interrupt..");
                        for (int i = 0; i < 2; i++) {
                            String extraLine = br.readLine();
                            if (extraLine != null) {
                                sb.append(extraLine).append("\n");
                                System.out.println("[process output] " + extraLine);
                            } else {
                                break;
                            }
                        }
                    }
                    process.destroyForcibly();
                    running = false;
                }
            }
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    private void clearLogFiles() {
        String directory = "./";
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory " + directory + " does not exist or is not a directory.");
            return;
        }
        File[] files = dir.listFiles();
        System.out.println("files:");
        System.out.println(Arrays.toString(files));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if ((name.startsWith("read") && name.endsWith(".txt"))
                        || (name.startsWith("update") && name.endsWith(".txt"))) {
                    if (file.delete()) {
                        System.out.println("Deleted file: " + file.getAbsolutePath());
                    } else {
                        System.err.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public void clearDB() throws Exception {
        TypeSerializerRegistry registry = TypeSerializerRegistry.build()
                .addRegistry(JanusGraphIoRegistry.instance())
                .create();
        Cluster cluster = Cluster.build()
                .addContactPoint("128.110.96.123")
                .port(8182)
                .minConnectionPoolSize(10)
                .maxConnectionPoolSize(100)
                .maxSimultaneousUsagePerConnection(48)
                .maxWaitForConnection(5000)
                .serializer(new GraphBinaryMessageSerializerV1(registry))
                .maxContentLength(524288)
                .create();
        Client client = cluster.connect();
        // clear everything
        try (GraphTraversalSource g = traversal().withRemote(DriverRemoteConnection.using(cluster))) {
            g.V().drop().iterate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
            cluster.close();
        }
    }

    public void readCmdArgs(String[] args) {
        /**
         * -workload: specified workload, should be a file
         * -latency: response time in milliseconds
         * -staleness: percentage of stale data
         * -duration: duration in milliseconds
         * -directory: log directory name
         * -minimum: min threads
         * -maximum: maxi threads
         * -objective: rate soar or socialites, "soar" means rate soar, "socialites" means rate socialites
         * -validation: true or false, do validation or not
         */
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-workload":
                    if (i + 1 < args.length) {
                        workload = args[++i];
                    } else {
                        System.err.println("Missing value for -workload");
                        System.exit(1);
                    }
                    break;
                case "-latency":
                    if (i + 1 < args.length) {
                        latency = Double.parseDouble(args[++i]);
                    } else {
                        System.err.println("Missing value for -latency");
                        System.exit(1);
                    }
                    break;
                case "-perc":
                    if (i + 1 < args.length) {
                        perc = Double.parseDouble(args[++i]);
                    } else {
                        System.err.println("Missing value for -perc");
                        System.exit(1);
                    }
                    break;
                case "-staleness":
                    if (i + 1 < args.length) {
                        staleness = Double.parseDouble(args[++i]);
                    } else {
                        System.err.println("Missing value for -staleness");
                        System.exit(1);
                    }
                    break;
                case "-duration":
                    if (i + 1 < args.length) {
                        duration = Integer.parseInt(args[++i]);
                    } else {
                        System.err.println("Missing value for -duration");
                        System.exit(1);
                    }
                    break;
                case "-directory":
                    if (i + 1 < args.length) {
                        directory = args[++i];
                    } else {
                        System.err.println("Missing value for -directory");
                        System.exit(1);
                    }
                    break;
                case "-minimum":
                    if (i + 1 < args.length) {
                        minimum = Integer.parseInt(args[++i]);
                    } else {
                        System.err.println("Missing value for -minimum");
                        System.exit(1);
                    }
                    break;
                case "-maximum":
                    if (i + 1 < args.length) {
                        maximum = Integer.parseInt(args[++i]);
                    } else {
                        System.err.println("Missing value for -maximum");
                        System.exit(1);
                    }
                    break;
                case "-objective":
                    if (i + 1 < args.length) {
                        objective = args[++i].toLowerCase();
                        if (!objective.equals("soar") && !objective.equals("socialites")) {
                            System.err.println("Invalid value for -objective. Must be 'soar' or 'socialites'.");
                            System.exit(1);
                        }
                    } else {
                        System.err.println("Missing value for -objective");
                        System.exit(1);
                    }
                    break;
                case "-validation":
                    if (i + 1 < args.length) {
                        String val = args[++i].toLowerCase();
                        if (!val.equals("true") && !val.equals("false")) {
                            System.err.println("Invalid value for -validation. Must be 'true' or 'false'.");
                            System.exit(1);
                        }
                        validation = Boolean.parseBoolean(val);
                    } else {
                        System.err.println("Missing value for -validation");
                        System.exit(1);
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    break;
            }
        }
    }
}
