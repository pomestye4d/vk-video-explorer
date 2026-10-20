# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

VK video explorer — это комбинация из веб приложения и android приложения (построенного на Capacitor) для просмотра видеоконтента с сервиса VK video.
It's built on top of **elsa**, an in-house Spring Boot + React application platform (`submodules/elsa`) that provides declarative-XML → generated-code scaffolding for domain models, admin UI, and web-app UI components.

## Repository layout

- `webapp/backend` — Spring Boot backend (`ru.vga.vk.video`), Gradle project `main-backend`.
- `webapp/frontend` — React + TypeScript + Vite frontend, Gradle project `main-frontend` (Gradle just wraps npm for CI purposes; day-to-day work uses npm directly).
- `submodules/elsa` — the platform submodule. `submodules/elsa/platform/*` are the reusable elsa modules (`common-core`, `server-core`, `server-admin`, `web-core`, `web-admin`, the `elsa-java`/`elsa-web` Gradle plugin, etc.) included as normal Gradle subprojects; `submodules/elsa/submodules/webpeer` is a nested submodule providing the web component runtime. Treat these as library code — changes here affect the whole platform, not just vk-video-exlorer.
- `local-dev` — the human developer's local environment config: `docker-compose.yml` Postgres and `application.yml` (Spring config).
- `claude-dev` — the equivalent environment for Claude Code / agent sessions in this workspace, kept separate so it doesn't collide with the developer's own `local-dev` stack (own Postgres data dir). **Always use `claude-dev`, not `local-dev`, when running the backend or its infra in this environment.** 

## Common commands

All Gradle commands are run from the repo root (single multi-project build defined in `settings.gradle.kts`).

**Never run `docker` / `docker compose` yourself, and never suggest doing so.** Docker isn't available on this machine — this is permanent, not a temporary limitation. The `claude-dev` infra stack (Postgres — see `claude-dev/docker-compose.yml`) always runs on a separate machine that the user starts manually; the backend reaches it over the network via the hosts (`192.168.1.47` etc.) baked into `claude-dev/application.yml`. Assume it's already running and reachable; if the backend can't connect, ask the user to (re)start it on that other machine instead of trying to run docker commands here.

Backend — Spring resolves `./application.yml` and `./logback-spring.xml` relative to the JVM process's **working directory**, not via `--spring.config.location`; this isn't wired into `bootRun` in `build.gradle.kts` (the IDE run configuration "VK Video exlorer claude" achieves it by setting `WORKING_DIRECTORY=claude-dev` directly on the `ru.vga.vk.video.boot.VkVideoApplication` run). To get the same behavior from the CLI, build the jar and launch it with `claude-dev` as the working directory:
```bash
./gradlew :main-backend:bootJar
(cd claude-dev && java -jar ../webapp/backend/build/libs/main-backend*.jar)   # listens on :9091, picks up claude-dev/application.yml
```
Plain `./gradlew :main-backend:bootRun` also runs the app, but its working directory is `webapp/backend`, so it will NOT pick up `claude-dev/application.yml` — don't use it to actually serve requests in this environment.

```bash
./gradlew :main-backend:test             # run all backend tests
./gradlew :main-backend:test --tests "ru.vga.vk.video.test.video.VideoTest"   # single test class
```

Code generation (regenerate `java-gen` / `src-gen` sources from the XML files under `webapp/backend/src/main/codegen`):
```bash
./gradlew eCodeGen                       # root task, regenerates codegen for common-core, server-admin, server-core, web-admin
./gradlew :main-backend:eCodeGen         # regenerate just this project's domain/admin-ui/webApp/remoting/l10n code
```
Run this after editing any file in `webapp/backend/src/main/codegen/*.xml`.

Frontend (`webapp/frontend`):
```bash
npm --prefix webapp/frontend start       # vite dev server, proxies /api, /_ui, /_resources, /websocket to backend on :9091
npm --prefix webapp/frontend run build   # tsc + vite build
npm --prefix webapp/frontend run lint    # eslint --fix, zero warnings allowed
npm --prefix webapp/frontend run preview
```
The dev server expects the backend reachable at `localhost:9091`. ** Test frontend on http://localhost:5174 **.

The repo is checked out on a network share (`Z:\` / `\\VBoxSvr\...`) in this environment, and plain `npm start` crashes immediately with `Error: EISDIR ... watch 'vite.config.ts'` because Node's native `fs.watch` doesn't work on this filesystem. Work around it with the polling-based watcher: `CHOKIDAR_USEPOLLING=true npm --prefix webapp/frontend start`.

## Architecture: the elsa codegen pipeline

This is the part that isn't obvious from browsing one file at a time. `webapp/backend/build.gradle.kts` wires the `elsa` Gradle plugin's `codegen` block to a set of XML files in `webapp/backend/src/main/codegen/`:

1. **`domain-*.xml`** — declares domain assets (entities/enums) with typed properties, collections, and i18n `displayName`s. Generates Java domain classes into `src/main/java-gen/ru/vga/vk/video/domain`.
2. **`l10n.xml`** — localization strings/config, generates into `.../java-gen/.../` via `L10nConfigurator`/`L10nFactory`.
3. **`admin-ui-*.xml`** — declarative admin screens for each domain, generates into `.../java-gen/.../ui`.
4. **`web-app.xml`** — declares the **web-app UI components** (widgets) exposed to the frontend, e.g. `ProjectPlanner`, `TasksTable`, `JTasksDocumentsList`, `LinePlot`, `CodeEditor`, `MdEditor`, `PdfViewer`, `AclGroupEditor`. For each one it generates **both**:
   - a Java `*Skeleton` base class in `webapp/backend/src/main/java-gen/ru/vga/vk/video/webApp/`, plus a one-time Java stub in `webapp/backend/src/main/java/ru/vga/vk/video/webApp/<Name>.java` that `extends <Name>Skeleton` (this stub file is where you add server-side behavior; it's only scaffolded once, not regenerated over your edits),
   - a matching TypeScript `*Skeleton.tsx`/`.ts` in `webapp/frontend/src-gen/webApp/`, which the hand-written implementation in `webapp/frontend/src/webApp/<Name>.tsx` imports and extends/wraps with the actual React implementation.
   All these components are auto-registered by the generated `webapp/frontend/src-gen/registry/RegistryConfigurator.ts` (maps a Java class FQN like `ru.vga.vk.video.webApp.ProjectPlanner` to a factory that constructs the frontend component).
5. **`remoting.xml`** — RPC/remoting contract between backend and frontend, generates into `.../java-gen/.../remoting`.

**Rule of thumb:** never hand-edit anything under `java-gen/` or `src-gen/` — it's overwritten by `eCodeGen`. Business logic goes in the paired hand-written files (`src/main/java/.../webApp/*.java`, `src/main/java/.../domain/**` extension points, `webapp/frontend/src/webApp/*.tsx`, `webapp/frontend/src/webApp/<component>/**`). To add or change a domain field, admin screen, or web-app component, edit the relevant `codegen/*.xml` and rerun `eCodeGen`.

Frontend components that have significant hand-written internals live in their own subfolder next to the wrapper, e.g. `ProjectPlanner.tsx` (thin wrapper implementing the generated `ProjectPlannerComponent`/`ProjectPlannerSkeleton` contract) delegates to `webapp/frontend/src/webApp/projectPlanner/` (Gantt chart, task list panel, drag/drop, PDF export, date/tree utilities, etc.).

## Backend domain modules


`webapp/backend/src/main/java/ru/vga/vk/video` groups by feature: `config`, `boot`, `common`.

## Frontend path aliases

Vite (`webapp/frontend/vite.config.ts`) defines aliases used throughout the frontend code:
- `vk` → `webapp/frontend` root (so imports like `vk/src/common/component` work from anywhere)
- `core` → `submodules/elsa/platform/web-core`

## Auth and multi-tenancy

VK video explorer uses authorization throw VK ID and does not have own authorization engine 

## Browser testing in this environment

**Use the Claude in Chrome extension (`mcp__claude-in-chrome__*`), not the in-app Browser pane (`mcp__Claude_Browser__*`).** The Browser pane pops a permission dialog on every single action for this site with no way to allowlist it, and its `computer` screenshot action fails outright here ("the Browser pane is not displayed, so the page is not compositing frames"). Claude in Chrome drives the real Chrome running on this same machine (confirmed via the local named pipe in `C:\Users\user\AppData\Local\Claude\Logs\chrome-native-host.log`) and has neither problem — no per-action prompts, and it's real Chrome so it actually renders.

If `list_connected_browsers` / `tabs_context_mcp` shows nothing connected, the extension needs to be installed and signed in: https://chromewebstore.google.com/detail/fcoeoabgfenejglbffodgkkbkcdhcgfn (ask the user to do this; it's their browser/account).

**`computer` action `screenshot` with `save_to_disk: true` is broken** — it reports success but writes no file anywhere findable on disk (checked `Downloads`, `Pictures`, Chrome's configured download dir, extension folders, and a full `C:\` scan for recently-written files). The image also isn't shown to the user automatically as inline tool output in this chat surface, so it has to be delivered explicitly. Workaround: capture the real screen via PowerShell/.NET after bringing the Chrome window to the foreground, save to a file, `SendUserFile` it, then restore focus to the Claude Code window (process `claude`, window title `Claude`) so the user isn't left staring at Chrome:
```powershell
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WinFoc {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);
}
"@
$chrome = Get-Process chrome | Where-Object { $_.MainWindowTitle -like '*Google Chrome' } | Select-Object -First 1
[WinFoc]::ShowWindowAsync($chrome.MainWindowHandle, 3) | Out-Null   # SW_MAXIMIZE
Start-Sleep -Milliseconds 400
[WinFoc]::SetForegroundWindow($chrome.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 600

Add-Type -AssemblyName System.Windows.Forms,System.Drawing
$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
$bitmap.Save("<scratchpad>\screenshot.png", [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose(); $bitmap.Dispose()

$claudeWin = Get-Process claude | Where-Object { $_.MainWindowTitle -eq 'Claude' } | Select-Object -First 1
[WinFoc]::SetForegroundWindow($claudeWin.MainWindowHandle) | Out-Null
```
Then call `SendUserFile` on the saved PNG (`display: "render"`). Maximizing Chrome before the grab keeps the shot clean (nothing else visible behind it) without needing to crop to a window rect.
