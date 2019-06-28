package de.hpi.fgis.pm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CheckApplicationLogs {

    public static void main(String[] args) {
        //System.out.println("Processed /san2/data/websci/usPatents/applications-j/ipa060413.zip; 5214 docs;".contains("ipa060413.zip;"));
        ArrayList<String> lines = new ArrayList<>();
        System.out.println("Starting check...");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(
                    "/san2/data/websci/usPatents/log.out"));
            String line = reader.readLine();
            while (line != null) {
                // read next line
                line = reader.readLine();
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("CATCH ERROR");
        }

        ArrayList<String> files = new ArrayList<>();
        try {
            BufferedReader readerB = new BufferedReader(new FileReader(
                    "/san2/data/websci/usPatents/allFiles.txt"));
            String lineB = readerB.readLine();
            while (lineB != null && lineB.length()>3) {
                // read next line
                lineB = readerB.readLine();
                files.add(lineB);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("CATCH ERROR");

        }

        System.out.println("Lines in Log: " + lines.size());

        System.out.println("Files in File: "+ files.size());


        for (String file : files) {
            if(file == null){continue;}
            boolean processing = false;
            boolean processed = false;
            System.out.println(file);
            for (String line : lines){
                if(line == null){continue;}
                line = line.replaceAll("\\r$", "");
                if(line.contains("Processing: "+file.substring(0,file.length()-3))){processing = true;}
                if(line.contains("Processed /san2/data/websci/usPatents/applications-j/" + file.substring(0,file.length()-3))){processed = true;}
                //Processed /san2/data/websci/usPatents/grants-n/
                //Processed /san2/data/websci/usPatents/applications-j/
            }
            String output = file;
            if(processing){
                output = output.concat(";started");
            }else{
                output = output.concat("; not started");
            }
            if (processed){
                output = output.concat(";processed");
            }else {
                output = output.concat(";NOT processed");
            }

            System.out.println(output);


        }

        }
}
