
public enum ResourcesExceptionType{

	INVALID_NAME("Invalid name (cannot be empty)!"),
	INVALID_DESCRIPTION("Invalid description (cannot be empty)!"),
	RESOURCE_TYPE_NOT_FOUND("ResourceType not found!"),
	INVALID_ATTRIBUTE_NAME("Invalid ResourceTypeAttribute 'name' (cannot be empty)!"),
	INVALID_ATTRIBUTE_TYPE("Invalid ResourceTypeAttribute 'type' (cannot be empty and must one of 'TEXT', 'NUMBER' or 'DATE')!"),
	INVALID_ATTRIBUTE_IS_MANDATORY("Invalid ResourceTypeAttribute 'mandatory' (cannot be empty, must be exactly equal to 'true' or 'false')!"),
	ATTRIBUTE_NAME_MUST_BE_UNIQUE("Invalid ResourceTypeAttribute 'name' (must be unique)!"),
	MANDATORY_ATTRIBUTE_OMMITED("ResourceAttribute '%s' of type '%s' is mandatory (cannot be null or empty)!"),
	INVALID_VALUE("Invalid ResourceAttribute '%s' value (is of type '%s', so cannot be: '%s')!"),
	RESOURCE_TYPE_UPDATE_FAILED("Update of ResourceType failed!"),
	RESOURCE_CREATION_FAILED("Creation of Resource failed!"),
	RESOURCE_NOT_FOUND("Resource not found!");

	public String message;
	ResourcesExceptionType(String message){
		this.message = message;
	}
}