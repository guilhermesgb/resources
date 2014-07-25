import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityTransaction;

import json.JsonObject;
import json.JsonValue;

import com.smartiks.voldemort.core.persistence.dao.DAO;


public class ResourceManager {

    public static Integer createResource(Integer id, String valuesRaw)
            throws ResourcesException{
		
    	ResourceType resourceType = ResourceTypeManager.findResourceType(id);
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
			new ResourcesException(ResourcesExceptionType.UNKNOWN_ATTRIBUTE,
					resource.getType().getName(), unknownName, unknownValue);
    	}
    	
    	ResourcesException.throwExceptionsFound();

    	EntityTransaction transaction = ResourcesEntityManager.getInstance().getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
        ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(ResourcesEntityManager.getInstance());
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
	    	throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_CREATION_FAILED_UNEXPECTEDLY, e.getMessage());
	    }
    	finally{
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
    	        ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
	    	}
	    }
    	return resource.getId();
    }

    public static void removeResource(Integer id)
    		throws ResourcesException {

		Resource resource = findResource(id);
		
    	EntityTransaction transaction = ResourcesEntityManager.getInstance().getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
        ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(ResourcesEntityManager.getInstance());		
		try {
			for ( ResourceAttribute attribute : resource.getAttributes(ResourcesEntityManager.getInstance()) ){
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
			throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_DELETION_FAILED_UNEXPECTEDLY, e.getMessage());
		}
		finally{
	    	if ( transaction.isActive() ){
	    		transaction.rollback();
	            ResourcesLog.log(ResourcesLogType.TRANSACTION_ROLLED_BACK, transactionUUID);
	    	}
		}
	}
	
    public static void updateResource(Integer id, String valuesRaw)
			throws ResourcesException {
		
		Resource resource = findResource(id);
		Set<ResourceAttribute> attributes = resource.getAttributes(ResourcesEntityManager.getInstance());
		JsonObject valuesJSON = JsonObject.readFrom(valuesRaw);
    	
    	EntityTransaction transaction = ResourcesEntityManager.getInstance().getTransaction();
    	String transactionUUID = UUID.randomUUID().toString();
    	transaction.begin();
        ResourcesLog.log(ResourcesLogType.TRANSACTION_BEGAN, transactionUUID);
    	DAO dataAccess = new DAO(ResourcesEntityManager.getInstance());
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
    		
    		ResourcesException.throwExceptionsFound();
    		
    		if ( valuesJSON.names().size() > 0 ){
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
    					new ResourcesException(ResourcesExceptionType.UNKNOWN_ATTRIBUTE,
    							resource.getType().getName(), unknownName, unknownValue);
    				}
    			}
    		}
    		
    		ResourcesException.throwExceptionsFound();
    		
    		transaction.commit();
            ResourcesLog.log(ResourcesLogType.TRANSACTION_COMMITED, transactionUUID);
    	}
    	catch (RuntimeException e){
    		throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_UPDATE_FAILED_UNEXPECTEDLY, e.getMessage());
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
    		resource= (new DAO(ResourcesEntityManager.getInstance())).find(Resource.class, id);
    		if ( resource== null ){
        		throw new IllegalArgumentException();
    		}
    	}
    	catch ( IllegalArgumentException e ){
    		throw new ResourcesException(true, ResourcesExceptionType.RESOURCE_NOT_FOUND);
    	}
		return resource;
	}
    
}