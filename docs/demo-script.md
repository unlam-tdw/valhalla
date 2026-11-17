# Guion de la demo final (12 minutos, 6 personas)

Recorrido de punta a punta: primero la experiencia de usuario, al final el admin.
Los datos salen de los seeders (`.\scripts\gate.ps1 seed`, ver `docs/setup.md`).

> Todos los pasos de este guion se recorrieron con un E2E contra una base sembrada.
> Los textos de botones son los que renderiza la app.

## Reparto

| Min | Persona | Bloque |
| --- | --- | --- |
| 0:00–2:00 | 1 | Entrada y descubrir |
| 2:00–4:00 | 2 | Armar un plan |
| 4:00–6:00 | 3 | Colaborar |
| 6:00–8:00 | 4 | Social y perfil |
| 8:00–10:00 | 5 | Admin |
| 10:00–12:00 | 6 | Mobile, cierre y preguntas |

Cada persona tiene 2 minutos y cierra con una frase de traspaso: *"Hasta acá vimos X. Te paso a Y, que te muestra Z."*

**Dependencias:**

- La **Persona 3** va antes que la **4**: la solicitud de Tomás y la invitación de Lucía tienen que estar resueltas antes de que la 4 muestre las notificaciones.
- La **Persona 2** usa "Finde en Palermo". No la borres ni la modifiques en otro bloque.
- La **Persona 5** trabaja en la ventana de admin y no toca la sesión demo.

## Antes de empezar (no cuenta como tiempo)

1. `docker compose up --build` y esperá a que responda `http://localhost:8080`.
2. Una ventana de **incógnito** para la sesión demo, en **una sola compu**. Cada persona toma el teclado en su turno.
3. Una ventana **normal** para el admin, a cargo de la Persona 5.
4. Si hiciste aceptaciones, bans o calificaciones en un ensayo, corré `.\scripts\gate.ps1 seed` de nuevo. Esos cambios no se revierten solos.
5. **Precalentá las dos ventanas** abriendo `/` una vez: la landing compila Tailwind en el navegador y baja las tipografías de Google, así que la primera pintura en frío puede demorar. Después del precalentado, arrancá la demo.

| Cuenta | Usuario | Clave | Uso |
| --- | --- | --- | --- |
| Demo | `demo@planit.demo` | `password` | Recorrido de usuario (Personas 1 a 4 y 6) |
| Admin | `test@unlam.edu.ar` | `password` | Bloque de admin (Persona 5) |
| Inactiva | `inactivo@planit.demo` | `password` | Solo para mostrar una cuenta desactivada |
| Fantasmas | `lucia.peralta@planit.demo`, `mateo.rios@planit.demo`, etc. | `fantasma` | Solo si alguien pregunta |

---

## Persona 1 · Entrada y descubrir (0:00–2:00)

1. Mostrá la landing `/`. Decí: *"PlanIt sirve para armar planes de salida con lugares reales de la ciudad."*
2. Tocá **Iniciar sesión**, debajo del CTA del hero, y entrá con `demo@planit.demo` / `password`.
3. De vuelta en la landing, ya con sesión el CTA principal es **Explorar lugares**: tocá y caés en `/explore`. (Sin sesión ese lugar lo ocupa *Crear cuenta gratis*, y "Explorar lugares" también vive en el pie.)
4. **`/explore`**: mapa con lugares del catálogo. Mostrá un filtro o la búsqueda, y abrí la ficha de **MALBA**. Señalá el link a Maps.

**Traspaso:** *"Te paso a Armar un plan, que vamos a ver un itinerario completo."*

## Persona 2 · Armar un plan (2:00–4:00)

1. **`/plans`** → en la fila de **"Finde en Palermo"** tocá **Ver detalle**.
2. Mostrá el itinerario: seis paradas repartidas en dos días, y **El Sanjuanino** cae el **día 2**. Decí: *"El horario se calcula solo, no lo escribimos a mano."*
3. Arrastrá una parada **desde su fila** para reordenarla: la fila entera es el origen del arrastre, y la manija de seis puntos (a la izquierda de cada parada) sigue existiendo para el teclado. El horario se recalcula en el acto. Decí: *"Con teclado también funciona: Enter, flechas y Enter."*
4. Mostrá el bloque **Presupuesto**: **Tu presupuesto: ARS 30.000,00 – 45.000,00 / persona**, con la nota *Entradas y comida*. Debajo, **Costo estimado: ARS 37.000,00 / persona**, que la app suma sola con el precio de cada categoría. Decí: *"Si el recorrido se pasa del tope, acá aparece en rojo cuánto te pasás."*
5. Mostrá el botón **Imprimir** (barra superior del plan): abre el diálogo del navegador, donde se elige *Guardar como PDF*. Decí: *"El plan se puede imprimir o guardar en PDF."*
6. **Opcional, si sobra tiempo (20 s)**: en el feed (**Planes públicos**) tocá **Sorprendeme**, poné una fecha de hoy o posterior, y la app arma un plan privado con paradas variadas. En su detalle aparece **Otra sugerencia** para regenerarlo. No es obligatorio y no toca a "Finde en Palermo".

**Traspaso:** *"Te paso a Colaborar, que muestra cómo trabajamos en equipo."*

## Persona 3 · Colaborar (4:00–6:00)

Plan: **"Asado en casa"** (privado, dueño: demo). Entrá desde **`/plans`** → **Ver detalle**.

1. En la card **Participantes** tocá **Ver**: **Lucía** figura como editora y **Mateo** como participante.
2. En la card **Solicitudes** (dice "Tenés 1 solicitud para responder") tocá **Ver**: está **Tomás** con la marca **Nueva**. Tocá **Aceptar**.
3. En la misma card de participantes, al pie, tocá **Ver miembros baneados**: está **Sofía**. Tocá **Quitar baneo a Sofía**. Decí: *"Solo el dueño y los editores pueden banear o levantar un baneo."*
4. Volvé a **`/plans`**: en la card **Invitaciones pendientes**, tocá **Aceptar** en **Noche de cine** (te invitó Lucía). El plan aparece en tu lista y la notificación de Lucía queda saldada.

**Traspaso:** *"Te paso a Social, que muestra lo que pasa entre usuarios."*

## Persona 4 · Social y perfil (6:00–8:00)

1. **Planes públicos** en el navbar (`/for-you`) y después la pestaña **Siguiendo**. Decí: *"Acá aparecen los planes de la gente que sigo."*
2. En el buscador del navbar escribí `Camila` y tocá **Seguir** en su fila.
3. Abrí **Cerveza artesanal y show en vivo** (`GHOST005`) desde el feed y **tocá una estrella** para calificarlo (con JavaScript no hay botón "Calificar": se clickea la estrella). Volvé y guardalo con el ícono de marcador de la card.
4. En tu perfil mostrá **Guardados** (ya tenés **Asado en el parque** y **Parrillada de fin de mes**) y **Mis calificaciones** (ya tenés **Sábado de museos**, 4★, y **Café, libros y conversación**, 5★). Sumá la fila nueva.
5. Click en la **campanita** y abrí **`/notifications`**. Mostrá la agrupación: *"Lucía y 2 más se unieron a tu plan"*, *"Lucía y Mateo te empezaron a seguir"*, la invitación de Lucía y la solicitud de Tomás.

**Traspaso:** *"Te paso a Admin, que muestra el otro lado de la plataforma."*

## Persona 5 · Admin (8:00–10:00)

Ventana normal, con `test@unlam.edu.ar` / `password`. El navbar de admin está en español: **Usuarios / Lugares / Planes**.

1. **`/admin/home`** como punto de partida.
2. **`/admin/users`**: mostrá que `inactivo@planit.demo` está desactivado. Activalo y desactivalo de nuevo. No uses esa cuenta para nada más.
3. **`/admin/places`**: el ABM de lugares. Editá uno para mostrar el formulario.
4. **`/admin/places/import`**: el mapa de Palermo ya tiene los **751 POIs** cargados (238 restaurantes, 178 de ropa, 164 cafés, 109 bares, 31 farmacias, 21 de noche, 10 teatros). Filtrá por categoría y señalá los pins. Decí: *"Los puntos vienen de OpenStreetMap, guardados en la base; el mapa los lee de ahí."* **No toques "Cargar puntos de esta zona" ni "Actualizar puntos de esta zona"**: consultan Overpass y dependen de internet.
5. **`/admin/plans`**: desactivá un plan (por ejemplo `GHOST006`) y volvé a activarlo.

**Traspaso:** *"Te paso a Mobile y cierre."*

## Persona 6 · Mobile, cierre y preguntas (10:00–12:00)

1. Abrí la app en el celular, o achicá la ventana / usá F12 → modo dispositivo. Si abrís otro navegador, **volvé a loguearte**: la sesión no se comparte.
2. Mostrá **`/explore`** en mobile: el mapa con las pestañas **Places / Plans** abajo, y el botón **hamburguesa** (≡) del navbar con el menú (**Planes públicos**, **Explorar lugares**, **Mis planes**, perfil, notificaciones). En mobile el navbar **no queda fijo**: si la página está scrolleada, volvé arriba para verlo.
3. Cierre en tres frases: *armá un plan con lugares reales → invitá y moderá a tu gente → guardá, calificá y seguí planes de otros.*
4. Preguntas. No uses este tiempo para features nuevas. Tené el video de respaldo listo.

**Controlá el reloj**: la Persona 6 avisa si alguien se pasa de los 2 minutos.

---

## Riesgos y cómo evitarlos

- **No resetees la base en medio de la demo.** Si necesitás volver al estado inicial, corré `.\scripts\gate.ps1 seed` antes de la demo, no durante.
- **La demo deja cambios.** Aceptar la solicitud, levantar el baneo, aceptar la invitación, seguir a Camila, calificar `GHOST005` y reordenar paradas **no se revierten solos**. Después de cada ensayo, `seed`.
- **Usá dos ventanas separadas** (incógnito para el demo, normal para el admin), así no se cierra una sesión al entrar con la otra.
- **Si Overpass o internet fallan**, no pasa nada: el mapa de importación lee la base, no la red.
- **Ensayá cada bloque con su dueño** y cronometrado.
