import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.smartiks.voldemort.core.persistence.DefaultEntityManagerProvider;
import com.smartiks.voldemort.core.persistence.dao.DAO;
import com.smartiks.voldemort.core.validator.ValidationException;
import com.smartiks.voldemort.core.validator.ValidationManager;

public class ResourceTypeManager {

	private static Logger logger = Logger.getLogger(ResourceType.class.getName());
	private static EntityManager entityManager = new DefaultEntityManagerProvider("resources")
	    .createEntityManager();
	
    public static Integer create(String name, String description)
            throws ResourceTypeException, ValidationException{
        if ( name == null || name.trim().isEmpty() ){
            throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_NAME);
        }
        else if ( description == null || description.trim().isEmpty() ){
            throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_DESCRIPTION);
        }
        ResourceType resourceType = new ResourceType(name, description);
        ValidationManager.getInstance().validate(resourceType);
        (new DAO(entityManager)).insert(resourceType);
        logger.info("Objeto da classe " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
            " foi criado com sucesso; detalhes: " + resourceType.toString());
        return resourceType.getId();
    }
    
    public static void remove(Integer id)
            throws ResourceTypeException{
    	ResourceType resourceType;
    	try{
    		resourceType = (new DAO(entityManager)).find(ResourceType.class, id);
    		if ( resourceType == null ){
        		throw new IllegalArgumentException();
    		}
    	}
    	catch ( IllegalArgumentException e ){
    		throw new ResourceTypeException(ResourceTypeExceptionKind.RESOURCE_TYPE_NOT_FOUND);
    	}
    	(new DAO(entityManager)).remove(resourceType);
        logger.info("Objeto da classe " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
                " foi removido com sucesso; detalhes: " + resourceType.toString());
    }
    
    public static void main(String[] args) throws Exception{
//    	ResourceTypeManager.create("TR1", "desc1");
//    	ResourceTypeManager.create("TR2", "desc2");
//    	ResourceType resourceType = (new DAO(entityManager)).find(ResourceType.class, id);
    }
}