package de.hpi.fgis.pm;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PgExtractor extends DefaultHandler implements Runnable, PatentMinerConstants {

  Collection<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
  Map<String, Object> doc;

  boolean isDoc;

  String zipfile;

  boolean heading = false;
  boolean cwu = false;
  boolean num = false;

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

  boolean publDocNo = false;
  String publDocNoString = null;
  boolean applDocNo = false;
  String applDocNoString = null;
  boolean kind = false;
  String kindString = null;

  boolean publDate = false;
  String publDateString = null;
  boolean applDate = false;
  String applDateString = null;

  boolean claims = false;
  String claimsString = null;

  boolean citations = false;
  String citationsString = null;

  boolean patcit = false;
  String patcitString = null;

  boolean author = false;
  boolean name = false;
  String authorNameString = null;

  boolean assignee = false;
  String assigneeString = null;

  SAXParser saxParser = null;

  public PgExtractor(String zipfile) {

    this.zipfile = zipfile;

    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      saxParser = factory.newSAXParser();
    } catch (ParserConfigurationException e2) {
      e2.printStackTrace();
    } catch (SAXException e2) {
      e2.printStackTrace();
    }
  }

  /*
   * Kind codes changed effective 2001-01-02 to accommodate pre-grant
   * publication status.
   * 
   * A1 - Utility Patent issued prior to January 2, 2001. A1 - Utility Patent
   * Application published on or after January 2, 2001. A2 - Second or
   * subsequent publication of a Utility Patent Application. A9 - Corrected
   * published Utility Patent Application. Bn - Reexamination Certificate issued
   * prior to January 2, 2001. NOTE: "n" represents a value 1 through 9. B1 -
   * Utility Patent (no pre-grant publication) issued on or after January 2,
   * 2001. B2 - Utility Patent (with pre-grant publication) issued on or after
   * January 2, 2001. Cn - Reexamination Certificate issued on or after January
   * 2, 2001. NOTE: "n" represents a value 1 through 9 denoting the publication
   * level. E1 - Reissue Patent. Fn - Reexamination Certificate of a Reissue
   * Patent NOTE: "n" represents a value 1 through 9 denoting the publication
   * level. H1 - Statutory Invention Registration (SIR) Patent Documents. SIR
   * documents began with the December 3, 1985 issue. I1 - "X" Patents issued
   * from July 31, 1790 to July 13, 1836. I2 - "X" Reissue Patents issued from
   * July 31, 1790 to July 13, 1836. I3 - Additional Improvements - Patents
   * issued between 1838 and 1861. I4 - Defensive Publication - Documents issued
   * from November 5, 1968 through May 5, 1987. I5 - Trial Voluntary Protest
   * Program (TVPP) Patent Documents. NP - Non-Patent Literature. P1 - Plant
   * Patent issued prior to January 2, 2001. P1 - Plant Patent Application
   * published on or after January 2, 2001. P2 - Plant Patent (no pre-grant
   * publication) issued on or after January 2, 2001. P3 - Plant Patent (with
   * pre-grant publication) issued on or after January 2, 2001. P4 - Second or
   * subsequent publication of a Plant Patent Application. P9 - Correction
   * publication of a Plant Patent Application. S1 - Design Patent.
   */

  /*
   * 
   * <B100> <B110><DNUM><PDAT>D0462153</PDAT></DNUM></B110>
   * <B130><PDAT>S1</PDAT></B130>
   * <B140><DATE><PDAT>20020903</PDAT></DATE></B140>
   * <B190><PDAT>US</PDAT></B190> </B100>
   * 
   * <B200> <B210><DNUM><PDAT>29133735</PDAT></DNUM></B210>
   * <B211US><PDAT>29</PDAT></B211US>
   * <B220><DATE><PDAT>20001204</PDAT></DATE></B220> </B200>
   */

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

    if (qName.equals("H")) {
      heading = true;
    }
    if (qName.equals("CWU")) {
      cwu = true;
    }
    if (qName.equals("DNUM")) {
      num = true;
    }
    if (qName.equals("PCIT")) {
      patcit = true;
      patcitString = "";
    }
    if (qName.equals("SDOAB")) {
      abst = true;
      abstString = "";
    }
    if (qName.equals("B110")) {
      publDocNo = true;
      publDocNoString = "";
    }
    if (qName.equals("B210")) {
      applDocNo = true;
      applDocNoString = "";
    }
    if (qName.equals("B130")) {
      kind = true;
      kindString = "";
    }
    if (qName.equals("B140")) {
      publDate = true;
      publDateString = "";
    }
    if (qName.equals("B220")) {
      applDate = true;
      applDateString = "";
    }
    if (qName.equals("B540")) {
      title = true;
      titleString = "";
    }
    if (qName.equals("B521") || qName.equals("B522")) {
      cl = true;
      clString = "";
    }
    if (qName.equals("CL")) {
      claims = true;
      claimsString = "";
    }
    if (qName.equals("B561")) {
      citations = true;
      citationsString = "";
    }
    if (qName.equals("B562")) {
      citations = true;
      citationsString = "";
    }
    if (qName.equals("BRFSUM")) {
      brfsum = true;
      brfsumString = "";
    }
    if (qName.equals("DETDESC")) {
      detdesc = true;
      detdescString = "";
    }
    if (qName.equals("B721")) {
      author = true;
      authorNameString = "";
    }
    if (qName.equals("B731")) {
      assignee = true;
      assigneeString = "";
    }
    if (qName.equals("NAM")) {
      name = true;
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    if (qName.equals("H")) {
      heading = false;
    }
    if (qName.equals("CWU")) {
      cwu = false;
    }
    if (qName.equals("DNUM")) {
      num = false;
    }
    if (qName.equals("PCIT")) {
      patcit = false;
      if (doc.get("patent_citations") == null){
        doc.put("patent_citations", new ArrayList<String>());
      }
      ArrayList<String> patentCitations = (ArrayList<String>) doc.get("patent_citations");
      patentCitations.add(patcitString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("BRFSUM")) {
      brfsum = false;
      doc.put("summary", brfsumString.trim());
    }
    if (qName.equals("DETDESC")) {
      detdesc = false;
      doc.put("details", detdescString.trim());
    }
    if (qName.equals("SDOAB")) {
      abst = false;
      doc.put("abstract", abstString.trim());
    }
    if (qName.equals("B210")) {
      applDocNo = false;
      doc.put("appl_docNo", applDocNoString.trim());
    }
    if (qName.equals("B110")) {
      publDocNo = false;
      doc.put("publ_docNo", publDocNoString.trim());
      doc.put("appl_type", "na");

    }
    if (qName.equals("B130")) {
      kind = false;
      doc.put("publ_kind", kindString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("B220")) {
      applDate = false;
      doc.put("appl_date", applDateString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("B140")) {
      publDate = false;
      doc.put("publ_date", publDateString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("B540")) {
      title = false;
      doc.put("title", titleString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("B521") || qName.equals("B522")) {
      cl = false;
      if (doc.get("classification") == null) {
    	  doc.put("classification", new ArrayList<String>());
      }
      ArrayList<String> newClassificationList = (ArrayList<String>) doc.get("classification");
      newClassificationList.add(clString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("CL")) {
      claims = false;
      doc.put("claims", claimsString.trim());
    }
    if (qName.equals("B561") || qName.equals("B562")) {
      citations = false;
      if (doc.get("citations") == null) {
    	  doc.put("citations", new ArrayList<String>());
      }
      ArrayList<String> newCitationsList = (ArrayList<String>) doc.get("citations");
      newCitationsList.add(citationsString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("B721")) {
      author = false;
      if (doc.get("authors") == null) {
    	  doc.put("authors", new ArrayList<String>());
      }
      ArrayList<String> authors = (ArrayList<String>) doc.get("authors");
      authors.add(authorNameString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("B731")) {
      assignee = false;
      doc.put("assignees", assigneeString.replaceAll("\\n", "").trim());
    }
    if (qName.equals("NAM")) {
      name = false;
    }
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {

    if (brfsum && !heading && !cwu) {
      String content = new String(ch, start, length);
      content =
          content.replaceAll("1. Field of the Invention", "").replaceAll("2. Description of Related Art", "")
              .replaceAll("2. Description of the Related Art", "");
      content = content.replaceAll("Botanical designation:", "").replaceAll("Cultivar denomination:", "");
      brfsumString += content;
    }
    if (detdesc && !heading && !cwu) {
      String content = new String(ch, start, length);
      detdescString += content;
    }
    if (abst && !cwu) {
      String content = new String(ch, start, length);
      abstString += content;
    }
    if (applDocNo) {
      String content = new String(ch, start, length);
      String num = content.replaceAll("D", "").trim();
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      applDocNoString += num;
      isDoc = true;
    }
    if (publDocNo) {
      String content = new String(ch, start, length);
      String num = content.replaceAll("D", "").trim();
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      publDocNoString += num;
    }
    if (kind) {
      String content = new String(ch, start, length);
      kindString += content;
    }
    if (applDate) {
      String content = new String(ch, start, length);
      content = content.substring(0, 4) + "-" + content.substring(4, 6) + "-" + content.substring(6, 8) + "T00:00:00Z";
      applDateString += content;
    }
    if (publDate) {
      String content = new String(ch, start, length);
      content = content.substring(0, 4) + "-" + content.substring(4, 6) + "-" + content.substring(6, 8) + "T00:00:00Z";
      publDateString += content;
    }
    if (title) {
      String content = new String(ch, start, length);
      titleString += content;
    }
    if (cl) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      clString += content.replaceAll(" D", "D ").replaceAll(" ", "0");
    }
    if (claims && !cwu) {
      String content = new String(ch, start, length);
      // System.out.print(content +" ");
      claimsString += content;
    }
    if (citations) {
      String content = new String(ch, start, length);
      citationsString += content.trim() + " | ";
    }
    if (citations && patcit && num) {
      String content = new String(ch, start, length);
      String num = content.replaceAll("D", "").trim();
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      if (num.startsWith("0"))
        num = num.substring(1, num.length());
      patcitString += num;
    }
    if (author && name) {
      String content = new String(ch, start, length);
      authorNameString += content + " ";
    }
    if (assignee && name) {
      String content = new String(ch, start, length);
      assigneeString += content + " ";
    }
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) {

    return new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {

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
          if (name.endsWith(".xml") || name.endsWith(".XML") || name.endsWith(".SGML") || name.endsWith(".sgm")) {
            System.out.println("Processing: " + name);
            saxParser.reset();
            int docCount = 0;
            InputStream in = zf.getInputStream(ze);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder builder = null;
            for (String line; (line = reader.readLine()) != null;) {
              if (name.endsWith(".sgm") || name.endsWith(".SGML")) {
                // System.out.println("pre: " +line);
                String patternStr = "<EMI (.*?)>";
                Pattern pattern = Pattern.compile(patternStr);
                String patternStr2 = "<COLSPEC (.*?)>";
                Pattern pattern2 = Pattern.compile(patternStr2);
                String patternStr3 = "<CHEMMOL (.*?)>";
                Pattern pattern3 = Pattern.compile(patternStr3);
                String patternStr4 = "<INS-E (.*?)>";
                Pattern pattern4 = Pattern.compile(patternStr4);
                String patternStr5 = "<MATHEMATICA (.*?)>";
                Pattern pattern5 = Pattern.compile(patternStr5);
                String patternStr6 = "<INS-S (.*?)>";
                Pattern pattern6 = Pattern.compile(patternStr6);
                String patternStr7 = "<CHEMCDX (.*?)>";
                Pattern pattern7 = Pattern.compile(patternStr7);
                String patternStr8 = "<DEL-E (.*?)>";
                Pattern pattern8 = Pattern.compile(patternStr8);
                String patternStr9 = "<DEL-S (.*?)>";
                Pattern pattern9 = Pattern.compile(patternStr9);
                String patternStr10 = "<CUSTOM-CHARACTER (.*?)>";
                Pattern pattern10 = Pattern.compile(patternStr10);

                line = line.replaceAll("circ ;", "circ;");
                line =
                    line.replace(
                        "<!DOCTYPE PATDOC PUBLIC \"-//USPTO//DTD ST.32 US PATENT GRANT V2.4 2000-09-20//EN\" ",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE PATDOC SYSTEM \"ST32-US-Grant-025xml.dtd\" ");
                line = line.replace("<CITED-BY-EXAMINER>", "<CITED-BY-EXAMINER/>");
                line = line.replace("<CITED-BY-OTHER>", "<CITED-BY-OTHER/>");
                line = line.replace("<none>", "<none/>");
                line = line.replace("<mprescripts>", "<mprescripts/>");
                line = line.replace("<B597US>", "<B597US/>");
                line = line.replace("<B473US>", "<B473US/>");
                line = line.replace("<B221US>", "<B221US/>");
                Matcher matcher = pattern.matcher(line);
                line = matcher.replaceAll("<EMI $1 />");
                matcher = pattern2.matcher(line);
                line = matcher.replaceAll("<COLSPEC $1 />");
                matcher = pattern3.matcher(line);
                line = matcher.replaceAll("<CHEMMOL $1 />");
                matcher = pattern4.matcher(line);
                line = matcher.replaceAll("<INS-E $1 />");
                matcher = pattern5.matcher(line);
                line = matcher.replaceAll("<MATHEMATICA $1 />");
                matcher = pattern6.matcher(line);
                line = matcher.replaceAll("<INS-S $1 />");
                matcher = pattern7.matcher(line);
                line = matcher.replaceAll("<CHEMCDX $1 />");
                matcher = pattern8.matcher(line);
                line = matcher.replaceAll("<DEL-E $1 />");
                matcher = pattern9.matcher(line);
                line = matcher.replaceAll("<DEL-S $1 />");
                matcher = pattern10.matcher(line);
                line = matcher.replaceAll("<CUSTOM-CHARACTER $1 />");
                // System.out.println("pos: " +line);
              }
              if (line.startsWith("<?xml")) {
                if (builder != null) {
                  String text = builder.toString();
                  if (text.length() > 100) {
                    // System.out.print("Parsing doc " +docCount +": ");
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
                        /*
                      if (docCount++ < 1) {
                        if (ElasticSearchHelpers.getDocById((String) doc.get("publ_docNo")) != null) {
                          System.out.println(name + " already in index...");
                          continue loop;
                        }
                      }
                      */
                      docs.add(doc);
                      // System.out.println(doc.getFieldValue("authors")
                      // +"                                       "
                      // +doc.getFieldValue("title"));
                    }
                  }
                }
                builder = new StringBuilder();
              }
              builder.append(PgExtractor.cleanText(line));
            }
            if (builder != null) {
              String text = builder.toString();
              if (text.length() > 100) {
                // System.out.print("Parsing doc " +docCount +": ");
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
