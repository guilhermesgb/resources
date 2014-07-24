
public class TestFacade {

    public int criarTipoRecurso(String nome, String descricao)
            throws Exception{
        return ResourcesManager.create(nome, descricao);
    }
	
    public void removerTipoRecurso(int idTipoRecurso)
            throws Exception{
    	ResourcesManager.remove(idTipoRecurso);
    }
    
    public void alterarTipoRecurso(int idTipoRecurso, String nome, String descricao, String atributos)
    		throws Exception{
    	ResourcesManager.update(idTipoRecurso, nome, descricao, atributos);
    }
}