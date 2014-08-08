

import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings("serial")
public class ResourcesException extends Exception{

	private static Logger logger = Logger.getLogger(ResourcesException.class.getName());
	private static ResourcesException peak = null;

	private ResourcesException previous;
	protected ResourcesExceptionType type;
	
	public ResourcesException(ResourcesExceptionType type){
	    super(type.message);
	    this.type = type;
	    this.previous = peak;
	    peak = this;
	}
	
	public ResourcesException(ResourcesExceptionType type, Object... params){
		super(String.format(type.message, params));
		this.type = type;
	    this.previous = peak;
	    peak = this;
	}
	
	public ResourcesException(boolean throwing, ResourcesExceptionType type, Object... params){
		this(type, params);
		if ( throwing ){
			peak = null;
		}
	}

	public ResourcesExceptionType getType() {
		return this.type;
	}
	
	protected static void resourcesWarning(ResourcesExceptionType type, Object... params){
		logger.log(Level.WARNING, String.format(type.message, params));
	}
	
	public boolean hasPrevious(){
		return this.previous != null;
	}
	
	public ResourcesException previous(){
		return this.previous;
	}

	public static void throwExceptionsFound() throws ResourcesException {
		if ( peak != null ){
			try{
				throw peak;
			}
			finally{
				peak = null;
			}
		}
	}

}