import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class ResourcesException extends Exception{

	private static Logger logger = Logger.getLogger(ResourceType.class.getName());
	
	protected ResourcesExceptionType type;
	protected ResourcesException(ResourcesExceptionType type){
	    super(type.message);
	    this.type = type;
	}
	
	protected ResourcesException(ResourcesExceptionType type, Object... params){
		super(String.format(type.message, params));
		this.type = type;
	}
	
	protected static void resourcesWarning(ResourcesExceptionType type, Object... params){
		logger.log(Level.WARNING, String.format(type.message, params));
	}
}