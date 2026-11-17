# Rellotge Parlant

Aplicació Android que diu l'hora en veu alta en català tradicional («són dos quarts i cinc de cinc de la tarda») cada vegada que s'encén la pantalla del telèfon bloquejat. Pensada per a una persona amb poca vista que no pot llegir els rellotges.

- Disseny: [docs/superpowers/specs/2026-11-17-rellotge-parlant-design.md](docs/superpowers/specs/2026-11-17-rellotge-parlant-design.md)
- Pla d'implementació: [docs/superpowers/plans/2026-11-17-rellotge-parlant.md](docs/superpowers/plans/2026-11-17-rellotge-parlant.md)
- Jira: projecte **RP** a pmsns.atlassian.net

## Instal·lació al telèfon

1. Al telèfon, obre la pàgina de [Releases](../../releases) i baixa l'últim `rellotge-parlant-vX.Y.Z.apk`.
2. Obre el fitxer i permet «instal·lar apps de fonts desconegudes» quan t'ho demani.
3. Obre **Rellotge Parlant** i accepta el permís de notificacions.
4. Repassa la **llista de comprovació** fins que tot estigui ✅ (o fet, per als ℹ️):
   - Optimització de bateria desactivada per a l'app.
   - Veu en català instal·lada (Ajustos → Text a veu → motor de Google → Instal·la dades de veu → Català). En un Samsung, canvia el motor per defecte al de Google.
   - Bloqueig de pantalla en «Lliscar» (sense PIN ni patró, i no «Cap»).
   - Desactiva que les notificacions encenguin la pantalla.
   - A Xiaomi/Huawei/Oppo/Vivo/Samsung: permet l'inici automàtic.
5. Prem **Prova**. Després bloqueja el telèfon, prem el botó lateral i escolta.

## Desenvolupament

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

La taula completa de frases (1.440 minuts) es genera a `app/build/phrase-table.txt` en executar els tests.

## Publicar una versió signada (una sola vegada: crear la clau)

Guarda la clau i les contrasenyes en un lloc segur: sense la mateixa clau no es poden instal·lar actualitzacions per sobre.

```bash
keytool -genkeypair -v -keystore release.jks -alias rellotge -keyalg RSA -keysize 4096 -validity 10000
```

Afegeix aquests secrets al repositori (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64` (sortida de `base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`rellotge`), `KEY_PASSWORD`. Després:

```bash
git tag v0.1.0 && git push origin v0.1.0
```
