import javax.persistence.EntityManager;

import com.smartiks.voldemort.core.persistence.DefaultEntityManagerProvider;


public class ResourcesEntityManager {
	
	private static EntityManager instance = null;

	private ResourcesEntityManager(){}
	
	public static EntityManager getInstance(){
		if ( instance == null ){
			instance = new DefaultEntityManagerProvider("resources")
		    .createEntityManager();
		}
		return instance;
	}
	
}
