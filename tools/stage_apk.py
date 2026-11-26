#!/usr/bin/env python3
"""把编译好的 APK 摆到 dl-xap/ 并同步 index.html 上的版本/体积/校验和。

用法:
    python3 tools/stage_apk.py gkd-app/build/outputs/apk/gkd/perf/gkd-app-gkd-perf.apk

之后由 agent 调用部署工具把 dl-xap/ 重新发布（站点链接保持不变）。
"""
import hashlib
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent   # GKD-XA 仓库
DL = ROOT.parent / "dl-xap"                     # 下载页与产物目录（workspace 根）
INDEX = DL / "index.html"
SDK = Path("C:/android/sdk")
AAPT2 = SDK / "build-tools/37.0.0/aapt2.exe"


def badging(apk: Path) -> tuple[str, str, str]:
    out = subprocess.check_output([str(AAPT2), "dump", "badging", str(apk)],
                                  text=True, encoding="utf-8", errors="replace")
    pkg = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", out)
    if not pkg:
        raise SystemExit("无法从 badging 解析包名，确认 aapt2 路径是否正确")
    return pkg.group(1), pkg.group(2), pkg.group(3)


def main() -> int:
    if len(sys.argv) < 2:
        return int(print(__doc__) or 1)
    apk = Path(sys.argv[1]).resolve()
    if not apk.is_file():
        raise SystemExit(f"APK 不存在: {apk}")

    package, code, name = badging(apk)
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    size_mb = f"{apk.stat().st_size / 1024 / 1024:.2f} MB"

    target = DL / f"GKD-XAP-{name.split('-')[0]}.apk"
    DL.mkdir(parents=True, exist_ok=True)
    if apk != target:
        shutil.copyfile(apk, target)

    html = INDEX.read_text(encoding="utf-8")
    html = re.sub(r"(<dt>版本</dt><dd>)[^<]*(</dd>)", rf"\g<1>{name}\g<2>", html)
    html = re.sub(r"(<dt>体积</dt><dd>)[^<]*(</dd>)", rf"\g<1>{size_mb}\g<2>", html)
    html = re.sub(r"(<dt>包名</dt><dd[^>]*>)[^<]*(</dd>)", rf"\g<1>{package}\g<2>", html)
    html = re.sub(r"SHA-256 · [0-9a-f]{64}", f"SHA-256 · {digest}", html)
    html = re.sub(r'href="[^"]*\.apk" download="[^"]*\.apk"',
                  f'href="{target.name}" download="{target.name}"', html)
    INDEX.write_text(html, encoding="utf-8")

    print(f"package : {package}")
    print(f"version : {name} ({code})")
    print(f"size    : {apk.stat().st_size} B ({size_mb})")
    print(f"sha256  : {digest}")
    print(f"staged  : {target}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
