package school.sptech.cache.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import school.sptech.cache.entity.Conteudo;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
}
