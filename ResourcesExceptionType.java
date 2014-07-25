
public enum ResourcesExceptionType{

	INVALID_RESOURCE_TYPE_NAME("Invalid ResourceType name (cannot be empty)!"),
	INVALID_RESOURCE_TYPE_DESCRIPTION("Invalid ResourceType description (cannot be null)!"),
	RESOURCE_TYPE_NOT_FOUND("ResourceType not found!"),
	INVALID_ATTRIBUTE_NAME("Invalid ResourceTypeAttribute property 'name' (cannot be empty)!"),
	INVALID_ATTRIBUTE_TYPE("Invalid ResourceTypeAttribute property 'type' (cannot be empty and must one of 'TEXT', 'NUMBER' or 'DATE')!"),
	INVALID_ATTRIBUTE_IS_MANDATORY("Invalid ResourceTypeAttribute property 'mandatory' (cannot be empty, must be exactly equal to 'true' or 'false')!"),
	ATTRIBUTE_NAME_MUST_BE_UNIQUE("Invalid ResourceTypeAttribute property 'name' (must be unique)!"),
	MANDATORY_ATTRIBUTE_OMMITED("ResourceAttribute '%s' of type '%s' is mandatory (cannot be null or empty)!"),
	INVALID_VALUE("Invalid ResourceAttribute '%s' value (is of type '%s', so cannot be: '%s')!"),
	RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY("Update of ResourceType failed unexpectedly: %s!"),
	RESOURCE_NOT_FOUND("Resource not found!"),
	RESOURCE_CREATION_FAILED_UNEXPECTEDLY("Creation of Resource failed unexpectedly: %s!"),
	RESOURCE_DELETION_FAILED_UNEXPECTEDLY("Deletion of Resource failed unexpectedly: %s!"),
	RESOURCE_UPDATE_FAILED_UNEXPECTEDLY("Update of Resource failed unexpectedly: %s!"),
	ATTRIBUTE_CANNOT_BE_REMOVED("ResourceAttribute '%s' of type '%s' cannot be removed, because %d Resources use it!"),
	ATTRIBUTE_CANNOT_BE_UPDATED("ResourceAttribute '%s' of type '%s' cannot be updated, because %d Resources use it!"),
	RESOURCE_TYPE_IN_USE("ResourceType '%s' cannot be removed, because there are Resources using it!"),
	UNKNOWN_ATTRIBUTE("Resource '%s' has no attribute named '%s' (whose value would be: '%s')!"),
	METHOD_EXECUTION_PROBLEM("A unexpected error happened while executing method '%s': '%s'");
	
	public String message;
	ResourcesExceptionType(String message){
		this.message = message;
	}
}