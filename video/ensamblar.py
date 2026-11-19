"""
Concatena las cinco secciones en un solo video y ajusta la duracion total
a exactamente 5 minutos (300 s).

Uso:
    python ensamblar.py [carpeta_con_los_clips] [archivo_salida]

Todos los clips se generan con la misma resolucion y tasa de fotogramas, asi
que basta con decodificarlos en orden y reasignar los indices de tiempo.
"""

import sys
import av
from fractions import Fraction
from pathlib import Path

SEGUNDOS_OBJETIVO = 300.0

ORDEN = [
    "Seccion1Introduccion",
    "Seccion2ModeloER",
    "Seccion3Relacional",
    "Seccion4IngenieriaInversa",
    "Seccion5Conclusiones",
]


def rutas(raiz: Path):
    """Localiza un mp4 por escena, prefiriendo la resolucion mas alta."""
    candidatos = {e: [] for e in ORDEN}
    for mp4 in raiz.rglob("Seccion*.mp4"):
        for escena in ORDEN:
            if mp4.stem.startswith(escena):
                candidatos[escena].append(mp4)

    def altura(carpeta: Path) -> int:
        """Manim nombra las carpetas '480p15', '720p30', '1080p60'.

        Ordenar alfabeticamente daria '720p30' > '1080p60' > '480p15', con lo
        que se elegiria una calidad INFERIOR. Se extrae el alto numerico.
        """
        import re
        m = re.search(r"(\d+)p", carpeta.name)
        return int(m.group(1)) if m else 0

    elegidas = {}
    for escena, lista in candidatos.items():
        if lista:
            elegidas[escena] = max(lista, key=lambda p: (altura(p.parent), p.parent.name))

    faltan = [e for e in ORDEN if e not in elegidas]
    if faltan:
        raise SystemExit(f"Faltan clips: {faltan}\nBuscando en: {raiz}")
    return [elegidas[e] for e in ORDEN]


def main():
    raiz = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("media/videos")
    salida = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("NexusFinance_Exposicion_5min.mp4")

    clips = rutas(raiz)

    print("Clips (en orden de exposicion):")
    total_in = 0.0
    for c in clips:
        with av.open(str(c)) as cont:
            v = cont.streams.video[0]
            d = float(cont.duration) / av.time_base
            print(f"  - {c.parent.name:8s} {c.stem:36s} {d:7.2f}s  "
                  f"{v.width}x{v.height} @ {float(v.average_rate):.0f}fps")
        total_in += d
    print(f"  TOTAL ENTRADA: {total_in:.2f}s\n")

    with av.open(str(clips[0])) as ref:
        v = ref.streams.video[0]
        ancho, alto = v.width, v.height
        # average_rate puede venir None si el contenedor no lo declara; se
        # intenta la tasa adivinada y, en ultimo caso, se asume 30 fps.
        fps = Fraction(v.average_rate or v.guessed_rate or v.r_frame_rate or 30)

    # Un cuadro de margen: el limite de la guia es "máximo 5 minutos",
    # asi que conviene quedar por debajo y no depender del redondeo.
    objetivo_cuadros = int(SEGUNDOS_OBJETIVO * float(fps)) - 1

    salida.parent.mkdir(parents=True, exist_ok=True)
    cont = av.open(str(salida), mode="w")
    stream = cont.add_stream("libx264", rate=fps)
    stream.width = ancho
    stream.height = alto
    stream.pix_fmt = "yuv420p"
    stream.codec_context.options = {"crf": "19", "preset": "slow"}

    indice = 0
    ultimo = None
    for clip in clips:
        if indice >= objetivo_cuadros:
            print(f"  (objetivo alcanzado; se omite {clip.stem})")
            break
        with av.open(str(clip)) as entrada:
            for frame in entrada.decode(video=0):
                if indice >= objetivo_cuadros:
                    break
                frame.pts = indice
                frame.time_base = Fraction(1, 1) / fps
                for packet in stream.encode(frame):
                    cont.mux(packet)
                ultimo = frame
                indice += 1
        print(f"  + {clip.stem:36s} acumulado {indice / float(fps):7.2f}s")

    # Si los clips quedan algo cortos (por el margen anti- redondeo de cada
    # seccion), se mantiene el ultimo cuadro hasta completar los 5 minutos.
    # El ultimo cuadro es la diapositiva de cierre, de modo que congelarla
    # no resulta visible ni pierde informacion.
    relleno = objetivo_cuadros - indice
    if relleno > 0 and ultimo is not None:
        for _ in range(relleno):
            ultimo.pts = indice
            ultimo.time_base = Fraction(1, 1) / fps
            for packet in stream.encode(ultimo):
                cont.mux(packet)
            indice += 1
        print(f"  + relleno de cierre{' ' * 24} acumulado {indice / float(fps):7.2f}s"
              f"  ({relleno} cuadros sostenidos)")

    for packet in stream.encode():
        cont.mux(packet)
    cont.close()

    dur = indice / float(fps)
    m, s = divmod(dur, 60)
    print(f"\nVideo final: {salida.resolve()}")
    print(f"  duracion : {dur:.3f}s  ->  {int(m)}:{s:05.2f}")
    print(f"  formato  : {ancho}x{alto} @ {float(fps):.0f}fps, H.264")
    print(f"  cuadros  : {indice}")


if __name__ == "__main__":
    main()