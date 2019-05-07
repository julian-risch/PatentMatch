package de.hpi.fgis.pm;

public interface PatentMinerConstants {

    String INDEX_NAME = "patents-english";
    String PATENT_TYPE_NAME = "patent_us";

    int NUMTHREADS = 1;

     String DATADIR = "/data/websci/usPatents/grants/";
   // String DATADIR = "D:/Work-Projects/2015-10_FGInfoSys/2015-11_PatentData/";
    String ES_HOST = "isfet";
    Integer ES_PORT = 9300;

    String LOGGINGLEVEL = "all";
}
