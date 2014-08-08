

public enum ResourcesExceptionType{

	INVALID_RESOURCE_TYPE_NAME("Nome de tipo de recurso inválido (não pode ser vazio)!"),
	INVALID_RESOURCE_TYPE_DESCRIPTION("Descrição de tipo de recurso inválida (não pode ser nulo)!"),
	RESOURCE_TYPE_NOT_FOUND("Tipo de recurso não encontrado!"),
	INVALID_ATTRIBUTE_NAME("Propriedade 'nome' de definição de atributo inválida (não pode ser vazio)!"),
	INVALID_ATTRIBUTE_TYPE("Propriedade 'nome' de definição de tipo de atributo inválida (não pode ser vazio e deve ser um dos seguintes valores: TEXT, NUMBER ou DATE)!"),
	INVALID_ATTRIBUTE_IS_MANDATORY("Propriedade 'mandatório' de definição de atributo inválida (não pode ser vazio e deve ser um exatamente 'true' ou 'false')!"),
	ATTRIBUTE_NAME_MUST_BE_UNIQUE("Propriedade 'nome' de definição de atributo inválida (não pode ser repetida)!"),
	MANDATORY_ATTRIBUTE_OMMITED("Atributo de nome '%s', cujo tipo é '%s' é mandatório (não pode ser nulo nem vazio)!"),
	INVALID_VALUE("Valor inválido para o atributo '%s' (é do tipo '%s', então o valor não pode ser: '%s')!"),
	RESOURCE_TYPE_UPDATE_FAILED_UNEXPECTEDLY("Atualização de definição de tipo de recurso falhou inesperadamente: %s!"),
	RESOURCE_NOT_FOUND("Recurso não encontrado!"),
	RESOURCE_CREATION_FAILED_UNEXPECTEDLY("Criação de definição de tipo de recurso falhou inesperadamente: %s!"),
	RESOURCE_DELETION_FAILED_UNEXPECTEDLY("Remoção de definição de tipo de recurso falhou inesperadamente: %s!"),
	RESOURCE_UPDATE_FAILED_UNEXPECTEDLY("Atualização de recurso falhou inesperadamente: %s!"),
	ATTRIBUTE_CANNOT_BE_REMOVED("Atributo de nome '%s', cujo tipo é '%s' não pode ser removido, porque '%d' recursos o possuem!"),
	ATTRIBUTE_CANNOT_BE_UPDATED("Atributo de nome '%s', cujo tipo é '%s' não pode ser atualizado, porque '%d' recursos o possuem!"),
	RESOURCE_TYPE_IN_USE("Tipo de recurso de nome '%s' não pode ser removido, porque foram criados recursos desse tipo!"),
	UNKNOWN_ATTRIBUTE("Recurso de nome '%s' não tem nenhum atributo com nome '%s' (cujo valor seria: '%s')!"),
	METHOD_EXECUTION_PROBLEM("Um erro inesperado ocorreu durante a execução da operação '%s': '%s'");
	
	public String message;
	ResourcesExceptionType(String message){
		this.message = message;
	}
}