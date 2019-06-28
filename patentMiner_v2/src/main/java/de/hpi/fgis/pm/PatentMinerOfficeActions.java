package de.hpi.fgis.pm;

import com.opencsv.CSVReaderHeaderAware;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PatentMinerOfficeActions implements PatentMinerConstants {
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

        String INDEX_OA = "patent_officeactions";
        String INDEX_REJ = "patent_rejections";
        String INDEX_CIT = "patent_citations";
        //Prüfe, ob Index bereits existiert

        try{
            if (indexExists(INDEX_OA)) {
                Logger.warning("Index " + INDEX_OA + " already exists.\nProgram aborted.");
                System.exit(1);
            }

        }catch(ResponseException e){
            Logger.info("Index "+INDEX_OA+" does not exist yet.");
            Logger.info("Index "+INDEX_OA+" will now be created.");
        }

        if(createIndexOfficeActions(INDEX_OA)){
            Logger.info("Index "+INDEX_OA+" creation successful");
        }else{
            Logger.warning("Something went wrong. Unsure if index created. Please check.");
            System.exit(1);
        }

        //Prüfe, ob Index bereits existiert
        try{
            if (indexExists(INDEX_REJ)) {
                Logger.warning("Index " + INDEX_REJ + " already exists.\nProgram aborted.");
                System.exit(1);
            }

        }catch(ResponseException e){
            Logger.info("Index "+INDEX_REJ+" does not exist yet.");
            Logger.info("Index "+INDEX_REJ+" will now be created.");
        }

        if(createIndexOfficeActions(INDEX_REJ)){
            Logger.info("Index "+INDEX_REJ+" creation successful");
        }else{
            Logger.warning("Something went wrong. Unsure if index created. Please check.");
            System.exit(1);
        }

        //Prüfe, ob Index bereits existiert
        try{
            if (indexExists(INDEX_CIT)) {
                Logger.warning("Index " + INDEX_CIT + " already exists.\nProgram aborted.");
                System.exit(1);
            }

        }catch(ResponseException e){
            Logger.info("Index "+INDEX_CIT+" does not exist yet.");
            Logger.info("Index "+INDEX_CIT+" will now be created.");
        }

        if(createIndexOfficeActions(INDEX_CIT)){
            Logger.info("Index "+INDEX_CIT+" creation successful");
        }else{
            Logger.warning("Something went wrong. Unsure if index created. Please check.");
            System.exit(1);
        }


        ThreadPoolExecutor tpe =
                new ThreadPoolExecutor(NUMTHREADS, NUMTHREADS, 50000L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());



        uploadOfficeActions("/san2/data/websci/usPatents/office-actions/office_actions.csv", INDEX_OA);
        uploadRejections("/san2/data/websci/usPatents/office-actions/rejections.csv", INDEX_REJ);
        uploadCitations("/san2/data/websci/usPatents/office-actions/citations.csv", INDEX_CIT);



        try {
            tpe.awaitTermination(10L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("TIMEOUT bzw. INTERRUPTED EXCEPTION");
        }

        tpe.shutdown();
        //  lowLevelRestClient.close();
        Logger.info("end.");

        System.exit(0);


    }
    public static void uploadCitations(String path, String index){

        CSVReaderHeaderAware reader = null;

        try {
            // Lese Zeile für Zeile ein. Ergebnis ist ein Dictionary mit Key = Attribut und Value = Attributausprägung
            reader = new CSVReaderHeaderAware(new FileReader(path));



        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not read file.");
        }

        System.out.println("starting bulk request");

        RestHighLevelClient client = ConnectElasticSearch.buildRestHighLevelClient();
        BulkProcessor builder = BulkProcessor.builder((request, bulkListener) ->
                        client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId, BulkRequest request) {
                        System.out.println("before");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                        System.out.println("after");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                        System.out.println("failure");
                    }
                }).setBulkActions(1000).setBulkSize(new ByteSizeValue(100, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(5)).setConcurrentRequests(1).build();


        Map<String, String> doc;
        try {
            while((doc = reader.readMap()) != null){

                IndexRequest request = new IndexRequest(index);
                request.id(doc.get("app_id").concat(doc.get("citation_pat_pgpub_id")));
                request.source(doc, XContentType.JSON);
                request.opType(DocWriteRequest.OpType.CREATE);
                builder.add(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            builder.awaitClose(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // helps to prevent failures?
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void uploadRejections(String path, String index){

        CSVReaderHeaderAware reader = null;

        try {
            // Lese Zeile für Zeile ein. Ergebnis ist ein Dictionary mit Key = Attribut und Value = Attributausprägung
            reader = new CSVReaderHeaderAware(new FileReader(path));



        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not read file.");
        }

        System.out.println("starting bulk request");

        RestHighLevelClient client = ConnectElasticSearch.buildRestHighLevelClient();
        BulkProcessor builder = BulkProcessor.builder((request, bulkListener) ->
                        client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId, BulkRequest request) {
                        System.out.println("before");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                        System.out.println("after");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                        System.out.println("failure");
                    }
                }).setBulkActions(1000).setBulkSize(new ByteSizeValue(100, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(5)).setConcurrentRequests(1).build();


        Map<String, String> doc;
        try {
            while((doc = reader.readMap()) != null){

                IndexRequest request = new IndexRequest(index);
                request.id(doc.get("ifw_number").concat(doc.get("action_type")).concat(doc.get("action_subtype")));
                request.source(doc, XContentType.JSON);
                request.opType(DocWriteRequest.OpType.CREATE);
                builder.add(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            builder.awaitClose(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // helps to prevent failures?
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void uploadOfficeActions(String path, String index){

        CSVReaderHeaderAware reader = null;

        try {
            // Lese Zeile für Zeile ein. Ergebnis ist ein Dictionary mit Key = Attribut und Value = Attributausprägung
            reader = new CSVReaderHeaderAware(new FileReader(path));



        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not read file.");
        }

        System.out.println("starting bulk request");

        RestHighLevelClient client = ConnectElasticSearch.buildRestHighLevelClient();
        BulkProcessor builder = BulkProcessor.builder((request, bulkListener) ->
                        client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId, BulkRequest request) {
                        System.out.println("before");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                        System.out.println("after");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                        System.out.println("failure");
                        System.out.println(failure.toString());
                    }
                }).setBulkActions(1000).setBulkSize(new ByteSizeValue(100, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(5)).setConcurrentRequests(1).build();


        Map<String, String> doc;
        try {
            while((doc = reader.readMap()) != null){
                //System.out.println(doc.toString());
                IndexRequest request = new IndexRequest(index);
                request.id(doc.get("ifw_number"));
                request.source(doc, XContentType.JSON);
                request.opType(DocWriteRequest.OpType.CREATE);
                builder.add(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            builder.awaitClose(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // helps to prevent failures?
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String,String> emptyMap(){
        return Collections.<String, String>emptyMap();
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

    public static boolean createIndexOfficeActions(String index) throws IOException {
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

                .startObject("app_id")
                .field("type", "keyword").endObject()

                .startObject("ifw_number")
                .field("type", "keyword").endObject()

                .startObject("document_cd")
                .field("type", "keyword").endObject()

                .startObject("mail_dt")
                .field("type", "date")
                .field("format", "yyyy-MM-dd").endObject()

                .startObject("art_unit")
                .field("type", "keyword").endObject()

                .startObject("header_missing")
                .field("type", "keyword").endObject()

                .startObject("uspc_subclass")
                .field("type", "keyword").endObject()

                .startObject("fp_missing")
                .field("type", "integer").endObject()

                .startObject("rejection_fp_mismatch")
                .field("type", "integer").endObject()

                .startObject("closing_missing")
                .field("type", "integer").endObject()

                .startObject("rejection_101")
                .field("type", "integer").endObject()

                .startObject("rejection_102")
                .field("type", "integer").endObject()

                .startObject("rejection_103")
                .field("type", "integer").endObject()

                .startObject("rejection_112")
                .field("type", "integer").endObject()

                .startObject("rejection_dp")
                .field("type", "integer").endObject()

                .startObject("objection")
                .field("type", "integer").endObject()

                .startObject("allowed_claims")
                .field("type", "integer").endObject()

                .startObject("cite102_gt1")
                .field("type", "integer").endObject()

                .startObject("cite103_gt3")
                .field("type", "integer").endObject()

                .startObject("cite103_eq1")
                .field("type", "integer").endObject()


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

    public static boolean createIndexRejection(String index) throws IOException {
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

                .startObject("app_id")
                .field("type", "keyword").endObject()

                .startObject("ifw_number")
                .field("type", "keyword").endObject()

                .startObject("action_type")
                .field("type", "keyword").endObject()

                .startObject("action_subtype")
                .field("type", "date")
                .field("format", "yyyy-MM-dd").endObject()

                .startObject("claim_numbers")
                .field("type", "text")
                .field("index","true")
                .field("analyzer","stop")
                .field("stopwords",",").endObject()

                .startObject("alice_in")
                .field("type", "integer").endObject()

                .startObject("bilski_in")
                .field("type", "integer").endObject()

                .startObject("mayo_in")
                .field("type", "integer").endObject()

                .startObject("myriad_in")
                .field("type", "integer").endObject()

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

    public static boolean createIndexCitation(String index) throws IOException {
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

                .startObject("app_id")
                .field("type", "integer").endObject()

                .startObject("ifw_number")
                .field("type", "integer").endObject()

                .startObject("action_type")
                .field("type", "integer").endObject()

                .startObject("action_subtype")
                .field("type", "date")
                .field("format", "yyyy-MM-dd").endObject()

                .startObject("claim_numbers")
                .field("type", "text")
                .field("index","true")
                .field("analyzer","stop")
                .field("stopwords",",").endObject()

                .startObject("alice_in")
                .field("type", "integer").endObject()

                .startObject("bilski_in")
                .field("type", "integer").endObject()

                .startObject("mayo_in")
                .field("type", "integer").endObject()

                .startObject("myriad_in")
                .field("type", "integer").endObject()

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