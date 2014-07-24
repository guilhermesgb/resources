import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.smartiks.voldemort.core.form.annotations.FormField;
import com.smartiks.voldemort.core.persistence.dao.DAO;
import com.smartiks.voldemort.core.persistence.dao.Identifiable;


@SuppressWarnings("serial")
@Entity
@Table(name = "resource", catalog = "", schema = "resources")
@NamedQueries({ @NamedQuery(name = "findResourceAttributes", query = "SELECT ra FROM ResourceAttribute ra WHERE ra.owner = :resource") })
public class Resource implements Identifiable, Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="rSequence")
    @SequenceGenerator(name="rSequence", sequenceName="rSequence", allocationSize=0)
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    private Integer id;
	
    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "type", referencedColumnName = "id", nullable = false)
    @FormField(name = "Type", description = "Type of this Resource", isEditable = false)
    private ResourceType type;
    
    protected Resource(){}

    protected Resource(ResourceType type){
    	this.type = type;
    }
    
	@Override
	public Integer getId() {
		return this.id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public ResourceType getType(){
		return this.type;
	}
	
	public Set<ResourceAttribute> getAttributes(EntityManager entityManager){
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("resource", this);
		Set<Object> attributes = new HashSet<Object>((new DAO(entityManager)).
				findByNamedQuery("findResourceAttributes", parameters));
		
		Set<ResourceAttribute> _attributes = new HashSet<ResourceAttribute>();
		for ( Object attribute : attributes ){
			_attributes.add((ResourceAttribute) attribute);
		}
		return _attributes;
	}
	
    public String toString(){
    	return "Type: [" + this.getType() + "]";
    }
}