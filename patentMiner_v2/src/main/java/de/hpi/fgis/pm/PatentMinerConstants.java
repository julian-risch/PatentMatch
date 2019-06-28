package de.hpi.fgis.pm;

public interface PatentMinerConstants {

    String INDEX_NAME = "patent_grants";
    String PATENT_TYPE_NAME = "patent_us";

    int NUMTHREADS = 1;

    String DATADIR = "/san2/data/websci/usPatents/grants-n"; ///san2/data/websci/usPatents/applications-j
    String ES_HOST = "172.16.64.23"; // default: isfet
    Integer ES_PORT = 9200;


    String LOGGINGLEVEL = "all";
}
