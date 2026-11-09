"""Bundle the pinned Apache-2.0 German Vosk model. No runtime model downloads."""
import hashlib
import os
from pathlib import Path
import shutil
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MODEL = "vosk-model-small-de-0.15"
SHA256 = "b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43"
DEST = ROOT / "app/src/main/assets/model-de"


def prepare():
    if (DEST / "uuid").is_file() and (DEST / "uuid").read_text().strip() == SHA256:
        if all((DEST / path).is_file() for path in ("am/final.mdl", "graph/HCLr.fst", "graph/Gr.fst", "conf/model.conf")):
            return
    cache = ROOT / "build/model"
    cache.mkdir(parents=True, exist_ok=True)
    archive = Path(os.environ.get("VOSK_MODEL_ARCHIVE", str(cache / (MODEL + ".zip"))))
    if not archive.exists():
        print("Downloading the offline German speech model …", flush=True)
        partial = archive.with_suffix(".part")
        urllib.request.urlretrieve("https://alphacephei.com/vosk/models/" + MODEL + ".zip", partial)
        partial.replace(archive)
    if hashlib.sha256(archive.read_bytes()).hexdigest() != SHA256:
        raise SystemExit("German speech model checksum mismatch; refusing to package it")
    stage = cache / "extract"
    shutil.rmtree(stage, ignore_errors=True)
    stage.mkdir()
    with zipfile.ZipFile(archive) as zipped:
        for item in zipped.infolist():
            target = (stage / item.filename).resolve()
            if not target.is_relative_to(stage.resolve()):
                raise SystemExit("Unsafe model archive path")
        zipped.extractall(stage)
    source = stage / MODEL
    (source / "uuid").write_text(SHA256 + "\n")
    shutil.rmtree(DEST, ignore_errors=True)
    shutil.move(source, DEST)
    print("Verified offline German model is ready.")


if __name__ == "__main__":
    prepare()
