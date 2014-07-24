import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import json.JsonArray;
import json.JsonObject;
import json.JsonValue;

import com.smartiks.voldemort.core.persistence.DefaultEntityManagerProvider;
import com.smartiks.voldemort.core.persistence.dao.DAO;
import com.smartiks.voldemort.core.util.time.DateFormatter;
import com.smartiks.voldemort.core.validator.ValidationException;
import com.smartiks.voldemort.core.validator.ValidationManager;


public class ResourcesManager {

	private static Logger logger = Logger.getLogger(ResourceType.class.getName());
	private static EntityManager entityManager = new DefaultEntityManagerProvider("resources")
	    .createEntityManager();
	private static DateFormatter dateFormatter = DateFormatter.getInstance();
	
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
    	DAO dataAccess = new DAO(entityManager);

    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
    		if ( resourceTypeIsUsed(dataAccess, resourceType) ){
    			throw new ResourcesException(ResourcesExceptionType.RESOURCE_TYPE_IN_USE, resourceType.getName());
    		}
            logger.info("Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
                    " is about to be removed; details: " + attribute.toString());
    	}

    	dataAccess.remove(resourceType);
        logger.info("Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
                " was removed successfully; details: " + resourceType.toString() +
                " - alongside with its ResourceTypeAttributes (whose details are logged above)");
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
    	
    	HashMap<String, ResourceTypeAttribute> oldAttributes = new HashMap<String, ResourceTypeAttribute>();
    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
    		oldAttributes.put(attribute.getName(), attribute);
    	}
    	
    	HashMap<String, ResourceTypeAttribute> newAttributes = new HashMap<String, ResourceTypeAttribute>();
    	for ( ResourceTypeAttribute attribute : __attributes ){
    		newAttributes.put(attribute.getName(), attribute);
    	}
    	
    	EntityTransaction transaction = entityManager.getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
    	logger.info("New transaction [" + transactionUUID + "] began!");
    	DAO dataAccess = new DAO(entityManager);
    	
    	try {
    		Set<ResourceTypeAttribute> updatedAttributes = new HashSet<ResourceTypeAttribute>();
    		
    		for ( String attribute : oldAttributes.keySet() ){
    			if ( newAttributes.containsKey(attribute) ){
    				if ( oldAttributes.get(attribute).equals(newAttributes.get(attribute)) ){
    					/* Attribute was left unchanged, so old instance of it should remain */ 

    					updatedAttributes.add(oldAttributes.get(attribute));
    				}
    				else {
    					/* Attribute was updated, so new instance of it will be used */
    					/* Which means the old one should be removed from the database and the new one, inserted */

    					ResourceTypeAttribute newAttribute = newAttributes.get(attribute);
    					updatedAttributes.add(newAttribute);

    					ResourceTypeAttribute oldAttribute = oldAttributes.get(attribute);

        				int amount = howManyResourcesUseThisAttribute(dataAccess, oldAttribute);
        				if ( amount > 0 ){
        					transaction.rollback();
        	    	    	logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
        	    			throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_UPDATED,
        	    					attribute, oldAttribute.getType(), amount);
        				}
    					
    					dataAccess.remove(oldAttribute);
        	            logger.info("Transaction [" + transactionUUID + "]: Object of class " + oldAttribute.getClass() + " (ID " + oldAttribute.getId() + ")" +
        	                    " will be removed; details: " + oldAttribute.toString());

        	            dataAccess.insert(newAttribute);
        	            logger.info("Transaction [" + transactionUUID + "]: Object of class " + newAttribute.getClass() + " (ID " + newAttribute.getId() + ")" +
        	                    " will be created; details: " + newAttribute.toString());
    				}
    				newAttributes.remove(attribute);
    			}
    			else {
    				/* Attribute was removed, so do remove it from the database */

    				ResourceTypeAttribute _attribute = oldAttributes.get(attribute);

    				int amount = howManyResourcesUseThisAttribute(dataAccess, _attribute);
    				if ( amount > 0 ){
    					transaction.rollback();
    	    	    	logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
    					throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_REMOVED,
    	    					attribute, _attribute.getType(), amount);
    				}

    				dataAccess.remove(_attribute);
    	            logger.info("Transaction [" + transactionUUID + "]: Object of class " + _attribute.getClass() + " (ID " + _attribute.getId() + ")" +
    	                    " will be removed; details: " + _attribute.toString());
    			}
    		}
    		for ( String attribute : newAttributes.keySet() ){
    			/* Attribute was added, insert it to the database */

    			ResourceTypeAttribute newAttribute = newAttributes.get(attribute);
				updatedAttributes.add(newAttribute);

				dataAccess.insert(newAttribute);
	    		logger.info("Transaction [" + transactionUUID + "]: Object of class " + newAttribute.getClass() + " (ID " + newAttribute.getId() + ")" +
	                    " will be created; details: " + newAttribute.toString());
    		}

	    	resourceType.setAttributes(updatedAttributes);
	    	dataAccess.update(resourceType);
	        logger.info("Transaction [" + transactionUUID + "]: Object of class " + resourceType.getClass() + " (ID " + resourceType.getId() + ")" +
	                " will be updated; details: " + resourceType.toString());

	        transaction.commit();
	    	logger.info("Transaction [" + transactionUUID + "] commited!");
    	} catch (RuntimeException e){
    		if ( transaction.isActive() ){
    			transaction.rollback();
    	    	logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
    			throw new ResourcesException(ResourcesExceptionType.RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    		}
    	}
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
    		if ( _value == null || _value.isEmpty() ){
    			_attribute = new ResourceAttribute("", attribute, resource);
    		}
    		else if ( attribute.getType().equals("DATE") ){
    			if ( !dateFormatter.isCorrectDateFormat(_value) ){
	    			throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
	    					attribute.getName(), attribute.getType(), _value);
				}
				_attribute = new ResourceAttribute(_value, attribute, resource);
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
    		_values.remove(attribute.getName());
    	}
    	
    	for ( String unknownName : _values.names() ){
    		String unknownValue = _values.get(unknownName).asString();
			throw new ResourcesException(ResourcesExceptionType.UNKNOWN_ATTRIBUTE,
					resource.getType().getName(), unknownName, unknownValue);
    	}

    	EntityTransaction transaction = entityManager.getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
    	logger.info("New transaction [" + transactionUUID + "] began!");
    	DAO dataAccess = new DAO(entityManager);
    	try {
	    	dataAccess.insert(resource);
	    	logger.info("Transaction [" + transactionUUID + "]: Object of class " + resource.getClass() + " (ID " + resource.getId() + ")" +
	    			" will be created; details: " + resource.toString());
	    	
	    	for ( ResourceAttribute attribute : _attributes ){
	    		dataAccess.insert(attribute);
	        	logger.info("Transaction [" + transactionUUID + "]: Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
	        			" will be created; details: " + attribute.toString());
	    	}
	    	transaction.commit();
	    	logger.info("Transaction [" + transactionUUID + "] commited!");
    	}
	    catch (RuntimeException e){
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
    			throw new ResourcesException(ResourcesExceptionType.RESOURCE_CREATION_FAILED_UNEXPECTEDLY, e.getMessage());
	    	}
	    }
    	return resource.getId();
    }

	public static void removeResource(int id) throws ResourcesException {

		Resource resource = findResource(id);
		
    	EntityTransaction transaction = entityManager.getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
		logger.info("New transaction [" + transactionUUID + "] began!");
    	DAO dataAccess = new DAO(entityManager);		
		try {
			for ( ResourceAttribute attribute : resource.getAttributes(entityManager) ){
				dataAccess.remove(attribute);
	        	logger.info("Transaction [" + transactionUUID + "]: Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
	        			" will be removed; details: " + attribute.toString());
			}
			dataAccess.remove(resource);
	    	logger.info("Transaction [" + transactionUUID + "]: Object of class " + resource.getClass() + " (ID " + resource.getId() + ")" +
	    			" will be removed; details: " + resource.toString());
			transaction.commit();
    		logger.info("Transaction [" + transactionUUID + "] commited!");
		}
		catch (RuntimeException e){
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
	    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_DELETION_FAILED_UNEXPECTEDLY, e.getMessage());
	    	}
		}
	}
	
	public static void updateResource(int id, String values)
			throws ResourcesException, ValidationException {
		
		Resource resource = findResource(id);
		Set<ResourceAttribute> attributes = resource.getAttributes(entityManager);
		JsonObject _values = JsonObject.readFrom(values);
    	
    	EntityTransaction transaction = entityManager.getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
    	logger.info("New transaction [" + transactionUUID + "] began!");
    	DAO dataAccess = new DAO(entityManager);
    	
    	try {
    		for ( ResourceAttribute attribute : attributes ){

    			JsonValue value = _values.get(attribute.getMetadata().getName());
        		String _value = null;
        		if ( value != null ){
        			_value = value.asString().trim();
        		}
        		else{
        			/* This attribute is not meant to be updated, skip it */
        			continue;
        		}

        		if ( ( _value == null || _value.isEmpty() ) && attribute.getMetadata().isMandatory() ){
    	    		transaction.rollback();
    	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
        			throw new ResourcesException(ResourcesExceptionType.MANDATORY_ATTRIBUTE_OMMITED,
        					attribute.getMetadata().getName(), attribute.getMetadata().getType());
        		}

        		if ( _value == null || _value.isEmpty() ){
        			attribute.setValue("");
        		}
        		else if ( attribute.getMetadata().getType().equals("DATE") ){
        			if ( !dateFormatter.isCorrectDateFormat(_value) ){
        	    		transaction.rollback();
        	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
        				throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
    	    					attribute.getMetadata().getName(), attribute.getMetadata().getType(), _value);
    				}
    				attribute.setValue(_value);
        		}
        		else if ( attribute.getMetadata().getType().equals("NUMBER") ){
        			try {
        				Double.valueOf(_value);
        				attribute.setValue(_value);
        			} catch (NumberFormatException e) {
        	    		transaction.rollback();
        	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
        				throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
    	    					attribute.getMetadata().getName(), attribute.getMetadata().getType(), _value);
        			}
        		}
        		else{
        			attribute.setValue(_value);
        		}
        		ValidationManager.getInstance().validate(attribute);
        		dataAccess.update(attribute);
            	logger.info("Transaction [" + transactionUUID + "]: Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
            			" will be updated; details: " + attribute.toString());
        		_values.remove(attribute.getMetadata().getName());
    		}
    		
    		if ( _values.size() > 0 ){
    			/* Some unknown attribute names were provided, so
    			 * check if new ResourceTypeAttributes were added to the ResourceType
    			 * but this Resource is unaware of this fact */

    			Map<String, ResourceTypeAttribute> _attributes = new HashMap<String, ResourceTypeAttribute>();
    			for ( ResourceTypeAttribute attribute : resource.getType().getAttributes() ){
    				_attributes.put(attribute.getName(), attribute);
    			}
    			
    			for ( String unknownName : _values.names() ){
    				if ( _attributes.containsKey(unknownName) ){
    					
    					/* This is a new ResourceTypeAttribute that this Resource didn't have */
    					
    					ResourceTypeAttribute attribute = _attributes.get(unknownName);

    		    		JsonValue value = _values.get(unknownName);
    		    		String _value = null;
    		    		if ( value != null ){
    		    			_value = value.asString().trim();
    		    		}

    		    		if ( ( _value == null || _value.isEmpty() ) && attribute.isMandatory() ){
    			    		transaction.rollback();
    			    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
    		    			throw new ResourcesException(ResourcesExceptionType.MANDATORY_ATTRIBUTE_OMMITED,
    		    					attribute.getName(), attribute.getType());
    		    		}

    		    		ResourceAttribute _attribute;
    		    		if ( _value == null || _value.isEmpty() ){
    		    			_attribute = new ResourceAttribute("", attribute, resource);
    		    		}
    		    		else if ( attribute.getType().equals("DATE") ){
    		    			if ( !dateFormatter.isCorrectDateFormat(_value) ){
    		    	    		transaction.rollback();
    		    	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
    		    				throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
    			    					attribute.getName(), attribute.getType(), _value);
    						}
    						_attribute = new ResourceAttribute(_value, attribute, resource);
    		    		}
    		    		else if ( attribute.getType().equals("NUMBER") ){
    		    			try {
    		    				Double.valueOf(_value);
    		    				_attribute = new ResourceAttribute(_value, attribute, resource);
    		    			} catch (NumberFormatException e) {
    		    	    		transaction.rollback();
    		    	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
    		    				throw new ResourcesException(ResourcesExceptionType.INVALID_VALUE,
    			    					attribute.getName(), attribute.getType(), _value);
    		    			}
    		    		}
    		    		else{
    		    			_attribute = new ResourceAttribute(_value, attribute, resource);
    		    		}
    		    		ValidationManager.getInstance().validate(_attribute);
    		    		attributes.add(_attribute);
    		    		dataAccess.insert(_attribute);
    		        	logger.info("Transaction [" + transactionUUID + "]: Object of class " + _attribute.getClass() + " (ID " + _attribute.getId() + ")" +
    		        			" will be created; details: " + _attribute.toString());
    				}
    				else{
	    	    		transaction.rollback();
	    	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
	    	    		String unknownValue = _values.get(unknownName).asString();
    					throw new ResourcesException(ResourcesExceptionType.UNKNOWN_ATTRIBUTE,
    							resource.getType().getName(), unknownName, unknownValue);
    				}
    			}
    		}
    		
    		transaction.commit();
    		logger.info("Transaction [" + transactionUUID + "] commited!");
    	}
    	catch (RuntimeException e){
    		if ( transaction.isActive() ){
	    		transaction.rollback();
	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
	    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    		}
    	}
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

    private static boolean resourceTypeIsUsed(DAO dataAccess, ResourceType resourceType){
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("resourceType", resourceType);
		List<Object> resources = dataAccess.findByNamedQuery("resourcesUsingResourceType", parameters);
		return resources.size() > 0;
    }
    
    private static int howManyResourcesUseThisAttribute(DAO dataAccess, ResourceTypeAttribute attribute) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("attributeMetadata", attribute);
		List<Object> resources = dataAccess.findByNamedQuery("resourcesUsingAttribute", parameters);
		return resources.size();
	}
    
    public static void main(String[] args) throws Exception{

    	int id1 = ResourcesManager.createResourceType("TR1", "desc1");
    	logger.info("Operation done");
    	int id2 = ResourcesManager.createResourceType("TR2", "desc2");
    	logger.info("Operation done");
    	ResourcesManager.updateResourceType(id1, "TR1 (updated)", "description1", "[{\"name\":\"att1\",\"type\":\"NUMBER\",\"mandatory\":\"false\"}]");
    	logger.info("Operation done");
    	ResourcesManager.updateResourceType(id2, "TR2 (updated)", "description2", "[{\"name\":\"data\",\"type\":\"DATE\",\"mandatory\":\"true\"}]");
    	logger.info("Operation done");
    	int id3 = ResourcesManager.createResource(id1, "{\"att2\":\"10\"}");
    	Resource r1 = findResource(id3);
    	logger.info("Resource " + r1.getId() + " has attrs: " + r1.getAttributes(entityManager).toString());
    	logger.info("Operation done");
    	int id4 = ResourcesManager.createResource(id2, "{\"data\":\"10/10/2014\"}");
    	Resource r2 = findResource(id4);
    	logger.info("Resource " + r2.getId() + " has attrs: " + r2.getAttributes(entityManager).toString());
    	logger.info("Operation done");
    }

}