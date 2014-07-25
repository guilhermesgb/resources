import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.smartiks.voldemort.core.form.annotations.FormField;
import com.smartiks.voldemort.core.persistence.dao.Identifiable;

@SuppressWarnings("serial")
@Entity
@Table(name = "resourceType", catalog = "", schema = "resources")
@NamedQueries({ @NamedQuery(name = "resourcesUsingResourceType", query = "SELECT r "
		+ "FROM Resource r "
		+ "WHERE r.type = :resourceType") })
public class ResourceType implements Identifiable, Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="rtSequence")
    @SequenceGenerator(name="rtSequence", sequenceName="rtSequence", allocationSize=0)
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    private Integer id;
    
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    @FormField(name = "Name", description = "Name of the Resource type", isEditable = true)
    private String name;
    
    @Basic(optional = false)
    @Column(name = "description", nullable = false)
    @FormField(name = "Description", description = "Description of the Resource type", isEditable = true)
    private String description;
    
    @Basic(optional = false)
    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "attributes", referencedColumnName = "id", nullable = true)
    @FormField(name = "Attributes", description = "Attributes of the Resource type", isEditable = true)
    private Set<ResourceTypeAttribute> attributes;

    protected ResourceType(){}
    
    protected ResourceType(String name, String description)
    		throws ResourcesException{

    	if ( name == null || name.trim().isEmpty() ){
            new ResourcesException(ResourcesExceptionType.INVALID_RESOURCE_TYPE_NAME);
        }
        if ( description == null ){
            new ResourcesException(ResourcesExceptionType.INVALID_RESOURCE_TYPE_DESCRIPTION);
        }
        ResourcesException.throwExceptionsFound();
    	this.name = name;
        this.description = description;
        this.attributes = new HashSet<ResourceTypeAttribute>();
    }
    
    @Override
    public Integer getId(){
        return this.id;
    }
    
    @Override
    public void setId(Integer id){
    	this.id = id;
    }

    public String getName(){
    	return this.name;
    }
    
    public void setName(String name)
    		throws ResourcesException{

    	if ( name == null || name.trim().isEmpty() ){
            throw new ResourcesException(ResourcesExceptionType.INVALID_RESOURCE_TYPE_NAME);
        }
    	this.name = name;
    }
    
    public String getDescription(){
    	return this.description;
    }

    public void setDescription(String description)
    		throws ResourcesException{

        if ( description == null ){
            throw new ResourcesException(ResourcesExceptionType.INVALID_RESOURCE_TYPE_DESCRIPTION);
        }
    	this.description = description;
    }
    
    public Set<ResourceTypeAttribute> getAttributes(){
    	return this.attributes;
    }
    
    public void setAttributes(Set<ResourceTypeAttribute> attributes){
    	this.attributes = attributes;
    }
    
    public String toString(){
    	return "Name: " + this.getName() + " - "
             + "Description: " + this.getDescription() + " - "
             + "Attributes: " + this.getAttributes().toString();
    }
}