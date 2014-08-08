

public class TestFacade {

    public int criarTipoRecurso(String nome, String descricao)
            throws CompoundResourcesException{
        return (int) ResourcesEntryPoint.executeOperation("createResourceType", nome, descricao);
    }
	
    public void removerTipoRecurso(int idTipoRecurso)
            throws CompoundResourcesException{
    	ResourcesEntryPoint.executeOperation("removeResourceType", idTipoRecurso);
    }
    
    public void alterarTipoRecurso(int idTipoRecurso, String nome, String descricao, String atributos)
    		throws CompoundResourcesException{
    	ResourcesEntryPoint.executeOperation("updateResourceType", idTipoRecurso, nome, descricao, atributos);
    }
    
    public int criarRecurso(int idTipoRecurso, String atributos)
    		throws CompoundResourcesException{
    	return (int) ResourcesEntryPoint.executeOperation("createResource", idTipoRecurso, atributos);
    }

    public void removerRecurso(int idRecurso)
    		throws CompoundResourcesException{
    	ResourcesEntryPoint.executeOperation("removeResource", idRecurso);
    }
    
    public void alterarRecurso(int idRecurso, String atributos)
    		throws CompoundResourcesException{
    	ResourcesEntryPoint.executeOperation("updateResource", idRecurso, atributos);
    }
}