# Potato FPS (Minecraft 1.21.11, Fabric)

## Сборка без компьютера (через GitHub)
1. Создай репозиторий на github.com и загрузи в него ВСЕ файлы из этого архива (включая папку .github).
2. Открой вкладку Actions → build → дождись зелёной галочки.
3. Внизу страницы запуска в Artifacts скачай potato-fps (внутри jar).

## Сборка на ПК
Нужны Java 21 и Gradle 9.2.1: `gradle build` → jar в build/libs/.

## Использование (нужен Fabric API 0.141.3+1.21.11)
- /potato — пресет «картошка»
- /potato auto — вкл/выкл авто-подстройку дальности прорисовки
- /potato target 90 — целевой FPS

Ставь вместе с Sodium, Lithium, FerriteCore, ImmediatelyFast, EntityCulling, ModernFix.
