package violet.controllers.xml;



import java.util.ArrayList;
import org.xml.sax.Attributes;

/**
 * Attributes from a single tag.
 * 
 * @author Erin
 */
public class XMLAttribute {
	/**
	 * Parses an ArrayList<XMLAttribute> from an org.xml.sax.Attributes
	 * object. 
	 * 
	 * @param attributes, the org.xml.sax.Attributes object to parse.
	 * @return the list of XMLAttribute objects
	 * */
    public static ArrayList<XMLAttribute> parseAttributes(Attributes attributes){
        ArrayList<XMLAttribute> list=new ArrayList<XMLAttribute>();
        
        for(int i=0;i<attributes.getLength();i++){
            list.add(new XMLAttribute(attributes.getQName(i),attributes.getValue(i)));
        }
        return list;
    }
    
    private String key;
    private String value;
    public XMLAttribute(String key, String value){
        this.key=key;
        this.value=value;
    }
    
    /**
     * Returns the value set to this XMLAttribute object
     * 
     * @return value
     * */
    public String getValue(){
       return this.value;
       
    }
    /**
     * Returns the key (the attr name) of this XMLAttribute object
     * 
     * @return key
     * */
    public String getKey(){
        return this.key;
    }
}
