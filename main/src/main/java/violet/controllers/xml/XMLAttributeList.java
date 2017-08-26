package violet.controllers.xml;
import java.util.ArrayList;
import org.xml.sax.Attributes;

/**
 * An ArrayList<XMLAttribute> extended class for easing the process
 * of getting a value from a specific attribute.
 * 
 * @author Erin
 */
public class XMLAttributeList extends ArrayList<XMLAttribute> {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public XMLAttributeList(Attributes attrs){
        super();
        for(int i=0;i<attrs.getLength();i++){
            add(new XMLAttribute(attrs.getQName(i),attrs.getValue(i)));
        }
    }
    /**
     * Returns the value set in the attribute with the 
     * specified key/name.
     * 
     * @return value of attribute with set key/name
     * */
    public String getValue(String key){
        for (XMLAttribute attr : this) {
            if(attr.getKey().equalsIgnoreCase(key)){
                return attr.getValue();
            }
        }
        return null;
    }
}
