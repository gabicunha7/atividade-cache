package school.sptech.cache.cache;

import org.junit.jupiter.api.Test;
import school.sptech.cache.entity.Conteudo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConteudoCacheTest {

    private final AtomicInteger chamadasNaFonte = new AtomicInteger();

    private final Function<Long, Conteudo> fonte = id -> {
        chamadasNaFonte.incrementAndGet();
        Conteudo c = new Conteudo();
        c.setId(id);
        c.setTitulo("Conteúdo " + id);
        return c;
    };

    @Test
    void primeiraBuscaEhMissESegundaEhHit() {
        ConteudoCache cache = new ConteudoCache(5, 60);

        assertFalse(cache.buscar(1L, fonte).hit());
        assertTrue(cache.buscar(1L, fonte).hit());
        assertEquals(1, chamadasNaFonte.get());
    }

    @Test
    void removeOMenosUsadoRecentementeQuandoEnche() throws InterruptedException {
        ConteudoCache cache = new ConteudoCache(2, 60);

        cache.buscar(1L, fonte);
        Thread.sleep(5);
        cache.buscar(2L, fonte);
        Thread.sleep(5);
        cache.buscar(1L, fonte);   // 1 vira o mais recente
        Thread.sleep(5);
        cache.buscar(3L, fonte);   // cheio -> sai o 2
        Thread.sleep(5);

        assertTrue(cache.buscar(1L, fonte).hit());
        Thread.sleep(5);
        assertFalse(cache.buscar(2L, fonte).hit()); // o 2 tinha saído, então é MISS
        assertEquals(2, cache.estatisticas().evictions()); // 2 saiu, depois saiu mais um para o 2 voltar
    }

    @Test
    void itemExpiradoPeloTtlViraMiss() throws InterruptedException {
        ConteudoCache cache = new ConteudoCache(5, 1);

        cache.buscar(1L, fonte);
        Thread.sleep(1100);

        assertFalse(cache.buscar(1L, fonte).hit());
        assertEquals(2, chamadasNaFonte.get());
    }

    @Test
    void invalidarRemoveDoCache() {
        ConteudoCache cache = new ConteudoCache(5, 60);

        cache.buscar(1L, fonte);
        cache.invalidar(1L);

        assertFalse(cache.buscar(1L, fonte).hit());
    }
}
