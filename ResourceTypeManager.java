
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityTransaction;

import json.JsonArray;
import json.JsonValue;

import com.smartiks.voldemort.core.persistence.dao.DAO;


public class ResourceTypeManager {

    public static Integer createResourceType(String name, String description)
            throws ResourcesException{
    	
        ResourceType resourceType = new ResourceType(name, description);
        (new DAO(ResourcesEntityManager.getInstance())).insert(resourceType);
        ResourcesLog.log(ResourcesLogType.OBJECT_INSERTED,
        		resourceType.getClass(), resourceType.getId(), resourceType.toString());
        return resourceType.getId();
    }
    
    public static void removeResourceType(Integer id)
            throws ResourcesException{

    	ResourceType resourceType = findResourceType(id);
    	DAO dataAccess = new DAO(ResourcesEntityManager.getInstance());

    	if ( resourceTypeIsUsed(dataAccess, resourceType) ){
    		throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_TYPE_IN_USE, resourceType.getName());
    	}

    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
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

    		String attributeName = attributeJSON.asObject().get("name").asString();
    		if ( usedAttributeNames.containsKey(attributeName) ){
    			new ResourcesException(ResourcesExceptionType.ATTRIBUTE_NAME_MUST_BE_UNIQUE);
    		}
    		usedAttributeNames.put(attributeName, true);
    	}
    	
    	ResourcesException.throwExceptionsFound();
    	
    	for ( JsonValue attributeJSON : attributesJSON ){

    		ResourceTypeAttribute attribute = new ResourceTypeAttribute(attributeJSON.asObject());
    		attributes.add(attribute);
    	}

    	ResourcesException.throwExceptionsFound();
    	
    	HashMap<String, ResourceTypeAttribute> oldAttributes = new HashMap<String, ResourceTypeAttribute>();
    	for ( ResourceTypeAttribute attribute : resourceType.getAttributes() ){
    		oldAttributes.put(attribute.getName(), attribute);
    	}
    	
    	HashMap<String, ResourceTypeAttribute> newAttributes = new HashMap<String, ResourceTypeAttribute>();
    	for ( ResourceTypeAttribute attribute : attributes ){
    		newAttributes.put(attribute.getName(), attribute);
    	}
    	
    	EntityTransaction transaction = ResourcesEntityManager.getInstance().getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
    	ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(ResourcesEntityManager.getInstance());
    	
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
        	    			throw new ResourcesException(true, ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_UPDATED,
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
    					throw new ResourcesException(true, ResourcesExceptionType.ATTRIBUTE_CANNOT_BE_REMOVED,
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

    		ResourcesException.throwExceptionsFound();
    		
	    	resourceType.setAttributes(updatedAttributes);
	    	dataAccess.update(resourceType);
			ResourcesLog.log(ResourcesLogType.TRANSACTION_OBJECT_UPDATED,
					transactionUUID, resourceType.getClass(),
					resourceType.getId(), resourceType.toString());

	        transaction.commit();
	        ResourcesLog.log(ResourcesLogType.TRANSACTION_COMMITED, transactionUUID);
    	} catch (RuntimeException e){
    		throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
    	}
    	finally{
    		if ( transaction.isActive() ){
    			transaction.rollback();
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
    		}
    	}
    }
	
    public static ResourceType findResourceType(Integer id) throws ResourcesException {
		ResourceType resourceType;
    	try{
    		resourceType = (new DAO(ResourcesEntityManager.getInstance())).find(ResourceType.class, id);
    		if ( resourceType == null ){
        		throw new IllegalArgumentException();
    		}
    	}
    	catch ( IllegalArgumentException e ){
    		throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_TYPE_NOT_FOUND);
    	}
		return resourceType;
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