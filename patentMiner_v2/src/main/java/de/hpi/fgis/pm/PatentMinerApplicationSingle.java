package de.hpi.fgis.pm;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PatentMinerApplicationSingle implements PatentMinerConstants {
    public final static Logger Logger = java.util.logging.Logger.getLogger("patentminerlogger");

    private static void setUpLogger() {
        Level logging_level = Level.OFF;
        switch (LOGGINGLEVEL) {
            case "info":
                logging_level = Level.INFO;
            case "all":
                logging_level = Level.ALL;
        }
        Logger.setLevel(logging_level);
        Logger.info("Logger is set up with logging level " + logging_level);
    }


    public static void main(String[] args) throws IOException, KeyManagementException, CertificateException, KeyStoreException {
        Logger.info("start");
        setUpLogger();

        // Frage Status von Elasticsearch ab. Nur bei grünem Status wird das Programm fortgeführt.
        String healthStatus = health();
        switch(healthStatus){
            case "green": Logger.info("health status: green"); break;
            case "yellow": Logger.warning("health status: yellow\nProgram aborted. Please check ElasticSearch engine."); System.exit(1);
            case "red": Logger.warning("health status: red\n Program aborted. Please check ElasticSearch engine."); System.exit(1);
            default: Logger.warning("health status undefined:\n Program aborted for unknown reason."); System.exit(1);
        }

        //Prüfe, ob Index bereits existiert
        try{
            if (indexExists(INDEX_NAME)) {
                Logger.warning("Index " + INDEX_NAME + " already exists.\nProgram continues.");
                ;
            }

        }catch(ResponseException e){
            Logger.info("Index "+INDEX_NAME+" does not exist yet.");
            Logger.info("Index "+INDEX_NAME+" will now be created.");
            if(createIndex(INDEX_NAME)){
                Logger.info("Index "+INDEX_NAME+" creation successful");
            }else{
                Logger.warning("Something went wrong. Unsure if index created. Please check.");
                System.exit(1);
            }
        }

        // Bekomme den ersten Parameter der gewünschten Datei in Ordner
        String argument = args[0];


        ThreadPoolExecutor tpe =
                new ThreadPoolExecutor(NUMTHREADS, NUMTHREADS, 50000L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());


            File dir = new File(DATADIR + "/");

            FilenameFilter filter2 = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".zip") && name.contains("ipa") && name.contains(argument);
                }
            };

            File[] children2 = dir.listFiles(filter2);
            if (children2 == null) {
                Logger.info("directory " + dir.getPath() + " does not exist!");
            } else {
                for (File file : children2) {
                    IpaExtractor ex = new IpaExtractor(file.getPath());
                    tpe.execute(ex);
                }
            }

            FilenameFilter filter3 = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".zip") && name.contains("pa") && !name.contains("ipa") && name.contains(argument);
                }
            };

            File[] children3 = dir.listFiles(filter3);
            if (children3 == null) {
                Logger.info("directory " + dir.getPath() + " does not exist!");
            } else {
                for (File file : children3) {
                    PaExtractor ex = new PaExtractor(file.getPath());
                    tpe.execute(ex);
                }
            }


        tpe.shutdown();
        //  lowLevelRestClient.close();
        Logger.info("end.");

        try {
            tpe.awaitTermination(10L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("TIMEOUT bzw. INTERRUPTED EXCEPTION");
        }

        System.exit(0);


    }




    public static Map<String,String> emptyMap(){
        return Collections.<String, String>emptyMap();
    }
    public static void createIndexRestClient(RestClient lowLevelRestClient) throws IOException{
        Settings indexSettings = Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .build();

        String payload = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("settings")
                .value(indexSettings)
                .endObject()
                .startObject("mappings")
                .startObject("patent_us")
                .startObject("properties")
                .startObject("summary")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("title")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("appl_date")
                .field("type", "date")
                .field("format", "yyyy-MM-dd'T'HH:mm:ss'Z'").endObject()

                .startObject("publ_docNo")
                .field("type", "keyword").endObject()

                .startObject("appl_type")
                .field("type", "keyword").endObject()

                .startObject("assignees")
                .field("type", "text").endObject()

                .startObject("abstract")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("classification")
                .field("type", "keyword").endObject()

                .startObject("patent_citations")
                .field("type", "keyword").endObject()

                .startObject("publ_date")
                .field("type", "date")
                .field("format", "yyyy-MM-dd'T'HH:mm:ss'Z'").endObject()

                .startObject("publ_kind")
                .field("type", "keyword").endObject()

                .startObject("citations")
                .field("type", "keyword").endObject()

                .startObject("claims")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("details")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("appl_docNo")
                .field("type", "keyword").endObject()

                .startObject("authors")
                .field("type", "text").endObject()
                .endObject()
                .endObject()
                .endObject()
                .endObject().toString();

        HttpEntity entity = new NStringEntity(payload, ContentType.APPLICATION_JSON);

       // Response response = lowLevelRestClient.performRequest("PUT", INDEX_NAME, emptyMap(), entity);
       // if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
       //     System.out.println("HTTP STATUS: OK. Index created.");
    //   }
    }
    public static String health() throws UnsupportedOperationException, IOException{

        Request request = new Request("GET", "/_cluster/health");
        RestClient restClient = ConnectElasticSearch.buildRestClient();
        Response response = restClient.performRequest(request);
        restClient.close();

        System.out.println(response.toString());

        ClusterHealthStatus healthStatus;
        try (InputStream is = response.getEntity().getContent()) {
            Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            healthStatus = ClusterHealthStatus.fromString((String) map.get("status"));
        }

        switch(healthStatus){
            case GREEN:
                return "green";
            case YELLOW:
                return "yellow";
            case RED:
                return "red";
        }

        return "undefined";
    }

    public static boolean indexExists(String index) throws UnsupportedOperationException, IOException{

        Request request = new Request("Head", index);
        RestClient restClient = ConnectElasticSearch.buildRestClient();
        Response response = restClient.performRequest(request);
        restClient.close();

        System.out.println(response.toString());
        if(response.getStatusLine().getStatusCode() == 200){
            return true;
        }
        return false;

    }

    public static boolean createIndex(String index) throws IOException {
        // Check index name limitations, index is converted to lowercase in all cases
        index = index.toLowerCase();
        boolean naming_ok = !(index.contains("\\") || index.contains("/") || index.contains("*") || index.contains("?") || index.contains("\"") || index.contains("<")
                || index.contains(">")|| index.contains("|") || index.contains(" ") || index.contains(",") || index.contains("#") || index.contains(":") || index.substring(0,1).contains("-")
                || (index.substring(0,1).contains("_")) || index.substring(0,1).contains("+") || (index.compareToIgnoreCase(".")==0) || (index.compareToIgnoreCase("..")==0) || (index.getBytes().length > 255));
        if(!naming_ok){
            Logger.warning("Naming conventions violated. Please check index name.");
            return false;
        }


        CreateIndexRequest request = new CreateIndexRequest(index);
        Settings indexSettings = Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .build();

        XContentBuilder payload = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("settings")
                .value(indexSettings)
                .endObject()
                .startObject("mappings")
                .startObject("properties")

                .startObject("summary")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("title")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("appl_date")
                .field("type", "date")
                .field("format", "yyyy-MM-dd'T'HH:mm:ss'Z'").endObject()

                .startObject("publ_docNo")
                .field("type", "keyword").endObject()

                .startObject("appl_type")
                .field("type", "keyword").endObject()

                .startObject("assignees")
                .field("type", "text").endObject()

                .startObject("abstract")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("classification")
                .field("type", "keyword").endObject()

          //      .startObject("patent_citations")
           //     .field("type", "keyword").endObject()

                .startObject("publ_date")
                .field("type", "date")
                .field("format", "yyyy-MM-dd'T'HH:mm:ss'Z'").endObject()

                .startObject("publ_kind")
                .field("type", "keyword").endObject()

         //       .startObject("citations")
          //      .field("type", "keyword").endObject()

                .startObject("claims")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("details")
                .field("type", "text")
                .field("index", "true")
                .field("analyzer", "english").endObject()

                .startObject("appl_docNo")
                .field("type", "keyword").endObject()

            //    .startObject("authors")
             //   .field("type", "text").endObject()
                .endObject()
                .endObject()
                .endObject();

        String mapping = Strings.toString(payload);

        System.out.println(mapping);

        request.source(mapping,XContentType.JSON);
        RestHighLevelClient highLevelClient = ConnectElasticSearch.buildRestHighLevelClient();
        CreateIndexResponse response = highLevelClient.indices().create(request, RequestOptions.DEFAULT);
        highLevelClient.close();

        boolean acknowledged = response.isAcknowledged();

        boolean shardsAcknowledged = response.isShardsAcknowledged();

        Logger.info("Index Creation Response - Do node(s) acknowledge request: " + acknowledged);
        Logger.info("Index Creation Response - Do shard(s) acknowledge request: " + shardsAcknowledged);


        if(acknowledged && shardsAcknowledged){return true;}{return false;}

    }

}