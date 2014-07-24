import java.text.DateFormat;
import java.text.ParseException;
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
	
    public static Integer createResourceType(String name, String description)
            throws ResourcesException, ValidationException{
        if ( name == null || name.trim().isEmpty() ){
            throw new ResourcesException(ResourcesExceptionType.INVALID_NAME);
        }
        else if ( description == null || description.trim().isEmpty() ){
            throw new ResourcesException(ResourcesExceptionType.INVALID_DESCRIPTION);
        }
        ResourceType resourceType = new ResourceType(name, description);
        ValidationManager.getInstance().validate(resourceType);
        (new DAO(entityManager)).insert(resourceType);
        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
            " was created successfully; details: " + resourceType.toString());
        return resourceType.getId();
    }
    
    public static void removeResourceType(Integer id)
            throws ResourcesException{
    	ResourceType resourceType = findResourceType(id);
    	(new DAO(entityManager)).remove(resourceType);
        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
                " was removed successfully; details: " + resourceType.toString());
    }

    public static void updateResourceType(Integer id, String name, String description, String attributes)
            throws ResourcesException, ValidationException{
    	ResourceType resourceType = findResourceType(id);

        if ( name == null || name.trim().isEmpty() ){
            throw new ResourcesException(ResourcesExceptionType.INVALID_NAME);
        }
        resourceType.setName(name);

        if ( description == null || description.trim().isEmpty() ){
            throw new ResourcesException(ResourcesExceptionType.INVALID_DESCRIPTION);
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
    			throw new ResourcesException(ResourcesExceptionType.INVALID_ATTRIBUTE_NAME);
    		}
    		else if ( usedAttributeNames.containsKey(attributeName) ){
    			throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_NAME_MUST_BE_UNIQUE);
    		}
    		usedAttributeNames.put(attributeName, true);
    		
    		String attributeType = _attribute.get("type").asString().trim();
    		if ( attributeType == null || attributeType.isEmpty() || !(attributeType.equals("TEXT") ||
    				attributeType.equals("NUMBER") || attributeType.equals("DATE")) ){
    			throw new ResourcesException(ResourcesExceptionType.INVALID_ATTRIBUTE_TYPE);
    		}

    		String attributeIsMandatory = _attribute.get("mandatory").asString();
    		if ( attributeIsMandatory == null || attributeIsMandatory.trim().isEmpty()
    				|| !(attributeIsMandatory.equals("true") || attributeIsMandatory.equals("false")) ){
    			throw new ResourcesException(ResourcesExceptionType.INVALID_ATTRIBUTE_IS_MANDATORY);
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

    public static Integer createResource(Integer id, String values)
            throws ResourcesException, ValidationException{
    	ResourceType resourceType = findResourceType(id);
    	Resource resource = new Resource(resourceType);
    	ValidationManager.getInstance().validate(resource);
    	
    	JsonObject _values = JsonObject.readFrom(values);
    	Set<ResourceTypeAttribute> attributes = resourceType.getAttributes();
    	Set<ResourceAttribute> _attributes = new HashSet<ResourceAttribute>();

    	for ( ResourceTypeAttribute attribute : attributes ){

    		String value = _values.get(attribute.getName()).asString().trim();

    		if ( ( value == null || value.isEmpty() ) && attribute.isMandatory() ){
    			throw new ResourcesException(ResourcesExceptionType.MANDATORY_ATTRIBUTE_OMMITED,
    					attribute.getName(), attribute.getType());
    		}

    		ResourceAttribute _attribute;
    		if ( value == null ){
    			_attribute = new ResourceAttribute(null, attribute, resource);
    		}
    		else if ( attribute.getType().equals("DATE") ){
				try {
					DateFormat.getDateInstance().parse(value);
					_attribute = new ResourceAttribute(value, attribute, resource);
				} catch (ParseException e) {
	    			throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
	    					attribute.getName(), attribute.getType(), value);
				}
    		}
    		else if ( attribute.getType().equals("NUMBER") ){
    			try {
    				Double.valueOf(value);
    				_attribute = new ResourceAttribute(value, attribute, resource);
    			} catch (NumberFormatException e) {
	    			throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
	    					attribute.getName(), attribute.getType(), value);
    			}
    		}
    		else{
    			_attribute = new ResourceAttribute(value, attribute, resource);
    		}
    		ValidationManager.getInstance().validate(_attribute);
    		_attributes.add(_attribute);
    	}
    	
    	(new DAO(entityManager)).insert(resource);
    	
    	for ( ResourceAttribute attribute : _attributes ){
    		(new DAO(entityManager)).insert(attribute);
    	}
    	
    	return resource.getId();
    }
    
    private static ResourceType findResourceType(Integer id) throws ResourcesException {
		ResourceType resourceType;
    	try{
    		resourceType = (new DAO(entityManager)).find(ResourceType.class, id);
    		if ( resourceType == null ){
        		throw new IllegalArgumentException();
    		}
    	}
    	catch ( IllegalArgumentException e ){
    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_TYPE_NOT_FOUND);
    	}
		return resourceType;
	}
    
    public static void main(String[] args) throws Exception{
    	int id1 = ResourcesManager.createResourceType("TR1", "desc1");
    	int id2 = ResourcesManager.createResourceType("TR2", "desc2");
    	ResourcesManager.updateResourceType(id1, "TR1 (updated)", "description1", "[{\"name\":\"att1\",\"type\":\"NUMBER\",\"mandatory\":\"true\"}]");
    	ResourcesManager.updateResourceType(id2, "TR2 (updated)", "description2", "[{\"name\":\"anotherAtt\",\"type\":\"DATE\",\"mandatory\":\"false\"}]");
    }
}