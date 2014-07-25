import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import json.JsonArray;
import json.JsonObject;
import json.JsonValue;

import com.smartiks.voldemort.core.persistence.DefaultEntityManagerProvider;
import com.smartiks.voldemort.core.persistence.dao.DAO;


public class ResourcesManager {

	private static EntityManager entityManager = new DefaultEntityManagerProvider("resources")
	    .createEntityManager();
	
    public static Integer createResourceType(String name, String description)
            throws ResourcesException{
    	
        ResourceType resourceType = new ResourceType(name, description);
        (new DAO(entityManager)).insert(resourceType);
        ResourcesLog.log(ResourcesLogType.OBJECT_INSERTED,
        		resourceType.getClass(), resourceType.getId(), resourceType.toString());
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
            ResourcesLog.log(ResourcesLogType.OBJECT_ABOUT_TO_BE_REMOVED,
            		attribute.getClass(), attribute.getId(), attribute.toString());
    	}

    	dataAccess.remove(resourceType);
        ResourcesLog.log(ResourcesLogType.OBJECT_REMOVED_RECURSIVELY,
        		resourceType.getClass(), resourceType.getId(),
        		resourceType.toString(), "ResourceTypeAttributes");
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
    	ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(entityManager);
    	
    	try {
    		Set<ResourceTypeAttribute> updatedAttributes = new HashSet<ResourceTypeAttribute>();
    		
    		for ( String attributeName : oldAttributes.keySet() ){
    			if ( newAttributes.containsKey(attributeName) ){
    				if ( oldAttributes.get(attributeName).equals(newAttributes.get(attributeName)) ){
    					/* Attribute was left unchanged, so old instance of it should remain */ 

    					updatedAttributes.add(oldAttributes.get(attributeName));
    				}
    				else {
    					/* Attribute was updated, so new instance of it will be used */
    					/* Which means the old one should be removed from the database and the new one, inserted */

    					ResourceTypeAttribute newAttributeMetadata = newAttributes.get(attributeName);
    					ResourceTypeAttribute oldAttributeMetadata = oldAttributes.get(attributeName);

        				int amount = howManyResourcesUseThisAttribute(dataAccess, oldAttributeMetadata);
        				if ( amount > 0 ){
        	    			throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_UPDATED,
        	    					attributeName, oldAttributeMetadata.getType(), amount);
        				}
    					
    					dataAccess.remove(oldAttributeMetadata);
    					ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_REMOVED,
    							transactionUUID, oldAttributeMetadata.getClass(),
    							oldAttributeMetadata.getId(), oldAttributeMetadata.toString());

        	            updatedAttributes.add(newAttributeMetadata);
        	            dataAccess.insert(newAttributeMetadata);
    					ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_INSERTED,
    							transactionUUID, newAttributeMetadata.getClass(),
    							newAttributeMetadata.getId(), newAttributeMetadata.toString());
    				}
    				newAttributes.remove(attributeName);
    			}
    			else {
    				/* Attribute was removed, so do remove it from the database */

    				ResourceTypeAttribute attributeMetadata = oldAttributes.get(attributeName);

    				int amount = howManyResourcesUseThisAttribute(dataAccess, attributeMetadata);
    				if ( amount > 0 ){
    					throw new ResourcesException(ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_REMOVED,
    	    					attributeName, attributeMetadata.getType(), amount);
    				}

    				dataAccess.remove(attributeMetadata);
					ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_REMOVED,
							transactionUUID, attributeMetadata.getClass(),
							attributeMetadata.getId(), attributeMetadata.toString());
    			}
    		}
    		for ( String attribute : newAttributes.keySet() ){
    			/* Attribute was added, insert it to the database */

    			ResourceTypeAttribute newAttributeMetadata = newAttributes.get(attribute);
				updatedAttributes.add(newAttributeMetadata);

				dataAccess.insert(newAttributeMetadata);
				ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_INSERTED,
						transactionUUID, newAttributeMetadata.getClass(),
						newAttributeMetadata.getId(), newAttributeMetadata.toString());
    		}

	    	resourceType.setAttributes(updatedAttributes);
	    	dataAccess.update(resourceType);
			ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_UPDATED,
					transactionUUID, resourceType.getClass(),
					resourceType.getId(), resourceType.toString());

	        transaction.commit();
	        ResourcesLog.log(ResourcesLogType.TRANSACTION_COMMITED, transactionUUID);
    	} catch (RuntimeException e){
    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    	}
    	finally{
    		if ( transaction.isActive() ){
    			transaction.rollback();
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
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
        ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(entityManager);
    	try {

    		dataAccess.insert(resource);
			ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_INSERTED,
					transactionUUID, resource.getClass(),
					resource.getId(), resource.toString());
	    	
	    	for ( ResourceAttribute attribute : attributes ){
	    		dataAccess.insert(attribute);
				ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_INSERTED,
						transactionUUID, attribute.getClass(),
						attribute.getId(), attribute.toString());
	    	}
	    	transaction.commit();
	        ResourcesLog.log(ResourcesLogType.TRANSACTION_COMMITED, transactionUUID);
    	}
	    catch (RuntimeException e){
	    	throw new ResourcesException(ResourcesExceptionType.RESOURCE_CREATION_FAILED_UNEXPECTEDLY, e.getMessage());
	    }
    	finally{
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
	    	}
	    }
    	return resource.getId();
    }

	public static void removeResource(int id) throws ResourcesException {

		Resource resource = findResource(id);
		
    	EntityTransaction transaction = entityManager.getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
        ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(entityManager);		
		try {
			for ( ResourceAttribute attribute : resource.getAttributes(entityManager) ){
				dataAccess.remove(attribute);
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_REMOVED,
    	        		transactionUUID, attribute.getClass(),
    	        		attribute.getId(), attribute.toString());
			}

			dataAccess.remove(resource);
	        ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_REMOVED,
	        		transactionUUID, resource.getClass(),
	        		resource.getId(), resource.toString());
	    	
	        transaction.commit();
	        ResourcesLog.log(ResourcesLogType.TRANSACTION_COMMITED, transactionUUID);
		}
		catch (RuntimeException e){
			throw new ResourcesException(ResourcesExceptionType.RESOURCE_DELETION_FAILED_UNEXPECTEDLY, e.getMessage());
		}
		finally{
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
	            ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
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
        ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(entityManager);
    	
    	try {
    		for ( ResourceAttribute attribute : attributes ){
    			/* First of all, updating attributes this Resource already has */

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
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_UPDATED,
    	        		transactionUUID, attribute.getClass(),
    	        		attribute.getId(), attribute.toString());
        		valuesJSON.remove(attribute.getMetadata().getName());
    		}
    		
    		if ( valuesJSON.size() > 0 ){
    			/* Then, as some unknown attribute names were provided,
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
    	    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_INSERTED,
    	    	        		transactionUUID, attribute.getClass(),
    	    	        		attribute.getId(), attribute.toString());
    				}
    				else{
	    	    		String unknownValue = valuesJSON.get(unknownName).asString();
    					throw new ResourcesException(ResourcesExceptionType.UNKNOWN_ATTRIBUTE,
    							resource.getType().getName(), unknownName, unknownValue);
    				}
    			}
    		}
    		
    		transaction.commit();
            ResourcesLog.log(ResourcesLogType.TRANSACTION_COMMITED, transactionUUID);
    	}
    	catch (RuntimeException e){
    		throw new ResourcesException(ResourcesExceptionType.RESOURCE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    	}
    	finally{
    		if ( transaction.isActive() ){
    			transaction.rollback();
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
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
    
}