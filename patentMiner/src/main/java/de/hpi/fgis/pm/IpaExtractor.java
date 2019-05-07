package de.hpi.fgis.pm;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IpaExtractor extends DefaultHandler implements Runnable, PatentMinerConstants {

  Collection<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
  Map<String, Object> doc;
  boolean isDoc;

  String zipfile;
  Client server;

  boolean desc = false;
  boolean heading = false;
  boolean country = false;

  boolean abst = false;
  String abstString = null;
  boolean title = false;
  String titleString = null;

  boolean cl = false;
  String clString = null;

  boolean brfsum = false;
  String brfsumString = null;
  boolean detdesc = false;
  String detdescString = null;

  boolean publ = false;
  boolean appl = false;
  boolean docNo = false;
  boolean kind = false;
  boolean date = false;

  boolean claims = false;
  String claimsString = null;

  boolean author = false;

  boolean authorFirstName = false;
  boolean authorLastName = false;
  boolean authorOrgName = false;

  String authorStringFirst = null;
  String authorStringLast = null;
  String authorStringOrg = null;

  SAXParser saxParser = null;

  public IpaExtractor(String zipfile, Client server) {

    this.zipfile = zipfile;
    this.server = server;

    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      saxParser = factory.newSAXParser();
    } catch (ParserConfigurationException e2) {
      e2.printStackTrace();
    } catch (SAXException e2) {
      e2.printStackTrace();
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

    if (qName.equals("description")) {
      desc = true;
      brfsumString = "";
      detdescString = "";
    }
    if (qName.equals("heading")) {
      heading = true;
    }
    if (qName.equals("country")) {
      country = true;
    }
    if (qName.equals("abstract")) {
      // System.out.print("<" + qName +">");
      abst = true;
      abstString = "";
    }
    if (qName.equals("publication-reference")) {
      // System.out.println("<" + qName +">");
      publ = true;
    }
    if (qName.equals("application-reference")) {
      String applType = attributes.getValue("appl-type");
      // System.out.println("<" + qName +" " +applType +">");
      doc.put("appl_type", applType);
      appl = true;
      isDoc = true;
    }
    if (qName.equals("doc-number")) {
      // if(appl || publ) System.out.print("<" + qName +">");
      docNo = true;
    }
    if (qName.equals("kind") || qName.equals("kind-code")) {
      // if(appl || publ) System.out.print("<" + qName +">");
      kind = true;
    }
    if (qName.equals("date") || qName.equals("document-date")) {
      // if(appl || publ) System.out.print("<" + qName +">");
      date = true;
    }
    if (qName.equals("invention-title")) {
      // System.out.print("<" + qName +">");
      title = true;
      titleString = "";
    }
    if (qName.equals("classification-ipc") || qName.equals("classification-ipcr")) {
      // System.out.print("<" + qName +">");
      cl = true;
      clString = "";
    }
    if (qName.equals("claims")) {
      // System.out.print("<" + qName +">");
      claims = true;
      claimsString = "";
    }
    if (qName.equals("applicant")) {
      String appType = attributes.getValue("app-type");
      // System.out.println("<" + qName +" " +applType +">");
      if (appType.equals("applicant-inventor")) {
        author = true;
      }
      authorStringFirst = "";
      authorStringLast = "";
      authorStringOrg = "";
    }
    if (qName.equals("last-name")) {
      authorLastName = true;
    }
    if (qName.equals("first-name")) {
      authorFirstName = true;
    }
    if (qName.equals("orgname")) {
      authorOrgName = true;
    }
    if (qName.equals("assignee")) {
      authorStringFirst = "";
      authorStringLast = "";
      authorStringOrg = "";
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    if (qName.equals("description")) {
      desc = false;
      doc.put("summary", brfsumString.trim());
      doc.put("details", detdescString.trim());
    }
    if (qName.equals("heading")) {
      heading = false;
    }
    if (qName.equals("country")) {
      country = false;
    }
    if (qName.equals("abstract")) {
      // System.out.println("</" + qName +">");
      abst = false;
      doc.put("abstract", abstString.trim());
    }
    if (qName.equals("publication-reference")) {
      // System.out.println("</" + qName +">");
      publ = false;
    }
    if (qName.equals("application-reference")) {
      // System.out.println("</" + qName +">");
      appl = false;
    }
    if (qName.equals("doc-number")) {
      // if(appl || publ) System.out.println("</" + qName +">");
      docNo = false;
    }
    if (qName.equals("kind") || qName.equals("kind-code")) {
      // if(appl || publ) System.out.println("</" + qName +">");
      kind = false;
    }
    if (qName.equals("date") || qName.equals("document-date")) {
      // if(appl || publ) System.out.println("</" + qName +">");
      date = false;
    }
    if (qName.equals("invention-title")) {
      // System.out.println("</" + qName +">");
      title = false;
      doc.put("title", titleString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("classification-ipc") || qName.equals("classification-ipcr")) {
      // System.out.println("</" + qName +">");
      cl = false;
      if (doc.get("classification") == null) {
    	  doc.put("classification", new ArrayList<String>());
      }
      ArrayList<String> newClassificationList = (ArrayList<String>) doc.get("classification");
      newClassificationList.add(clString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("claims")) {
      // System.out.println("</" + qName +">");
      claims = false;
      doc.put("claims", claimsString.trim());
    }
    if (qName.equals("applicant") && author) {
      // System.out.println("</" + qName +">");
      author = false;
      if (doc.get("authors") == null) {
          doc.put("authors", new ArrayList<String>());
      }
      ArrayList<String> authors = (ArrayList<String>) doc.get("authors");
      authors.add((authorStringFirst + " " + authorStringLast + " " + authorStringOrg).trim());
    }
    if (qName.equals("last-name")) {
      authorLastName = false;
    }
    if (qName.equals("first-name")) {
      authorFirstName = false;
    }
    if (qName.equals("orgname")) {
      authorOrgName = false;
    }
    if (qName.equals("assignee")) {
      doc.put("assignees", (authorStringFirst + " " + authorStringLast + " " + authorStringOrg).trim());
    }
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {

    if (desc && brfsum && !heading) {
      String content = new String(ch, start, length);
      content =
          content.replaceAll("1. Field of the Invention", "").replaceAll("2. Description of Related Art", "")
              .replaceAll("2. Description of the Related Art", "");
      content = content.replaceAll("Botanical designation:", "").replaceAll("Cultivar denomination:", "");
      brfsumString += content;
    }
    if (desc && detdesc && !heading) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      detdescString += content;
    }
    if (abst) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      abstString += content;
    }
    if (appl && docNo) {
      String content = new String(ch, start, length);
      String num = content.replaceAll("D", "").trim();
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      // System.out.print(content +" ");
      doc.put("appl_docNo", num);
    }
    if (publ && docNo) {
      String content = new String(ch, start, length);
      String num = content.replaceAll("D", "").trim();
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      // System.out.print(content +" ");
      // if(content.equals("D0631037")) System.exit(5);
      doc.put("publ_docNo", num);
    }
    if (publ && kind) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      doc.put("publ_kind", content.trim());
    }
    if (appl && date) {
      String content = new String(ch, start, length);
      content = content.substring(0, 4) + "-" + content.substring(4, 6) + "-" + content.substring(6, 8) + "T00:00:00Z";
      // System.out.print(content +" ");
      doc.put("appl_date", content.trim());
    }
    if (publ && date) {
      String content = new String(ch, start, length);
      content = content.substring(0, 4) + "-" + content.substring(4, 6) + "-" + content.substring(6, 8) + "T00:00:00Z";
      // System.out.print(content +" ");
      doc.put("publ_date", content.trim());
    }
    if (title) {
      String content = new String(ch, start, length);
      // System.out.println(content);
      titleString += content;
    }
    if (cl) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      clString += content + " | ";
    }
    if (claims) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      claimsString += content;
    }
    if (authorLastName) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      authorStringLast += content;
    }
    if (authorFirstName) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      authorStringFirst += content;
    }
    if (authorOrgName) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      authorStringOrg += content;
    }
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) {

    return new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {

    if (target.equals("summary-of-invention")) {
      // System.out.print(":" + target +":");
      // if(brfsum==true) System.out.println();
      brfsum = !brfsum;
    }
    if (target.equals("detailed-description")) {
      // System.out.print(":" + target +":");
      // if(detdesc==true) System.out.println();
      detdesc = !detdesc;
    }
    // System.out.println("PI: " +target +"; " +data);

  }

  @Override
  public void run() {

    docs = new ArrayList<Map<String, Object>>();

    try {
      ZipFile zf = new ZipFile(zipfile);
      try {
        loop: for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
          ZipEntry ze = e.nextElement();
          String name = ze.getName();

          if (name.endsWith(".xml") || name.endsWith(".XML")) {
            System.out.println("Processing: " + name);
            saxParser.reset();
            int docCount = 0;
            InputStream in = zf.getInputStream(ze);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder builder = null;
            for (String line; (line = reader.readLine()) != null;) {
              if (line.startsWith("<?xml")) {
                if (builder != null) {
                  String text = builder.toString();
                  if (text.length() > 100) {
                    doc = new HashMap<String, Object>();
                    isDoc = false;
                    InputSource is = new InputSource(new StringReader(text));
                    is.setEncoding("UTF-8");
                    try {
                      saxParser.parse(is, this);
                    } catch (SAXException e1) {
                      e1.printStackTrace();
                    }
                    if (isDoc) {
                      if (docCount++ < 1) {
                        if (ElasticSearchHelpers.getDocById((String) doc.get("publ_docNo")) != null) {
                          System.out.println(name + " already in index...");
                          continue loop;
                        }
                      }
                      docs.add(doc);
                      // System.out.println(doc.getFieldValue("authors")
                      // +"                                       "
                      // +doc.getFieldValue("title"));
                    }
                  }
                }
                builder = new StringBuilder();
              }
              builder.append(IpaExtractor.cleanText(line));
            }
            if (builder != null) {
              String text = builder.toString();
              if (text.length() > 100) {
                doc = new HashMap<String, Object>();
                isDoc = false;
                InputSource is = new InputSource(new StringReader(text));
                is.setEncoding("UTF-8");
                try {
                  saxParser.parse(is, this);
                } catch (SAXException e1) {
                  e1.printStackTrace();
                }
                if (isDoc) {
                  docCount++;
                  docs.add(doc);
                }
              }
            }
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
      Client client = ElasticSearchHelpers.buildClient();
      BulkProcessor bulkProcessor =
          BulkProcessor.builder(client, new BulkProcessor.Listener() {

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
        bulkProcessor.add(new IndexRequest(INDEX_NAME, PATENT_TYPE_NAME, (String) doc.get("publ_docNo")).source(doc));
      }
      try {
        bulkProcessor.awaitClose(10, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } // helps to prevent failures?
      client.close();
    }
    System.out.println("Processed " + zipfile + "; " + docs.size() + " docs;");
  }

  public static String cleanText(String content) {

    String res =
        content.replaceAll("&#x2018;", "'").replaceAll("&#x2019;", "'").replaceAll("&#x201c;", "'")
            .replaceAll("&#x201d;", "'");
    res = res.replaceAll("<i>", "").replaceAll("</i>", "").replaceAll("&#x2013;", "-").replaceAll("&#x2014;", "-");
    res = res.replaceAll("<sub>", "").replaceAll("</sub>", "").replaceAll("&#x201a;", "'").replaceAll("&#x201e;", "'");
    return res;
  }
}
