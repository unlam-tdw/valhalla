# Guion de la demo final (12 minutos, 6 oradores y un operador)

Recorrido de punta a punta: primero la experiencia de usuario, al final el admin.
Los datos salen de los seeders (`.\scripts\gate.ps1 seed`, ver `docs/setup.md`).

> Los textos de botones y las etiquetas de este guion son los que renderiza la app.

## Reparto

| Min | Orador | Bloque | Señal de entrada |
| --- | --- | --- | --- |
| 0:00–2:00 | 1 | Entrada y descubrir | `landing` |
| 2:00–4:00 | 2 | Armar un plan | `armar` |
| 4:00–6:00 | 3 | Colaborar | `colaborar` |
| 6:00–8:00 | 4 | Social y perfil | `social` |
| 8:00–10:00 | 5 | Admin | `admin` |
| 10:00–12:00 | 6 | Mobile, cierre y preguntas | `mobile` |

**Operador:** una sola persona ejecuta los seis bloques y no habla.

- Al arrancar su bloque, cada orador dice su **señal**: es la orden de salto para el
  operador y no forma parte del discurso.
- El orador **describe lo que se ve** (*"estamos en el itinerario de dos días"*) y nunca
  dirige al operador (*"tocá Ver detalle"*): el público mira la pantalla, no el teclado.
- El operador va **15–20 segundos adelante**. Cuando arranca un bloque, la pantalla de ese
  bloque ya está abierta.
- Cada orador cierra con una frase de traspaso: *"Hasta acá vimos X. Te paso a Y, que te
  muestra Z."* La frase cambia el dueño de la voz; el teclado no se mueve.

**Dependencias:**

- El bloque **3 va antes que el 4**: la solicitud de Tomás y la invitación de Lucía tienen
  que estar resueltas antes de que el 4 muestre las notificaciones.
- El bloque **2 usa "Finde en Palermo"**. No la borres ni la modifiques en otro bloque.
- El bloque **5** trabaja en la ventana de admin y no toca la sesión demo.

## Antes de empezar (no cuenta como tiempo)

1. `docker compose up --build` y esperá a que responda `http://localhost:8080`.
2. `.\scripts\gate.ps1 seed`: resetea la base y siembra los datos de la demo.
3. Ventana de **incógnito** para la sesión demo, que arranca **sin sesión**: el login se
   hace en vivo en el bloque 1. Ventana **normal** con el admin ya logueado, para el bloque 5.
4. **Precalentá las dos ventanas** abriendo `/` una vez: la landing compila Tailwind en el
   navegador y baja las tipografías de Google, así que la primera pintura en frío puede
   demorar. Después del precalentado, arrancá la demo.
5. Video de respaldo listo en otra pestaña.

| Cuenta | Usuario | Clave | Uso |
| --- | --- | --- | --- |
| Demo | `demo@planit.demo` | `password` | Bloques 1 a 4 y 6 |
| Admin | `test@unlam.edu.ar` | `password` | Bloque 5 |
| Inactiva | `inactivo@planit.demo` | `password` | Solo para mostrar una cuenta desactivada |
| Fantasmas | `lucia.peralta@planit.demo`, `mateo.rios@planit.demo`, etc. | `fantasma` | Solo si alguien pregunta |

---

## Bloque 1 · Entrada y descubrir (0:00–2:00) · Orador 1

1. La landing `/` ya está abierta. Decí: *"PlanIt sirve para armar planes de salida con
   lugares reales de la ciudad."*
2. El operador toca **Iniciar sesión**, debajo del CTA del hero, y entra con
   `demo@planit.demo` / `password`.
3. De vuelta en la landing, ya con sesión el CTA principal es **Explorar lugares**: el
   operador lo abre. (Sin sesión ese lugar lo ocupa *Crear cuenta gratis*, y "Explorar
   lugares" también vive en el pie.)
4. **`/explore`**: mapa con lugares del catálogo. El operador aplica un filtro o usa la
   búsqueda y abre la ficha de **MALBA**. Señalá el link a Maps.

**Traspaso:** *"Te paso a Armar un plan, que vamos a ver un itinerario completo."*

## Bloque 2 · Armar un plan (2:00–4:00) · Orador 2

1. El operador abre **`/plans`** y entra en **Ver detalle** de **"Finde en Palermo"**.
2. El itinerario muestra seis paradas repartidas en dos días, y **El Sanjuanino** cae el
   **día 2**. Decí: *"El horario se calcula solo, no lo escribimos a mano."*
3. El operador arrastra una parada **desde su fila** para reordenarla: la fila entera es el
   origen del arrastre, y la manija de seis puntos (a la izquierda de cada parada) sigue
   existiendo para el teclado. El horario se recalcula en el acto. Decí: *"Con teclado
   también funciona: Enter, flechas y Enter."*
4. El bloque **Presupuesto** muestra **Tu presupuesto: ARS 30.000,00 – 45.000,00 /
   persona**, con la nota *Entradas y comida*. Debajo, **Costo estimado: ARS 37.000,00 /
   persona**, que la app suma sola con el precio de cada categoría. Decí: *"Si el recorrido
   se pasa del tope, acá aparece en rojo cuánto te pasás."*
5. El botón **Imprimir** (barra superior del plan) abre el diálogo del navegador, donde se
   elige *Guardar como PDF*. No hace falta guardar: alcanza con mostrar el diálogo. Decí:
   *"El plan se puede imprimir o guardar en PDF."*
6. **Opcional, si sobra tiempo (20 s)**: en el feed (**Planes públicos**) está
   **Sorprendeme**: con una fecha de hoy o posterior arma un plan privado con paradas
   variadas, y en su detalle aparece **Otra sugerencia** para regenerarlo. No es
   obligatorio y no toca a "Finde en Palermo".

**Traspaso:** *"Te paso a Colaborar, que muestra cómo trabajamos en equipo."*

## Bloque 3 · Colaborar (4:00–6:00) · Orador 3

Plan: **"Asado en casa"** (privado, dueño: demo). El operador lo abre desde **`/plans`** →
**Ver detalle**.

1. En la card **Participantes** toca **Ver**: **Lucía** figura como editora y **Mateo**
   como participante.
2. En la card **Solicitudes** (dice "Tenés 1 solicitud para responder") toca **Ver**: está
   **Tomás** con la marca **Nueva**. **Aceptar**.
3. En la misma card de participantes, al pie, **Ver miembros baneados**: está **Sofía**;
   toca **Quitar baneo a Sofía**. Decí: *"Solo el dueño y los editores pueden banear o
   levantar un baneo."*
4. De vuelta en **`/plans`**, en la card **Invitaciones pendientes**, toca **Aceptar** en
   **Noche de cine** (te invitó Lucía). El plan aparece en la lista y la notificación de
   Lucía queda saldada.

**Traspaso:** *"Te paso a Social, que muestra lo que pasa entre usuarios."*

## Bloque 4 · Social y perfil (6:00–8:00) · Orador 4

1. El operador abre **Planes públicos** en el navbar (`/for-you`) y después la pestaña
   **Siguiendo**. Decí: *"Acá aparecen los planes de la gente que sigo."*
2. Escribe `Camila` en el buscador del navbar y toca **Seguir** en su fila.
3. Abre **Cerveza artesanal y show en vivo** (`GHOST005`) desde el feed y **toca una
   estrella** para calificarlo (con JavaScript no hay botón "Calificar": se clickea la
   estrella). Vuelve y lo guarda con el ícono de marcador de la card.
4. En el perfil muestra **Guardados** (ya están **Asado en el parque** y **Parrillada de fin
   de mes**) y **Mis calificaciones** (ya están **Sábado de museos**, 4★, y **Café, libros
   y conversación**, 5★). La calificación nueva se suma a la lista.
5. Toca la **campanita** y abre **`/notifications`**. Decí: *"Lucía y 2 más se unieron a tu
   plan"*, *"Lucía y Mateo te empezaron a seguir"*, la invitación de Lucía y la solicitud de
   Tomás.

**Traspaso:** *"Te paso a Admin, que muestra el otro lado de la plataforma."*

## Bloque 5 · Admin (8:00–10:00) · Orador 5

El operador cambia a la **ventana normal**, ya logueada con `test@unlam.edu.ar`. El navbar
de admin está en español: **Usuarios / Lugares / Planes**. Avisá el cambio de ventana antes
de que ocurra, para que el salto no quede en silencio.

1. **`/admin/home`** como punto de partida.
2. **`/admin/users`**: muestra que `inactivo@planit.demo` está desactivado; lo activa y lo
   desactiva de nuevo. No usa esa cuenta para nada más.
3. **`/admin/places`**: el ABM de lugares. Edita uno para mostrar el formulario.
4. **`/admin/places/import`**: el mapa de Palermo ya tiene los **751 POIs** cargados (238
   restaurantes, 178 de ropa, 164 cafés, 109 bares, 31 farmacias, 21 de noche, 10
   teatros). Filtra por categoría y señala los pins. Decí: *"Los puntos vienen de
   OpenStreetMap, guardados en la base; el mapa los lee de ahí."* **No toques "Cargar
   puntos de esta zona" ni "Actualizar puntos de esta zona"**: consultan Overpass y
   dependen de internet.
5. **`/admin/plans`**: desactiva un plan (por ejemplo `GHOST006`) y lo vuelve a activar.

**Traspaso:** *"Te paso a Mobile y cierre."*

## Bloque 6 · Mobile, cierre y preguntas (10:00–12:00) · Orador 6

1. El operador vuelve a la **ventana demo** y la pasa a modo dispositivo (F12). Si alguien
   abre otro navegador, **volvé a loguearte**: la sesión no se comparte.
2. El operador muestra **`/explore`** en mobile: el mapa con las pestañas **Places / Plans**
   abajo, y el botón **hamburguesa** (≡) del navbar con el menú (**Planes públicos**,
   **Explorar lugares**, **Mis planes**, perfil, notificaciones). En mobile el navbar **no
   queda fijo**: si la página está scrolleada, hay que volver arriba para verlo.
3. Cierre en tres frases: *armá un plan con lugares reales → invitá y moderá a tu gente →
   guardá, calificá y seguí planes de otros.*
4. Preguntas. No uses este tiempo para features nuevas. Tené el video de respaldo listo.

**Controlá el reloj**: el operador lleva un cronómetro visible y el Orador 6 avisa si un
bloque se pasa de los 2 minutos.

---

## Riesgos y cómo evitarlos

- **No resetees la base en medio de la demo.** Si necesitás volver al estado inicial, corré
  `.\scripts\gate.ps1 seed` antes de la demo, no durante.
- **La demo deja cambios.** Aceptar la solicitud, levantar el baneo, aceptar la invitación,
  seguir a Camila, calificar `GHOST005`, reordenar paradas y desactivar `GHOST006` **no se
  revierten solos**. Después de cada ensayo, `seed`.
- **Aceptar, banear, seguir, calificar y desactivar son de un solo intento.** Si una falla,
  se saltea y se sigue: un bloque roto no hunde los otros cinco.
- **El operador no habla.** Si algo sale mal, no lo explica en vivo: sigue con el beat
  siguiente y el orador cubre con contexto.
- **Usá dos ventanas separadas** (incógnito para el demo, normal para el admin), así no se
  cierra una sesión al entrar con la otra.
- **Si Overpass o internet fallan**, no pasa nada: el mapa de importación lee la base, no la
  red. Solo dependen de red las tipografías y Overpass.

## Ensayo

1. **Solo voz**: cada orador, cronometrado y sin app. Si no entra en 2 minutos sin pantalla,
   no entra con pantalla.
2. **Coordinación**: los seis oradores hablando y el operador ejecutando, de corrido. `seed`
   antes de cada pasada.
3. **Técnico**: el operador solo, sin voz, las seis pantallas en menos de 4 minutos. Si
   tarda más, el guion tiene demasiados clicks y hay que recortar.
4. **General**: una sola pasada completa con el reloj visible. `seed` al final.
