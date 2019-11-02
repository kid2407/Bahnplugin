# BahnPlugin

Ein kleines Plugin für https://edelmaenner.net, welches die Bahnverwaltung erleichert

Mit diesem Plugin können die Bahnhofsanbindungen eingetragener Spieler angefragt, verändert und entfernt werden.
Jeder kann nur seinen eigenen Eintrag bearbeiten, nicht die der Anderen.

Das Format für die Kennzeichnung eines Bahnhofs lautet wie folgt:
* Kennzeichnung für eine Himmelsrichtung [N,O,S,W] + eine Zahl von 1 bis 99 (ggf. eine zweites Mal anhängen für Bahnhöfe die nicht an einer Hauptlinie liegen, wie z.B. W5S2)

Beispiele für Bahnhöfe, die an einer Hauptlinie liegen: `S2, O13, W6`

Beispiele für Bahnhöfe, die **nicht** an einer Hauptlinie liegen: `O8S7, W7S78, N47O17`

Es stehen insgesamt vier Kommandos zur Verfügung:
* **/bahn help** - Gibt diese Hilfe aus
* **/bahn get** <Spielername> - Gibt den Bahnhof für diesen Spieler aus. Akzeptiert auch angefangene Namen. z.B "kid" findet auch "kid2407"
* **/bahn delete** <Spielername> - Löscht den Eintrag für diesen Spieler
* **/bahn set** <Spielername> <Bahnhof> - Fügt den Eintrag für diesen Spieler hinzu oder aktualisiert ihn.