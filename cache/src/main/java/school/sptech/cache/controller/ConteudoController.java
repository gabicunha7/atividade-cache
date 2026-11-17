package school.sptech.cache.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import school.sptech.cache.cache.ResultadoBusca;
import school.sptech.cache.dto.ConteudoRequest;
import school.sptech.cache.dto.ConteudoResponse;
import school.sptech.cache.entity.Conteudo;
import school.sptech.cache.mapper.ConteudoMapper;
import school.sptech.cache.service.ConteudoService;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

    private final ConteudoService service;

    public ConteudoController(ConteudoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConteudoResponse> buscar(@PathVariable Long id) {
        ResultadoBusca resultado = service.buscarPorId(id);
        return ResponseEntity.ok()
                .header("X-Cache", resultado.hit() ? "HIT" : "MISS")
                .body(ConteudoMapper.toResponse(resultado.conteudo()));
    }

    @PostMapping
    public ResponseEntity<ConteudoResponse> criar(@RequestBody ConteudoRequest request) {
        Conteudo salvo = service.criar(ConteudoMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ConteudoMapper.toResponse(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConteudoResponse> atualizar(@PathVariable Long id,
                                                      @RequestBody ConteudoRequest request) {
        Conteudo atualizado = service.atualizar(id, ConteudoMapper.toEntity(request));
        return ResponseEntity.ok(ConteudoMapper.toResponse(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
