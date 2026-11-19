"""
NEXUS FINANCE — Video de exposicion (5 minutos exactos)
Generado con Manim Community 0.22

Estructura cronometrada segun la guia del parcial:
    0:00 - 0:45  Introduccion y dominio
    0:45 - 2:00  Modelo ER y reglas de negocio
    2:00 - 3:15  Modelo Relacional y Script SQL
    3:15 - 4:15  Ingenieria inversa y verificacion
    4:15 - 5:00  Conclusiones y cierre

Cada seccion declara su presupuesto y el cronometro verifica que el contenido
no lo exceda; el tiempo restante se absorbe con una pausa final.
"""

from manim import *

# ---------------------------------------------------------------------------
#  Paleta
# ---------------------------------------------------------------------------
FONDO      = "#05050a"
FONDO_ALT  = "#0a0f1e"
CIAN       = "#00f2fe"
VERDE      = "#13C784"
ROJO       = "#ff3366"
AMBAR      = "#ffa500"
MORADO     = "#9b59ff"
GRIS       = "#a2b0b8"
GRIS_OSC   = "#666688"
FUENTE     = "Consolas"

TOTAL_VIDEO = 300.0  # 5 minutos


# ---------------------------------------------------------------------------
#  Cronometro: controla que cada seccion respete su presupuesto
# ---------------------------------------------------------------------------
class Cronometro:
    def __init__(self):
        self.t = 0.0

    def anotar(self, duracion):
        self.t += duracion

    def restante(self, presupuesto):
        return presupuesto - self.t


# ---------------------------------------------------------------------------
#  Escena base cyberpunk
# ---------------------------------------------------------------------------
class CyberEscena(Scene):

    PRESUPUESTO = 0.0
    NOMBRE = ""
    OFFSET = 0.0          # Segundo inicial de la sección dentro del video
    ORDEN = 0

    def setup(self):
        self.camera.background_color = FONDO
        self.crono = Cronometro()
        self._offset = self.OFFSET

        # Cuadrícula de fondo muy tenue
        rejilla = VGroup()
        for i in range(-6, 7):
            linea = Line(
                [i * 1.6, -4, 0], [i * 1.6, 4, 0],
                stroke_color=CIAN, stroke_width=0.5, stroke_opacity=0.06
            )
            rejilla.add(linea)
        for i in range(-3, 4):
            linea = Line(
                [-8, i * 1.2, 0], [8, i * 1.2, 0],
                stroke_color=GRIS, stroke_width=0.5, stroke_opacity=0.04
            )
            rejilla.add(linea)
        self.add(rejilla)
        self.rejilla = rejilla

        self._ambiente()
        self._construir_pie()

    def _ambiente(self):
        """
        Movimiento ambiental continuo.

        Sin esto, las pausas entre fragmentos quedaban en imagen congelada:
        el video era correcto en duracion pero estatico en el 95% del tiempo.
        Una linea de barrido y un pulso lento mantienen la pantalla viva y
        evocan el HUD de la aplicacion, sin distraer del contenido.
        """
        # Linea de barrido que desciende lentamente y vuelve a empezar.
        self.scanline = always_redraw(self._scanline)
        # Dos puntos que laten, como indicadores de sistema activos.
        self.pulso_izq = always_redraw(lambda: self._latido(LEFT * 6.6 + UP * 3.25, CIAN))
        self.pulso_der = always_redraw(lambda: self._latido(RIGHT * 6.6 + UP * 3.25, VERDE))
        self.add(self.scanline, self.pulso_izq, self.pulso_der)

    def _scanline(self):
        # Recorre de arriba abajo en un ciclo de 7 segundos.
        fase = (self.time % 7.0) / 7.0
        y = 3.2 - 6.4 * fase
        return Line(
            [-8, y, 0], [8, y, 0],
            stroke_color=CIAN, stroke_width=1.6, stroke_opacity=0.10
        )

    def _latido(self, posicion, color):
        # Ciclo de 2.4 segundos: crece y se apaga.
        fase = (self.time % 2.4) / 2.4
        radio = 0.05 + 0.05 * abs(fase - 0.5) * 2
        opacidad = 0.25 + 0.35 * (1 - abs(fase - 0.5) * 2)
        return Dot(point=posicion, radius=radio,
                   color=color, fill_opacity=opacidad, stroke_opacity=0)

    def _construir_pie(self):
        # Etiqueta de seccion (arriba izquierda)
        self.etiqueta_seccion = Text(
            self.NOMBRE, font=FUENTE, font_size=17, color=GRIS_OSC
        ).to_corner(UL, buff=0.35)
        self.add(self.etiqueta_seccion)

        # Barra de fondo del progreso
        self._ancho_barra = 13.6
        self.barra_fondo = Line(
            [-self._ancho_barra / 2, -3.42, 0], [self._ancho_barra / 2, -3.42, 0],
            stroke_color=GRIS, stroke_width=3, stroke_opacity=0.18
        )
        self.add(self.barra_fondo)

        # Reloj y barra se redibujan cada cuadro con el tiempo real de la escena,
        # de modo que avanzan de forma continua también durante las pausas largas.
        self.marca_tiempo = always_redraw(self._reloj)
        self.barra_progreso = always_redraw(self._barra)
        self.add(self.marca_tiempo, self.barra_progreso)

        # Nombre del proyecto (abajo izquierda)
        self.marca_proyecto = Text(
            "NEXUS FINANCE // CORE_SYSTEM", font=FUENTE, font_size=11, color="#2a3550"
        ).next_to(self.barra_fondo, DOWN, buff=0.12).to_edge(LEFT, buff=0.4)
        self.add(self.marca_proyecto)

    def _segundos_absolutos(self):
        return min(TOTAL_VIDEO, self._offset + self.time)

    def _reloj(self):
        return Text(self._mm_ss(self._segundos_absolutos()),
                    font=FUENTE, font_size=17, color=GRIS_OSC).to_corner(UR, buff=0.35)

    def _barra(self):
        frac = self._segundos_absolutos() / TOTAL_VIDEO
        x = -self._ancho_barra / 2 + self._ancho_barra * frac
        return Line([-self._ancho_barra / 2, -3.42, 0], [x, -3.42, 0],
                    stroke_color=CIAN, stroke_width=3)

    @staticmethod
    def _mm_ss(segundos):
        m, s = divmod(int(segundos), 60)
        return f"{m:02d}:{s:02d}"

    # -- Helpers de animacion ------------------------------------------

    def play_t(self, *animaciones, run_time=0.7, espera=0.0, offset=0.0):
        """Reproduce animaciones y las descuenta del cronometro."""
        self.play(*animaciones, run_time=run_time)
        self.crono.anotar(run_time)
        if espera > 0:
            self.wait(espera)
            self.crono.anotar(espera)

    def protegidos(self):
        """
        Objetos que sobreviven a `limpiar_escena()`.

        Incluye el pie de pagina, la rejilla y el movimiento ambiental. Si se
        olvidara alguno, el `FadeOut` masivo lo eliminaba de la escena y el
        efecto desaparecia para el resto de la seccion.
        """
        return (self.rejilla,
                self.scanline, self.pulso_izq, self.pulso_der,
                self.etiqueta_seccion, self.marca_tiempo,
                self.barra_fondo, self.barra_progreso, self.marca_proyecto)

    def limpiar_escena(self, run_time=0.9):
        """Desvanece todo el contenido salvo los elementos protegidos."""
        self.play(*[FadeOut(m) for m in list(self.mobjects) if m not in self.protegidos()],
                  run_time=run_time)
        self.crono.anotar(run_time)

    def esperar(self, segundos, offset=0.0):
        self.wait(segundos)
        self.crono.anotar(segundos)

    def resumen_seccion(self, titulo, puntos):
        """
        Construye las tarjetas de recapitulacion que se revelan durante el
        tiempo sobrante de la seccion. Devuelve una lista (sin anadir a escena)
        para que `cerrar()` las vaya mostrando.
        """
        cab = Text(titulo, font=FUENTE, font_size=26, color=CIAN)
        cab.to_edge(UP, buff=1.05)

        items = []
        y = 1.35
        for punto in puntos:
            marca = Text(">", font=FUENTE, font_size=20, color=VERDE)
            txt = Text(punto, font=FUENTE, font_size=21, color=GRIS)
            fila = VGroup(marca, txt).arrange(RIGHT, buff=0.28)
            fila.move_to([-5.4, y, 0], aligned_edge=LEFT)
            items.append(fila)
            y -= 0.72

        # El título se muestra de inmediato: solo los puntos se revelan.
        self.add(cab)
        return [cab] + items

    def cerrar(self, resumen=None):
        """
        Cierra la seccion respetando el presupuesto.

        `MARGEN` descuenta un pequeño colchón: aunque el cronómetro cuadre
        exactamente, el redondeo a cuadros de imagen hace que cada sección
        termine algo más larga. Sin ese margen el ensamblado recortaría los
        últimos segundos y se perdería la diapositiva de cierre.
        """
        MARGEN = 0.6
        restante = (self.PRESUPUESTO - MARGEN) - self.crono.t
        if restante < -0.5:
            raise ValueError(
                f"Seccion '{self.NOMBRE}' excedio su presupuesto en {-restante:.1f}s"
            )

        objetos = list(resumen) if resumen else []

        if objetos:
            # Se revela con ritmo agitado (1.5 s por punto) y la holgura
            # restante se sostiene sobre la diapositiva ya completa, que es lo
            # que el expositor comenta. Si la holgura es menor que el ritmo
            # agitado, el slot se reduce para no exceder el presupuesto.
            n = len(objetos)
            slot = min(1.5, restante / n)
            fundido = min(0.55, max(0.15, slot * 0.5))
            holgura = max(0.0, restante - slot * n)

            for i, obj in enumerate(objetos):
                self.play_t(FadeIn(obj, shift=UP * 0.12), run_time=fundido)
                if i == n - 1:
                    self.esperar(max(0.02, slot - fundido + holgura))
                else:
                    self.esperar(max(0.02, slot - fundido))
            return

        if restante > 0.02:
            self.esperar(restante)

    # -- Constructores visuales ----------------------------------------

    def tarjeta(self, titulo, lineas, color=CIAN, alto=None):
        """Tarjeta con titulo y cuerpo, centrada."""
        cuerpo = VGroup(*[
            Text(l, font=FUENTE, font_size=22, color=GRIS) for l in lineas
        ]).arrange(DOWN, buff=0.22, aligned_edge=LEFT)

        caja = RoundedRectangle(
            corner_radius=0.14,
            width=12.2,
            height=alto if alto else (cuerpo.height + 1.5),
            stroke_color=color, stroke_width=1.6, stroke_opacity=0.75,
            fill_color=color, fill_opacity=0.035,
        )
        tit = Text(titulo, font=FUENTE, font_size=32, color=color)
        grupo_interno = VGroup(tit, cuerpo).arrange(DOWN, buff=0.32)
        grupo_interno.move_to(caja.get_center())

        return VGroup(caja, grupo_interno)

    def insignia(self, texto, color=CIAN, font_size=20):
        """Chip redondeado con etiqueta corta."""
        txt = Text(texto, font=FUENTE, font_size=font_size, color=color)
        caja = RoundedRectangle(
            corner_radius=0.12,
            width=txt.width + 0.5, height=txt.height + 0.32,
            stroke_color=color, stroke_width=1.4, stroke_opacity=0.8,
            fill_color=color, fill_opacity=0.08,
        )
        txt.move_to(caja.get_center())
        return VGroup(caja, txt)

    def caja_entidad(self, nombre, color=CIAN, font_size=24, ancho=None):
        txt = Text(nombre, font=FUENTE, font_size=font_size, color=color)
        caja = RoundedRectangle(
            corner_radius=0.1,
            width=ancho if ancho else txt.width + 0.8,
            height=txt.height + 0.42,
            stroke_color=color, stroke_width=2,
            fill_color=color, fill_opacity=0.06,
        )
        txt.move_to(caja.get_center())
        return VGroup(caja, txt)


# ===========================================================================
#  SECCION 1 — Introduccion y dominio (0:00 - 0:45)
# ===========================================================================
class Seccion1Introduccion(CyberEscena):
    OFFSET = 0.0
    NOMBRE = "01 // INTRODUCCION Y DOMINIO"
    PRESUPUESTO = 45.0

    def construct(self):
        # Titulo principal
        titulo = Text("NEXUS FINANCE", font=FUENTE, font_size=68, color=CIAN)
        titulo.set_stroke(CIAN, width=0.6, opacity=0.35)
        halo = titulo.copy().set_stroke(VERDE, width=8, opacity=0.18)
        self.play_t(FadeIn(halo, scale=1.06), run_time=1.4)
        self.play_t(AnimationGroup(
            FadeIn(titulo, shift=UP * 0.25),
            FadeOut(halo)
        ), run_time=0.9)

        # Subtitulo, situado claramente por debajo del titulo
        sub = Text("Sistema de gestion de finanzas personales",
                   font=FUENTE, font_size=25, color=GRIS)
        linea = Line([-5, -1.75, 0], [5, -1.75, 0], stroke_color=VERDE, stroke_width=2)
        cabecera = VGroup(titulo, sub, linea).arrange(DOWN, buff=0.45).move_to(UP * 1.15)
        self.play_t(AnimationGroup(FadeIn(sub), GrowFromCenter(linea)), run_time=0.9)
        self.play_t(linea.animate.set_opacity(0.3), run_time=0.4, espera=0.8)

        # La cabecera se retira antes de mostrar el problema: de lo contrario
        # ambos textos se pisan en la misma franja vertical.
        self.play_t(FadeOut(cabecera), run_time=0.8)

        # El problema
        prob = Text("EL PROBLEMA QUE RESUELVE", font=FUENTE, font_size=27, color=AMBAR)
        prob.to_edge(UP, buff=1.15)
        self.play_t(FadeIn(prob, shift=DOWN * 0.2), run_time=0.7)

        preguntas = VGroup(
            self.tarjeta_pregunta("¿Cuanto tengo?", "saldo consolidado de todas las cuentas", VERDE),
            self.tarjeta_pregunta("¿En que gaste?", "distribucion por categoria", CIAN),
            self.tarjeta_pregunta("¿Me paso del presupuesto?", "control de limites por categoria", ROJO),
        ).arrange(RIGHT, buff=0.55)

        for p in preguntas:
            self.play_t(FadeIn(p, shift=UP * 0.25), run_time=0.65)

        self.play_t(Wait(0.1), run_time=0.6)

        # Transicion al objeto del parcial
        self.play_t(
            *[FadeOut(m) for m in [prob, *preguntas]],
            run_time=1.1
        )

        nota = Text("El objeto de este parcial no es el codigo:\nes el MODELO DE DATOS que sostiene el sistema.",
                    font=FUENTE, font_size=29, color=VERDE, line_spacing=0.9)
        marco = SurroundingRectangle(nota, color=VERDE, buff=0.45, corner_radius=0.12,
                                     stroke_width=1.6, stroke_opacity=0.7)
        VGroup(nota, marco).move_to(ORIGIN)

        self.play_t(FadeIn(marco), run_time=0.6)
        self.play_t(FadeIn(nota, shift=UP * 0.2), run_time=0.9)
        self.play_t(Wait(0.1), run_time=0.8)

        self.play_t(FadeOut(nota), FadeOut(marco), run_time=0.8)

        resumen = self.resumen_seccion("EL DOMINIO EN UNA LINEA", [
            "El dinero entra como INGRESO y sale como GASTO",
            "Todo movimiento pertenece a una cuenta",
            "Cada gasto se clasifica en una categoria",
            "Cada categoria tiene un limite mensual opcional",
        ])
        self.cerrar(resumen)

    def tarjeta_pregunta(self, pregunta, detalle, color):
        p = Text(pregunta, font=FUENTE, font_size=25, color=color)
        d = Text(detalle, font=FUENTE, font_size=15, color=GRIS_OSC)
        VGroup(p, d).arrange(DOWN, buff=0.28)
        caja = RoundedRectangle(
            corner_radius=0.14, width=3.5, height=2.0,
            stroke_color=color, stroke_width=1.8, stroke_opacity=0.8,
            fill_color=color, fill_opacity=0.05
        )
        txt = VGroup(p, d).arrange(DOWN, buff=0.28).move_to(caja.get_center())
        return VGroup(caja, txt)


# ===========================================================================
#  SECCION 2 — Modelo ER y reglas de negocio (0:45 - 2:00)
# ===========================================================================
class Seccion2ModeloER(CyberEscena):
    OFFSET = 45.0
    NOMBRE = "02 // MODELO ER Y REGLAS DE NEGOCIO"
    PRESUPUESTO = 75.0

    def construct(self):
        self._vinieta("MODELO ENTIDAD-RELACION", CIAN, "10 entidades", 3.6)

        # ---- Jerarquia -------------------------------------------------
        jer_titulo = self._titulo_item(1, "JERARQUIA DE ESPECIALIZACION", ROJO)
        self.play_t(FadeIn(jer_titulo, shift=RIGHT * 0.3), run_time=0.8)

        superc = self.caja_entidad("TRANSACCION", ROJO, 26, ancho=3.9)
        superc.move_to(UP * 1.45)
        ing = self.caja_entidad("INGRESO", VERDE, 22, ancho=2.9)
        gas = self.caja_entidad("GASTO", ROJO, 22, ancho=2.9)

        # Posicionamiento explicito. Antes se usaba align_to(superc, UP), que
        # alinea los bordes superiores y superponia las subclases sobre la
        # superclase en lugar de separarlas.
        ing.move_to(superc.get_center() + DOWN * 1.9 + RIGHT * 2.1)
        gas.move_to(superc.get_center() + DOWN * 1.9 + LEFT * 2.1)

        a_ing = Arrow(superc.get_bottom(), ing.get_top(), buff=0.12,
                      stroke_color=VERDE, stroke_width=2.2)
        a_gas = Arrow(superc.get_bottom(), gas.get_top(), buff=0.12,
                      stroke_color=ROJO, stroke_width=2.2)
        # Las etiquetas se colocan hacia AFUERA de las flechas. Antes iban hacia el
        # centro y ambas terminaban encimadas sobre el mismo punto.
        lbl_ing = Text("ISA (P)", font=FUENTE, font_size=14, color=VERDE).next_to(a_ing, RIGHT, buff=0.12)
        lbl_gas = Text("ISA (P)", font=FUENTE, font_size=14, color=ROJO).next_to(a_gas, LEFT, buff=0.12)

        jer = VGroup(superc, ing, gas, a_ing, a_gas, lbl_ing, lbl_gas)

        # Halo de la superclase, construido aparte para no arrastrar los hijos.
        sup_caja = superc[0].copy().set_stroke(width=6, opacity=0.15)
        self.play_t(FadeIn(sup_caja), run_time=0.5)
        self.play_t(FadeIn(superc, scale=0.9), run_time=0.8)
        self.play_t(
            AnimationGroup(GrowArrow(a_ing), FadeIn(ing, scale=0.92), FadeIn(lbl_ing)),
            run_time=0.8
        )
        self.play_t(
            AnimationGroup(GrowArrow(a_gas), FadeIn(gas, scale=0.92), FadeIn(lbl_gas)),
            run_time=0.8
        )

        comun = Text("comparten:  fecha · monto · descripcion",
                     font=FUENTE, font_size=15, color=GRIS)
        # Se ancla arriba a la izquierda: la zona bajo el titulo esta libre y
        # evita solaparse con la caja de la superclase.
        comun.to_corner(UL, buff=0.35).shift(DOWN * 1.38)
        propio = Text("propio: origen", font=FUENTE, font_size=15, color=VERDE).next_to(ing, DOWN, buff=0.3)
        propio2 = Text("propio: categoria", font=FUENTE, font_size=15, color=ROJO).next_to(gas, DOWN, buff=0.3)
        self.play_t(
            AnimationGroup(FadeIn(comun), FadeIn(propio), FadeIn(propio2)),
            run_time=0.8
        )

        ambig = Text("Se parecen en todo salvo en un detalle.\nSin jerarquia quedarian columnas vacias y ambiguedad de tipo.",
                     font=FUENTE, font_size=16, color=AMBAR, line_spacing=0.85)
        ambig.move_to(DOWN * 2.15)
        self.play_t(FadeIn(ambig, shift=UP * 0.15), run_time=0.8)
        self.play_t(Wait(0.1), run_time=0.9)

        self.play_t(*[FadeOut(m) for m in [jer_titulo, jer, comun, propio, propio2,
                                            ambig, sup_caja]],
                    run_time=0.9)

        # ---- N:M --------------------------------------------------------
        self._vinieta("RELACION N:M", ROJO, "cuentas compartidas", 3.6)
        nm_titulo = self._titulo_item(2, "UN USUARIO ACCEDE A MUCHAS CUENTAS", ROJO)
        self.play_t(FadeIn(nm_titulo, shift=RIGHT * 0.3), run_time=0.7)

        u = self.caja_entidad("USUARIOS", VERDE, 22)
        c = self.caja_entidad("CUENTAS", CIAN, 22)
        rombo = Polygon(UP * 0.85, RIGHT * 1.5, DOWN * 0.85, LEFT * 1.5,
                         stroke_color=ROJO, stroke_width=2.4,
                         fill_color=ROJO, fill_opacity=0.10)
        texto_rombo = Text("ACCEDE_A", font=FUENTE, font_size=15, color=ROJO)
        texto_rombo.move_to(rombo.get_center()).scale(0.85)
        trio = VGroup(u, rombo, texto_rombo, c).arrange(RIGHT, buff=1.1).move_to(UP * 0.75)

        a1 = Line(u.get_right(), rombo.get_left(), stroke_color=VERDE, stroke_width=2)
        a2 = Line(rombo.get_right(), c.get_left(), stroke_color=CIAN, stroke_width=2)

        txt_m = Text("N", font=FUENTE, font_size=22, color=GRIS)
        txt_1 = Text("1", font=FUENTE, font_size=22, color=GRIS)
        txt_n2 = Text("1", font=FUENTE, font_size=22, color=GRIS)
        txt_n3 = Text("N", font=FUENTE, font_size=22, color=GRIS)
        m1 = txt_m.next_to(a1, UP, buff=0.08)
        m2 = txt_1.next_to(a1, DOWN, buff=0.08)
        m3 = txt_n2.next_to(a2, UP, buff=0.08)
        m4 = txt_n3.next_to(a2, DOWN, buff=0.08)

        self.play_t(
            AnimationGroup(FadeIn(u), FadeIn(rombo, scale=0.85), FadeIn(texto_rombo), FadeIn(c),
                           Create(a1), Create(a2),
                           FadeIn(m1), FadeIn(m2), FadeIn(m3), FadeIn(m4)),
            run_time=1.2
        )

        puente = Text("Tabla puente:  USUARIO_CUENTA", font=FUENTE, font_size=22, color=ROJO)
        pk = Text("PK compuesta ( usuario_id , cuenta_id )  ->  sin accesos duplicados",
                  font=FUENTE, font_size=17, color=GRIS)
        rol = Text("atributo rol:  PROPIETARIO | EDITOR | VISUALIZADOR",
                   font=FUENTE, font_size=17, color=GRIS)
        VGroup(puente, pk, rol).arrange(DOWN, buff=0.26).next_to(trio, DOWN, buff=1.0)
        self.play_t(FadeIn(puente, shift=UP * 0.2), run_time=0.7)
        self.play_t(AnimationGroup(FadeIn(pk), FadeIn(rol)), run_time=0.7)
        self.play_t(Wait(0.1), run_time=0.8)

        self.play_t(*[FadeOut(m) for m in [nm_titulo, trio, a1, a2, m1, m2, m3, m4,
                                           puente, pk, rol]], run_time=0.9)

        # ---- 1:1 + multivaluado + derivados ------------------------------
        self._vinieta("1:1 · MULTIVALUADO · DERIVADOS", CIAN, "tres elementos", 3.6)

        trio_titulo = self._titulo_item(3, "LOS TRES ELEMENTOS RESTANTES", CIAN)
        self.play_t(FadeIn(trio_titulo, shift=RIGHT * 0.3), run_time=0.7)

        # 1:1
        t11 = Text("1:1", font=FUENTE, font_size=30, color=AMBAR)
        e11a = self.caja_entidad("USUARIOS", AMBAR, 18, ancho=2.6)
        e11b = self.caja_entidad("PERFILES", AMBAR, 18, ancho=2.6)
        linea11 = DoubleArrow(e11a.get_right(), e11b.get_left(), buff=0.15,
                              stroke_color=AMBAR, stroke_width=2, tip_length=0.18)
        d11 = Text("PK = FK\ncomo maximo un perfil", font=FUENTE, font_size=13, color=GRIS_OSC,
                   line_spacing=0.8)
        g11 = VGroup(t11, e11a, linea11, e11b, d11).arrange(DOWN, buff=0.32).scale(0.82)

        # Multivaluado
        tmv = Text("MULTIVALUADO", font=FUENTE, font_size=20, color=ROJO)
        emv = self.caja_entidad("TELEFONOS", ROJO, 18, ancho=2.6)
        elipse = Ellipse(width=1.9, height=1.0, stroke_color=ROJO, stroke_width=2)
        elipse2 = Ellipse(width=2.3, height=1.35, stroke_color=ROJO, stroke_width=1.4, stroke_opacity=0.5)
        txt_tel = Text("telefonos", font=FUENTE, font_size=15, color=ROJO)
        VGroup(elipse2, elipse, txt_tel).move_to(elipse2.get_center())
        d_mv = Text("doble ovalo:\ncero o varios", font=FUENTE, font_size=13, color=GRIS_OSC,
                    line_spacing=0.8)
        gmv = VGroup(tmv, emv, elipse2, d_mv).arrange(DOWN, buff=0.3).scale(0.86)

        # Derivado
        tdr = Text("DERIVADOS", font=FUENTE, font_size=20, color=MORADO)
        edr = self.caja_entidad("VISTAS SQL", MORADO, 18, ancho=2.6)
        ovalo_base = Ellipse(width=2.4, height=1.2, stroke_color=MORADO, stroke_width=2)
        ovalo = DashedVMobject(ovalo_base, num_dashes=18, dashed_ratio=0.5, color=MORADO)
        txt_dr = Text("saldo_total", font=FUENTE, font_size=14, color=MORADO)
        txt_dr.move_to(ovalo.get_center())
        d_dr = Text("ovalo punteado:\nse calcula, no se guarda", font=FUENTE, font_size=13,
                    color=GRIS_OSC, line_spacing=0.8)
        gdr = VGroup(tdr, edr, ovalo, txt_dr, d_dr).arrange(DOWN, buff=0.3).scale(0.86)

        fila = VGroup(g11, gmv, gdr).arrange(RIGHT, buff=0.7).scale(0.95)
        fila.next_to(trio_titulo, DOWN, buff=1.0).shift(DOWN * 0.1)

        self.play_t(FadeIn(fila, shift=UP * 0.3), run_time=1.1)
        self.play_t(Wait(0.1), run_time=1.0)

        self.play_t(FadeOut(fila), FadeOut(trio_titulo), run_time=0.9)

        # ---- Regla de negocio --------------------------------------------
        self._vinieta("REGLA DE NEGOCIO", ROJO, "90 dias", 3.6)
        regla_t = self._titulo_item(4, "RESTRICCION QUE NO ADMITE PK NI FK", ROJO)
        self.play_t(FadeIn(regla_t, shift=RIGHT * 0.3), run_time=0.8)

        enunciado = Text("“No se pueden registrar movimientos con mas de 90 dias de antiguedad”",
                         font=FUENTE, font_size=21, color=ROJO)
        caja_enun = RoundedRectangle(
            corner_radius=0.12,
            width=min(13.2, enunciado.width + 0.7), height=1.0,
            stroke_color=ROJO, stroke_width=1.6, stroke_opacity=0.7,
            fill_color=ROJO, fill_opacity=0.06,
        )
        enunciado.move_to(caja_enun.get_center())
        enunciado.set_width(min(enunciado.width, caja_enun.width - 0.5))
        caja_enun.move_to(UP * 1.55)
        enunciado.move_to(caja_enun.get_center())
        self.play_t(FadeIn(caja_enun), run_time=0.6)
        self.play_t(FadeIn(enunciado), run_time=0.8)

        porque = VGroup(
            Text("¿Por que no se puede con CHECK?", font=FUENTE, font_size=21, color=AMBAR),
            Text("Compara contra CURDATE() -> funcion no determinista",
                 font=FUENTE, font_size=17, color=GRIS),
            Text("MySQL responde:  ERROR 3814: disallowed function: curdate",
                 font=FUENTE, font_size=17, color=ROJO),
            Text("Tampoco es FK: no compara claves, compara contra el instante actual",
                 font=FUENTE, font_size=17, color=GRIS),
        ).arrange(DOWN, buff=0.22, aligned_edge=LEFT)
        porque.next_to(caja_enun, DOWN, buff=0.62).shift(UP * 0.1)

        for linea in porque:
            self.play_t(FadeIn(linea, shift=RIGHT * 0.2), run_time=0.6)

        solucion = Text("Solucion:  TRIGGER BEFORE INSERT  ->  SIGNAL SQLSTATE '45000'",
                        font=FUENTE, font_size=20, color=VERDE)
        solucion.next_to(porque, DOWN, buff=0.5)
        self.play_t(FadeIn(solucion, shift=UP * 0.2), run_time=0.9)
        self.play_t(Wait(0.1), run_time=0.8)

        self.play_t(
            *[FadeOut(m) for m in [regla_t, caja_enun, enunciado, *porque, solucion]],
            run_time=0.9
        )

        resumen = self.resumen_seccion("LOS CINCO ELEMENTOS DEL MODELO", [
            "Jerarquia:  TRANSACCION -> INGRESO / GASTO",
            "N:M:  USUARIO_CUENTA (cuentas compartidas con rol)",
            "1:1:  USUARIOS <-> PERFILES",
            "Multivaluado: telefonos   |   Derivados: vistas",
            "Regla de negocio: trigger de los 90 dias",
        ])
        self.cerrar(resumen)

    # -- helpers de seccion ---------------------------------------------

    def _titulo_item(self, numero, texto, color):
        num = Text(f"{numero}", font=FUENTE, font_size=30, color=color, weight=BOLD)
        circulo = Circle(radius=0.28, stroke_color=color, stroke_width=2,
                         fill_color=color, fill_opacity=0.12).move_to(num)
        num.move_to(circulo)
        txt = Text(texto, font=FUENTE, font_size=26, color=color)
        grupo = VGroup(circulo, num, txt).arrange(RIGHT, buff=0.35)
        grupo.to_edge(UP, buff=0.95)
        return grupo

    def _vinieta(self, titulo, color, subtitulo, duracion_extra=0.0):
        """Barra divisoria entre sub-temas."""
        barra = Line([-6.5, 0, 0], [6.5, 0, 0], stroke_color=color, stroke_width=2.5,
                     stroke_opacity=0.55)
        t = Text(titulo, font=FUENTE, font_size=25, color=color).next_to(barra, UP, buff=0.28)
        s = Text(subtitulo, font=FUENTE, font_size=15, color=GRIS_OSC).next_to(t, DOWN, buff=0.12)
        self.play_t(
            AnimationGroup(GrowFromCenter(barra), FadeIn(t), FadeIn(s)),
            run_time=0.9
        )
        self.play_t(FadeOut(barra), FadeOut(t), FadeOut(s), run_time=0.5)


# ===========================================================================
#  SECCION 3 — Modelo Relacional y Script SQL (2:00 - 3:15)
# ===========================================================================
class Seccion3Relacional(CyberEscena):
    OFFSET = 120.0
    NOMBRE = "03 // MODELO RELACIONAL Y SCRIPT SQL"
    PRESUPUESTO = 75.0

    def construct(self):
        # Regla de conversion
        cab = Text("DEL DIAGRAMA ER AL MODELO RELACIONAL", font=FUENTE,
                   font_size=30, color=CIAN).to_edge(UP, buff=1.05)
        self.play_t(FadeIn(cab, shift=DOWN * 0.25), run_time=0.9)

        filas = [
            ("Entidad fuerte", "una tabla", "USUARIOS, CUENTAS", VERDE),
            ("Entidad debil", "PK = FK", "PERFILES  (1:1)", AMBAR),
            ("Atributo multivaluado", "tabla secundaria", "TELEFONOS", ROJO),
            ("Relacion N:M", "tabla puente", "USUARIO_CUENTA", ROJO),
            ("Jerarquia", "superclase + 2 tablas", "TRANSACCIONES -> INGRESO, GASTO", VERDE),
        ]

        self.play_t(FadeIn(cab), run_time=0.1)

        y = 1.55
        for concepto, tecnica, ejemplo, color in filas:
            c = Text(concepto, font=FUENTE, font_size=19, color=color)
            flecha = Text("→", font=FUENTE, font_size=19, color=GRIS_OSC)
            t = Text(tecnica, font=FUENTE, font_size=19, color=GRIS)
            ej = Text(ejemplo, font=FUENTE, font_size=18, color=GRIS)
            separador = Line([-6.2, y - 0.30, 0], [6.2, y - 0.30, 0],
                             stroke_color=GRIS, stroke_width=0.8, stroke_opacity=0.16)

            c.move_to([-6.2, y + 0.10, 0], aligned_edge=LEFT)
            flecha.next_to(c, RIGHT, buff=0.28)
            t.next_to(flecha, RIGHT, buff=0.28)
            ej.next_to(t, RIGHT, buff=0.45)

            self.play_t(
                AnimationGroup(FadeIn(c, shift=RIGHT * 0.2), FadeIn(flecha),
                               FadeIn(t), FadeIn(ej), FadeIn(separador)),
                run_time=0.62
            )
            y -= 0.72

        self.play_t(Wait(0.1), run_time=0.7)

        # Detalle de la jerarquia
        self.limpiar_escena(run_time=1.0)

        det = Text("LO CLAVE: LA JERARQUIA EN SQL", font=FUENTE, font_size=30,
                   color=VERDE).to_edge(UP, buff=1.05)
        self.play_t(FadeIn(det, shift=DOWN * 0.25), run_time=0.8)

        self._codigo_falso(
            "CREATE TABLE transacciones (",
            "    id            INT AUTO_INCREMENT PRIMARY KEY,",
            "    fecha         DATE,",
            "    monto         DECIMAL(14,2),",
            "    cuenta_id     INT,  -- FK",
            "    usuario_id    INT   -- FK",
            ");",
        ).shift(UP * 0.75)
        self.play_t(FadeIn(self.code_group, shift=UP * 0.2), run_time=1.0)

        sub = VGroup(
            self._tabla_mini("ingresos", "transaccion_id PK/FK", VERDE),
            self._tabla_mini("gastos", "transaccion_id PK/FK", ROJO),
        ).arrange(RIGHT, buff=1.2).next_to(self.code_group, DOWN, buff=0.85)
        self.play_t(FadeIn(sub, shift=UP * 0.25), run_time=0.9)

        nota = Text("La PK de cada subclase ES la PK de la superclase\n"
                    "-> cardinalidad 0..1 : un movimiento es ingreso O gasto",
                    font=FUENTE, font_size=19, color=AMBAR, line_spacing=0.9)
        nota.next_to(sub, DOWN, buff=0.55)
        self.play_t(FadeIn(nota), run_time=0.9)
        self.play_t(Wait(0.1), run_time=0.9)

        self.limpiar_escena(run_time=1.0)

        # Restricciones del script
        rest_t = Text("RESTRICCIONES DEL SCRIPT", font=FUENTE, font_size=30,
                      color=CIAN).to_edge(UP, buff=1.05)
        self.play_t(FadeIn(rest_t, shift=DOWN * 0.25), run_time=0.8)

        items = [
            ("PRIMARY KEY", "en las 10 tablas", VERDE),
            ("FOREIGN KEY", "con ON DELETE CASCADE", CIAN),
            ("UNIQUE", "correo de usuario; periodo del presupuesto", MORADO),
            ("CHECK", "montos > 0 · mes 1-12 · sin sobregiro", AMBAR),
            ("TRIGGER", "regla de los 90 dias", ROJO),
        ]

        grupo = VGroup()
        for etiqueta, detalle, color in items:
            chip = self.insignia(etiqueta, color, font_size=18)
            txt = Text(detalle, font=FUENTE, font_size=18, color=GRIS)
            chip.next_to(txt, LEFT, buff=0.45)
            grupo.add(VGroup(chip, txt).arrange(RIGHT, buff=0.45))

        grupo.arrange(DOWN, buff=0.42, aligned_edge=LEFT)
        grupo.next_to(rest_t, DOWN, buff=0.95)

        for g in grupo:
            self.play_t(FadeIn(g, shift=RIGHT * 0.25), run_time=0.6)

        self.play_t(Wait(0.1), run_time=0.8)

        self.play_t(FadeOut(grupo), FadeOut(rest_t), run_time=0.9)

        resumen = self.resumen_seccion("DEL ER A LAS TABLAS", [
            "Entidad fuerte  ->  una tabla",
            "Entidad debil (1:1)  ->  PK que es a la vez FK",
            "Atributo multivaluado  ->  tabla secundaria",
            "Relacion N:M  ->  tabla puente con PK compuesta",
            "Jerarquia  ->  superclase + una tabla por subclase",
        ])
        self.cerrar(resumen)

    def _codigo_falso(self, *lineas):
        textos = VGroup(*[
            Text(l if l else " ", font=FUENTE, font_size=18,
                 color=GRIS if not l.startswith("CREATE") else VERDE)
            for l in lineas
        ]).arrange(DOWN, buff=0.12, aligned_edge=LEFT)
        caja = RoundedRectangle(
            corner_radius=0.12,
            width=max(textos.width + 0.9, 6), height=textos.height + 0.7,
            stroke_color=GRIS, stroke_width=1.4, stroke_opacity=0.5,
            fill_color="#000000", fill_opacity=0.4,
        )
        textos.move_to(caja.get_center())
        self.code_group = VGroup(caja, textos)
        return self.code_group

    def _tabla_mini(self, nombre, nota, color):
        t = Text(nombre, font=FUENTE, font_size=20, color=color)
        n = Text(nota, font=FUENTE, font_size=14, color=GRIS_OSC)
        caja = RoundedRectangle(
            corner_radius=0.1, width=3.6, height=1.35,
            stroke_color=color, stroke_width=2, fill_color=color, fill_opacity=0.06
        )
        VGroup(t, n).arrange(DOWN, buff=0.2).move_to(caja.get_center())
        return VGroup(caja, t, n)


# ===========================================================================
#  SECCION 4 — Ingenieria inversa y verificacion (3:15 - 4:15)
# ===========================================================================
class Seccion4IngenieriaInversa(CyberEscena):
    OFFSET = 195.0
    NOMBRE = "04 // INGENIERIA INVERSA Y VERIFICACION"
    PRESUPUESTO = 60.0

    def construct(self):
        cab = Text("DE LA BASE DE DATOS AL DIAGRAMA EER", font=FUENTE, font_size=30,
                   color=CIAN).to_edge(UP, buff=1.05)
        self.play_t(FadeIn(cab, shift=DOWN * 0.25), run_time=0.8)

        pasos = Text("MySQL Workbench  >  Schema  >  Reverse Engineer  >  nexus_finance",
                     font=FUENTE, font_size=19, color=GRIS)
        pasos.next_to(cab, DOWN, buff=0.45)
        self.play_t(FadeIn(pasos), run_time=0.8)

        self.play_t(Wait(0.1), run_time=0.6)

        # Representacion del EER generado
        eer_box = RoundedRectangle(
            corner_radius=0.14, width=9.6, height=4.6,
            stroke_color=VERDE, stroke_width=2, fill_color=FONDO_ALT, fill_opacity=0.5,
        )
        eer_box.move_to(UP * 0.45)
        eer_titulo = Text("Diagrama EER generado", font=FUENTE, font_size=17, color=VERDE)
        eer_titulo.next_to(eer_box.get_top(), UP, buff=0.18)

        # Jerarquia visible
        sup = self.caja_entidad("TRANSACCIONES", ROJO, 19, ancho=3.1)
        ing = self.caja_entidad("INGRESOS", VERDE, 16, ancho=2.5)
        gas = self.caja_entidad("GASTOS", ROJO, 16, ancho=2.5)
        sup.move_to(eer_box.get_center() + UP * 1.15)
        ing.move_to(eer_box.get_center() + DOWN * 0.85 + LEFT * 1.9)
        gas.move_to(eer_box.get_center() + DOWN * 0.85 + RIGHT * 1.9)
        fl1 = Arrow(sup.get_bottom(), ing.get_top(), buff=0.08, stroke_color=VERDE, stroke_width=1.6)
        fl2 = Arrow(sup.get_bottom(), gas.get_top(), buff=0.08, stroke_color=ROJO, stroke_width=1.6)

        uc = self.caja_entidad("USUARIO_CUENTA", ROJO, 14, ancho=2.7)
        uc.move_to(eer_box.get_center() + DOWN * 1.85)
        pf = self.caja_entidad("PERFILES", AMBAR, 14, ancho=2.2)
        pf.move_to(eer_box.get_center() + UP * 1.15 + RIGHT * 2.6)

        eer = VGroup(eer_box, eer_titulo, sup, ing, gas, fl1, fl2, uc, pf)

        self.play_t(FadeIn(eer_box), run_time=0.7)
        self.play_t(AnimationGroup(
            FadeIn(sup, scale=0.92),
            FadeIn(ing, shift=UP * 0.15), FadeIn(gas, shift=UP * 0.15),
            GrowArrow(fl1), GrowArrow(fl2)
        ), run_time=0.9)
        self.play_t(AnimationGroup(FadeIn(uc, shift=UP * 0.15), FadeIn(pf)), run_time=0.7)
        self.play_t(Wait(0.1), run_time=0.6)

        # Verificaciones
        ver_t = Text("VERIFICACION CON EL DISENO INICIAL", font=FUENTE, font_size=24,
                     color=AMBAR).move_to(DOWN * 1.55)
        self.play_t(FadeIn(ver_t), run_time=0.6)

        checks = [
            ("10 tablas en el EER = 10 entidades en el diseno", VERDE),
            ("INGRESOS y GASTOS aparecen bajo TRANSACCIONES", VERDE),
            ("USUARIO_CUENTA con PK compuesta: resuelve el N:M", VERDE),
            ("PERFILES con PK = FK: la firma del 1:1", VERDE),
        ]
        # Se mantiene por encima de la barra de progreso (y = -3.42).
        y = -2.15
        for texto, color in checks:
            marca = Text("[ OK ]", font=FUENTE, font_size=14, color=VERDE)
            t = Text(texto, font=FUENTE, font_size=14, color=GRIS)
            marca.next_to(t, LEFT, buff=0.22)
            g = VGroup(marca, t).move_to([-0.2, y, 0], aligned_edge=LEFT)
            self.play_t(FadeIn(g, shift=RIGHT * 0.2), run_time=0.5)
            y -= 0.36

        self.play_t(Wait(0.1), run_time=0.5)

        self.limpiar_escena(run_time=1.0)

        # Objetos que no son tablas
        cab2 = Text("LO QUE NO SON TABLAS", font=FUENTE, font_size=29,
                    color=MORADO).to_edge(UP, buff=1.05)
        self.play_t(FadeIn(cab2, shift=DOWN * 0.25), run_time=0.7)

        objetos = [
            ("3 VISTAS", "v_saldo_usuario\nv_gasto_categoria_periodo\nv_historial_movimientos",
             "atributos derivados", MORADO),
            ("1 TRIGGER", "trg_transacciones_fecha_limita\nBEFORE INSERT  ->  SIGNAL '45000'",
             "regla de negocio", ROJO),
        ]

        tarjetas = VGroup()
        for titulo, cuerpo, pie, color in objetos:
            t = Text(titulo, font=FUENTE, font_size=26, color=color)
            c = Text(cuerpo, font=FUENTE, font_size=17, color=GRIS, line_spacing=0.95)
            p = Text(pie, font=FUENTE, font_size=14, color=GRIS_OSC)
            caja = RoundedRectangle(
                corner_radius=0.13, width=6.6, height=3.3,
                stroke_color=color, stroke_width=1.8, stroke_opacity=0.75,
                fill_color=color, fill_opacity=0.05
            )
            VGroup(t, c, p).arrange(DOWN, buff=0.3).move_to(caja.get_center())
            tarjetas.add(VGroup(caja, t, c, p))

        tarjetas.arrange(RIGHT, buff=0.7).shift(DOWN * 0.3)

        for tarjeta in tarjetas:
            self.play_t(FadeIn(tarjeta, shift=UP * 0.25), run_time=0.8)

        demo = Text("Inserto un movimiento de hace 200 dias  ->  el motor lo rechaza",
                    font=FUENTE, font_size=19, color=AMBAR)
        demo.next_to(tarjetas, DOWN, buff=0.55)
        self.play_t(FadeIn(demo), run_time=0.7)
        self.play_t(Wait(0.1), run_time=0.8)

        self.play_t(FadeOut(tarjetas), FadeOut(demo), FadeOut(cab2), run_time=0.9)

        resumen = self.resumen_seccion("LA CORRESPONDENCIA SE CUMPLE", [
            "10 tablas en el EER  =  10 entidades en el diseno",
            "INGRESOS y GASTOS cuelgan de TRANSACCIONES en el EER",
            "USUARIO_CUENTA con PK compuesta  ->  el N:M",
            "PERFILES con PK = FK  ->  el 1:1",
            "3 vistas y 1 trigger: los derivados y la regla",
        ])
        self.cerrar(resumen)


# ===========================================================================
#  SECCION 5 — Conclusiones y cierre (4:15 - 5:00)
# ===========================================================================
class Seccion5Conclusiones(CyberEscena):
    OFFSET = 255.0
    NOMBRE = "05 // CONCLUSIONES Y CIERRE"
    PRESUPUESTO = 45.0

    def construct(self):
        cab = Text("CONCLUSIONES", font=FUENTE, font_size=44, color=CIAN)
        cab.set_stroke(CIAN, width=0.5, opacity=0.3)
        cab.to_edge(UP, buff=1.0)
        self.play_t(FadeIn(cab, shift=DOWN * 0.3), run_time=0.9)

        aprendizajes = [
            ("01", "El modelado obliga a decidir antes de programar",
             "La jerarquia entre INGRESO y GASTO se entendio de verdad\n"
             "al decidir en que tabla vivia cada atributo.\n"
             "El codigo vino despues, y fue mecanico.", VERDE),
            ("02", "Hay restricciones que el modelo no alcanza a expresar",
             "La regla de los 90 dias no es CHECK ni foreign key: es un trigger.\n"
             "Saber donde termina el modelo y empieza la logica\n"
             "de procedimiento es parte del diseno.", CIAN),
            ("03", "La ingenieria inversa es verificacion, no tramite",
             "Mi primera version tenia la jerarquia como tabla plana.\n"
             "El EER me mostro que eso no se veía.\n"
             "De ahi salio el cambio de diseno.", AMBAR),
        ]

        y = 1.35
        for num, titulo, cuerpo, color in aprendizajes:
            circulo = Circle(radius=0.33, stroke_color=color, stroke_width=2,
                             fill_color=color, fill_opacity=0.1)
            nt = Text(num, font=FUENTE, font_size=18, color=color).move_to(circulo)
            ntb = Text(titulo, font=FUENTE, font_size=21, color=color)
            cir = circulo.copy()
            cir.move_to([-5.6, y + 0.18, 0])
            n2 = nt.copy().move_to(cir)

            cuerpo_t = Text(cuerpo, font=FUENTE, font_size=15, color=GRIS, line_spacing=0.95)
            cuerpo_t.move_to([-4.95, y - 0.28, 0], aligned_edge=LEFT)

            linea = Line([-5.6, y - 0.95, 0], [5.9, y - 0.95, 0],
                         stroke_color=GRIS, stroke_width=0.8, stroke_opacity=0.14)

            g = VGroup(cir, n2, ntb, cuerpo_t, linea)

            self.play_t(
                AnimationGroup(FadeIn(cir, scale=0.8), FadeIn(n2), FadeIn(ntb, shift=RIGHT * 0.2),
                               FadeIn(cuerpo_t), FadeIn(linea)),
                run_time=0.85
            )
            y -= 1.42

        self.play_t(Wait(0.1), run_time=0.7)

        self.limpiar_escena(run_time=1.0)

        # Cierre
        gracias = Text("MUCHAS GRACIAS", font=FUENTE, font_size=58, color=VERDE)
        gracias.set_stroke(VERDE, width=0.6, opacity=0.3)
        sub = Text("¿Preguntas?", font=FUENTE, font_size=26, color=GRIS)
        VGroup(gracias, sub).arrange(DOWN, buff=0.5).move_to(ORIGIN)

        linea_1 = Line([-4.5, -1.55, 0], [4.5, -1.55, 0], stroke_color=CIAN, stroke_width=2)
        linea_2 = Line([-4.5, 1.55, 0], [4.5, 1.55, 0], stroke_color=CIAN, stroke_width=2)

        self.play_t(GrowFromCenter(linea_1), run_time=0.5)
        self.play_t(GrowFromCenter(linea_2), run_time=0.5)
        self.play_t(FadeIn(gracias, scale=1.05), run_time=1.0)
        self.play_t(FadeIn(sub), run_time=0.7)

        self.play_t(
            *[FadeOut(m) for m in [gracias, sub, linea_1, linea_2]],
            run_time=0.9
        )

        resumen = self.resumen_seccion("PARA LLEVAR", [
            "El modelo ER quedo completo: 10 tablas, 3 vistas, 1 trigger",
            "Los 8 requisitos obligatorios, cubiertos y verificados",
            "29 pruebas automatizadas, 12 contra la base de datos real",
            "El modelo RELACIONAL se genero con ingenieria inversa",
            "Gracias.  Proyecto disponible para revision.",
        ])
        self.cerrar(resumen)