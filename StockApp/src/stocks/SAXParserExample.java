package stocks;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

public class SAXParserExample extends DefaultHandler {

    List myStocks;

    private String tempVal;

    private static final String DIRECTORY = "C:/dev/repos/StockApp/list";

    //to maintain context
    private Stock tempStock;

    public SAXParserExample() {
        myStocks = new ArrayList();
    }

    public void runExample() {
        parseDocument();
        printData();
    }

    private void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {

            //look through dir
            File directory = new File(DIRECTORY);
            String fileNames[] = directory.list();

            for (int x = 0; x < fileNames.length; x++) {

                //Skip indices
                if (fileNames[x].equals(".DJI.xml") || fileNames[x].equals(".INX.xml") || fileNames[x].equals(".IXIC.xml")) {
                    continue;
                }
                //get a new instance of parser
                SAXParser sp = spf.newSAXParser();

                //parse the file and also register this class for call backs
                sp.parse(DIRECTORY + "/" + fileNames[x], this);
            }
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Iterate through the list and print
     * the contents
     */

    private void printData() {

        System.out.println("No of Stocks '" + myStocks.size() + "'.");

        Iterator it = myStocks.iterator();
        while (it.hasNext()) {
            System.out.println(it.next().toString());
        }
    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
        if (qName.equalsIgnoreCase("xml_api_reply")) {
            //create a new instance of employee
            tempStock = new Stock();
            //   tempStock.setType(attributes.getValue("type"));
        } else if (qName.equalsIgnoreCase("pretty_symbol")) {
            tempStock.setSymbol(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("symbol_url")) {
            tempStock.setLookupURL(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("company")) {
            tempStock.setName(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("exchange")) {
            tempStock.setExchange(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("last")) {
            tempStock.setLastPrice(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("high")) {
            tempStock.setHighPrice(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("low")) {
            tempStock.setLowPrice(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("volume")) {
            tempStock.setVolume(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("market_cap")) {
            tempStock.setMarketCap(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("open")) {
            tempStock.setOpenPrice(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("y_close")) {
            tempStock.setClosePrice(attributes.getValue("data"));
        } else if (qName.equalsIgnoreCase("perc_change")) {
            tempStock.setPerChange(attributes.getValue("data"));
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("xml_api_reply")) {
            //add it to the list
            myStocks.add(tempStock);
        }
    }

    public static void main(String[] args) {
        SAXParserExample spe = new SAXParserExample();
        spe.runExample();
    }
}




