package fr.industryportal.ontomapper.helpers;

import fr.industryportal.ontomapper.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    public List<String[]> getOntologyFromTripleStore(String acronym) {

        Set<String> graphNames = getGraphNamesFromTripleStore();

        String graphName = "";
        for (String name : graphNames) {
            if (name.contains(acronym)) {
                graphName = name;
                break;
            }
        }

        String sparqlQuery = "SELECT ?subject ?predicate ?object\n" +
                "WHERE {\n" +
                "  GRAPH <"+graphName+"> {\n" +
                "    ?subject ?predicate ?object .\n" +
                "  }\n" +
                "}\n";

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

            List<String[]> triples = getTriplesFromSparqlResponse(response.toString());


            return triples;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    Set<String> getGraphNamesFromTripleStore() {

        String sparqlQuery = "SELECT DISTINCT ?graph\n" +
                "WHERE {\n" +
                "  GRAPH ?graph { ?s ?p ?o }\n" +
                "}\n";

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

            Set<String> result = parseGraphNamesFromResponse(response.toString());


            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.EMPTY_SET;
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

    private static List<String[]> getTriplesFromSparqlResponse(String xmlResponse) throws ParserConfigurationException, IOException, SAXException {
        List<String[]> triples = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(is);

        NodeList resultNodes = doc.getElementsByTagName("result");
        for (int i = 0; i < resultNodes.getLength(); i++) {
            NodeList bindingNodes = resultNodes.item(i).getChildNodes();
            String subject = "", predicate = "", object = "";
            for (int j = 0; j < bindingNodes.getLength(); j++) {
                if (bindingNodes.item(j).getNodeName().equals("binding")) {
                    String name = bindingNodes.item(j).getAttributes().getNamedItem("name").getNodeValue();
                    String value = bindingNodes.item(j).getTextContent().trim();
                    switch (name) {
                        case "subject":
                            subject = value;
                            break;
                        case "predicate":
                            predicate = value;
                            break;
                        case "object":
                            object = value;
                            break;
                    }
                }
            }
            if (predicate.contains("metadata")) continue;
            triples.add(new String[]{subject, predicate, object});
        }

        return triples;
    }


    private Set<String> parseGraphNamesFromResponse(String xmlResponse) {
        Set<String> graphNames = new HashSet<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
            document.getDocumentElement().normalize();

            NodeList nList = document.getElementsByTagName("binding");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Element element = (Element) nList.item(temp);
                if (element.getAttribute("name").equals("graph")) {
                    NodeList uriList = element.getElementsByTagName("uri");
                    if (uriList.getLength() > 0) {
                        String graphName = uriList.item(0).getTextContent();
                        graphNames.add(graphName);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return graphNames;
    }

}
