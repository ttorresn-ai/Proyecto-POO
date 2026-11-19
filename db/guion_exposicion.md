# GUION DE EXPOSICIÓN — NEXUS FINANCE (5 minutos)

> El apoyo visual es **el propio informe** (`informe_ieee.pdf`), que se abre en
> paralelo y se va recorriendo. Cada indicación `[INFORME]` dice a qué página
> saltar.
>
> Las páginas suman 15 y el recorrido es **lineal y en orden creciente**: nunca
> hay que volver atrás, solo avanzar.
>
> **Ensaya con cronómetro.** El texto hablado son **596 palabras**: unos 4:35
> a un ritmo cómodo de 130 palabras por minuto, y 4:58 al más lento razonable
> de 120. Si te pasas, lo primero que se cae es la segunda conclusión: deja las
> tres ideas y suelta el ejemplo del `SUM` y la comparación entre la jerarquía y
> la tabla plana.

---

## 0:00 – 0:45 · Introducción y dominio

`[INFORME pág. 1 — Portada]`

> Buenos días. Mi proyecto es **Nexus Finance**, un sistema de gestión de
> finanzas personales.
>
> Resuelve tres preguntas que casi nadie tiene resueltas: **¿cuánto tengo?, ¿en
> qué gasté? y ¿me estoy pasando del presupuesto?**

`[INFORME pág. 3 — Figura 1: la aplicación sobre datos reales]`

> Esta es la aplicación funcionando. Pero el objeto de este parcial no es el
> código: es **el modelo de datos** que la sostiene.

---

## 0:45 – 2:00 · Modelo ER y reglas de negocio

`[INFORME pág. 4 — Figura 2: diagrama ER completo]`

> El modelo tiene **diez entidades**. Voy a destacar los cinco elementos que
> exige la guía.

`[INFORME pág. 5 — Figura 3: la jerarquía; Cuadro I; Figura 4: el 1:1]`

> **La jerarquía.** TRANSACCIÓN es la superclase y se especializa en INGRESO y
> GASTO. Ambos comparten fecha, monto y descripción, y cada uno aporta algo
> propio: el ingreso, su origen; el gasto, su categoría. Sin esa jerarquía el
> modelo quedaría lleno de columnas vacías.
>
> **El 1:1.** USUARIOS y PERFILES: como máximo un perfil por usuario, resuelto
> con la clave primaria compartida.

`[INFORME pág. 6 — Figura 5: multivaluado; Figura 6: derivado]`

> **El multivaluado:** varios teléfonos por usuario, materializados como la tabla
> TELEFONOS.
>
> **El derivado:** el saldo total no se guarda, se calcula con una vista SQL, y
> así nunca puede quedar desincronizado.

`[INFORME pág. 7 — Cuadro II: reglas de conversión]`

> **El N:M** se resuelve con la tabla puente USUARIO_CUENTA, de clave primaria
> compuesta: piénsenlo en una cuenta familiar compartida.
>
> Y una última regla: no se admiten movimientos de más de **90 días**. Es la que
> no se puede expresar con claves, y ahorita vemos por qué.

---

## 2:00 – 3:15 · Modelo relacional y script SQL

`[INFORME pág. 7 — Cuadro III: especificación de las diez tablas]`

> El diagrama se convierte en tablas. Esta es la especificación completa: clave
> primaria, claves foráneas y restricciones de las diez.

`[INFORME pág. 8 — Listado 2: la jerarquía en SQL]`

> Lo más importante del script está aquí. Fíjense en una cosa: **INGRESOS** y
> **GASTOS** tienen como clave primaria la misma clave foránea que apunta a
> TRANSACCIONES. Eso garantiza la cardinalidad 0..1: un movimiento es un ingreso
> o un gasto, nunca los dos.

`[INFORME pág. 9 — Listado 4: el error 3814; Listado 5: el trigger]`

> Y ahora la regla de los 90 días. ¿Por qué no un `CHECK`? Porque compara contra
> el día de hoy, y MySQL prohíbe las funciones no deterministas dentro de un
> `CHECK`: el motor devuelve el error 3814, que está en pantalla. Tampoco es una
> foreign key, porque no compara claves. La respuesta es un **trigger**.

`[INFORME pág. 10 — Figura 8: ejecución del script]`

> Y el script corre limpio: diez tablas, tres vistas, un disparador.

---

## 3:15 – 4:15 · Ingeniería inversa y verificación

`[INFORME pág. 10 — Figura 7: el motor rechaza el movimiento de 200 días]`

> Primero, la prueba de que la regla funciona: inserto un movimiento de hace 200
> días y el motor lo rechaza.

`[INFORME pág. 11 — Cuadro IV: equivalencia; Figura 9: suite de pruebas]`

> Ahora la verificación. Diez entidades en mi diseño, diez tablas en el EER: una
> a una. Y la suite: **29 pruebas en verde**, doce contra la base real.

`[INFORME pág. 12 — Figura 10: diagrama EER generado por ingeniería inversa]`

> Este es el diagrama que genera MySQL Workbench a partir de la base ya
> construida.

`[INFORME pág. 13 — Figura 11: la jerarquía; Figura 12: el N:M]`

> Miren estas tres tablas: la jerarquía **sí se ve** en el EER, que es lo que no
> habría pasado con una sola tabla y una columna de tipo. Y USUARIO_CUENTA
> aparece con sus dos claves foráneas y la primaria compuesta.

`[INFORME pág. 14 — Figura 13: el 1:1; Figura 14: las tres vistas]`

> PERFILES con una sola columna que es clave primaria y foránea: la firma del
> uno a uno. Y las tres vistas sí aparecen, aunque en amarillo y sin columnas.

---

## 4:15 – 5:00 · Conclusiones y cierre

`[INFORME pág. 15 — Conclusiones]`

> Para cerrar, tres aprendizajes.
>
> **Primero:** el modelado obliga a decidir antes de programar. La jerarquía
> entre INGRESO y GASTO la entendí de verdad cuando tuve que decidir en qué
> tabla viviría cada atributo. El código vino después, y fue mecánico.
>
> **Segundo:** hay restricciones que el modelo relacional no alcanza a expresar,
> y reconocerlo también es diseñar. La regla de los 90 días no es un `CHECK` ni
> una foreign key: es un trigger.
>
> **Tercero:** la ingeniería inversa no es un trámite, es la forma de confirmar
> que lo que construí es lo que diseñé.
>
> Muchas gracias. ¿Preguntas?

---

## Notas de apoyo

> Nada de esta sección se dice en voz alta.

### Frases de riesgo y cómo evitarlas

| Si pasa esto... | Di esto |
|---|---|
| Te preguntan por qué `cuentas.propietario_id` si existe `usuario_cuenta` | "No es redundante: uno responde a propiedad y otro a permiso. Permite exigir exactamente un PROPIETARIO por cuenta." |
| Te preguntan por `balance_actual` si es derivado | "Es un derivado **materializado**. Se mantiene dentro de la misma transacción que inserta el movimiento, y hay un `CHECK` que impide el sobregiro. Si eliminara la columna, cada pantalla pagaría un `SUM`." |
| Te preguntan cómo se garantiza que un movimiento tenga subclase | "Con la clave primaria compartida se garantiza que haya **como máximo** una. La cardinalidad mínima la garantiza el DAO al insertar ambas filas en la misma transacción." |
| Te preguntan por las vistas que no se ven en el EER | "Sí aparecen, pero Workbench las dibuja como objetos amarillos sin columnas. El trigger, en cambio, no llega a ser objeto del lienzo." |
| Te preguntan por el tiempo | "Estoy en el minuto 3:40." *(mira el cronómetro, no improvises)* |

### Verificación previa a exponer

- [ ] El PDF abierto en la **página 1**, con la vista de dos páginas activa
- [ ] El zoom al **100 %**: a menor tamaño las figuras del EER no se leen
- [ ] `informe_ieee.pdf` y el cronómetro en la misma pantalla, sin que se
      superpongan
- [ ] Ensayado al menos una vez con cronómetro, sin pasar de 5:00
- [ ] Respuesta preparada para las 5 frases de riesgo de arriba

### Mapa de páginas (referencia rápida)

| Página | Contenido |
|---|---|
| 1 | Portada |
| 3 | Figura 1 — la aplicación |
| 4 | Figura 2 — ER completo |
| 5 | Figura 3 jerarquía · Cuadro I · Figura 4 (1:1) |
| 6 | Figura 5 multivaluado · Figura 6 derivado |
| 7 | Cuadro II conversiones · Cuadro III diez tablas |
| 8 | Listados 1–3 (vistas, jerarquía, tabla puente) |
| 9 | Listados 4–5 (error 3814, trigger) |
| 10 | Figura 7 rechazo del motor · Figura 8 ejecución |
| 11 | Cuadro IV equivalencia · Figura 9 pruebas |
| 12 | Figura 10 — EER completo |
| 13 | Figura 11 jerarquía · Figura 12 N:M |
| 14 | Figura 13 (1:1) · Figura 14 vistas |
| 15 | Conclusiones |
