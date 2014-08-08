
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


@SuppressWarnings("serial")
public class CompoundResourcesException extends Exception{

	protected List<ResourcesExceptionType> types = new LinkedList<ResourcesExceptionType>();
	protected String message;
	
	public CompoundResourcesException(ResourcesException last) {

		List<String> messages = new ArrayList<String>();
		messages.add(last.getMessage());
		types.add(last.getType());

		ResourcesException current = last;
		while ( current.hasPrevious() ){
			current = current.previous();
			messages.add(current.getMessage());
			types.add(current.getType());
		}

		String[] sortedMessages = new String[messages.size()];
		for ( int i=0; i<messages.size(); i++ ){
			sortedMessages[i] = messages.get(i);
		}
		Arrays.sort(sortedMessages);
		messages = Arrays.asList(sortedMessages);
		
		StringBuilder sb = new StringBuilder();
		for ( String message : messages ){
			if ( sb.length() > 0 ){
				sb.append(", ");
			}
			sb.append(message.replace("!", ""));
		}
		sb.append("!");
		this.message = sb.toString();
	}
	
	@Override
	public String toString(){
		return this.message;
	}
	
	@Override
	public String getMessage(){
		return this.message;
	}
}