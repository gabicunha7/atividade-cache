package school.sptech.cache.cache;

import school.sptech.cache.entity.Conteudo;

// conteúdo encontrado e se hit ou miss
public record ResultadoBusca(Conteudo conteudo, boolean hit) {
}
