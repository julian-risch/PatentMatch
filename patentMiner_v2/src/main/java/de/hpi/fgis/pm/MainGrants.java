package de.hpi.fgis.pm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainGrants {

    public static void main(String[] args) throws IOException, InterruptedException {
        ArrayList<String> files = new ArrayList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(
                    "/san2/data/websci/usPatents/allGrants.txt"));
            String line = reader.readLine();
            while (line != null) {
                // read next line
                line = reader.readLine();
                files.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (String file : files) {



            Runtime rt = Runtime.getRuntime();
            System.out.println("Starte Miner für "+file);
            String command = "java -jar /san2/data/websci/usPatents/PatentMinerGrants.jar " + file;
            Process proc = rt.exec(command);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }


            /*Thread.sleep(1 *   // minutes to sleep
                    60 *   // seconds to a minute
                    1000); // milliseconds to a second}
            System.out.println("Sleep...");
*/


        }

    }
}
