import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

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
    	
    	EntityTransaction transaction = entityManager.getTransaction();
    	transaction.begin();
    	DAO dataAccess = new DAO(entityManager);
    	try {
	    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
	    		dataAccess.remove(attribute);
	            logger.info("Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
	                    " was removed successfully; details: " + attribute.toString());
	    	}
	    	for ( ResourceTypeAttribute attribute : __attributes ){
	    		dataAccess.insert(attribute);
	            logger.info("Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
	                    " was created successfully; details: " + attribute.toString());
	    	}
	    	
	    	resourceType.setAttributes(__attributes);
	    	dataAccess.update(resourceType);
	        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
	                " was updated successfully; details: " + resourceType.toString());

	        transaction.commit();
    	} catch (RuntimeException e){
    		if ( transaction.isActive() ){
    			transaction.setRollbackOnly();
    			throw new ResourcesException(ResourcesExceptionType.RESOURCE_TYPE_UPDATE_FAILED);
    		}
    	}
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

    		JsonValue value = _values.get(attribute.getName());
    		String _value = null;
    		if ( value != null ){
    			_value = value.asString().trim();
    		}
    		

    		if ( ( _value == null || _value.isEmpty() ) && attribute.isMandatory() ){
    			throw new ResourcesException(ResourcesExceptionType.MANDATORY_ATTRIBUTE_OMMITED,
    					attribute.getName(), attribute.getType());
    		}

    		ResourceAttribute _attribute;
    		if ( _value == null ){
    			_attribute = new ResourceAttribute("", attribute, resource);
    		}
    		else if ( attribute.getType().equals("DATE") ){
				try {
					DateFormat.getDateInstance().parse(_value);
					_attribute = new ResourceAttribute(_value, attribute, resource);
				} catch (ParseException e) {
	    			throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
	    					attribute.getName(), attribute.getType(), _value);
				}
    		}
    		else if ( attribute.getType().equals("NUMBER") ){
    			try {
    				Double.valueOf(_value);
    				_attribute = new ResourceAttribute(_value, attribute, resource);
    			} catch (NumberFormatException e) {
	    			throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
	    					attribute.getName(), attribute.getType(), _value);
    			}
    		}
    		else{
    			_attribute = new ResourceAttribute(_value, attribute, resource);
    		}
    		ValidationManager.getInstance().validate(_attribute);
    		_attributes.add(_attribute);
    	}

    	EntityTransaction transaction = entityManager.getTransaction();
    	transaction.begin();
    	DAO dataAccess = new DAO(entityManager);
    	try {
	    	dataAccess.insert(resource);
	    	logger.info("Object of class " + resource.getClass() + " (ID " + resource.getId() + ")" +
	    			" was created successfully; details: " + resource.toString());
	    	
	    	for ( ResourceAttribute attribute : _attributes ){
	    		dataAccess.insert(attribute);
	        	logger.info("Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
	        			" was created successfully; details: " + attribute.toString());
	    	}
	    	transaction.commit();
    	}
	    catch (RuntimeException e){
	    	if ( transaction.isActive() ){
	    		transaction.setRollbackOnly();
    			throw new ResourcesException(ResourcesExceptionType.RESOURCE_CREATION_FAILED);
	    	}
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

    private static Resource findResource(Integer id) throws ResourcesException {
		Resource resource;
    	try{
    		resource= (new DAO(entityManager)).find(Resource.class, id);
    		if ( resource== null ){
        		throw new IllegalArgumentException();
    		}
    	}
    	catch ( IllegalArgumentException e ){
    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_NOT_FOUND);
    	}
		return resource;
	}
    
    public static void main(String[] args) throws Exception{
    	int id1 = ResourcesManager.createResourceType("TR1", "desc1");
    	logger.info("Operation done");
    	int id2 = ResourcesManager.createResourceType("TR2", "desc2");
    	logger.info("Operation done");
    	ResourcesManager.updateResourceType(id1, "TR1 (updated)", "description1", "[{\"name\":\"att1\",\"type\":\"NUMBER\",\"mandatory\":\"false\"}]");
    	logger.info("Operation done");
    	ResourcesManager.updateResourceType(id2, "TR2 (updated)", "description2", "[{\"name\":\"anotherAtt\",\"type\":\"DATE\",\"mandatory\":\"false\"}]");
    	logger.info("Operation done");
    	int id3 = ResourcesManager.createResource(id1, "{\"att2\":\"10\"}");
    	Resource r1 = findResource(id3);
    	logger.info("Resource " + r1.getId() + " has attrs: " + r1.getAttributes(entityManager).toString());
    	logger.info("Operation done");
    	int id4 = ResourcesManager.createResource(id1, "{\"att1\":\"10\"}");
    	Resource r2 = findResource(id4);
    	logger.info("Resource " + r2.getId() + " has attrs: " + r2.getAttributes(entityManager).toString());
    	logger.info("Operation done");
    }
}