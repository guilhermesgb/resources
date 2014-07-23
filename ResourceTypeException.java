@SuppressWarnings("serial")
public class ResourceTypeException extends Exception{

	protected ResourceTypeExceptionKind type;
	protected ResourceTypeException(ResourceTypeExceptionKind type){
	    super(type.message);
	    this.type = type;
	}
	
}