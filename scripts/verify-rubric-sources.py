"""Contrasta los ocho catálogos con las transcripciones de las pautas del cliente.

Uso desde la raíz del repositorio: python scripts/verify-rubric-sources.py
Los documentos fuente permanecen en ../04_Documentacion_Cliente.
"""
import html
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT.parent / "04_Documentacion_Cliente"


def clean(value):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>|[*_]", "", html.unescape(value))).strip()


def read_indicators(source):
    indicators, dimension = [], None
    for line in source.read_text(encoding="utf-8-sig").splitlines():
        if not line.startswith("|"):
            continue
        match = re.search(r"DIMENSI[ÓO]N\s+([ABCDE])", line)
        if match:
            dimension = match.group(1)
        if dimension is None:
            continue
        cells = [clean(c) for c in line.split("|")[1:-1]]
        for index, cell in enumerate(cells[:-1]):
            if cell.isdigit() and 1 <= int(cell) <= 10 and len(cells[index + 1]) > 25:
                indicators.append((dimension + cell, cells[index + 1]))
                break
    return indicators


if __name__ == "__main__":
    count = 0
    for role in ("collaborator", "tutor"):
        definitions = json.loads((ROOT / f"src/main/resources/rubrics/{role}.json").read_text(encoding="utf-8"))
        for rubric in definitions:
            sources = list(SOURCES.rglob(rubric["source"]))
            assert len(sources) == 1, f"Fuente no encontrada o ambigua: {rubric['source']}"
            expected = read_indicators(sources[0])
            assert len(expected) == rubric["indicatorCount"] == len(rubric["indicators"]), rubric["type"]
            assert rubric["maxPoints"] == len(expected) * 7
            for item, (code, text) in zip(rubric["indicators"], expected):
                assert item["criterionCode"] == code, (rubric["type"], code)
                assert clean(item["text"]) == text, (rubric["type"], code, "Texto distinto a la fuente")
            count += 1
    assert count == 8
    print("OK: ocho pautas, codigos, orden, textos y puntajes maximos coinciden con las fuentes.")
