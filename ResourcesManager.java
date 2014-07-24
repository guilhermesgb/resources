import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import json.JsonArray;
import json.JsonObject;
import json.JsonValue;

import com.smartiks.voldemort.core.persistence.DefaultEntityManagerProvider;
import com.smartiks.voldemort.core.persistence.dao.DAO;
import com.smartiks.voldemort.core.validator.ValidationException;
import com.smartiks.voldemort.core.validator.ValidationManager;

public class ResourcesManager {

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
        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
            " was created successfully; details: " + resourceType.toString());
        return resourceType.getId();
    }
    
    public static void remove(Integer id)
            throws ResourceTypeException{
    	ResourceType resourceType = find(id);
    	(new DAO(entityManager)).remove(resourceType);
        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
                " was removed successfully; details: " + resourceType.toString());
    }

    public static void update(Integer id, String name, String description, String attributes)
            throws ResourceTypeException, ValidationException{
    	ResourceType resourceType = find(id);

        if ( name == null || name.trim().isEmpty() ){
            throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_NAME);
        }
        resourceType.setName(name);

        if ( description == null || description.trim().isEmpty() ){
            throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_DESCRIPTION);
        }
    	resourceType.setDescription(description);
        ValidationManager.getInstance().validate(resourceType);
    	
    	JsonArray _attributes = JsonArray.readFrom(attributes);
    	Set<ResourceTypeAttribute> __attributes = new HashSet<ResourceTypeAttribute>();
    	
    	Map<String, Boolean> usedAttributeNames = new HashMap<String, Boolean>();
    	
    	for ( JsonValue attribute : _attributes ){
    		JsonObject _attribute = (JsonObject) attribute;

    		String attributeName = _attribute.get("name").asString();
    		if ( attributeName == null || attributeName.trim().isEmpty() ){
    			throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_ATTRIBUTE_NAME);
    		}
    		else if ( usedAttributeNames.containsKey(attributeName) ){
    			throw new ResourceTypeException(ResourceTypeExceptionKind.ATTRIBUTE_NAME_MUST_BE_UNIQUE);
    		}
    		usedAttributeNames.put(attributeName, true);
    		
    		String attributeType = _attribute.get("type").asString();
    		if ( attributeType == null || attributeType.trim().isEmpty() ){
    			throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_ATTRIBUTE_TYPE);
    		}

    		String attributeIsMandatory = _attribute.get("mandatory").asString();
    		if ( attributeIsMandatory == null || attributeIsMandatory.trim().isEmpty()
    				|| !(attributeIsMandatory.equals("true") || attributeIsMandatory.equals("false")) ){
    			throw new ResourceTypeException(ResourceTypeExceptionKind.INVALID_ATTRIBUTE_IS_MANDATORY);
    		}
    		ResourceTypeAttribute __attribute = new ResourceTypeAttribute(attributeName, attributeType, new Boolean(attributeIsMandatory));
            ValidationManager.getInstance().validate(__attribute);
    		__attributes.add(__attribute);
    	}
    	
    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
    		(new DAO(entityManager)).remove(attribute);
    	}
    	for ( ResourceTypeAttribute attribute : __attributes ){
    		(new DAO(entityManager)).insert(attribute);
    	}
    	
    	resourceType.setAttributes(__attributes);
    	(new DAO(entityManager)).update(resourceType);
        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
                " was updated successfully; details: " + resourceType.toString());
    }

    private static ResourceType find(Integer id) throws ResourceTypeException {
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
		return resourceType;
	}
    
    public static void main(String[] args) throws Exception{
    	int id1 = ResourcesManager.create("TR1", "desc1");
    	int id2 = ResourcesManager.create("TR2", "desc2");
    	ResourcesManager.update(id1, "TR1 (updated)", "description1", "[{\"name\":\"att1\",\"type\":\"NUMBER\",\"mandatory\":\"true\"}]");
    	ResourcesManager.update(id2, "TR2 (updated)", "description2", "[{\"name\":\"anotherAtt\",\"type\":\"DATE\",\"mandatory\":\"false\"}]");
    }
}