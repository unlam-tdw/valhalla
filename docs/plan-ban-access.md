# Acceso a planes y baneos

El dueño no puede ser expulsado, baneado ni degradado por un editor. Un editor
tampoco puede eliminar el plan. Estas restricciones se validan en el servicio,
incluso cuando se envía una petición manual sin usar los botones de la interfaz.

El baneo impide volver a unirse mediante código o aceptar una invitación pendiente.
También bloquea el detalle, la vista pública, la lectura del itinerario y la
clonación. La visibilidad pública no habilita el acceso de una sesión baneada.
Los visitantes anónimos conservan el acceso habitual a los planes públicos.

En **Mis planes**, debajo del aside de invitaciones, se muestra un segundo aside
con el desplegable **Planes con acceso restringido**, cerrado por defecto y con
colores neutros. El resumen muestra la cantidad; al abrirlo se ven los nombres
de los planes y la explicación de que el acceso está restringido por un baneo
activo. No ocupa el encabezado ni el contenido principal. Si no hay baneos, el
aside no aparece. El aviso no expone descripción, fechas, lugares, participantes,
código ni enlaces al detalle. Estos planes se excluyen de **Para vos** y del feed
público de la API utilizado por **Explorar**.

## Consultar y quitar baneos

El detalle del plan incluye un botón **Más opciones**, identificado con tres
puntos, junto a las acciones de clonar y eliminar. Su desplegable contiene
**Ver miembros baneados**, que abre un modal con la cantidad, nombres, correos
y un botón **Quitar baneo** por persona. Si no hay baneos, muestra un mensaje
indicándolo. La lista ya no ocupa espacio dentro del recorrido o los participantes.

El menú utiliza el estado `planOptionsOpen` de la aplicación Vue existente. Se
cierra al perder el foco o presionar Escape. Su contenido permanece oculto antes
de montar Vue mediante `v-cloak`. El modal utiliza `dialog.showModal()`, con fondo
atenuado, desplazamiento para listas largas y cierre mediante Escape o el botón
de cerrar. Al cerrarse devuelve el foco a **Más opciones**. El desplegable de
**Mis planes** utiliza `details`/`summary` nativo y no necesita JavaScript.

Solo el dueño y los editores pueden consultar esa lista y quitar cualquier baneo
del plan, incluso el de una persona que antes era editora. Los participantes,
visitantes y usuarios baneados no tienen esos permisos. La lista no se incorpora
al modelo ni al HTML de sus vistas, incluida la vista pública.

Quitar el baneo permite volver a unirse por código o recibir una nueva invitación.
No reincorpora automáticamente al usuario ni restaura su rol anterior. En un plan
privado, debe volver a unirse antes de poder consultar el detalle. El aviso de
baneo en **Mis planes** desaparece cuando se vuelve a cargar esa vista.

`PlanService.getBannedParticipants` exige acceso editable y devuelve registros
`BannedParticipant` con correo, nombre y apellido ordenados por correo, sin exponer
entidades completas de usuario. `PlanController.showPlan` consulta esa lista solo
para el dueño y los editores. La lectura se realiza dentro de una transacción.

`POST /plans/{id}/participants/unban` recibe `participantEmail` y requiere sesión y
token CSRF. `PlanServiceImpl.unbanParticipant` verifica permisos mediante
`getEditablePlan`, elimina únicamente el baneo correspondiente a ese plan y guarda
el cambio. Una persona que no está baneada se trata como recurso inexistente.
El controlador redirige al detalle y muestra un mensaje de confirmación.

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
- Listado de baneados visible solo para el dueño y editores, sin filtrar nombres o
  correos a participantes y visitantes.
- Quitar baneos como dueño o editor, persistir el cambio y volver a unirse mediante
  código sin recuperar el rol de editor anterior.
- Protección CSRF y rechazo de intentos de quitar baneos sin permisos, desde otro
  plan o para una persona que no está baneada.
- Envío y aceptación de una nueva invitación después de quitar el baneo.

Las pruebas existentes de `PlanServiceImplTest` también cubren que el baneo impide
invitar nuevamente y que una expulsión permite volver a unirse.

Las comprobaciones de `PlanControllerIntegrationTest` usan la etiqueta vigente
«Expulsar del plan a» para verificar qué controles puede ver cada rol.

Validación realizada con `mvn test -Djacoco.gate.skip=true`: 476 pruebas aprobadas,
sin fallos, errores ni omisiones. También pasaron las comprobaciones de formato,
PMD y duplicación del ciclo Maven. El umbral de cobertura se omitió explícitamente;
no se ejecutaron pruebas de navegador.
