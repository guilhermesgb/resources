import java.util.logging.Logger;


public class ResourcesLog {

	private static Logger logger = Logger.getLogger(ResourcesManager.class.getName());

	public static void log(ResourcesLogType type, Object... params) {
		logger.log(type.level, String.format(type.message, params));
	}
	
}