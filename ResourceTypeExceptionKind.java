
public enum ResourceTypeExceptionKind{

	INVALID_NAME("Invalid name (cannot be empty)!"),
	INVALID_DESCRIPTION("Invalid description (cannot be empty)!"),
	RESOURCE_TYPE_NOT_FOUND("ResourceType not found!"),
	INVALID_ATTRIBUTE_NAME("Invalid ResourceTypeAttribute 'name' (cannot be empty)!"),
	INVALID_ATTRIBUTE_TYPE("Invalid ResourceTypeAttribute 'type' (cannot be empty)!"),
	INVALID_ATTRIBUTE_IS_MANDATORY("Invalid ResourceTypeAttribute 'mandatory' (cannot be empty, must be exactly equal to 'true' or 'false')!"),
	ATTRIBUTE_NAME_MUST_BE_UNIQUE("Invalid ResourceTypeAttribute 'name' (must be unique)!");

	public String message;
	ResourceTypeExceptionKind(String message){
		this.message = message;
	}
}