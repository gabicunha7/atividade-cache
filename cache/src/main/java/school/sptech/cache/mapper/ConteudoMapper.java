package school.sptech.cache.mapper;

import school.sptech.cache.dto.ConteudoRequest;
import school.sptech.cache.dto.ConteudoResponse;
import school.sptech.cache.entity.Conteudo;

public class ConteudoMapper {

    private ConteudoMapper() {
    }

    public static Conteudo toEntity(ConteudoRequest request) {
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo(request.titulo());
        conteudo.setCorpo(request.corpo());
        conteudo.setAutor(request.autor());
        return conteudo;
    }

    public static ConteudoResponse toResponse(Conteudo conteudo) {
        return new ConteudoResponse(conteudo.getId(), conteudo.getTitulo(), conteudo.getCorpo(),
                conteudo.getAutor(), conteudo.getAtualizadoEm());
    }
}
