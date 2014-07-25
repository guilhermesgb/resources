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


public class ResourcesManager {

	private static Logger logger = Logger.getLogger(ResourceType.class.getName());
	private static EntityManager entityManager = new DefaultEntityManagerProvider("resources")
	    .createEntityManager();
	
    public static Integer createResourceType(String name, String description)
            throws ResourcesException{
    	
        ResourceType resourceType = new ResourceType(name, description);
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

    public static void updateResourceType(Integer id, String name, String description, String attributesRaw)
            throws ResourcesException{

    	ResourceType resourceType = findResourceType(id);
        resourceType.setName(name);
    	resourceType.setDescription(description);
    	
    	JsonArray attributesJSON = JsonArray.readFrom(attributesRaw);
    	Set<ResourceTypeAttribute> attributes = new HashSet<ResourceTypeAttribute>();
    	
    	Map<String, Boolean> usedAttributeNames = new HashMap<String, Boolean>();
    	
    	for ( JsonValue attributeJSON : attributesJSON ){

    		ResourceTypeAttribute attribute = new ResourceTypeAttribute(attributeJSON.asObject());

    		if ( usedAttributeNames.containsKey(attribute.getName()) ){
    			throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_NAME_MUST_BE_UNIQUE);
    		}
    		usedAttributeNames.put(attribute.getName(), true);
    		attributes.add(attribute);
    	}
    	
    	HashMap<String, ResourceTypeAttribute> oldAttributes = new HashMap<String, ResourceTypeAttribute>();
    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
    		oldAttributes.put(attribute.getName(), attribute);
    	}
    	
    	HashMap<String, ResourceTypeAttribute> newAttributes = new HashMap<String, ResourceTypeAttribute>();
    	for ( ResourceTypeAttribute attribute : attributes ){
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
    					ResourceTypeAttribute oldAttribute = oldAttributes.get(attribute);

        				int amount = howManyResourcesUseThisAttribute(dataAccess, oldAttribute);
        				if ( amount > 0 ){
        	    			throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_UPDATED,
        	    					attribute, oldAttribute.getType(), amount);
        				}
    					
    					dataAccess.remove(oldAttribute);
        	            logger.info("Transaction [" + transactionUUID + "]: Object of class " + oldAttribute.getClass() + " (ID " + oldAttribute.getId() + ")" +
        	                    " will be removed; details: " + oldAttribute.toString());

        	            updatedAttributes.add(newAttribute);
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
    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    	}
    	finally{
    		if ( transaction.isActive() ){
    			transaction.rollback();
    			logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
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
    
	public static Integer createResource(Integer id, String valuesRaw)
            throws ResourcesException{
		
    	ResourceType resourceType = findResourceType(id);
    	Resource resource = new Resource(resourceType);
    	
    	JsonObject valuesJSON = JsonObject.readFrom(valuesRaw);
    	Set<ResourceTypeAttribute> attributesMetadata = resourceType.getAttributes();
    	Set<ResourceAttribute> attributes = new HashSet<ResourceAttribute>();

    	for ( ResourceTypeAttribute attributeMetadata : attributesMetadata ){

    		JsonValue valueJSON = valuesJSON.get(attributeMetadata.getName());
    		String value = null;
    		if ( valueJSON != null ){
    			value = valueJSON.asString().trim();
    		}
    		
    		attributes.add(new ResourceAttribute(resource, attributeMetadata, value));
    		valuesJSON.remove(attributeMetadata.getName());
    	}
    	
    	for ( String unknownName : valuesJSON.names() ){
    		String unknownValue = valuesJSON.get(unknownName).asString();
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
	    	
	    	for ( ResourceAttribute attribute : attributes ){
	    		dataAccess.insert(attribute);
	        	logger.info("Transaction [" + transactionUUID + "]: Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
	        			" will be created; details: " + attribute.toString());
	    	}
	    	transaction.commit();
	    	logger.info("Transaction [" + transactionUUID + "] commited!");
    	}
	    catch (RuntimeException e){
	    	throw new ResourcesException(ResourcesExceptionType.RESOURCE_CREATION_FAILED_UNEXPECTEDLY, e.getMessage());
	    }
    	finally{
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
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
			throw new ResourcesException(ResourcesExceptionType.RESOURCE_DELETION_FAILED_UNEXPECTEDLY, e.getMessage());
		}
		finally{
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
	    		logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
	    	}
		}
	}
	
	public static void updateResource(int id, String valuesRaw)
			throws ResourcesException {
		
		Resource resource = findResource(id);
		Set<ResourceAttribute> attributes = resource.getAttributes(entityManager);
		JsonObject valuesJSON = JsonObject.readFrom(valuesRaw);
    	
    	EntityTransaction transaction = entityManager.getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
    	logger.info("New transaction [" + transactionUUID + "] began!");
    	DAO dataAccess = new DAO(entityManager);
    	
    	try {
    		for ( ResourceAttribute attribute : attributes ){

    			JsonValue valueJSON = valuesJSON.get(attribute.getMetadata().getName());
        		String value = null;
        		if ( valueJSON != null ){
        			value = valueJSON.asString().trim();
        		}
        		else{
        			/* This attribute is not meant to be updated, skip it */
        			continue;
        		}

        		attribute.setValue(value);
        		dataAccess.update(attribute);
            	logger.info("Transaction [" + transactionUUID + "]: Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
            			" will be updated; details: " + attribute.toString());
        		valuesJSON.remove(attribute.getMetadata().getName());
    		}
    		
    		if ( valuesJSON.size() > 0 ){
    			/* Some unknown attribute names were provided, so
    			 * check if new ResourceTypeAttributes were added to the ResourceType
    			 * but this Resource is unaware of this fact */

    			Map<String, ResourceTypeAttribute> attributesMetadata = new HashMap<String, ResourceTypeAttribute>();
    			for ( ResourceTypeAttribute attributeMetadata : resource.getType().getAttributes() ){
    				attributesMetadata.put(attributeMetadata.getName(), attributeMetadata);
    			}
    			
    			for ( String unknownName : valuesJSON.names() ){
    				if ( attributesMetadata.containsKey(unknownName) ){
    					
    					/* This is a new ResourceTypeAttribute that this Resource didn't have */
    					
    					ResourceTypeAttribute attributeMetadata = attributesMetadata.get(unknownName);

    		    		JsonValue valueJSON = valuesJSON.get(unknownName);
    		    		String value = null;
    		    		if ( valueJSON != null ){
    		    			value = valueJSON.asString().trim();
    		    		}

    		    		ResourceAttribute attribute = new ResourceAttribute(resource, attributeMetadata, value);
    		    		attributes.add(attribute);
    		    		dataAccess.insert(attribute);
    		        	logger.info("Transaction [" + transactionUUID + "]: Object of class " + attribute.getClass() + " (ID " + attribute.getId() + ")" +
    		        			" will be created; details: " + attribute.toString());
    				}
    				else{
	    	    		String unknownValue = valuesJSON.get(unknownName).asString();
    					throw new ResourcesException(ResourcesExceptionType.UNKNOWN_ATTRIBUTE,
    							resource.getType().getName(), unknownName, unknownValue);
    				}
    			}
    		}
    		
    		transaction.commit();
    		logger.info("Transaction [" + transactionUUID + "] commited!");
    	}
    	catch (RuntimeException e){
    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    	}
    	finally{
    		if ( transaction.isActive() ){
    			transaction.rollback();
    			logger.info("Transaction [" + transactionUUID + "] failed and is rolled back!");
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