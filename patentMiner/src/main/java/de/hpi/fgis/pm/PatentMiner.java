package de.hpi.fgis.pm;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import de.hpi.fgis.pm.ElasticSearchHelpers;


public class PatentMiner implements PatentMinerConstants {
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

    public static void testGetDoc() {
        HashMap<String, Object> doc = new HashMap<String, Object>();
        doc.put("_id", "AUwJzBkg5h6RFq6gQ43D");
        try {
			if (ElasticSearchHelpers.getDocById((String) doc.get("_id")) != null) {
			    Logger.info("GetDoc is working.");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void main(String[] args) throws IOException, KeyManagementException, CertificateException, KeyStoreException {

    	Logger.info("start");
        setUpLogger();
        RestHighLevelClient restClient = null;
        RestClient lowLevelRestClient =null;
        try {
        	Logger.info("building rest client...");
        	lowLevelRestClient = ElasticSearchHelpers.buildRestClient();
        	restClient = new RestHighLevelClient(lowLevelRestClient);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

        Logger.info("executing request...");
        MainResponse response = restClient.info();
        ClusterName clusterName = response.getClusterName();
    	try {
    	    Logger.info("REST FOUND");
    	    Logger.info(clusterName.toString());
    	} catch (ElasticsearchException e) {
    	    if (e.status() == RestStatus.NOT_FOUND) {
    	    	Logger.info("REST NOT FOUND");
    	    }
    	    Logger.info("exception "+e.toString());
    	}

    	health(lowLevelRestClient);
    	createIndexRestClient(lowLevelRestClient);


    	//test
       /*Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("publ_docNo", "id1");
        jsonMap.put("abstract", "trying out Elasticsearch");
        IndexRequest indexRequest = new IndexRequest(INDEX_NAME, PATENT_TYPE_NAME, "id1")
                .source(jsonMap);
        try {
    		IndexResponse response1 = restClient.index(indexRequest);
    		System.out.println(response1.status().toString());
    		System.out.println("patentminer");
    	} catch (IOException e1) {
    		e1.printStackTrace();
    	}
    	*/

        //testGetDoc();
        // searchDocument(INDEX_NAME, generalClient, "providing");

        ThreadPoolExecutor tpe =
                new ThreadPoolExecutor(NUMTHREADS, NUMTHREADS, 50000L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());

        // 1976
        // granted docs
        for (int year = 1976; year <= 2001; year++) {
       // for (int year = 2002; year <= 2001; year++) {
            File dir = new File(DATADIR + year + "/");
            FilenameFilter filter2 = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".zip");
                }
            };

            File[] children2 = dir.listFiles(filter2);

            if (children2 == null) {
                Logger.info("directory " + dir.getPath() + " does not exist!");
            } else {
                for (File file : children2) {
                    PftapsExtractor ex = new PftapsExtractor(file.getPath());
                    tpe.execute(ex);
                }
            }
        }

        // granted docs
        for (int year = 2001; year <= 2015; year++) {
            File dir = new File(DATADIR + year + "/");

            FilenameFilter filter2 = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".zip") && name.contains("ipg");
                }
            };

            File[] children2 = dir.listFiles(filter2);
            if (children2 == null) {
                Logger.info("directory " + dir.getPath() + " does not exist!");
            } else {
                for (File file : children2) {
                    IpgExtractor ex = new IpgExtractor(file.getPath());
                    tpe.execute(ex);
                }
            }

            FilenameFilter filter3 = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".zip") && name.contains("pg") && !name.contains("ipg");
                }
            };

            File[] children3 = dir.listFiles(filter3);
            if (children3 == null) {
                Logger.info("directory " + dir.getPath() + " does not exist!");
            } else {
                for (File file : children3) {
                    PgExtractor ex = new PgExtractor(file.getPath());
                    tpe.execute(ex);
                }
            }
        }

        tpe.shutdown();
    	lowLevelRestClient.close();
    	Logger.info("end.");

        try {
            tpe.awaitTermination(10L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    	        .endObject().string();

    	HttpEntity entity = new NStringEntity(payload, ContentType.APPLICATION_JSON); 

    	Response response = lowLevelRestClient.performRequest("PUT", INDEX_NAME, emptyMap(), entity); 
    	if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
    		System.out.println("HTTP STATUS: OK. Index created.");
    	}
    }
    public static void health( RestClient lowLevelRestClient) throws UnsupportedOperationException, IOException{

    	Response response = lowLevelRestClient.performRequest("GET", "/_cluster/health"); 

    	ClusterHealthStatus healthStatus;
    	try (InputStream is = response.getEntity().getContent()) { 
    	    Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true); 
    	    healthStatus = ClusterHealthStatus.fromString((String) map.get("status")); 
    	}
    	switch(healthStatus){
    	case GREEN:
    		Logger.info("green");
    		break;
    	case YELLOW:
    		Logger.warning("yellow");
    		break;
    	case RED:
    		Logger.warning("red");
    		break;	
    	}
    }
}
