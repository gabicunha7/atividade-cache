# Cache Spring e ConcurrentHashMap
Usando concurrent pq o hashmap não é thread safe oq pode se fizermos várias requisições
ao mesmo tempo pode causar inconsistencias.


**Feito por:** Gabriela Pinheiro Lopes da Cunha
**RA:** 04251015

## Como rodar
1. Abra a pasta `cache`.
2. Rode a classe `CacheApplication` ou `mvn spring-boot:run` no terminal.
3. App roda em `http://localhost:8080`.

Para rodar os testes do cache: `mvn test`.

## Configuração (`application.properties`)
```properties
app.cache.max-itens=5      # quantidade máxima de itens no cache
app.cache.ttl-segundos=60  # tempo de vida de cada item
```

## Endpoints
- `GET /conteudos/{id}` → busca o conteúdo, primeiro no cache e se não achar na fonte. Devolve o header `X-Cache: HIT` ou `X-Cache: MISS`.
- `POST /conteudos` → cadastra um conteúdo.
- `PUT /conteudos/{id}` → atualiza o conteúdo e tira ele do cache.
- `DELETE /conteudos/{id}` → exclui o conteúdo e tira ele do cache.
- `GET /cache/estatisticas` → mostra tamanho, limite, hits, misses, evictions e hit ratio.
- `DELETE /cache` → limpa o cache inteiro.

Exemplo de corpo para POST/PUT:
```json
{ "titulo": "Artigo 1", "corpo": "Texto do artigo", "autor": "Fulano" }
```

## Como funciona a solução?

### Classe EntradaCache
- É cada item guardado no cache. Ela guarda o conteúdo, o momento em que ele entrou no cache (usado no TTL) e o último acesso (usado no LRU).

### Classe ConteudoCache
- É o cache em si, um `@Component` com um `ConcurrentHashMap<Long, EntradaCache>` onde a chave é o id do conteúdo. O limite de itens e o TTL vêm do `application.properties` com `@Value`.

### Lógica da Busca
- Recebe o id e olha no mapa. Se achar e não estiver expirado é um **HIT**: atualiza o último acesso e devolve na hora. Se não achar, ou se estiver expirado, é um **MISS**: busca na fonte (que tem um `Thread.sleep(2000)` para simular lentidão), guarda no cache e devolve. Toda busca gera um log com HIT ou MISS, o id e o tempo:
  ```
  INFO  ConteudoCache : Cache MISS - id=3 - 2015 ms
  INFO  ConteudoCache : Cache HIT  - id=3 - 1 ms
  ```

### Lógica do Armazenar (LRU)
- Antes de guardar um item novo, verifica se o cache já está cheio. Se estiver, remove o item com o último acesso mais antigo, ou seja, o que está há mais tempo sem ser usado (LRU), e soma 1 nas evictions. O método é `synchronized` para que "ver se está cheio, remover e inserir" aconteça de uma vez só e duas requisições juntas não passem do limite.

### Lógica do TTL
- Cada item tem a hora em que entrou no cache. Se já passou mais tempo que o `app.cache.ttl-segundos`, ele é considerado velho: é removido e a busca vira MISS, indo de novo na fonte para pegar o dado atualizado.

### Invalidação
- Quando um conteúdo é atualizado (`PUT`) ou excluído (`DELETE`), o service remove ele do cache. Assim a próxima busca vai na fonte e ninguém recebe o dado antigo.

### Estatísticas
- O cache conta hits, misses e evictions com `AtomicLong`, que é seguro com várias requisições ao mesmo tempo. O hit ratio é `hits / (hits + misses)`.

### Organização
- O controller recebe e devolve DTOs e faz a conversão com o `ConteudoMapper`. O service recebe e devolve a entidade e é quem usa o cache. O `ResultadoBusca` só junta a entidade com a informação de HIT ou MISS, que o controller precisa para montar o header `X-Cache`.

## Prints

Teste com `app.cache.max-itens=5` e `app.cache.ttl-segundos=60`. Dá para saber se foi HIT ou MISS pelo tempo de resposta: o MISS demora uns 2 s por causa do `Thread.sleep(2000)` e o HIT volta em poucos ms.

### Cadastro de conteúdo
- `POST /conteudos` cadastrando um conteúdo, retorna `201 Created`. Foram cadastrados 7 conteúdos.

![Cadastro de conteúdo](prints/post_conteudos.png)

### Cache MISS
- Primeira busca do conteúdo 2. Ele não está no cache, então vai na fonte e demora **2,02 s**.

![Cache MISS](prints/cache_miss.png)

### Cache HIT
- Mesma busca logo em seguida. Agora o conteúdo já está no cache e volta em **5 ms**.

![Cache HIT](prints/cache_hit.png)

### Cache cheio e remoção pelo LRU
- Com o cache cheio (5 itens), a busca do conteúdo 6 é MISS e, para ele entrar, sai o item usado há mais tempo, que era o 2. No log aparece `Cache EVICTION (LRU) - id=2 removido`.

![GET no 6 remove o 2](prints/get_no_6_faz_2_comecar_a_dar_miss.png)

- Buscando o 2 de novo ele volta a dar MISS (**2,03 s**), o que prova que ele tinha saído do cache.

![Cache cheio, o 2 dá MISS](prints/2_cache_cheio_da_miss.png)

### Invalidação após PUT
- `PUT /conteudos/1` atualiza o título para "Artigo 1 (ATUALIZADO)" e remove o 1 do cache. No log aparece `Cache INVALIDADO - id=1`.

![PUT no conteúdo 1](prints/atualiza_1.png)

- A busca seguinte do 1 é MISS (**2,03 s**) e já volta com o dado novo, sem entregar a versão antiga.

![Depois do PUT dá MISS com o dado novo](prints/apos_atualizar_da_miss_dado_novo.png)

### Expiração pelo TTL
- Depois de mais de 60 s, o conteúdo 6 ainda estava no cache mas tinha passado do TTL, então virou MISS (**2,04 s**) e foi buscado de novo. No log aparece `Cache EXPIRADO (TTL) - id=6`.

![Expirou pelo TTL](prints/6_expira_por_ttl.png)

### Estatísticas
- Resultado no final dos testes: 5 itens no cache (o limite), 2 hits, 12 misses e 2 evictions. O hit ratio ficou baixo (~14%) porque a demonstração força vários MISS de propósito (cache cheio, TTL e invalidação).

![Estatísticas do cache](prints/estatisticas_cache.png)

### Exclusão de conteúdo
- `DELETE /conteudos/7` exclui o conteúdo, retorna `204 No Content` e também tira ele do cache.

![DELETE no conteúdo 7](prints/delete_conteudo_7.png)

### Logs da aplicação
- Os logs do `ConteudoCache` durante os testes. Dá para ver cada MISS (~2000 ms), cada HIT (0 a 1 ms), as remoções pelo LRU, as expirações pelo TTL, a invalidação depois do PUT e a limpeza do cache no final.

![Logs da aplicação](prints/exemplos_logs_aplicacao.png)
