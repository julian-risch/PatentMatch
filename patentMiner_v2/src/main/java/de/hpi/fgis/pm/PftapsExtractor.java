package de.hpi.fgis.pm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.node.Node;
import org.elasticsearch.threadpool.ThreadPool;

public class PftapsExtractor implements Runnable, PatentMinerConstants {

  Collection<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
  Map<String, Object> doc;

  boolean patn, invt, assg, prir, reis, rlap, clas, xref, uref, lrep, pcta, abst, govt, parn, bsum, drwd, detd, clms,
      dclm;

  String currentString = "";
  boolean error = false;
  int errorCount;

  String zipfile;

  public PftapsExtractor(String zipfile) {

    this.zipfile = zipfile;
  }

  @Override
  public void run() {

    setFalse();
    docs = new ArrayList<Map<String, Object>>();
    doc = null;

    try {
      ZipFile zf = new ZipFile(zipfile);
      try {
        loop: for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
          ZipEntry ze = e.nextElement();
          String name = ze.getName();
          if (name.endsWith(".txt")) {
            System.out.println("Processing: " + name);
            errorCount = 0;
            InputStream in = zf.getInputStream(ze);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String oldLine = "";
            for (String line; (line = reader.readLine()) != null;) {
              if (line.length() == 0)
                continue;
              if (line.startsWith(" ")) {
                oldLine += line.trim() + " ";
              } else {
                extractLine(oldLine, false);
                oldLine = line.trim() + " ";
              }
            }
            extractLine(oldLine, true);
            docs.add(doc);
          }
        }
      } finally {
        zf.close();
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }


    if (docs.size() > 0) {
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



      for (Map<String, Object> doc : docs) {
        IndexRequest request = new IndexRequest(INDEX_NAME);
        request.id(doc.get("publ_docNo").toString());
        request.source(doc, XContentType.JSON);
        request.opType(DocWriteRequest.OpType.CREATE);
        builder.add(request);
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
    System.out.println("Processed " + zipfile + "; " + docs.size() + " docs;");

    //System.out.println("Processed " + zipfile + "; " + docs.size() + " docs; Erroneous documents: " + errorCount);
  //  for (Map<String, Object> doc : docs) {
  //    System.out.println(doc.keySet());
  //  }
  }

  private void extractLine(String line, boolean lastLine) {

    // System.out.println("F:" +line);
    line = line.trim();
    if (line.length() == 0)
      return;
    if (line.length() == 4 || lastLine) {
      String qName = line;
      if (abst) {
        doc.put("abstract", currentString);
        currentString = "";
      }
      if (bsum) {
        doc.put("summary", currentString);
        currentString = "";
      }
      if (clms) {
        doc.put("claims", currentString);
        currentString = "";
      }
      if (detd) {
        if (doc.get("details") != null) {
          System.out.println("ERROR duplicate details field: " + doc.get("title"));
        } else {
          doc.put("details", currentString);
        }
        currentString = "";
      }
      if (xref) {
        doc.put("citations", currentString);
        currentString = "";
      }

      setFalse();
      if (qName.equals("PATN")) {
        patn = true;
        if (doc != null && !error) {
          // System.out.println(this.doc.getFieldValue("publ_docNo") +" + "
          // +doc.getFieldValue("publ_kind") +" + "
          // +doc.getFieldValue("appl_type") +" + "
          // +doc.getFieldValues("classification") +" + "
          // +doc.getFieldValues("patent_citations") +" + "
          // +doc.getFieldValues("citations") );
          docs.add(doc);
        }
        doc = new HashMap<String, Object>();
        error = false;
      }
      if (qName.equals("INVT")) {
        invt = true;
      }
      if (qName.equals("ASSG")) {
        assg = true;
      }
      if (qName.equals("PRIR")) {
        prir = true;
      }
      if (qName.equals("REIS")) {
        reis = true;
      }
      if (qName.equals("RLAP")) {
        rlap = true;
      }
      if (qName.equals("CLAS")) {
        clas = true;
      }
      if (qName.equals("UREF") || qName.equals("FREF") || qName.equals("OREF")) {
        xref = true;
      }
      if (qName.equals("UREF")) {
        uref = true;
      }
      if (qName.equals("LREP")) {
        rlap = true;
      }
      if (qName.equals("PCTA")) {
        pcta = true;
      }
      if (qName.equals("ABST")) {
        abst = true;
      }
      if (qName.equals("GOVT")) {
        govt = true;
      }
      if (qName.equals("PARN")) {
        parn = true;
      }
      if (qName.equals("BSUM")) {
        bsum = true;
      }
      if (qName.equals("DRWD")) {
        drwd = true;
      }
      if (qName.equals("DETD")) {
        detd = true;
      }
      if (qName.equals("CLMS")) {
        clms = true;
      }
      if (qName.equals("DCLM")) {
        dclm = true;
      }
    } else {
      String[] parts = line.split("  ");
      if (parts.length < 2) {
        error = true;
        errorCount++;
        return;
      }
      String qName = parts[0].trim();
      String value = parts[1].trim();
      if (parts.length > 2)
        value += "  " + parts[2];
      if (parts.length > 3)
        value += "  " + parts[3];

      if (qName.equals("PAR") || qName.equals("PAL") || qName.equals("PA1") || qName.equals("PA2")
          || qName.equals("PA3") || qName.equals("PA4") || qName.equals("PA5") || qName.equals("FNT")) {
        currentString += value + " ";
      }
      if (qName.equals("PNO") || (qName.equals("CNT") && xref) || (qName.equals("OCL") && xref) || qName.equals("ISD")
          || qName.equals("NAM")) {
        currentString += value + " ";
      }
      if (qName.equals("NAM") && invt) {
        // insert or update as multi-value field
        if (doc.get("authors") == null){
          doc.put("authors", new ArrayList<String>());
        }
        ArrayList<String> authors = (ArrayList<String>) doc.get("authors");
        String name = value;
        if(name.contains(";")){
        	String[] splitName = name.split(";");
        	name = splitName[1].trim()+" "+splitName[0].trim();
        }
        authors.add(name);
        
        currentString = "";
      }
      if (qName.equals("NAM") && assg) {
        doc.put("assignees", value);
        currentString = "";
      }
      if (qName.equals("APN") && patn) {
        String num = value.trim();
        num = num.substring(0, num.length() - 1);
        if (num.startsWith("0"))
          num = num.substring(1, num.length());
        if (num.startsWith("0"))
          num = num.substring(1, num.length());
        doc.put("appl_docNo", num);
        currentString = "";
      }
      if (qName.equals("WKU") && patn) {

        if (value.startsWith("P"))
          doc.put("publ_kind", "P2");
        if (value.matches("\\d*"))
          doc.put("publ_kind", "B1");
        if (value.startsWith("H"))
          doc.put("publ_kind", "H");
        if (value.startsWith("D"))
          doc.put("publ_kind", "S");
        if (value.startsWith("R"))
          doc.put("publ_kind", "E");
        if (value.startsWith("T"))
          doc.put("publ_kind", "C");
        String num = value.trim();
        num = num.substring(0, num.length() - 1);
        num = num.replaceAll("\\D+", "");
        if (num.startsWith("0"))
          num = num.substring(1, num.length());
        if (num.startsWith("0"))
          num = num.substring(1, num.length());
        doc.put("publ_docNo", num);
        currentString = "";
      }
      if (qName.equals("TTL")) {
        doc.put("title", value);
        currentString = "";
      }
      if (qName.equals("APT")) {
        if (value.equals("6"))
          doc.put("appl_type", "plant");
        if (value.equals("5") || value.equals("7"))
          doc.put("appl_type", "sir");
        if (value.equals("4"))
          doc.put("appl_type", "design");
        if (value.equals("2"))
          doc.put("appl_type", "reissue");
        if (value.equals("1"))
          doc.put("appl_type", "utility");
        currentString = "";
      }
      if (qName.equals("PNO") && uref) {
        if (!value.trim().contains(" ") && !value.contains("/")) {
          if (value.startsWith("0"))
            value = value.substring(1, value.length());
          if (value.startsWith("0"))
            value = value.substring(1, value.length());
          value = value.replaceAll("\\D+", "").trim();

          // insert or update as multi-value field
          if (doc.get("patent_citations") == null){
            doc.put("patent_citations", new ArrayList<String>());
          }
          ArrayList<String> patentCitations = (ArrayList<String>) doc.get("patent_citations");
          patentCitations.add(value);
        }
      }
      if (qName.equals("APD") && patn) {
        value = value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8) + "T00:00:00Z";
        doc.put("appl_date", value);
        currentString = "";
      }
      if (qName.equals("ISD") && patn) {
        value = value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8) + "T00:00:00Z";
        doc.put("publ_date", value);
        currentString = "";
      }
      if ((qName.equals("OCL") || qName.equals("XCL")) && clas) {
        value = value.replaceAll(" D", "D ").replaceAll(" ", "0");
        
        // insert or update as multi-value field
        if (doc.get("classification") == null){
          doc.put("classification", new ArrayList<String>());
        }
        ArrayList<String> classification = (ArrayList<String>) doc.get("classification");
        classification.add(value);        
        currentString = "";
      }
    }
  }

  private void setFalse() {

    patn = false;
    invt = false;
    assg = false;
    prir = false;
    reis = false;
    rlap = false;
    clas = false;
    xref = false;
    uref = false;
    lrep = false;
    pcta = false;
    abst = false;
    govt = false;
    parn = false;
    bsum = false;
    drwd = false;
    detd = false;
    clms = false;
    dclm = false;
  }

  public static String cleanText(String content) {
    if (content == null)
      return "";
    String res =
        content.replaceAll("&#x2018;", "'").replaceAll("&#x2019;", "'").replaceAll("&#x201c;", "'")
            .replaceAll("&#x201d;", "'");
    res = res.replaceAll("<i>", "").replaceAll("</i>", "").replaceAll("&#x2013;", "-").replaceAll("&#x2014;", "-");
    res = res.replaceAll("<sub>", "").replaceAll("</sub>", "").replaceAll("&#x201a;", "'").replaceAll("&#x201e;", "'");
    return res;
  }
}
