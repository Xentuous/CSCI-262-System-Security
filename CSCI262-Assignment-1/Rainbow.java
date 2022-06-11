/**
 * This program will read in a valid text file containing a list of passwords
 * and generate a Rainbow.txt file at /Desktop/Rainbow.txt (Windows OS).
 * Program will prompt user for a valid 32 Hexadecimal value and then check
 * against the Rainbow table for its respective key/value to produce password(pre-image).
 * <p>
 * This program uses Java's Security Class MessageDigest to perform necessary hashing as required.
 * <p>
 * Exit code 0: Program terminated by user
 * Exit code 1: invalid file extension
 * Exit code 2: invalid file content inside .txt file
 */

import java.io.*;
import java.util.*;
import java.security.*;
import java.math.*;

public class Rainbow {
    private static final ArrayList<String> passwordList = new ArrayList<>();
    private static final HashMap<String, String> rainbowHash = new HashMap<>();
    private static final LinkedHashMap<String, String> sortedRainbowHash = new LinkedHashMap<>();
    private static final Scanner kb = new Scanner(System.in);

    /**
     * This method will initialise the start of the program.
     * It will check the text file and then process a rainbow table
     * Upon successful initialisation, it will generate a Rainbow.txt
     * and output the total number of passwords read in.
     *
     * @param args cli argument to parse in file.
     */
    private static void init(String args) {
        try {
            if (!args.contains(".txt")) {
                System.out.println("Process finished with exit code 1");
                System.exit(1);
            }
            
            FileReader file = new FileReader(args);
            BufferedReader br = new BufferedReader(file);
            String line = br.readLine();
            
            if (line == null) {
                System.out.println("Process finished with exit code 2");
                System.exit(2);
            }
            passwordList.add(line);
            
            while (line != null) {
                line = br.readLine();
                if (line != null) passwordList.add(line);
            }

            br.close();
            file.close();

            int pos = passwordList.indexOf(passwordList.get(0));
            boolean[] marked = new boolean[passwordList.size()];
            boolean notAllMarked = true;
            
            Arrays.fill(marked, false);

            do {
                int counter = 0;
                if (!marked[pos] && pos < passwordList.size()) {
                    String value = passwordList.get(pos), key = value, hex;
                    marked[pos] = true;
                    for (int i = 0; i < 4; i++) {
                        hex = (hash(value));
                        value = reduct(hex);
                        marked[passwordList.indexOf(value)] = true;
                    }
                    hex = adjustHex(hash(value));
                    rainbowHash.put(key, hex);
                } else if (marked[pos] && pos < passwordList.size()) {
                    pos++;
                } else if (pos >= passwordList.size() - 1) {
                    break;
                }
                
                for (boolean bool : marked) if (bool) counter++;

                if (counter == marked.length) notAllMarked = false;
                
            } while (notAllMarked);

            genTxtFile(args);

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method uses Java's Security Class Message Digest to
     * perform necessary hashing as required from password to hexadecimal.
     *
     * @param password a String of password.
     * @return positive String value of BigInteger in base 16.
     * @throws NoSuchAlgorithmException if MD5 is not available inside this program.
     */
    private static String hash(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(password.getBytes());
        BigInteger bigInt = new BigInteger(1, digest);
        
        return bigInt.toString(16);
    }

    /**
     * This method converts 32 hexadecimal value into a String then modded by
     * size of passwordList arraylist.
     *
     * @param hex a valid 32 hexadecimal value
     * @return a password value from passwordList arraylist
     */
    private static String reduct(String hex) {
        BigInteger bigint = new BigInteger(hex, 16);
        
        return passwordList.get((bigint.mod(BigInteger.valueOf(passwordList.size())).intValue()));
    }

    /**
     * This method generates a text file at /Desktop/Rainbow.txt.
     *
     * @param args cli argument to parse in file.
     * @throws IOException IO error
     */
    private static void genTxtFile(String args) throws IOException {
        System.out.println("==============================================================");
        
        File desktop = new File(System.getProperty("user.home"), "/Desktop/Rainbow.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(desktop));
        
        rainbowHash.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x -> sortedRainbowHash.put(x.getKey(), x.getValue()));
        bw.write("Plaintext\t: Final current hash value\n==================================================\n");
        
        for (Map.Entry<String, String> entry : sortedRainbowHash.entrySet())
            bw.write(String.format("%-15s : %s%n", entry.getKey(), entry.getValue()));
        
        bw.flush();
        bw.close();
        
        System.out.printf("%s contains %d password(s).%nGenerated Rainbow.txt at /Desktop/Rainbow.txt%n", args, passwordList.size());
        System.out.print("Rainbow.txt contains: " + sortedRainbowHash.size() + " number of lines.");
        System.out.println("\n==============================================================");
    }

    /**
     * This method ensures that all hashed value from hash(String password)
     * returns a 32 character long hexadecimal value.
     *
     * @param hex a valid hexadecimal value
     * @return a 32 character long hexadecimal value
     */
    private static String adjustHex(String hex) {
        if (hex.length() < 32 && hex.length() != 31) {
            int padding = 32 - hex.length();
            
            for (int j = 0; j < padding; j++) 
                hex = "0" + hex;

        }
        if (hex.length() == 31) hex = "0" + hex;
        return hex;
    }

    /**
     * This method will prompt user for a 32 hex long value and perform the necessary steps required to produce
     * the password iff the password exist inside Rainbow Table
     *
     * @throws NoSuchAlgorithmException if MD5 is not available inside this program.
     */
    private static void attack() throws NoSuchAlgorithmException {
        boolean insideRT = false;

        System.out.print("Enter a 32 hexadecimal value: ");
        
        String inputHex = kb.nextLine(), valueInRT = "";

        while (!hexCheck(inputHex)) {
            System.out.print("INVALID HEX VALUE!\nEnter \"QUIT\" to exit(0) the program.\n\nEnter a proper 32 hexadecimal value: ");
            inputHex = kb.nextLine();

            if (inputHex.equalsIgnoreCase("quit")) {
                System.out.println("Process finished with exit code 0");
                System.exit(0);
            }
        }

        if (hexCheck(inputHex)) {
            for (Map.Entry<String, String> entry : sortedRainbowHash.entrySet()) {
                if (entry.getValue().equals(inputHex)) {
                    valueInRT = entry.getKey();
                    insideRT = true;
                    break;
                }
            }
        }

        if (!insideRT) {
            String value, hex = inputHex, possibleValueInRT = "";
            boolean possibleInRT = false;
            
            value = reduct(hex);
            hex = hash(value);

            for (int i = 0; i <= 5; i++) {
                for (Map.Entry<String, String> entry : sortedRainbowHash.entrySet()) {
                    if (entry.getValue().equals(hex)) {
                        possibleValueInRT = entry.getKey();
                        possibleInRT = true;
                        break;
                    }
                }

                if (possibleInRT) break;
                
                value = reduct(hex);
                hex = hash(value);
            }

            if (possibleInRT) getPassword(possibleValueInRT, inputHex);
            if (!possibleInRT) System.out.println("Unable to find password");
        }
        if (insideRT) getPassword(valueInRT, inputHex);
    }

    /**
     * This method will perform the hashing and reduction required to produce the password to the user.
     * @param valueInRT a value that happens to reside somewhere inside the rainbow table.
     * @param inputHex user input hex value.
     * @throws NoSuchAlgorithmException if MD5 is not available inside this program.
     */
    private static void getPassword(String valueInRT, String inputHex) throws NoSuchAlgorithmException {
        boolean foundPasswordInsideRT = false;
        String tempValue = valueInRT;

        // There are 5 words in each chain hence need check 5 times
        for (int i = 0; i < 5; i++) {
            String hex = hash(valueInRT);
            
            hex = adjustHex(hex);
            
            if (hex.equals(inputHex)) {
                System.out.println("Password: " + valueInRT);
                foundPasswordInsideRT = true;
                break;
            }

            valueInRT = reduct(hex);
        }

        if (!foundPasswordInsideRT) {
            Set<String> sortedRainbowHashKeySet = sortedRainbowHash.keySet();
            List<String> sortedRainbowHashKeyList = new ArrayList<>(sortedRainbowHashKeySet);
            int startIndex = 0;
            int index = sortedRainbowHashKeyList.indexOf(tempValue) + 1;
            int controlIndex = index;
            boolean foundPassword = false;

            while (index < sortedRainbowHashKeyList.size()) {
                String possVal = sortedRainbowHashKeyList.get(index), possHex;

                for (int i = 0; i < 5; i++) {
                    possHex = hash(possVal);
                    possHex = adjustHex(possHex);
                    
                    if (possHex.equals(inputHex)) {
                        foundPassword = true;
                        System.out.println("Password: " + possVal);
                        break;
                    } else {
                        possVal = reduct(possHex);
                    }
                }
                
                index++;
                
                if (foundPassword) break;
            }

            while (!foundPassword && startIndex < controlIndex) {
                String possVal = sortedRainbowHashKeyList.get(startIndex), possHex;

                for (int i = 0; i < 5; i++) {
                    possHex = hash(possVal);
                    possHex = adjustHex(possHex);
                    
                    if (possHex.equals(inputHex)) {
                        foundPassword = true;
                        System.out.println("Password: " + possVal);
                        break;
                    } else {
                        possVal = reduct(possHex);
                    }
                }
                startIndex++;
                if (foundPassword) break;
            }
        }
    }

    /**
     * This method will ensure that user input value of a hexadecimal value is
     * valid and of 32 length.
     *
     * @param inputHex user input hexadecimal value
     * @return true iff hexadecimal value is valid and of 32 length.
     */
    private static boolean hexCheck(String inputHex) {
        if (inputHex.matches("([\\d]{32})|([a-zA-Z]{32})|\\Q.\\E") || inputHex.length() < 32) {
            return false;
        } else return inputHex.matches("^[A-Fa-f0-9]{32}+$");
    }

    /**
     * Main method. It will prompt user for continuous input of 32 hexadecimal value.
     *
     * @param args reads in cli argument which is later parsed into init(args[0])
     * @throws NoSuchAlgorithmException if MD5 is not available inside this program.
     */
    public static void main(String[] args) throws NoSuchAlgorithmException {
        init(args[0]);
        String resume;
        do {
            attack();
            System.out.print("Resume? [Y]/[N]: ");
            resume = kb.nextLine();
            System.out.println("==============================================================");
        } while (resume.equalsIgnoreCase("y"));
        System.out.println("Program terminated.");
    }
}