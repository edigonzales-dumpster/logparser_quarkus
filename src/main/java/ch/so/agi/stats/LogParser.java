package ch.so.agi.stats;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.jboss.logging.Logger;

public class LogParser {
    private static final Logger log = Logger.getLogger(LogParser.class);
    
    private static final String LOG_ENTRY_PATTERN =
            // 1:IP  2:client 3:user 4:date time 5:method 6:req 7:proto 8:respcode 9:size
            "^(\\S+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+)";
    private static final Pattern PATTERN = Pattern.compile(LOG_ENTRY_PATTERN);
    private static final String DATETIME_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z";

    int j=0;

    @Transactional
    public void doImport(String fileName) throws InterruptedException {
        log.info(fileName);
        
        int i=0;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                i++;
                log.info("Zeile: " + i);
                //if (i>1000) break;
                
                if (line.toLowerCase().contains("piwik") || line.toLowerCase().contains("statuscake") ||
                        line.toLowerCase().contains("nagios")) continue; 

                Matcher m = parseFromLogLine(line);
                
                if (m == null) {
                    continue;
                }              
                
                if (line.toLowerCase().contains("wms") && line.toLowerCase().contains("service") && 
                        line.toLowerCase().contains("request")) {
                        try {
                            readWmsLine(m, line);
                        } catch (SQLException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                }

               //log.info(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }
    
    private void readWmsLine(Matcher m, String line) throws SQLException, URISyntaxException, UnsupportedEncodingException, InterruptedException {
        WmsRequest wmsRequest = new WmsRequest();
        
        wmsRequest.md5 = DigestUtils.md5Hex(line).toUpperCase();
        wmsRequest.ip = m.group(1);
        
        // TODO: Das stimmt noch nicht.
        // Welche Zeit/Zone steckt im Logfile?
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(m.group(4), formatter);  
        Timestamp timestamp = Timestamp.from(zonedDateTime.toInstant());
        Calendar cal = GregorianCalendar.from(zonedDateTime);
        wmsRequest.requestTime = timestamp;
                
        wmsRequest.requestMethod = m.group(5);
        wmsRequest.request = m.group(6);

        String[] layers = new String[0];
        
        URIBuilder builder = new URIBuilder(m.group(6), Charset.forName("UTF-8"));
        List<NameValuePair> params = builder.getQueryParams();
        for (NameValuePair param : params) {
            String paramName = param.getName();
            String paramValue = param.getValue();
            
            if (paramName.equalsIgnoreCase("request")) {
                wmsRequest.wmsRequestType = paramValue.toLowerCase();
            } else if (paramName.equalsIgnoreCase("srs") || paramName.equalsIgnoreCase("crs")) {
                if (paramValue.length() > 5) {
                    String decodedValue = URLDecoder.decode(paramValue, "UTF-8");
                    wmsRequest.wmsSrs = Integer.valueOf(decodedValue.split(":")[1]);
                }
            } else if (paramName.equalsIgnoreCase("bbox")) {
                wmsRequest.wmsBbox = paramValue;
            } else if (paramName.equalsIgnoreCase("width")) {
                try {
                    wmsRequest.wmsWidth = Integer.valueOf(paramValue);
                } catch (NumberFormatException e) {}
            } else if (paramName.equalsIgnoreCase("height")) {
                try {
                    wmsRequest.wmsHeight = Integer.valueOf(paramValue);
                } catch (NumberFormatException e) {}
            } else if (paramName.equalsIgnoreCase("dpi")) {
                try {
                    wmsRequest.dpi = Double.valueOf(paramValue);
                } catch (NumberFormatException e) {}
            } else if (paramName.equalsIgnoreCase("layers")) {
                String decodedValue = URLDecoder.decode(paramValue, "UTF-8");
                layers = decodedValue.split(",");
                wmsRequest.wmsLayers = decodedValue;
            } 
        }
        
        // TODO: Das verstehe ich nicht ganz. Ich habe es so verstanden,
        // dass persistAndFlush immer gleich schreibt. Oder liegt es
        // daran, dass es eben immer noch eine Transaktion ist? Und
        // aus diesem Grund bei einem Fehler gar keine Daten in der DB
        // sind?
        // Oder vielleicht sind auch die Transactional etc. Annotation
        // am falschen Ort.
        // Das Beispiel dünkt mich sehr ähnlich: https://quarkus.io/guides/hibernate-orm-panache#transactions
        // Ah, LogParser ist noch keine Bean.
        if (wmsRequest.find("md5", wmsRequest.md5).firstResult() != null) {
            return;
        }

        wmsRequest.persistAndFlush();

        for (String layer : layers) {
            WmsRequestLayer wmsRequestLayer = new WmsRequestLayer();
            wmsRequestLayer.request = wmsRequest;
            wmsRequestLayer.layerName = layer;
            wmsRequestLayer.persistAndFlush();
        }
        
        
        //Thread.sleep(10000);
    }
        
        
    // https://databricks.gitbooks.io/databricks-spark-reference-applications/content/logs_analyzer/chapter1/java8/src/main/java/com/databricks/apps/logs/ApacheAccessLog.java
    private Matcher parseFromLogLine(String logline) {
        Matcher m = PATTERN.matcher(logline);
        if (!m.find()) {
          return null;
        }
        return m;
    }

}
