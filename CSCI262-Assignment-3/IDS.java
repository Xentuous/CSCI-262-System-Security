import java.io.*;
import java.util.*;

/**
 * This program runs a simulation of a system of Email System IDS.
 * This program does not tolerate any inconsistent data.
 * Any inconsistencies will result in early termination of the program.
 *
 * @author: Min Zhan Foo (7058810), Yin Li Zheng (6959593), Lee Yu Xian (7233164)
 * <p>
 * Exit codes:
 * 0: Success
 * 1: Consistency errors in configuration file
 * 2: File not found error
 * </p>
 *
 * <p>
 * 2D array map for storing of event data:           2D array map for storing of stats data:
 *    _____0________1______2_________3_________4___      _____0________1______2______
 * 0 |EventName1 | CD | Minimum | Maximum | Weight|   0 |EventName1 | Mean | StdDev |
 *   |============================================|     |===========================|
 * 1 |EventName2 | CD | Minimum | Maximum | Weight|   1 |EventName2 | Mean | StdDev |
 *   |============================================|     |===========================|
 * 2 |EventName3 | CD | Minimum | Maximum | Weight|   2 |EventName3 | Mean | StdDev |
 *   |============================================|     |===========================|
 * 3 |EventName4 | CD | Minimum | Maximum | Weight|   3 |EventName4 | Mean | StdDev |
 *   |============================================|     |===========================|
 * 4 |EventName5 | CD | Minimum | Maximum | Weight|   4 |EventName5 | Mean | StdDev |
 *   |============================================|     |===========================|
 * </p>
 * <p>
 * 2D array map of logging data:
 * <p>
 * y = Random generated value
 * <p>
 * _____0________D1______D2______D3______Dx____
 * 0 |Event      | Day 1 | Day 2 | Day 3 | Day x |
 * |===========================================|
 * 1 |EventName1 |   y   |   y   |   y   |   y   |
 * |===========================================|
 * 2 |EventName2 |   y   |   y   |   y   |   y   |
 * |===========================================|
 * 3 |EventName3 |   y   |   y   |   y   |   y   |
 * |===========================================|
 * 4 |EventName4 |   y   |   y   |   y   |   y   |
 * |===========================================|
 * 5 |EventName5 |   y   |   y   |   y   |   y   |
 * |===========================================|
 * </p>
 */

public class IDS {
    private static final ArrayList<String> eEventLists = new ArrayList<>();
    private static final ArrayList<String> sEventLists = new ArrayList<>();
    public static final String GREEN = "\u001B[32m";
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    private static final Random rand = new Random();
    private static final int eDataField = 5;
    private static final int sDataField = 3;
    private static String[][] eventArr;
    private static String[][] statsArr;
    private static String[][] weights;
    private static double[] alertDailyCounter;
    private static double[][] anomalyArr;
    private static int statsMonitor = 0;
    private static int eventMonitor = 0;
    private static int days;

    /**
     * <p>
     * Driver code that will read in CLI arguments and pass them to the
     * File class and day.
     * </p>
     * <p>
     * Consistence checks: <br>
     * 1) If the file is not found, the program will exit with code 2. <br>
     * 2) If the file is found, but the DAYS value is not an integer, the program will exit with code 1. <br>
     * 3) If the file is found, but the DAYS value is less than 1, the program will exit with code 1. <br>
     * 4) If the file is found, but the Events.txt and/or Stats.txt is empty, the program will exit with code 1. <br>
     * 5) If the file is found, but the Events.txt and/or Stats.txt is not consistent, the program will exit with code 1. <br>
     * </p>
     * @param args CLI arguments
     */
    public static void main(String[] args) {
        Scanner kb = new Scanner(System.in);
        File fileEvent = new File(args[0]);
        File fileStats = new File(args[1]);
        art();
        days = Integer.parseInt(args[2]);
        if (days < 1) {
            System.out.println(RED + "[!!] Number of days must be more than 0!" + RESET);
            System.exit(1);
        }

        if (fileEvent.exists() && fileStats.exists() && fileEvent.length() > 0 && fileStats.length() > 0) {
            try {
                init(fileEvent, fileStats);
                if (filter()) {
                    System.out.println(GREEN + "[+] Files integrity have been verified!" + RESET);

                    // copy statsArr to another array
                    String[][] statsArrCopy = new String[statsArr.length][statsArr[0].length];
                    for (int i = 0; i < statsArr.length; i++) {
                        System.arraycopy(statsArr[i], 0, statsArrCopy[i], 0, statsArr[0].length);
                    }

                    activityEngine("defaultBaseLineData.txt", statsArrCopy);
                    analysisEngine("defaultBaseLineData.txt", "defaultDayTotals.txt", "defaultBaseLineStats.txt");
                    alertEngine(kb);
                } else {
                    System.out.println(RED + "[!!] Early termination due to consistency checks" + RESET);
                    System.exit(1);
                }
                kb.close();
                System.out.println("Program terminated. Exit code: 0");
                System.exit(0);
            } catch (NumberFormatException | IOException e) {
                if (e instanceof NumberFormatException) {
                    System.out.println(RED + "[!!] Early termination due to DAYS input must be an integer." + RESET);
                } else {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        } else {
            System.out.printf(RED + "[!!] Early termination due to %s or %s does not exist or is empty OR Days value is less than 1!" + RESET, fileEvent.getName(), fileStats.getName());
            System.exit(2);
        }
    }

    /**
     * <p>
     * To initialise the program with the input files which it will be stored and
     * filtered in to arraylists and 2D arrays.
     * It will also check for consistency between the input files, ensuring both
     * have the same number of monitored events.
     * </p>
     * <p>
     * Consistency checks:<br>
     * 1) Ensure that number of recorded events compared with actual number of events<br>
     * 2) Ensure that number of events in Events.txt compared with number of events in Stats.txt<br>
     * </p>
     * <p>
     * <br>
     * Events.txt information:<br>
     * C - continuous event, float, double or integer value, occur at any value,
     * continuous event recorded at 2 decimal places<br>
     * D - discrete event, take in only integer values, occur 1 at a time.<br>
     * </p>
     * <p>EVENT_NAME: [CD]:Minimum:Maximum:Weight</p>
     * <p>
     * <br>
     * Stats.txt information:<br>
     * The first line again contains the number of events being monitored.<br>
     * </p>
     * <p>
     * EVENT_NAME:Mean:Standard Dev:
     * </p>
     *
     * @param eventFile Events.txt
     * @param statsFile Stats.txt
     * @throws IOException If the file is not found
     */
    private static void init(File eventFile, File statsFile) throws IOException {
        FileReader fr = new FileReader(eventFile);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        int fileCount = 0, noEventCount = 0, actualNoEventCount;

        while (line != null && fileCount < 2) {
            if (line.length() == 1) {
                if (fileCount == 0) eventMonitor = Integer.parseInt(line);
                if (fileCount == 1) statsMonitor = Integer.parseInt(line);
                noEventCount++;
                line = br.readLine();
                continue;
            }

            if (fileCount == 0) eEventLists.add(line);
            if (fileCount == 1) sEventLists.add(line);
            noEventCount++;
            actualNoEventCount = noEventCount - 1;
            line = br.readLine();

            if (line == null && fileCount == 0 && actualNoEventCount != eventMonitor) {
                System.out.println(RED + "[!!] Consistency error between actual number of event and number of event recorded in Events.txt!" + RESET);
                System.exit(1);
            }

            if (line == null && fileCount == 1 && actualNoEventCount != statsMonitor) {
                System.out.println(RED + "[!!] Consistency error between actual number of event and number of event recorded in Stats.txt!" + RESET);
                System.exit(1);
            }

            if (fileCount == 0 && noEventCount > eventMonitor && line == null) {
                fr = new FileReader(statsFile);
                br = new BufferedReader(fr);
                line = br.readLine();
                fileCount++;
                noEventCount = 0;
            }
        }
        br.close();

        if (eventMonitor != statsMonitor) {
            System.out.println(RED + "[!!] Number of monitored event between Event and Stats files do not match!\nExiting program now!" + RESET);
            System.exit(1);
        }
    }

    /**
     * <p>
     * To filter the arraylist information and store the data into 2D arrays.
     * </p>
     * <p>
     * It will replace the "::" with "-1 " to indicate that it has any value,<br>
     * which will later be replaced with a sensible value from by:<br>
     * replacing -1 with (mean - minimum) * 2 as a sensible maximum.<br>
     * replacing -1 with (maximum - mean) * 2 as a sensible minimum.<br>
     * </p>
     * <p>
     * Consistency checks:
     * </p>
     * <p>
     * 1) Ensure that Stats.txt contains any invalid value such as ::<br>
     * 2) Ensure that Stats.txt does not contain any negative values<br>
     * 3) Ensure that recorded Discrete values contain only integer value only [CD] events and positive weight value<br>
     * 4) Ensure that the Event.txt and Stats.txt have the same name of events<br>
     * 5) Ensure that values inserted matches [CD] requirements<br>
     * 6) Ensure that minimum value is not greater than the maximum value in the Events.txt<br>
     * 7) Ensure that Stats.txt Mean value is not greater than the Maximum value in the Events.txt<br>
     * </p>
     */
    private static boolean filter() {
        int satisfied = 0;
        eventArr = new String[eventMonitor][eDataField];
        statsArr = new String[statsMonitor][sDataField];

        // Ensure that Stats.txt does not contain any invalid value such as ::
        boolean invalidValue = sEventLists.stream().anyMatch(s -> s.contains("::"));

        // Ensure that stats.txt does not contain any negative values
        boolean negativeValue = sEventLists.stream().anyMatch(s -> {
            String[] split = s.split(":");
            for (String value : split) {
                try {
                    if (Double.parseDouble(value) < 0) return true;
                } catch (Exception ignored) {
                }
            }
            return false;
        });

        // Successful consistency check will lead to storing of Stats.txt data into statsArr 2D array
        if (!invalidValue && !negativeValue) {
            for (int i = 0; i < statsMonitor; i++)
                System.arraycopy(sEventLists.get(i).split(":"), 0, statsArr[i], 0, sDataField);
        } else if (invalidValue) {
            System.out.println(RED + "[!!] Some of the stats events have no value!" + RESET);
            return false;
        } else {
            System.out.println(RED + "[!!] Stats.txt cannot contain negative values!" + RESET);
            return false;
        }
        satisfied++;

        // Replacing :: with -1
        eEventLists.replaceAll(s -> s.replace("::", ":-1:"));

        // Store Event.txt event from arraylist into array
        for (int i = 0; i < eventMonitor; i++)
            System.arraycopy(eEventLists.get(i).split(":"), 0, eventArr[i], 0, eDataField);

        // Replacing -1 with (mean - minimum) * 2 as a sensible maximum.
        // Replacing -1 with (maximum - mean) * 2 as a sensible minimum.
        // Ensure that only [CD] events
        // Ensure that only have positive weight value
        // Ensure that recorded Discrete values contain only integer values,
        for (int i = 0; i < eventMonitor; i++) {
            if (eventArr[i][2].equals("-1")) {
                eventArr[i][2] = String.valueOf((int) ((Double.parseDouble(statsArr[i][1]) * 2) - Double.parseDouble(statsArr[i][3])));
            }

            if (eventArr[i][3].equals("-1")) {
                eventArr[i][3] = String.valueOf((int) ((Double.parseDouble(statsArr[i][1]) * 2) - Double.parseDouble(eventArr[i][2])));
            }

            for (int j = 1; j < eDataField; j++) {
                if (!eventArr[i][1].equals("C") && !eventArr[i][1].equals("D")) {
                    System.out.println(RED + "[!!] Events.txt event detail contains invalid event! [CD] only!" + RESET);
                    return false;
                }
                if (Double.parseDouble(eventArr[i][4]) < 0) {
                    System.out.println(RED + "[!!] Weights cannot contain negative integer values!" + RESET);
                    return false;
                }
            }
        }
        satisfied++;

        // Ensure that the event and stats files have the same name of events
        for (int i = 0; i < eventMonitor; ) {
            for (int j = 0; j < statsMonitor; j++) {
                if (!eventArr[i][0].equals(statsArr[j][0])) {
                    System.out.println(RED + "[!!] Monitored event does not match!" + eventArr[i][0] + " != " + statsArr[j][0] + RESET);
                    return false;
                }
                i++;
            }
        }
        satisfied++;

        // Ensure that minimum value is not greater than the maximum value in the Events.txt
        // Ensure that Stats.txt Mean value is not greater than the Maximum value in the Events.txt
        // Ensure that values inserted matches [CD] requirements
        for (int i = 0; i < eventMonitor; i++) {
            String eventDescription = (eventArr[i][1].equals("C")) ? "C" : "D";

            if (Double.parseDouble(eventArr[i][2]) > Double.parseDouble(eventArr[i][3])) {
                System.out.println(RED + "[!!] Minimum value cannot be greater than the maximum value!" + RESET);
                return false;
            }

            if (Double.parseDouble(eventArr[i][3]) < Double.parseDouble(statsArr[i][1])) {
                System.out.println(RED + "[!!] Maximum value cannot be greater than the Mean value!" + RESET);
                return false;
            }
            // Continuous Condition
            if (eventDescription.equalsIgnoreCase("C")) {
                for (int j = 2; j < eDataField; j++) {
                    if (eventArr[i][j].matches("[a-zA-Z]")) {
                        System.out.println(RED + "[!!] Event data field contains alphabets instead of numbers!" + RESET);
                        return false;
                    }
                    // If Continuous value is not recorded to 2 d.p., assign it to 2 d.p.
                    if (!eventArr[i][j].matches("^\\d*[.]\\d{2}$")) {
                        eventArr[i][j] = String.format("%.2f", Double.parseDouble(eventArr[i][j]));
                    }
                    // If Continuous value has a decimal place of more than 2, assign it to 2 d.p.
                    if (eventArr[i][j].matches("^\\d*[.]\\d{3,}$")) {
                        eventArr[i][j] = String.format("%.2f", Double.parseDouble(eventArr[i][j]));
                    }
                }
            }
            // Discrete Condition
            if (eventDescription.equalsIgnoreCase("D")) {
                for (int j = 2; j < eDataField; j++) {
                    if (eventArr[i][j].matches("[a-zA-Z]")) {
                        System.out.println(RED + "[!!] Event data field contains alphabets instead of numbers!" + RESET);
                        return false;
                    }

                    // Discrete value to only contain integer values
                    if (eventArr[i][j].matches("^\\d*[.]\\d+$")) {
                        System.out.println(RED + "[!!] Discrete event cannot contain non-integer values!" + RESET);
                        return false;
                    }
                }
            }
        }
        satisfied++;

        weights = new String[eventMonitor][2];
        for (int i = 0; i < eventMonitor; i++) {
            weights[i][0] = eventArr[i][0];
            weights[i][1] = eventArr[i][4];
        }

        return satisfied == 4;
    }

    /**
     * <p>To start generating and logging events which include the number of Days specified as args.</p>
     * <p>It will generate statistics which is consistent with Stats.txt and user's live.txt</p>
     * <p>
     * C - continuous event, float, double or integer value, occur at any value,
     * continuous event recorded at 2 decimal places<br>
     * D - discrete event, take in only integer values, occur 1 at a time.<br>
     * </p>
     * <p>EVENT_NAME: [CD]:Minimum:Maximum:Weight</p>
     * <p>
     * <br>
     * <p>EVENT_NAME:Mean:Standard Dev:</p>
     */
    private static void activityEngine(String baseLineDataFile, String[][] statsArr) {
        System.out.println("[*] Activity Simulation Engine is running...");
        String[][] baseLineData = new String[eDataField + 1][days + 1];
        baseLineData[0][0] = "Event(s)";

        for (int i = 1; i <= days; i++)
            baseLineData[0][i] = "D" + i;
        for (int i = 0; i < eventMonitor; i++)
            baseLineData[i + 1][0] = eventArr[i][0];

        for (int day = 1; day < days + 1; day++) {
            for (int CD = 0; CD < eventMonitor; CD++) {
                for (int eventCounter = 0; eventCounter < eventMonitor; eventCounter++) {
                    if (eventArr[CD][eventCounter].equalsIgnoreCase("C"))
                        baseLineData[CD + 1][day] = String.format("%.2f", generateC(Double.parseDouble(statsArr[CD][1]), Double.parseDouble(statsArr[CD][2])));
                    if (eventArr[CD][eventCounter].equalsIgnoreCase("D"))
                        baseLineData[CD + 1][day] = String.valueOf((generateD(Double.parseDouble(statsArr[CD][1]), Double.parseDouble(statsArr[CD][2]))));
                }
            }
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(baseLineDataFile));
            for (String[] strings : baseLineData) {
                bw.write(String.format("%-20s", strings[0]) + ": ");
                for (int j = 1; j < strings.length; j++)
                    bw.write(String.format("%-7s", strings[j]) + " ");
                bw.newLine();
            }
            bw.flush();
            bw.close();
            System.out.println(GREEN + "[+] Log file successfully generated to " + baseLineDataFile + "!" + RESET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * It will measure baseLineStats data for event and determine statistics associated with baseLineStats. <br>
     * 1) Compute the total for each event for each day E.g. (Day1eventName1 + Day1eventName2 + Day1eventName3)<br>
     * 2) Compute the overall mean for each event. E.g. (Day1eventName1 + Day2eventName1 + Day3eventName1) / 3<br>
     * 3) Compute the overall stdDev for each event. E.g. (Day1eventName1 - Day2eventName1)^2 + (Day1eventName1 - Day3eventName1)^2 + (Day2eventName1 - Day3eventName1)^2 / 3<br>
     * 4) Output the overall mean and overall stdDev for each event and save it as baseLineStats.txt<br>
     * 5) Output the total for each event for each day and save it as dayTotal.txt<br>
     * </p>
     */
    private static void analysisEngine(String baseLineDataFile, String dayTotalFile, String baseLineStatsFile) {
        ArrayList<String> logBaseLineData = new ArrayList<>();
        String[][] baseLineData;

        try {
            System.out.println("[*] Analysis Engine is running...");
            BufferedReader br = new BufferedReader(new FileReader(baseLineDataFile));
            String line;
            int lineCounter = 0;

            while ((line = br.readLine()) != null) {
                if (lineCounter != 0) logBaseLineData.add(line);
                lineCounter++;
            }

            baseLineData = new String[logBaseLineData.size()][days + 1];
            for (int i = 0; i < baseLineData.length; i++) {
                StringTokenizer tokenizer = new StringTokenizer(logBaseLineData.get(i), ":");
                baseLineData[i][0] = tokenizer.nextToken();
                tokenizer = new StringTokenizer(tokenizer.nextToken(), " ");
                for (int j = 1; j < baseLineData[i].length; j++) {
                    baseLineData[i][j] = tokenizer.nextToken();
                }
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(dayTotalFile));
            double dayTotal = 0.0;
            for (int i = 1; i < days + 1; i++) {
                for (String[] strings : baseLineData) {
                    dayTotal += Double.parseDouble(strings[i]);
                }
                bw.write(String.format("Day %d : %.2f \n", i, dayTotal));
                dayTotal = 0.0;
            }
            bw.flush();
            bw.close();
            System.out.println(GREEN + "[+] Day totals successfully generated to " + dayTotalFile + "!" + RESET);

            bw = new BufferedWriter(new FileWriter(baseLineStatsFile));
            for (String[] str : baseLineData) {
                double mean;
                double total = 0.0, variance = 0.0;

                for (int i = 1; i < str.length; i++)
                    total += Double.parseDouble(str[i]);

                mean = total / days;

                for (int i = 1; i < str.length; i++)
                    variance += Math.pow((Double.parseDouble(str[i]) - mean), 2);
                bw.write(String.format("%20s: %7.2f %7.2f \n", str[0], mean, Math.sqrt(variance / days)));
            }
            bw.flush();
            bw.close();
            System.out.println(GREEN + "[+] Statistics successfully generated to " + baseLineStatsFile + "!" + RESET);

            anomalyArr = new double[eventMonitor][days];
            alertDailyCounter = new double[days];
            String[][] dailyValue = new String[eventMonitor][days];

            File fileRead = new File(baseLineDataFile);
            int row = 0;
            String input;

            if (fileRead.exists() && fileRead.length() > 0) {
                br = new BufferedReader(new FileReader(fileRead));
                input = br.readLine();
                while(input != null) {
                    if (row == 0) {
                        row++;
                        input = br.readLine();
                        continue;
                    }

                    System.arraycopy(input.split("\\W\\D+"), 1, dailyValue[row - 1], 0, days);
                    input = br.readLine();
                    row++;
                }
            }

            for (int i = 0; i < days; i++) {
                for (int j = 0; j < eventMonitor; j++) {
                    double dailyValueDouble = Double.parseDouble(dailyValue[j][i]);
                    double dailyMeanDouble = Double.parseDouble(statsArr[j][1]);
                    double dailyStdDevDouble = Double.parseDouble(statsArr[j][2]);
                    anomalyArr[j][i] = (Math.abs(dailyValueDouble - dailyMeanDouble) / dailyStdDevDouble) * Double.parseDouble(weights[j][1]);
                }
            }

            for (int i = 0; i < days; i++)
                for (int j = 0; j < eventMonitor; j++)
                    alertDailyCounter[i] += anomalyArr[j][i];

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * To perform consistency checks between "live data" and baseLineStats.txt.<br>
     * It will prompt users for a text file and a new day integer value.<br>
     * </p>
     * <p>
     * Anomaly counter: (Abs(dailyValue - mean) / stdDiv) * weight<br>
     * Threshold detection: 2 * sums of weights<br>
     * </p>
     * <p>
     * dailyValue 2D array:   dailyMeanStdDev 2D array: <br>
     *    _D1___D2__D3__Dx_         __M___sD<br>
     * E1|_y_|_y_|_y_|_y_|       E1|_y_|_y_|<br>
     * E2|_y_|_y_|_y_|_y_|       E2|_y_|_y_|<br>
     * E3|_y_|_y_|_y_|_y_|       E3|_y_|_y_|<br>
     * Ex|_y_|_y_|_y_|_y_|       Ex|_y_|_y_|<br>
     * </p>
     * @param kb user input to do test on the IDS
     */
    private static void alertEngine(Scanner kb) {
        String[][] liveStatsArr = new String[statsMonitor][sDataField];
        String input;
        int threshold = 0;
        int run = 0;

        for (int i = 0; i < eventMonitor; i++) {
            threshold += (int) Double.parseDouble(weights[i][1]);
        }
        threshold *= 2;

        do {
            run++;
            int newStatsMonitored, lineCounter = 0;

            try {
                System.out.println("===================================================================================");
                System.out.println("[*] Alert Engine is running...");
                System.out.print("[*] Please input a live data text file with .txt extension: ");
                File newStatsFile = new File(kb.nextLine());
                System.out.print("[*] Please enter the number of days for the baseLineStats data: ");
                days = Integer.parseInt(kb.nextLine());

                if (days < 1) {
                    System.out.println(RED + "[!!] Number of days must be more than 0!" + RESET);
                    System.exit(1);
                }

                if (newStatsFile.exists() && newStatsFile.length() > 0) {
                    BufferedReader br = new BufferedReader(new FileReader(newStatsFile));
                    input = br.readLine();
                    newStatsMonitored = Integer.parseInt(input);
                    if ((newStatsMonitored != statsMonitor)) {
                        System.out.println(RED + "[!!] Number of stats monitored in the Events.txt file is different from the live data file!" + RESET);
                        System.exit(1);
                    }

                    input = br.readLine();
                    while (input != null) {
                        String[] split = input.split(":");
                        System.arraycopy(split, 0, liveStatsArr[lineCounter], 0, split.length);
                        lineCounter++;
                        input = br.readLine();
                        if (lineCounter == newStatsMonitored && input == null) {
                            System.out.printf(GREEN + "[+] %s has been verified!\n" + RESET, newStatsFile.getName());
                            break;
                        }
                        if (lineCounter > newStatsMonitored && input != null) {
                            System.out.println(RED + "[!!] Error: The number of lines in the text file is greater than the number of monitored IDs." + RESET);
                            System.exit(1);
                        }
                    }
                    br.close();
                } else {
                    System.out.println(RED + "[!!] Early termination due to " + newStatsFile.getName() + " does not exist or is empty!" + RESET);
                    System.exit(2);
                }

                activityEngine("baseLineData" + run + ".txt", liveStatsArr);
                analysisEngine("baseLineData" + run + ".txt", "dayTotals" + run + ".txt", "baseLineStats" + run + ".txt");

                for (int i = 0; i < days; i++) {
                    if (alertDailyCounter[i] < threshold) {
                        System.out.printf(GREEN + "[+] Day %s is good to go! Anomaly score: %.2f%n" + RESET, i + 1, alertDailyCounter[i]);
                    } else {
                        System.out.printf(RED + "[!!] ALERT! ANOMALY DETECTED ON DAY %s! ANOMALY SCORE: %.2f%n" + RESET, i + 1, alertDailyCounter[i]);
                    }
                }

                System.out.print("[*] Do you want to save result in text file? [Y]es [N]o: ");
                input = kb.next();
                while (!input.equalsIgnoreCase("Y") && !input.equalsIgnoreCase("N")) {
                    System.out.println("[!!] Please enter a valid input!");
                    System.out.println("[*] Do you want to save result in text file? [Y]es [N]o: ");
                    input = kb.next();
                }

                if (input.equalsIgnoreCase("Y")) {
                    FileWriter fw = new FileWriter("result" + run + ".txt");
                    BufferedWriter bw = new BufferedWriter(fw);

                    bw.write(String.format("%8s%n%9s", "Anomaly score for each day:", " "));
                    for (int i = 0; i < days; i++) {
                        bw.write(String.format("%-7s", "D" + (i + 1)));
                    }
                    bw.newLine();

                    for (int i = 0; i < eventMonitor; i++) {
                        bw.write(String.format("Event %s: ", i + 1));
                        for (int j = 0; j < days; j++) {
                            bw.write(String.format("%-6.2f ", anomalyArr[i][j]));
                        }
                        bw.newLine();
                    }

                    bw.write(String.format("%n%s%n%9s", "Daily Score:", " "));
                    for (int i = 0; i < days; i++) {
                        bw.write(String.format("%-7s", "D" + (i + 1)));
                    }
                    bw.write(String.format("%n%9s", " "));

                    for (int i = 0; i < days; i++) {
                        bw.write(String.format("%-7.2f", alertDailyCounter[i]));
                    }

                    bw.close();
                    System.out.println(GREEN + "[+] Result has been saved in result" + run + ".txt!" + RESET);
                } else {
                    System.out.println("[-] Result has not been saved!");
                }
            } catch (NumberFormatException | IOException e) {
                if (e instanceof NumberFormatException) {
                    System.out.println(RED + "[!!] Early termination due to new day value must be an integer!" + RESET);
                } else e.printStackTrace();
                System.exit(1);
            }

            System.out.print("[*] Do you wish to continue? [Y]es [N]o: ");
            input = kb.next();
            while (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n")) {
                System.out.println(RED +  "[!!] Invalid input! Please enter 'Y' or 'N'" + RESET);
                input = kb.next();
            }
            kb.nextLine();
        } while (input.equalsIgnoreCase("y"));
        System.out.println("===================================================================================");
    }

    /**
     * <p>
     * To generate a number which follows normal distribution by using Gaussian.
     * </p>
     * <p>
     * nextGaussian() returns a random number from a Gaussian distribution with mean 0 and standard deviation 1. <br>
     * Scaling the random generated value with Standard Deviation then adding the mean <br>
     * will generate a value which follows normal distribution, producing statistics <br>
     * approximately consistent with given Stats.txt values. <br>
     * </p>
     *
     * @param mean   mean of the distribution
     * @param stdDev standard deviation of the distribution
     * @return a random integer value which follows normal distribution
     */
    private static int generateD(double mean, double stdDev) {
        int r;
        do {
            r = (int) Math.floor((rand.nextGaussian() * stdDev) + mean);
        } while (r < 0);
        return r;
    }

    /**
     * <p>
     * To generate a number which follows log normal distribution by using Gaussian.
     * </p>
     * <p>
     * nextGaussian() returns a random number from a Gaussian distribution with mean 0 and standard deviation 1. <br>
     * Following the exp(-0.5 * ((ln(x) - m) / s)^2) / (s * sqrt(2 * pi) * x) <br>
     * taking euler's constant and raise it to the power of a random generated value with ess then adding the mu<br>
     * will generate a value which follows normal distribution, producing statistics <br>
     * approximately consistent with given Stats.txt values. <br>
     * </p>
     *
     * @param mean   mean of the distribution
     * @param stdDev standard deviation of the distribution
     * @return a random double value which follows log-normal distribution
     */
    private static double generateC(double mean, double stdDev) {
        double ess = Math.log(1.0 + (Math.pow(stdDev, 2) / Math.pow(mean, 2)));
        double mu = Math.log(mean) - (0.5 * Math.pow(ess, 2));
        double r;
        do {
            r = Math.pow(2.7182818284590452353602874713527, rand.nextGaussian() * ess + mu);
        } while (r < 0);
        return r;
    }

    private static void art() {
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(" _____  _____     ____"); 
        System.out.println("|_   _||  __ \\  / ___|         ");
        System.out.println("  | |  | |  | || (___           ");
        System.out.println("  | |  | |  | | \\___ \\        ");
        System.out.println(" _| |_ | |__| | ____) |        CSCI 262, System Security");
        System.out.println("|_____||_____/ |_____/         Email System Modeller & Intrusion Detection System.");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }
}