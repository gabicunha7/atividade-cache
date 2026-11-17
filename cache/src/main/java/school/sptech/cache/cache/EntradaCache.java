package school.sptech.cache.cache;

import school.sptech.cache.entity.Conteudo;

import java.time.Duration;
import java.time.Instant;


 // Uma posição do cache: o conteúdo, quando foi guardado e quando foi usado pela última vez.
public class EntradaCache {

    private final Conteudo conteudo;
    private final Instant armazenadoEm;
    private volatile Instant ultimoAcesso;

    public EntradaCache(Conteudo conteudo) {
        this.conteudo = conteudo;
        this.armazenadoEm = Instant.now();
        this.ultimoAcesso = this.armazenadoEm;
    }

    // Passou do TTL? buscar dnv
    public boolean expirou(Duration ttl) {
        return Instant.now().isAfter(armazenadoEm.plus(ttl));
    }

    // Atualiza o último acesso (é isso que o LRU usa para decidir quem sai).
    public void registrarAcesso() {
        this.ultimoAcesso = Instant.now();
    }

    public Conteudo getConteudo() { return conteudo; }
    public Instant getArmazenadoEm() { return armazenadoEm; }
    public Instant getUltimoAcesso() { return ultimoAcesso; }
}
