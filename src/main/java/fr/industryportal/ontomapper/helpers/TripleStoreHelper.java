package fr.industryportal.ontomapper.helpers;

import fr.industryportal.ontomapper.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class TripleStoreHelper {

    @Autowired
    private AppConfig appConfig;

    String getLabelFromTripleStore(String classUri) {

        String sparqlQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?object\n" +
                "WHERE {\n" +
                "<" + classUri + ">" + " <http://www.w3.org/2000/01/rdf-schema#label> ?object .\n" +
                "}\n" +
                "LIMIT 1";

        try {
            URL url = new URL(appConfig.getTripleStoreUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            String queryParam = "query=" + java.net.URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = queryParam.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    response.append("\n");
                }
            }

            String label = parseXMLResponse(response.toString());
            return ((label == null ) ? classUri : label);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            return classUri;
        }
    }

    private static String parseXMLResponse(String xmlResponse) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

        NodeList nodeList = document.getElementsByTagName("binding");
        if (nodeList.getLength() > 0) {
            Element bindingElement = (Element) nodeList.item(0);
            NodeList literalList = bindingElement.getElementsByTagName("literal");
            if (literalList.getLength() > 0) {
                return literalList.item(0).getTextContent();
            }
        }

        return null;
    }



}
