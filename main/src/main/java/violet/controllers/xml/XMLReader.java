package violet.controllers.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.EmptyStackException;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * A class for reading easier the XML output by the NintendoGatherer
 * class. It's based upon the SAXParser, which is explained in this ORACLE doc:
 * https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html
 * 
 * @author Erin
 */
public abstract class XMLReader<T> {
	/**
	 * 
	 * @param the main XMLTag object
	 * @return each of the XML children as the specified T class
	 */
    public abstract T parseObject(XMLTag mainTag);
    
    private ArrayList<T> list;
    private String urlPath;
    private String elementTag;
    
    /**
     * 
     * This updates the urlPath for reading the XML data,
     * so we can specify more URLs, depending on the offset
     * to read from, for example.
     * 
     * @param urlPath - the url from which the XML data will be read
     * 
     * */
    public void updateURLPath(String urlPath){
        this.urlPath=urlPath;
        
       initSaxReader();
    }
    /**
     * Updates the urlPath, and then reads the new specified URL.
     * 
     * @param urlPath
     * */
    public void updateToActualList(String urlPath) throws IOException{
        updateURLPath(urlPath);
        read();
    }
    /**
     * @param urlPath, the url from which to read the XML data
     * @param elementTag, the name of the tag to be taken as item
     * for building up the item list
     * 
     * */
    public XMLReader(String urlPath, String elementTag){
       
       this.urlPath=urlPath;
       this.elementTag=elementTag;
       this.list=new ArrayList<T>();
       
       initSaxReader();
    }
  
    
   /**
    * @return the actual list of already read elements.
    * */
    public ArrayList<T> getElementList(){
        return list;
    }
    /**
     * Erases the actual items in the list, and reads the
     * data again. 
     * 
     * @return the actual list of already read elements.
     * */
    
    public ArrayList<T> readElementList() throws IOException{
        list.clear();
        read();
        return list;
    }
	/**
	 * Starts the reading the XML from the specified URL 
	 * using the existing SAXParser object.
	 * 
	 * @throws IOException if the url can't be read
	 */
    private void read() throws IOException{
        
    
        try {
        
            String path;
            InputStream inputStream;
                
                path=urlPath;
               
            
            inputStream = new URL(path).openStream();
            if(inputStream==null){
                System.err.println("URL not found");
            }
            
            Reader reader=new  InputStreamReader(inputStream,"UTF-8");
            InputSource inputSource=new InputSource(reader);
            inputSource.setEncoding("UTF-8");
            parser.parse(inputSource,handler);
            
        } catch (SAXException ex) {
        	ex.printStackTrace();
        } 
        
        catch(NullPointerException ex){
           ex.printStackTrace();
        }
    }
    private SAXParser parser;
    private DefaultHandler handler;
    /**
     * Sets up the SAXParser. It's called from the constructor of 
     * this class.
     * 
     * */
    private void initSaxReader(){
        try {
            
            SAXParserFactory factory=SAXParserFactory.newInstance();
            parser=factory.newSAXParser();
            
            handler=new DefaultHandler(){
                boolean newElement=false;
                private XMLAttributeList lastAttributes;
                
                private XMLTag lastTag;
                private XMLTag topTag;
                private boolean saveValue;

                 private Stack<XMLTag> parentStack;
            
                @Override
                 public void startElement(String uri, String localName, String tagName, Attributes attributes)throws SAXException{
                    if(tagName.equalsIgnoreCase(elementTag)){

                        parentStack=new Stack<XMLTag>();
                        newElement=true;
                        
                       
                    }
                    if(newElement){
                        
                         lastTag=new XMLTag(tagName);
                        lastAttributes=new XMLAttributeList(attributes);
                        lastTag.setAttributeList(lastAttributes);
                        if(tagName.equalsIgnoreCase(elementTag)){
                            topTag=lastTag;
                        }
                        saveValue=true;
                       // lastTag.setTopTag(topTag);
                        try{
                        lastTag.setParentTag(parentStack.peek());
                            parentStack.peek().addChild(lastTag);
                        }
                        catch(EmptyStackException ex){
                            lastTag.setParentTag(null);
                        }
                        catch(NullPointerException ex){
                            lastTag.setParentTag(null);
                        }
                        
                        if(lastTag!=null){
//                        tags.add(lastTag);                        
                        
                        parentStack.push(lastTag);
                        }
                   
                    }
                   
                    
                    
                    
                }
                @Override
                public void endElement(String uri, String localName, String tagName) throws SAXException{
                    
                    
                    if(tagName.equalsIgnoreCase(elementTag)){
                        T object=parseObject(topTag);
                        list.add(object);
                        lastAttributes=null;
                        parentStack=null;
                        newElement=false;
                        
                    }
                    else{
                        XMLTag beforeTag;
                         try{
                             
                        beforeTag=parentStack.peek();
                            
                        }
                        catch(EmptyStackException ex){
                            beforeTag=null;
                        }
                         catch(NullPointerException ex){
                             beforeTag=null;
                         }
                        if(beforeTag!=null && tagName.equalsIgnoreCase(beforeTag.getName())){
                            parentStack.pop();
                        }
                    }
                   
                }
                
                @Override
                public void characters(char[] ch, int start, int length) throws SAXException{
                   
                    if(saveValue){
                    lastTag.setValue(new String(ch,start,length).trim());
                    
                       saveValue=false;
                    }
                    
                }
            
            
            };
            
           
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(XMLReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(XMLReader.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    
    
    
    
    
    public interface SingleTagReadCallback{
       void onSingleRead(XMLTag tag);
    }
    /**
     * Reads a single tag with the specified tagName. It
     * will read the first element matching the tagName.
     * 
     * @param urlPath, the url from which the data will be read
     * @param tagName, the name that the tag should match with
     * @param a SingleTagReadCallback interface for firing 
     * its onSingleRead(XMLTag) when the data is read.
     * 
     * */
    public static void readSingleTag(final String urlPath,final String tagName,final SingleTagReadCallback callback) throws IOException{
         
    
        try {
        
            String path;
            InputStream inputStream;
                
                path=urlPath;
               
            
            inputStream = new URL(path).openStream();
            if(inputStream==null){
                System.err.println("URL not found");
            }
            
            Reader reader=new  InputStreamReader(inputStream,"UTF-8");
            InputSource inputSource=new InputSource(reader);
            inputSource.setEncoding("UTF-8");
             
            
             try {
                 
                 SAXParserFactory factory=SAXParserFactory.newInstance();
           
                SAXParser singleParser=factory.newSAXParser();
                
                DefaultHandler singleHandler=new DefaultHandler(){
                    private XMLTag singleTag;
                    private boolean saveValue;
                     @Override
                     public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                         if(qName.equalsIgnoreCase(tagName)){
                             singleTag=new XMLTag(tagName);
                             singleTag.setAttributeList(new XMLAttributeList(attributes));
                             saveValue=true;
                         }
                     }

                     @Override
                     public void characters(char[] ch, int start, int length) throws SAXException {
                         if(saveValue){
                             singleTag.setValue(new String(ch,start,length).trim());
                             saveValue=false;
                         }
                   
                     }

                     @Override
                     public void endElement(String uri, String localName, String qName) throws SAXException {
                         if(qName.equalsIgnoreCase(tagName)){
                             callback.onSingleRead(singleTag);
                         }
                     }
                     
                };
                singleParser.parse(inputSource, singleHandler);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(XMLReader.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        } catch (SAXException ex) {
            Logger.getLogger(XMLReader.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        catch(NullPointerException ex){
           ex.printStackTrace();
        }
        
    }
    
}

