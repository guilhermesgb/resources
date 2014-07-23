
public class TestFacade {

    public int criarTipoRecurso(String nome, String descricao)
            throws Exception{
        return ResourceTypeManager.create(nome, descricao);
    }
	
    public void removerTipoRecurso(int idTipoRecurso)
            throws Exception{
    	ResourceTypeManager.remove(idTipoRecurso);
    }
    
    public void alterarTipoRecurso(int idTipoRecurso, String nome, String descricao, String atributos)
    		throws Exception{
    	ResourceTypeManager.update(idTipoRecurso, nome, descricao, atributos);
    }
}