package violet.controllers.xml;
import java.util.ArrayList;
import java.util.List;

/**
 * This class serves to structure the data similar to that
 * of JSONObjects, by allowing to set children and parent
 * elements.
 * 
 * @author Erin
 */
public class XMLTag {
    
    public XMLTag(String name){
        setName(name);
    }
    private String value, name;
    private XMLTag parentTag;
    private XMLAttributeList attrList;
    /**
     * Sets the String value saved in this tag.
     * For example: <tag>value</tag>
     * 
     * @param value to be set in the XMLTag object.
     * 
     * */
    public void setValue(String value){
        this.value=value;
    }
    /**
     * Sets the tagName.
     * For example: <tag-name></tag-name>
     * 
     * @param name
     * 
     * */
    public void setName(String name){
        this.name=name;
    }
    
    /**
     * Sets the attributes list of this tag.
     * 
     * @param XMLAttributeList list
     * */
    public void setAttributeList(XMLAttributeList list){
        this.attrList=list;
    }
    /**
     * @return value
     * */
    public String getValue(){
        return value;
        
    }
    /**
     * @return name
     * */
    public String getName(){
        return name;
        
    }
    /**
     * @return attrList
     * */
    public XMLAttributeList getAttributeList(){
        return attrList;
    }
    /**
     * Returns the parent tag from this tag.
     * It can be null for the upmost tag.
     * 
     * @return parentTag
     * */
    public XMLTag getParentTag(){
        return parentTag;
    }
    /**
     * Sets the parentTag.
     * 
     * @param tag
     * */
    public void setParentTag(XMLTag tag){
        this.parentTag=tag;
    }
    
    private List<XMLTag> childrenList;
    /**
     * Returns the childrenList. If it's null,
     * it creates it as a new ArrayList.
     * 
     * @return childrenList
     * */
    public List<XMLTag> getChildren(){
        if(childrenList==null){
            childrenList=new ArrayList<XMLTag>();
        }
        return childrenList;
    }
    /**
     * Returns the child with the specified tagName.
     * It only looks for it on its immediate children.
     * 
     * @param tagName
     * @return the matching XMLTag child
     * */
    public XMLTag getChild(String tagName){
        for(XMLTag tag: getChildren()){
            if(tag.getName().equalsIgnoreCase(tagName)){
                return tag;
            }
        }
        return null;
    }
    /**
     * 
     * */
    public boolean hasChild(String tagName) {
    	return getChild(tagName)!=null;
    }
    
    /**
     * Returns the child with the specified "id" attribute.
     * 
     * @param id
     * @return the matching XMLTag child
     * */
    public XMLTag getChildWithId(String idValue){
        return getChildWithAttribute("id",idValue);
    }
    /**
     * Returns the child with the specified value in the
     * speficied attribute.
     * 
     * @param attrField, the name of the attribute to match
     * @param attrValue, the value the attribute must have to match
     * @return the matching XMLTag child
     * 
     * */
    public XMLTag getChildWithAttribute(String attrField, String attrValue){
        for(XMLTag tag: getChildren()){
            XMLAttribute attr=tag.getAttribute(attrField);
            if(attr!=null){
                
                if(attr.getValue().equalsIgnoreCase(attrValue)){
                    return tag;
                }
            }
            
        }
        return null;
    }
    
    /**
     * Returns the attribute with the specified name.
     * 
     * @param the attribute name
     * @return the XMLAttribute matching object
     * */
    public XMLAttribute getAttribute(String field){
        if(getAttributeList()!=null){
            for(XMLAttribute attr: getAttributeList()){
                if(field.equalsIgnoreCase(attr.getKey())){
                    return attr;
                }
            }
        }
        return null;
    }
    /**
     * Adds a child to this XMLTag.
     * 
     * @param the child XMLTag to be added
     * */
    public void addChild(XMLTag tag){
        getChildren().add(tag);
    }
    
}
