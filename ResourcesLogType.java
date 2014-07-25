import java.util.logging.Level;

public enum ResourcesLogType {
	
	OBJECT_INSERTED(Level.INFO, "Object of class '%s' (ID '%s') was inserted successfully; details: '%s'"),
	OBJECT_ABOUT_TO_BE_REMOVED(Level.INFO, "Object of class '%s' (ID '%s') is about to be removed; details: '%s'"),
	OBJECT_REMOVED_RECURSIVELY(Level.INFO, "Object of class '%s' (ID '%s') was removed successfully; details: '%s' - alongside with its '%s' (whose details are logged above)"),
	TRANSACTION_BEGAN(Level.INFO, "New transaction ['%s'] began!"),
	TRANSACTION_COMMITED(Level.INFO, "Transaction ['%s'] commited!"),
	TRANSACTION_ROLLED_BACK(Level.INFO, "Transaction ['%s'] failed and is rolled back"),
	TRANSACTION_OBJECT_INSERTED(Level.INFO, "Transaction ['%s']: Object of class '%s' (ID '%s') will be inserted; details: '%s'"),
	TRANSACTION_OBJECT_REMOVED(Level.INFO, "Transaction ['%s']: Object of class '%s' (ID '%s') will be removed; details: '%s'"),
	TRANSACTION_OBJECT_UPDATED(Level.INFO, "Transaction ['%s']: Object of class '%s' (ID '%s') will be updated; details: '%s'");

	public Level level;
	public String message;
	ResourcesLogType(Level level, String message){
		this.level = level;
		this.message = message;
	}
}
