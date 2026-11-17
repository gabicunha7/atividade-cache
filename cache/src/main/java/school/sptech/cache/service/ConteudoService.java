package school.sptech.cache.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import school.sptech.cache.cache.ConteudoCache;
import school.sptech.cache.cache.ResultadoBusca;
import school.sptech.cache.entity.Conteudo;
import school.sptech.cache.repository.ConteudoRepository;

import java.time.LocalDateTime;

@Service
public class ConteudoService {

    private static final long ATRASO_FONTE_MS = 2000;

    private final ConteudoRepository repository;
    private final ConteudoCache cache;

    public ConteudoService(ConteudoRepository repository, ConteudoCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public ResultadoBusca buscarPorId(Long id) {
        return cache.buscar(id, this::buscarNaFonte);
    }

    // Simula uma fonte lenta (banco sobrecarregado, serviço externo...).
    private Conteudo buscarNaFonte(Long id) {
        try {
            Thread.sleep(ATRASO_FONTE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return buscarOuFalhar(id);
    }

    public Conteudo criar(Conteudo conteudo) {
        conteudo.setId(null);
        conteudo.setAtualizadoEm(LocalDateTime.now());
        return repository.save(conteudo);
    }

    public Conteudo atualizar(Long id, Conteudo dados) {
        Conteudo existente = buscarOuFalhar(id);
        existente.setTitulo(dados.getTitulo());
        existente.setCorpo(dados.getCorpo());
        existente.setAutor(dados.getAutor());
        existente.setAtualizadoEm(LocalDateTime.now());
        Conteudo salvo = repository.save(existente);
        cache.invalidar(id);
        return salvo;
    }

    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado");
        }
        repository.deleteById(id);
        cache.invalidar(id);
    }

    private Conteudo buscarOuFalhar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));
    }
}
