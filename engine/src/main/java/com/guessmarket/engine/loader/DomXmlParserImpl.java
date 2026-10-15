package com.guessmarket.engine.loader;

import com.guessmarket.engine.dto.IStagingMarketData;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DomXmlParserImpl implements IXmlParser {

    private static final Logger LOGGER = Logger.getLogger(DomXmlParserImpl.class.getName());

    
    private static final String EVENT_TAG = "GM-event";
    private static final String MARKET_MAKER_TAG = "GM-market-maker";
    private static final String EVENT_IDENTIFIER_ATTR = "name";

    @Override
    public IStagingMarketData parseToStaging(File xmlFile) throws Exception {
        if (xmlFile == null || !xmlFile.exists()) {
            throw new IllegalArgumentException("XML file cannot be null or missing.");
        }

        try (InputStream is = new FileInputStream(xmlFile)) {
            return parseToStaging(is, "SystemUser");
        }
    }

    @Override
    public IStagingMarketData parseToStaging(InputStream xmlStream, String uploaderUsername) throws Exception {
        if (xmlStream == null) {
            throw new IllegalArgumentException("XML stream cannot be null.");
        }

        LOGGER.info("Initiating secure DOM parsing for incremental upload by user: " + uploaderUsername);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlStream);
        document.getDocumentElement().normalize();

        
        injectMarketMakerNodes(document, uploaderUsername);

        
        
        IStagingMarketData stagingData = null; 

        
        
        

        LOGGER.info("Successfully parsed incremental XML into Staging Area.");

        if (stagingData == null) {
            throw new UnsupportedOperationException("Developer Action Required: Instantiate your concrete IStagingMarketData implementation.");
        }

        return stagingData;
    }

    
    private void injectMarketMakerNodes(Document doc, String uploaderUsername) {
        NodeList eventNodes = doc.getElementsByTagName(EVENT_TAG);

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node node = eventNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eventElement = (Element) node;

                
                Element marketMakerElement = doc.createElement(MARKET_MAKER_TAG);
                marketMakerElement.setTextContent(uploaderUsername);

                
                eventElement.appendChild(marketMakerElement);

                
                String eventName = eventElement.getAttribute(EVENT_IDENTIFIER_ATTR);
                LOGGER.log(Level.FINE, "In-memory DOM Mutation: Injected Market Maker [{0}] into Event [{1}]",
                        new Object[]{uploaderUsername, eventName});
            }
        }
    }
}