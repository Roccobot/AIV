#!/usr/bin/env python3
"""Il documento di feedback prima di pubblicarlo: lo apre davvero, e lo prova come lo usa l'utente.

⚠️⚠️ **MISURA IL FILE CHE SI STA PER PUBBLICARE, e non lo script che lo compone**: il file HTML è
la cosa che arriva all'utente, quindi il presidio vale comunque sia stato costruito il documento.
Nasce dal difetto del giro della `2.67`, quando un campo mancante nei dati spegneva la pagina
intera e nessuna rilettura del codice lo poteva vedere.

Le prove vivono in `feedback-interactive-check.py`; questo è il nome che la guida e il workflow del
DF lanciano. Dalla `6.30` (rilievo H29 della revisione Setaccio) è soltanto il passaggio: la metà
che controllava il formato di prima del DF (`var SEZIONI` e simili) non serviva più a nessun
documento di AIV, e dal 2026-10-17 (`Gemella`) è uscita anche dalla copia di Aomidori.

Uso:
    python3 tools/feedback-check.py <documento.html>
"""
import importlib.util
from pathlib import Path
import sys


def main():
    if len(sys.argv) != 2:
        sys.exit('uso: python3 tools/feedback-check.py <documento.html>')
    percorso = sys.argv[1]
    if 'data-feedback="agent"' not in Path(percorso).read_text(encoding='utf-8'):
        sys.exit('%s non è un documento di feedback di AIV (manca data-feedback="agent")' % percorso)
    spec = importlib.util.spec_from_file_location(
        'agent_feedback_check', Path(__file__).with_name('feedback-interactive-check.py'))
    prove = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prove)
    prove.check(percorso)


if __name__ == '__main__':
    main()
