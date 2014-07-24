
public class TestFacade {

    public int criarTipoRecurso(String nome, String descricao)
            throws Exception{
        return ResourcesManager.createResourceType(nome, descricao);
    }
	
    public void removerTipoRecurso(int idTipoRecurso)
            throws Exception{
    	ResourcesManager.removeResourceType(idTipoRecurso);
    }
    
    public void alterarTipoRecurso(int idTipoRecurso, String nome, String descricao, String atributos)
    		throws Exception{
    	ResourcesManager.updateResourceType(idTipoRecurso, nome, descricao, atributos);
    }
    
    public int criarRecurso(int idTipoRecurso, String atributos)
    		throws Exception{
    	return ResourcesManager.createResource(idTipoRecurso, atributos);
    }

    public void removerRecurso(int idRecurso)
    		throws Exception{
    	ResourcesManager.removeResource(idRecurso);
    }
    
    public void alterarRecurso(int idRecurso, String atributos)
    		throws Exception{
    	ResourcesManager.updateResource(idRecurso, atributos);
    }
}