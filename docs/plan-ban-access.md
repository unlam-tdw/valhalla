# Acceso a planes y baneos

El dueño no puede ser expulsado, baneado ni degradado por un editor. Un editor
tampoco puede eliminar el plan. Estas restricciones se validan en el servicio,
incluso cuando se envía una petición manual sin usar los botones de la interfaz.

El baneo impide volver a unirse mediante código o aceptar una invitación pendiente.
También bloquea el detalle, la vista pública, la lectura del itinerario y la
clonación. La visibilidad pública no habilita el acceso de una sesión baneada.
Los visitantes anónimos conservan el acceso habitual a los planes públicos.

En **Mis planes** se muestra un aviso persistente con el nombre de cada plan que
baneó al usuario y el texto «Fuiste baneado de este plan. No podés acceder ni
volver a unirte». El aviso no expone descripción, fechas, lugares, participantes,
código ni enlaces al detalle. Estos planes se excluyen de **Para vos** y del feed
público de la API utilizado por **Explorar**.

## Implementación

- `JpaPlanRepository.findByBannedUsersEmail` consulta los baneos persistidos.
  `PlanRepositoryImpl` adapta la consulta al puerto de dominio.
- `PlanService.getBannedPlans` devuelve únicamente registros `BannedPlan` con el
  nombre. `PlanController.listPlans` los entrega a la plantilla del listado.
- `PlanBannedException` distingue un baneo de un plan inexistente y conserva la
  jerarquía de `PlanNotFoundException`. `GlobalExceptionHandler` responde HTTP 403
  con una explicación, sin cargar el detalle en el modelo de la respuesta.
- `PlanServiceImpl` comprueba el baneo al consultar el detalle editable o legible,
  unirse, aceptar invitaciones y clonar. `PlanController.showPublicPlan` comprueba
  la identidad autenticada antes de construir el modelo público.
- `PlanPlaceServiceImpl.getItinerary` rechaza usuarios baneados. El manejador REST
  existente devuelve HTTP 404 sin datos del itinerario.
- Los listados públicos y la vista pública inicializan la colección de baneos
  dentro de la transacción para poder verificarla después en los controladores.
  `ForYouController` y `PlanRestController` filtran los planes baneados.

## Verificación

`PlanBanSecurityTest` utiliza seguridad MVC y persistencia reales para comprobar:

- Protección del dueño frente a expulsión, baneo, cambio de rol y eliminación del
  plan por parte de un editor.
- Bloqueo de detalle, vista pública, itinerario, ingreso por código y clonación,
  tanto con visibilidad pública como privada.
- Aviso persistente luego de guardar y recargar el baneo, sin enlace al detalle.
- Exclusión de los feeds y acceso conservado para el dueño y visitantes anónimos.
- Rechazo de una invitación pendiente aunque vuelva a existir después del baneo.

Las pruebas existentes de `PlanServiceImplTest` también cubren que el baneo impide
invitar nuevamente y que una expulsión permite volver a unirse.

Las comprobaciones de `PlanControllerIntegrationTest` usan la etiqueta vigente
«Expulsar del plan a» para verificar qué controles puede ver cada rol.

Validación realizada con `mvn test -Djacoco.gate.skip=true`: 471 pruebas aprobadas,
sin fallos, errores ni omisiones. También pasaron las comprobaciones de formato,
PMD y duplicación del ciclo Maven. El umbral de cobertura se omitió explícitamente;
no se ejecutaron pruebas de navegador.
