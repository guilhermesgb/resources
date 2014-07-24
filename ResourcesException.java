@SuppressWarnings("serial")
public class ResourcesException extends Exception{

	protected ResourcesExceptionType type;
	protected ResourcesException(ResourcesExceptionType type){
	    super(type.message);
	    this.type = type;
	}
	
	protected ResourcesException(ResourcesExceptionType type, Object... params){
		super(String.format(type.message, params));
		this.type = type;
	}
}