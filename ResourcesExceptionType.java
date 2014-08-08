

public enum ResourcesExceptionType{

	INVALID_RESOURCE_TYPE_NAME("Nome de tipo de recurso inv�lido (n�o pode ser vazio)!"),
	INVALID_RESOURCE_TYPE_DESCRIPTION("Descri��o de tipo de recurso inv�lida (n�o pode ser nulo)!"),
	RESOURCE_TYPE_NOT_FOUND("Tipo de recurso n�o encontrado!"),
	INVALID_ATTRIBUTE_NAME("Propriedade 'nome' de defini��o de atributo inv�lida (n�o pode ser vazio)!"),
	INVALID_ATTRIBUTE_TYPE("Propriedade 'nome' de defini��o de tipo de atributo inv�lida (n�o pode ser vazio e deve ser um dos seguintes valores: TEXT, NUMBER ou DATE)!"),
	INVALID_ATTRIBUTE_IS_MANDATORY("Propriedade 'mandat�rio' de defini��o de atributo inv�lida (n�o pode ser vazio e deve ser um exatamente 'true' ou 'false')!"),
	ATTRIBUTE_NAME_MUST_BE_UNIQUE("Propriedade 'nome' de defini��o de atributo inv�lida (n�o pode ser repetida)!"),
	MANDATORY_ATTRIBUTE_OMMITED("Atributo de nome '%s', cujo tipo � '%s' � mandat�rio (n�o pode ser nulo nem vazio)!"),
	INVALID_VALUE("Valor inv�lido para o atributo '%s' (� do tipo '%s', ent�o o valor n�o pode ser: '%s')!"),
	RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY("Atualiza��o de defini��o de tipo de recurso falhou inesperadamente: %s!"),
	RESOURCE_NOT_FOUND("Recurso n�o encontrado!"),
	RESOURCE_CREATION_FAILED_UNEXPECTEDLY("Cria��o de defini��o de tipo de recurso falhou inesperadamente: %s!"),
	RESOURCE_DELETION_FAILED_UNEXPECTEDLY("Remo��o de defini��o de tipo de recurso falhou inesperadamente: %s!"),
	RESOURCE_UPDATE_FAILED_UNEXPECTEDLY("Atualiza��o de recurso falhou inesperadamente: %s!"),
	ATTRIBUTE_CANNOT_BE_REMOVED("Atributo de nome '%s', cujo tipo � '%s' n�o pode ser removido, porque '%d' recursos o possuem!"),
	ATTRIBUTE_CANNOT_BE_UPDATED("Atributo de nome '%s', cujo tipo � '%s' n�o pode ser atualizado, porque '%d' recursos o possuem!"),
	RESOURCE_TYPE_IN_USE("Tipo de recurso de nome '%s' n�o pode ser removido, porque foram criados recursos desse tipo!"),
	UNKNOWN_ATTRIBUTE("Recurso de nome '%s' n�o tem nenhum atributo com nome '%s' (cujo valor seria: '%s')!"),
	METHOD_EXECUTION_PROBLEM("Um erro inesperado ocorreu durante a execu��o da opera��o '%s': '%s'");
	
	public String message;
	ResourcesExceptionType(String message){
		this.message = message;
	}
}