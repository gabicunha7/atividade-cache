package school.sptech.cache.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import school.sptech.cache.entity.Conteudo;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 cache feito com ConcurrentHashMap.
 limite (app.cache.max-itens)
 LRU para remover quando ta cheio
 TTL quando expira (app.cache.ttl-segundos)
 estatísticas
 */
@Component
public class ConteudoCache {

    private static final Logger log = LoggerFactory.getLogger(ConteudoCache.class);

    private final Map<Long, EntradaCache> mapa = new ConcurrentHashMap<>();
    private final int maxItens;
    private final Duration ttl;

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    public ConteudoCache(@Value("${app.cache.max-itens}") int maxItens,
                         @Value("${app.cache.ttl-segundos}") long ttlSegundos) {
        this.maxItens = maxItens;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }


     // Procura no cache se não achar ele busca

    public ResultadoBusca buscar(Long id, Function<Long, Conteudo> fonte) {
        long inicio = System.currentTimeMillis();
        EntradaCache entrada = mapa.get(id);

        // HIT caiu em cache valido
        if (entrada != null && !entrada.expirou(ttl)) {
            entrada.registrarAcesso();
            hits.incrementAndGet();
            log.info("Cache HIT  - id={} - {} ms", id, System.currentTimeMillis() - inicio);
            return new ResultadoBusca(entrada.getConteudo(), true);
        }

        // passou do TTL cai para MISS mesmo, foi expirado
        if (entrada != null) {
            mapa.remove(id, entrada);
            log.info("Cache EXPIRADO (TTL) - id={}", id);
        }

        // MISS: vai na fonte lenta e guarda
        misses.incrementAndGet();
        Conteudo conteudo = fonte.apply(id);
        armazenar(id, conteudo);
        log.info("Cache MISS - id={} - {} ms", id, System.currentTimeMillis() - inicio);
        return new ResultadoBusca(conteudo, false);
    }

    /** synchronized: "ver se está cheio -> remover -> inserir" precisa acontecer de uma vez só. */
    private synchronized void armazenar(Long id, Conteudo conteudo) {
        if (!mapa.containsKey(id) && mapa.size() >= maxItens) {
            removerMenosUsadoRecentemente();
        }
        mapa.put(id, new EntradaCache(conteudo));
    }

    // LRU: remove a entrada com o último acesso mais antigo.
    private void removerMenosUsadoRecentemente() {
        mapa.entrySet().stream()
                .min(Map.Entry.comparingByValue(Comparator.comparing(EntradaCache::getUltimoAcesso)))
                .ifPresent(maisAntiga -> {
                    if (mapa.remove(maisAntiga.getKey(), maisAntiga.getValue())) {
                        evictions.incrementAndGet();
                        log.info("Cache EVICTION (LRU) - id={} removido", maisAntiga.getKey());
                    }
                });
    }

    public void invalidar(Long id) {
        if (mapa.remove(id) != null) {
            log.info("Cache INVALIDADO - id={}", id);
        }
    }

    public void limpar() {
        mapa.clear();
        log.info("Cache LIMPO");
    }

    public EstatisticasCache estatisticas() {
        long h = hits.get();
        long m = misses.get();
        double hitRatio = (h + m) == 0 ? 0.0 : (double) h / (h + m);
        return new EstatisticasCache(mapa.size(), maxItens, h, m, evictions.get(), hitRatio);
    }
}
