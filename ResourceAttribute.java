import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.smartiks.voldemort.core.form.annotations.FormField;

@Entity
@Table(name = "resourceAttribute", catalog = "", schema = "resources")
public class ResourceAttribute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="raSequence")
    @SequenceGenerator(name="raSequence", sequenceName="raSequence", allocationSize=0)
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    private Integer id;
	
    @Basic(optional = false)
    @Column(name = "value", nullable = true)
    @FormField(name = "value", description = "Resource attribute value", isEditable = true)
    private String value;
    
    @Basic(optional = false)
    @OneToOne
    @JoinColumn(name = "metadata", referencedColumnName = "id", nullable = false)
    @FormField(name = "metadata", description = "Resource attribute metadata", isEditable = false)
    private ResourceTypeAttribute metadata;
    
    @Basic(optional = false)
    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "owner", referencedColumnName = "id", nullable = false)
    @FormField(name = "owner", description = "Resource that owns this Resource attribute", isEditable = false)
    private Resource owner;
    
    protected ResourceAttribute(){}
    
    protected ResourceAttribute(String value, ResourceTypeAttribute metadata, Resource owner){
    	this.value = value;
    	this.metadata = metadata;
    	this.owner = owner;
    }

    public String getValue(){
    	return this.value;
    }
    
    public void setValue(String value){
    	this.value = value;
    }
    
    public ResourceTypeAttribute getMetadata(){
    	return this.metadata;
    }
    
    public Resource getOwner(){
    	return this.owner;
    }
    
    public String toString(){
    	return "Value: " + this.getValue() + " - "
             + "Metadata: " + this.getMetadata() + " - "
             + "Owner: " + this.getOwner();
    }
}