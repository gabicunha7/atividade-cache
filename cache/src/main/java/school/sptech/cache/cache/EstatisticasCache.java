package school.sptech.cache.cache;

public record EstatisticasCache(int tamanho, int limite, long hits, long misses,
                                long evictions, double hitRatio) {
}
