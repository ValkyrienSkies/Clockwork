import argparse
import base64
import json
import os
from pathlib import Path
import shutil
import subprocess

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description='Render Clockwork sketch shader variants without launching Minecraft.')
parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
args = parser.parse_args()
os.chdir(root)
out = root / 'build/sketch-preview'
out.mkdir(parents=True, exist_ok=True)
java = str(Path(args.java_home) / 'bin/java') if args.java_home else shutil.which('java')
javac = str(Path(args.java_home) / 'bin/javac') if args.java_home else shutil.which('javac')
if not java or not javac:
    parser.error('A JDK is required. Set JAVA_HOME or pass --java-home.')
cache = root / 'forge/.gradle/loom-cache/forge_minecraft_classpath.txt'
if not cache.exists() or not (root / 'common/build/classes/kotlin/main').exists():
    parser.error('Run :common:compileKotlin and configure the Forge Gradle project once first.')
mc = next(p for p in (root / '.gradle/loom-cache/minecraftMaven').rglob('*minecraft-merged*.jar')
          if p.name.startswith('forge-') and 'sources' not in p.name and 'intermediary' not in p.name)
classpath = [mc, root / 'common/build/classes/kotlin/main', out]
classpath += [Path(p) for p in cache.read_text(encoding='utf-8').splitlines() if p and not any(x in p for x in ('natives-windows-arm64', 'natives-windows-x86'))]
gradle = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle'))
classpath += list((gradle / 'caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.0.21').rglob('*.jar'))
cp = os.pathsep.join(str(p.resolve()).replace('\\', '/') for p in classpath)
compile_args = out / 'javac.args'
compile_args.write_text('-classpath\n"' + cp + '"\n-d\n"' + out.as_posix() + '"\n"scripts/sketch-preview/SketchPreview.java"\n"scripts/sketch-preview/WorldSketchCheck.java"\n')
subprocess.run([javac, '@' + str(compile_args)], check=True)
for fixture in ('mechanism', 'dense', 'connections'):
    run_args = out / 'java.args'
    run_args.write_text('-classpath\n"' + cp + '"\nSketchPreview\n' + fixture + '\n')
    subprocess.run([java, '@' + str(run_args)], check=True)
shader = (root / 'common/src/main/resources/assets/minecraft/shaders/core/clockwork_blueprint_sketch.fsh').read_text(encoding='utf-8')
reveal = (root / 'common/src/main/resources/assets/minecraft/shaders/include/clockwork_blueprint_reveal.glsl').read_text(encoding='utf-8')
shader = shader.replace('#moj_import <clockwork_blueprint_reveal.glsl>', reveal)
shader = shader.replace('#version 150', '#version 300 es\nprecision highp float;')
fixtures = {}
for fixture in ('mechanism', 'dense', 'connections'):
    fixtures[fixture] = dict(image='data:image/png;base64,' + base64.b64encode((out / (fixture + '-geometry.png')).read_bytes()).decode(),
                             ink='data:image/png;base64,' + base64.b64encode((out / (fixture + '-ink.png')).read_bytes()).decode(),
                             tones=json.loads((out / (fixture + '-tones.json')).read_text(encoding='utf-8')))
html = (root / 'scripts/sketch-preview/preview.html').read_text(encoding='utf-8')
html = html.replace('__REVEAL_SHADER__', json.dumps(reveal)).replace('__FRAGMENT_SHADER__', json.dumps(shader)).replace('__FIXTURES__', json.dumps(fixtures))
(out / 'index.html').write_text(html, encoding='utf-8')
print(out / 'index.html')
