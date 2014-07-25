import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import json.JsonObject;

import com.smartiks.voldemort.core.form.annotations.FormField;
import com.smartiks.voldemort.core.persistence.dao.Identifiable;

@SuppressWarnings("serial")
@Entity
@Table(name = "resourceTypeAttribute", catalog = "", schema = "resources")
@NamedQueries({ @NamedQuery(name = "resourcesUsingAttribute", query = "SELECT r "
		+ "FROM Resource r, ResourceAttribute ra "
		+ "WHERE ra.metadata = :attributeMetadata "
		+ "AND ra.owner = r") })
public class ResourceTypeAttribute implements Identifiable, Serializable{
	
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="rtaSequence")
    @SequenceGenerator(name="rtaSequence", sequenceName="rtaSequence", allocationSize=0)
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    private Integer id;
	
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    @FormField(name = "Name", description = "Name of this Resource attribute", isEditable = false)
	private String name;

    @Basic(optional = false)
    @Column(name = "type", nullable = false)
    @FormField(name = "Type", description = "Type of this Resource attribute", isEditable = true)
    private String type;
    
    @Basic(optional = false)
    @Column(name = "mandatory")
    @FormField(name = "Mandatory", description = "Is this Resource attribute mandatory?", isEditable = true)
    private Boolean mandatory;
    
    protected ResourceTypeAttribute() {}
    
    protected ResourceTypeAttribute(JsonObject data)
    		throws ResourcesException{

    	String name = data.get("name").asString();
		if ( name == null || name.trim().isEmpty() ){
			throw new ResourcesException(ResourcesExceptionType.INVALID_ATTRIBUTE_NAME);
		}
		
		String type = data.get("type").asString().trim();
		if ( type == null || type.isEmpty() || !(type.equals("TEXT") ||
				type.equals("NUMBER") || type.equals("DATE")) ){
			throw new ResourcesException(ResourcesExceptionType.INVALID_ATTRIBUTE_TYPE);
		}

		String mandatory = data.get("mandatory").asString();
		if ( mandatory == null || mandatory.trim().isEmpty()
				|| !(mandatory.equals("true") || mandatory.equals("false")) ){
			throw new ResourcesException(ResourcesExceptionType.INVALID_ATTRIBUTE_IS_MANDATORY);
		}
    	this.name = name;
    	this.type = type;
    	this.mandatory = new Boolean(mandatory);
    }

	@Override
	public Integer getId() {
		return this.id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}
    
    public String getName(){
    	return this.name;
    }
    
    public void setName(String name){
    	this.name = name;
    }
    
    public String getType(){
    	return this.type;
    }
    
    public void setType(String type){
    	this.type = type;
    }
    
    public Boolean isMandatory(){
    	return this.mandatory;
    }
    
    public void setMandatory(Boolean mandatory){
    	this.mandatory = mandatory;
    }
    
    @Override
    public boolean equals(Object other){
    	boolean result = false;
    	if ( other instanceof ResourceTypeAttribute ){
    		ResourceTypeAttribute that = (ResourceTypeAttribute) other;
    		result = this.getType().equals(that.getType())
                && this.isMandatory().equals(that.isMandatory());
    	}
		return result;
    }
    
    @Override
    public int hashCode(){
    	return ( 41 * (41 + this.getType().hashCode()) + this.isMandatory().hashCode());
    }
    
    public String toString(){
    	return "Name: " + this.getName() + " - "
             + "Type: " + this.getType() + " - "
             + "Mandatory: " + this.isMandatory();
    }
}