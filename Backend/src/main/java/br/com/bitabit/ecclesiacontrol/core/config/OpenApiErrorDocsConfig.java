package br.com.bitabit.ecclesiacontrol.core.config;

import br.com.bitabit.ecclesiacontrol.auth.controller.AuthController;
import br.com.bitabit.ecclesiacontrol.core.exception.ApiError;   // ajuste o pacote se o ApiError estiver em outro
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documenta, para todos os endpoints de uma vez, o que o frontend precisa saber e não está nos controllers:
 * o formato único de erro (ApiError), os status de erro possíveis e o passo a passo de autenticação.
 * É aditivo: não substitui a OpenApiConfig existente (esquema de segurança Bearer).
 */
@Configuration
public class OpenApiErrorDocsConfig {

    private static final String MARKER = "## Como autenticar";

    private static final String GUIDE = MARKER + """


            1. `POST /auth/login` com e-mail e senha. Use o valor de `accessToken` no botão **Authorize**,
               **sem aspas e sem a palavra `Bearer`**.
            2. O `accessToken` dura 1 hora. Renove com `POST /auth/refresh-token`; o token de renovação
               é de uso único e vem um novo a cada renovação.
            3. **Escopo.** Um token **GLOBAL** (papéis da Sede) enxerga todas as filiais. Um token **LOCAL**
               enxerga só a filial do token. `POST /auth/switch-filial` troca de filial e devolve um token LOCAL;
               para voltar à visão global é preciso fazer login de novo.
            4. As permissões são recalculadas a cada requisição: desativar um usuário ou remover um papel vale na hora.

            ## Erros

            Todo erro vem no mesmo formato (`ApiError`): `timestamp`, `status`, `error`, `message`, `path`.

            | Status | Quando |
            |---|---|
            | 400 | corpo ou parâmetro malformado, campo inválido |
            | 401 | token ausente, inválido ou expirado; credenciais inválidas; usuário inativo |
            | 403 | sem a permissão exigida pelo endpoint |
            | 404 | não existe **ou pertence a outra filial** (a API não revela a diferença) |
            | 422 | regra de negócio violada (período fechado, intervalo inválido, duplicidade...) |

            ## Paginação

            Listagens aceitam `page` (a partir de 0), `size` e `sort=campo,asc|desc`, e devolvem
            `content`, `totalElements`, `totalPages`, `number` e `size`.
            """;

    /** Registra o schema ApiError e acrescenta o guia à descrição da API (uma única vez). */
    @Bean
    public OpenApiCustomizer apiErrorSchemaAndGuide() {
        return openApi -> {
            Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
            ModelConverters.getInstance().readAll(ApiError.class).forEach(components::addSchemas);
            openApi.setComponents(components);

            Info info = openApi.getInfo() != null ? openApi.getInfo() : new Info();
            String current = info.getDescription() != null ? info.getDescription() : "";
            if (!current.contains(MARKER)) {
                info.setDescription(current.isBlank() ? GUIDE : current + "\n\n" + GUIDE);
            }
            openApi.setInfo(info);
        };
    }

    /** Acrescenta a cada operação as respostas de erro que ela pode produzir, sem anotar controller por controller. */
    @Bean
    public OperationCustomizer standardErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses() != null ? operation.getResponses() : new ApiResponses();
            operation.setResponses(responses);

            boolean isAuth = AuthController.class.isAssignableFrom(handlerMethod.getBeanType());
            boolean hasPathParam = operation.getParameters() != null
                    && operation.getParameters().stream().anyMatch(p -> "path".equals(p.getIn()));
            boolean hasInput = operation.getRequestBody() != null
                    || (operation.getParameters() != null && !operation.getParameters().isEmpty());

            addIfAbsent(responses, "401", isAuth
                    ? "Credenciais inválidas, token de renovação inválido ou usuário inativo"
                    : "Não autenticado: token ausente, inválido ou expirado");
            if (!isAuth) {
                addIfAbsent(responses, "403", "Sem a permissão exigida por este endpoint");
            }
            if (hasInput) {
                addIfAbsent(responses, "400", "Requisição malformada ou campo inválido");
                addIfAbsent(responses, "422", "Regra de negócio violada");
            }
            if (hasPathParam) {
                addIfAbsent(responses, "404", "Não encontrado, ou pertence a outra filial");
            }
            return operation;
        };
    }

    private void addIfAbsent(ApiResponses responses, String status, String description) {
        if (responses.get(status) != null) return;   // respeita o que o controller já documentou
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError")))));
    }
}