import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.smartiks.voldemort.core.form.annotations.FormField;
import com.smartiks.voldemort.core.persistence.dao.Identifiable;

@SuppressWarnings("serial")
@Entity
@Table(name = "resourceType", catalog = "", schema = "resources", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class ResourceType implements Identifiable, Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="resourceTypeSequence")
    @SequenceGenerator(name="resourceTypeSequence", sequenceName="resourceTypeSequence", allocationSize=0)
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    private Integer id;
    
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    @FormField(name = "Nome", description = "Nome desse Tipo de Recurso", isEditable = true)
    private String name;
    
    @Basic(optional = false)
    @Column(name = "description", nullable = false)
    @FormField(name = "Descrição", description = "Descrição desse Tipo de Recurso", isEditable = true)
    private String description;

    protected ResourceType(){}
    
    protected ResourceType(String name, String description){
        this.name = name;
        this.description = description;
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
    
    public void setName(String name){
    	this.name = name;
    }
    
    public String getDescription(){
    	return this.description;
    }

    public void setDescription(String description){
    	this.description = description;
    }
    
    public String toString(){
    	return "Nome: " + this.getName() + " - "
             + "Descr.: " + this.getDescription();
    }
}