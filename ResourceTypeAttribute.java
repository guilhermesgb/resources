import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@SuppressWarnings("serial")
@Entity
@Table(name = "resourceTypeAttribute", catalog = "", schema = "resources", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class ResourceTypeAttribute implements Serializable{
	
	@Id
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    private String name;

    @Basic(optional = false)
    @Column(name = "type", nullable = false)
    private String type;
    
    @Basic(optional = false)
    @Column(name = "mandatory")
    private Boolean mandatory;
    
    protected ResourceTypeAttribute() {}
    
    protected ResourceTypeAttribute(String name, String type, Boolean mandatory){
    	this.name = name;
    	this.type = type;
    	this.mandatory = mandatory;
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
    
    public String toString(){
    	return "Name: " + this.getName() + " - "
             + "Type: " + this.getType() + " - "
             + "Mandatory: " + this.isMandatory();
    }
}