
public enum ResourceTypeExceptionKind{

	INVALID_NAME("Nome invalido!"),
	INVALID_DESCRIPTION("Descricao invalido!"),
	RESOURCE_TYPE_NOT_FOUND("Tipo de recurso n�o encontrado!");

	public String message;
	ResourceTypeExceptionKind(String message){
		this.message = message;
	}
}