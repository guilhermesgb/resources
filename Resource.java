import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.smartiks.voldemort.core.form.annotations.FormField;
import com.smartiks.voldemort.core.persistence.dao.Identifiable;


@SuppressWarnings("serial")
@Entity
@Table(name = "resource", catalog = "", schema = "resources")
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
	
    public String toString(){
    	return "Type: " + this.getType();
    }
}