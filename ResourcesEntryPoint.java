import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


public class ResourcesEntryPoint {

	public static Object executeOperation(String methodName, Object... parameters)
			throws CompoundResourcesException{

		Map<String, Class<?>[]> parameterTypes = new HashMap<String, Class<?>[]>();
		Map<String, Class<?>> managerName = new HashMap<String, Class<?>>();

		Class<?>[] createResourceType = { String.class, String.class };
		parameterTypes.put("createResourceType", createResourceType);
		managerName.put("createResourceType", ResourceTypeManager.class);
		
		Class<?>[] removeResourceType = { Integer.class };
		parameterTypes.put("removeResourceType", removeResourceType);
		managerName.put("removeResourceType", ResourceTypeManager.class);
		
		Class<?>[] updateResourceType = { Integer.class, String.class, String.class, String.class };
		parameterTypes.put("updateResourceType", updateResourceType);
		managerName.put("updateResourceType", ResourceTypeManager.class);
		
		Class<?>[] createResource = { Integer.class, String.class };
		parameterTypes.put("createResource", createResource);
		managerName.put("createResource", ResourceManager.class);
		
		Class<?>[] removeResource = { Integer.class };
		parameterTypes.put("removeResource", removeResource);
		managerName.put("removeResource", ResourceManager.class);
		
		Class<?>[] updateResource = { Integer.class, String.class };
		parameterTypes.put("updateResource", updateResource);
		managerName.put("updateResource", ResourceManager.class);
		
		try{
			return managerName.get(methodName).getMethod(methodName,
					parameterTypes.get(methodName)).invoke(null, parameters);
		}
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | 
				IllegalArgumentException unexpected){
			throw new CompoundResourcesException(new ResourcesException(
					true, ResourcesExceptionType.METHOD_EXECUTION_PROBLEM,
					methodName, unexpected.getCause().getMessage()));
		}
		catch (InvocationTargetException exceptions){
			throw new CompoundResourcesException((ResourcesException) exceptions.getCause());
		}
	}

}