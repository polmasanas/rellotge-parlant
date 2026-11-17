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

Fes-ho al **teu ordinador** (no en un servidor temporal). Guarda la clau i les contrasenyes en un lloc segur, per exemple un gestor de contrasenyes: sense la mateixa clau no es poden instal·lar actualitzacions per sobre. No pugis mai `release.jks` al repositori (el `.gitignore` ja ho impedeix).

### 1. Comprova que tens `keytool`

`keytool` ve amb Java. Si tens Android Studio ja el tens; si no, instal·la un JDK gratuït (p. ex. Temurin 17 d'adoptium.net). Comprova-ho en una terminal (Mac/Linux) o a PowerShell (Windows):

```bash
keytool -help
```

A Windows, si PowerShell no el troba però tens Android Studio, fes servir el que porta inclòs:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -help
```

### 2. Crea la clau

Igual a Windows, Mac i Linux (et demanarà una contrasenya i unes dades de nom, que poden ser qualsevol cosa):

```bash
keytool -genkeypair -v -keystore release.jks -alias rellotge -keyalg RSA -keysize 4096 -validity 10000
```

### 3. Converteix la clau a text per a GitHub

Windows (PowerShell, a la mateixa carpeta; el resultat queda copiat al porta-retalls):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\release.jks")) | Set-Clipboard
```

Mac (queda copiat al porta-retalls):

```bash
base64 -i release.jks | pbcopy
```

Linux:

```bash
base64 -w0 release.jks
```

### 4. Afegeix els secrets a GitHub

Al repositori: **Settings → Secrets and variables → Actions → New repository secret**:

| Nom | Valor |
|---|---|
| `KEYSTORE_BASE64` | el text del pas 3 |
| `KEYSTORE_PASSWORD` | la contrasenya de la clau |
| `KEY_ALIAS` | `rellotge` |
| `KEY_PASSWORD` | la mateixa contrasenya (si no n'has posat una de diferent per a la clau) |

### 5. Publica la versió

```bash
git tag v0.1.0
```

```bash
git push origin v0.1.0
```

GitHub compilarà l'APK signat i l'adjuntarà a la pàgina de Releases.
